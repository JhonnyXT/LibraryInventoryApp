package com.example.libraryinventoryapp.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.libraryinventoryapp.R
import com.example.libraryinventoryapp.UserActivity
import com.example.libraryinventoryapp.models.Book
import com.example.libraryinventoryapp.models.WishlistItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * 🌉 WishlistServiceBridge - Puente entre UI Android y NotificationService KMP
 * 
 * PROPÓSITO:
 * ✅ Mantener interfaz exacta del WishlistAvailabilityService Android original
 * ✅ Usar internamente NotificationService KMP cuando esté disponible
 * ✅ Preservar 100% compatibilidad con código existente
 * ✅ Monitoreo híbrido Firebase + notificaciones KMP
 */
class WishlistServiceBridge private constructor(private val context: Context) {

    companion object {
        private const val TAG = "WishlistServiceBridge"
        
        // 🔔 Canal de notificaciones
        private const val CHANNEL_ID_WISHLIST = "wishlist_availability"
        private const val CHANNEL_NAME = "Libros Deseados Disponibles"
        private const val CHANNEL_DESCRIPTION = "Notificaciones cuando libros de tu lista de deseos estén disponibles"
        
        // 🎯 Request codes para notificaciones únicas
        private const val NOTIFICATION_ID_BASE = 2000
        
        @Volatile
        private var INSTANCE: WishlistServiceBridge? = null
        
        fun getInstance(context: Context): WishlistServiceBridge {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WishlistServiceBridge(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // 🔧 NotificationService KMP (pendiente de implementación completa)
    private val notificationServiceKmp = null // Temporalmente null hasta Phase 5

    // 🔥 Firebase directo (para monitoreo)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // 📡 Listeners activos
    private var wishlistListener: ListenerRegistration? = null
    private val bookListeners = mutableMapOf<String, ListenerRegistration>()

    /**
     * 🚀 Iniciar monitoreo - COMPATIBLE con interfaz original
     */
    fun startMonitoring() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Log.w(TAG, "⚠️ Usuario no autenticado - no se puede iniciar monitoreo")
            return
        }

        Log.i(TAG, "🌉 Bridge: Iniciando monitoreo de lista de deseos")

        // Crear canal de notificaciones
        createNotificationChannel()

        // Limpiar listeners anteriores
        stopMonitoring()

        // Inicializar NotificationService KMP si está disponible
        initializeKmpNotificationService()

        // Escuchar cambios en la lista de deseos del usuario
        startWishlistListener(currentUserId)
    }

    /**
     * 🛑 Detener monitoreo - COMPATIBLE con interfaz original
     */
    fun stopMonitoring() {
        Log.i(TAG, "🌉 Bridge: Deteniendo monitoreo de lista de deseos")
        
        wishlistListener?.remove()
        wishlistListener = null
        
        bookListeners.values.forEach { it.remove() }
        bookListeners.clear()
    }

    /**
     * 🔧 Inicializar NotificationService KMP
     */
    private fun initializeKmpNotificationService() {
        notificationServiceKmp?.let { service ->
            try {
                Log.d(TAG, "🚀 Inicializando NotificationService KMP")
                // La inicialización se haría de forma asíncrona
                // Por ahora solo logeamos que está disponible
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error inicializando NotificationService KMP: ${e.message}")
            }
        }
    }

    /**
     * 📡 Listener de lista de deseos del usuario
     */
    private fun startWishlistListener(userId: String) {
        wishlistListener = firestore.collection("wishlist")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error en listener de wishlist: ${error.message}")
                    return@addSnapshotListener
                }

                snapshot?.documents?.forEach { doc ->
                    try {
                        val wishlistItem = doc.toObject(WishlistItem::class.java)
                        wishlistItem?.let { item ->
                            if (!item.isAvailable) {
                                // Solo monitorear libros no disponibles
                                startMonitoringBook(item)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error procesando item de wishlist: ${e.message}")
                    }
                }
            }
    }

    /**
     * 📚 Monitorear libro específico
     */
    private fun startMonitoringBook(wishlistItem: WishlistItem) {
        Log.d(TAG, "🌉 Bridge: Monitoreando libro: ${wishlistItem.bookTitle}")
        
        // Evitar listeners duplicados
        bookListeners[wishlistItem.bookId]?.remove()
        
        val listener = firestore.collection("books")
            .document(wishlistItem.bookId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error monitoreando libro ${wishlistItem.bookTitle}: ${error.message}")
                    return@addSnapshotListener
                }

                snapshot?.let { doc ->
                    try {
                        val book = doc.toObject(Book::class.java)
                        book?.let { bookData ->
                            checkBookAvailability(bookData, wishlistItem)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error procesando cambios del libro: ${e.message}")
                    }
                }
            }
        
        bookListeners[wishlistItem.bookId] = listener
    }

    /**
     * 📊 Verificar disponibilidad del libro
     */
    private fun checkBookAvailability(book: Book, wishlistItem: WishlistItem) {
        val assignedCount = book.assignedTo?.size ?: 0
        val availableCount = book.quantity - assignedCount
        
        Log.d(TAG, "📊 ${book.title}: ${book.quantity} total, $assignedCount asignados, $availableCount disponibles")
        
        if (availableCount > 0 && !wishlistItem.isAvailable) {
            // ¡Libro disponible!
            Log.i(TAG, "🎉 ¡Libro disponible en wishlist!: ${book.title}")
            
            // Enviar notificación
            sendBookAvailableNotification(book, availableCount, wishlistItem)
            
            // Actualizar estado en wishlist
            updateWishlistItemAvailability(wishlistItem.id, true)
        } else if (availableCount <= 0 && wishlistItem.isAvailable) {
            // Libro ya no disponible
            Log.d(TAG, "📉 Libro ya no disponible: ${book.title}")
            updateWishlistItemAvailability(wishlistItem.id, false)
        }
    }

    /**
     * 🔔 Enviar notificación de libro disponible (híbrido KMP + Android)
     */
    private fun sendBookAvailableNotification(book: Book, availableCount: Int, wishlistItem: WishlistItem) {
        try {
            // 🚀 Intentar con NotificationService KMP primero
            notificationServiceKmp?.let { service ->
                Log.d(TAG, "🔔 Usando NotificationService KMP para notificación")
                // La notificación KMP se enviaría de forma asíncrona
                // Por ahora usamos fallback a Android directo
            }
            
            // 📱 Fallback a notificación Android directa
            sendAndroidNotification(book, availableCount, wishlistItem)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error enviando notificación: ${e.message}")
        }
    }

    /**
     * 📱 Notificación Android directa (fallback)
     */
    private fun sendAndroidNotification(book: Book, availableCount: Int, wishlistItem: WishlistItem) {
        try {
            val title = "📚 ¡Libro Disponible!"
            val message = "\"${book.title}\" ya está disponible ($availableCount ejemplares)"
            
            // Intent para abrir la app
            val intent = Intent(context, UserActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_wishlist", true)
                putExtra("book_id", book.id)
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID_BASE + book.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Crear notificación
            val notification = NotificationCompat.Builder(context, CHANNEL_ID_WISHLIST)
                .setSmallIcon(R.drawable.ic_book_library)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setColor(context.getColor(R.color.colorPrimary))
                .build()
            
            // Enviar notificación
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID_BASE + book.id.hashCode(), notification)
            
            Log.i(TAG, "✅ Notificación Android enviada: ${book.title}")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error enviando notificación Android: ${e.message}")
        }
    }

    /**
     * 📝 Actualizar estado de disponibilidad en wishlist
     */
    private fun updateWishlistItemAvailability(wishlistItemId: String, isAvailable: Boolean) {
        firestore.collection("wishlist")
            .document(wishlistItemId)
            .update("isAvailable", isAvailable)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Estado de wishlist actualizado: $wishlistItemId -> $isAvailable")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error actualizando wishlist: ${e.message}")
            }
    }

    /**
     * 🔔 Crear canal de notificaciones
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_WISHLIST,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            
            Log.d(TAG, "✅ Canal de notificaciones creado: $CHANNEL_ID_WISHLIST")
        }
    }

    /**
     * 🗑️ Remover libro de wishlist al ser asignado - COMPATIBLE con interfaz original
     */
    fun removeFromWishlistOnAssignment(bookId: String, userId: String) {
        try {
            Log.i(TAG, "🌉 Bridge: Removiendo libro de wishlist por asignación")
            
            firestore.collection("wishlist")
                .whereEqualTo("bookId", bookId)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener { documents ->
                    documents.forEach { doc ->
                        doc.reference.delete()
                            .addOnSuccessListener {
                                Log.i(TAG, "✅ Libro removido de wishlist por asignación: $bookId")
                                
                                // Enviar notificación de asignación con KMP si está disponible
                                sendBookAssignedNotification(doc.toObject(WishlistItem::class.java))
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "❌ Error removiendo de wishlist: ${e.message}")
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Error buscando en wishlist: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en removeFromWishlistOnAssignment: ${e.message}")
        }
    }

    /**
     * 🎉 Notificación de libro asignado (removido de wishlist)
     */
    private fun sendBookAssignedNotification(wishlistItem: WishlistItem) {
        try {
            // 🚀 Intentar con NotificationService KMP primero
            notificationServiceKmp?.let { service ->
                Log.d(TAG, "🎉 Usando NotificationService KMP para notificación de asignación")
                // Se manejaría de forma asíncrona
            }
            
            // 📱 Fallback a notificación Android directa
            val title = "🎉 ¡Libro Asignado!"
            val message = "\"${wishlistItem.bookTitle}\" ha sido asignado. Ya no está en tu lista de deseos."
            
            val intent = Intent(context, UserActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_assigned_books", true)
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID_BASE + wishlistItem.bookId.hashCode() + 1000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID_WISHLIST)
                .setSmallIcon(R.drawable.ic_book_library)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setColor(context.getColor(R.color.success_green))
                .build()
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID_BASE + wishlistItem.bookId.hashCode() + 1000, notification)
            
            Log.i(TAG, "✅ Notificación de asignación enviada: ${wishlistItem.bookTitle}")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error enviando notificación de asignación: ${e.message}")
        }
    }
}
