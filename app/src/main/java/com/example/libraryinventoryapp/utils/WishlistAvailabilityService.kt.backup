package com.example.libraryinventoryapp.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.libraryinventoryapp.R
import com.example.libraryinventoryapp.UserActivity
import com.example.libraryinventoryapp.models.Book
import com.example.libraryinventoryapp.models.WishlistItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * 🌟 WishlistAvailabilityService - Monitor inteligente de disponibilidad
 * 
 * FUNCIONALIDADES:
 * ✅ Monitora en tiempo real cuando libros de la lista de deseos estén disponibles
 * ✅ Envía notificaciones push automáticas al usuario
 * ✅ Remueve automáticamente libros de lista de deseos al ser asignados
 * ✅ Sistema eficiente con listeners de Firebase
 */
class WishlistAvailabilityService private constructor(private val context: Context) {

    companion object {
        private const val TAG = "WishlistAvailabilityService"
        
        // 🔔 Canal de notificaciones
        private const val CHANNEL_ID_WISHLIST = "wishlist_availability"
        private const val CHANNEL_NAME = "Libros Deseados Disponibles"
        private const val CHANNEL_DESCRIPTION = "Notificaciones cuando libros de tu lista de deseos estén disponibles"
        
        // 🎯 Request codes para notificaciones únicas
        private const val NOTIFICATION_ID_BASE = 2000
        
        @Volatile
        private var INSTANCE: WishlistAvailabilityService? = null
        
        fun getInstance(context: Context): WishlistAvailabilityService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WishlistAvailabilityService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // Firebase
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // Listeners activos
    private val activeListeners = mutableMapOf<String, ListenerRegistration>()
    private var wishlistListener: ListenerRegistration? = null
    
    // Estado interno
    private val monitoredBooks = mutableMapOf<String, WishlistItem>() // bookId -> WishlistItem
    private val isInitialized = false

    init {
        createNotificationChannel()
    }

    /**
     * 🚀 Inicializar servicio de monitoreo
     */
    fun startMonitoring() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Log.w(TAG, "⚠️ Usuario no autenticado - no se puede iniciar monitoreo")
            return
        }

        Log.i(TAG, "🚀 Iniciando monitoreo de lista de deseos para usuario: $currentUserId")
        
        // Listener para cambios en la lista de deseos del usuario
        wishlistListener = firestore.collection("wishlist")
            .whereEqualTo("userId", currentUserId)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error escuchando wishlist: ${error.message}")
                    return@addSnapshotListener
                }
                
                snapshots?.let { documents ->
                    handleWishlistChanges(documents.documents.map { document ->
                        val wishlistItem = document.toObject(WishlistItem::class.java)!!
                        wishlistItem.id = document.id
                        wishlistItem
                    })
                }
            }
    }

    /**
     * 🔄 Manejar cambios en la lista de deseos
     */
    private fun handleWishlistChanges(currentWishlistItems: List<WishlistItem>) {
        Log.d(TAG, "🔄 Procesando cambios en lista de deseos: ${currentWishlistItems.size} items")
        
        // Actualizar libros monitoreados
        val currentBookIds = currentWishlistItems.map { it.bookId }.toSet()
        val previousBookIds = monitoredBooks.keys.toSet()
        
        // Libros añadidos a la lista de deseos
        val addedBookIds = currentBookIds - previousBookIds
        // Libros removidos de la lista de deseos  
        val removedBookIds = previousBookIds - currentBookIds
        
        // Remover listeners de libros que ya no están en la lista
        removedBookIds.forEach { bookId ->
            activeListeners[bookId]?.remove()
            activeListeners.remove(bookId)
            monitoredBooks.remove(bookId)
            Log.d(TAG, "🗑️ Removido monitoreo de libro: $bookId")
        }
        
        // Añadir listeners para nuevos libros
        addedBookIds.forEach { bookId ->
            val wishlistItem = currentWishlistItems.find { it.bookId == bookId }
            if (wishlistItem != null) {
                startMonitoringBook(wishlistItem)
            }
        }
        
        // Actualizar items existentes
        currentWishlistItems.forEach { wishlistItem ->
            monitoredBooks[wishlistItem.bookId] = wishlistItem
        }
        
        Log.i(TAG, "📊 Estado actual: ${monitoredBooks.size} libros monitoreados")
    }

    /**
     * 📚 Iniciar monitoreo de un libro específico
     */
    private fun startMonitoringBook(wishlistItem: WishlistItem) {
        Log.d(TAG, "📚 Iniciando monitoreo: ${wishlistItem.bookTitle}")
        
        val listener = firestore.collection("books")
            .document(wishlistItem.bookId)
            .addSnapshotListener { documentSnapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error monitoreando libro ${wishlistItem.bookTitle}: ${error.message}")
                    return@addSnapshotListener
                }
                
                documentSnapshot?.let { document ->
                    if (document.exists()) {
                        val book = document.toObject(Book::class.java)
                        if (book != null) {
                            book.id = document.id
                            checkBookAvailability(book, wishlistItem)
                        }
                    }
                }
            }
        
        activeListeners[wishlistItem.bookId] = listener
        monitoredBooks[wishlistItem.bookId] = wishlistItem
    }

    /**
     * ✅ Verificar disponibilidad del libro
     */
    private fun checkBookAvailability(book: Book, wishlistItem: WishlistItem) {
        val availableCount = calculateAvailableBooks(book)
        val wasUnavailable = !wishlistItem.isAvailable
        val isNowAvailable = availableCount > 0
        
        Log.d(TAG, "📊 Verificando '${book.title}': Disponibles=$availableCount, Antes=$wasUnavailable, Ahora=$isNowAvailable")
        
        // Si el libro pasó de no disponible a disponible
        if (wasUnavailable && isNowAvailable) {
            Log.i(TAG, "🎉 ¡Libro ahora disponible! ${book.title}")
            
            // Actualizar estado en wishlist
            wishlistItem.isAvailable = true
            updateWishlistItemAvailability(wishlistItem.id, true)
            
            // Enviar notificación
            sendAvailabilityNotification(book, availableCount)
        } else if (!wasUnavailable && !isNowAvailable) {
            // Si el libro pasó de disponible a no disponible
            wishlistItem.isAvailable = false
            updateWishlistItemAvailability(wishlistItem.id, false)
        }
    }

    /**
     * 📊 Calcular libros disponibles
     */
    private fun calculateAvailableBooks(book: Book): Int {
        val totalBooks = book.quantity
        val assignedBooks = book.assignedTo?.size ?: 0
        return maxOf(0, totalBooks - assignedBooks)
    }

    /**
     * 📝 Actualizar disponibilidad en Firestore
     */
    private fun updateWishlistItemAvailability(wishlistItemId: String, isAvailable: Boolean) {
        firestore.collection("wishlist").document(wishlistItemId)
            .update("isAvailable", isAvailable)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Actualizada disponibilidad: $wishlistItemId -> $isAvailable")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error actualizando disponibilidad: ${e.message}")
            }
    }

    /**
     * 🔔 Enviar notificación de disponibilidad
     */
    private fun sendAvailabilityNotification(book: Book, availableCount: Int) {
        val notificationId = (book.title.hashCode() + NOTIFICATION_ID_BASE).let { 
            if (it < 0) -it else it 
        }
        
        // Intent para abrir la app en la pantalla de usuario
        val intent = Intent(context, UserActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_wishlist", true) // Para navegar directamente a wishlist
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationText = if (availableCount == 1) {
            "¡Ya está disponible! Apúrate a solicitarlo"
        } else {
            "¡Ya hay $availableCount disponibles! No te quedes sin el tuyo"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_WISHLIST)
            .setSmallIcon(R.drawable.ic_book_library) // Usar icono de libro existente
            .setContentTitle("📚 ${book.title}")
            .setContentText(notificationText)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("¡Buenas noticias! 📖\n\nEl libro \"${book.title}\" de ${book.author} que tienes en tu lista de deseos ahora está disponible.\n\n$notificationText"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(context.getColor(R.color.primary_color)) // Color de la app
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            Log.i(TAG, "🔔 Notificación enviada: ${book.title} (ID: $notificationId)")
        } catch (e: SecurityException) {
            Log.w(TAG, "⚠️ Permisos de notificación no otorgados: ${e.message}")
        }
    }

    /**
     * 🗑️ Remover libro de lista de deseos (cuando se asigna)
     */
    fun removeFromWishlistOnAssignment(bookId: String, userId: String) {
        Log.d(TAG, "🔍 Verificando si remover libro $bookId de wishlist para usuario $userId")
        
        firestore.collection("wishlist")
            .whereEqualTo("userId", userId)
            .whereEqualTo("bookId", bookId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    for (document in documents) {
                        val wishlistItem = document.toObject(WishlistItem::class.java)
                        
                        // Remover de Firestore
                        document.reference.delete()
                            .addOnSuccessListener {
                                Log.i(TAG, "✅ Libro '${wishlistItem.bookTitle}' removido automáticamente de lista de deseos")
                                
                                // Remover listener si existe
                                activeListeners[bookId]?.remove()
                                activeListeners.remove(bookId)
                                monitoredBooks.remove(bookId)
                                
                                // Enviar notificación de asignación
                                sendAssignmentNotification(wishlistItem)
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "❌ Error removiendo de lista de deseos: ${e.message}")
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error buscando en lista de deseos: ${e.message}")
            }
    }

    /**
     * 🎉 Notificación de asignación exitosa
     */
    private fun sendAssignmentNotification(wishlistItem: WishlistItem) {
        val notificationId = (wishlistItem.bookTitle.hashCode() + NOTIFICATION_ID_BASE + 100).let {
            if (it < 0) -it else it
        }
        
        val intent = Intent(context, UserActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_assigned_books", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_WISHLIST)
            .setSmallIcon(R.drawable.ic_book_library)
            .setContentTitle("🎉 ¡Libro asignado!")
            .setContentText("${wishlistItem.bookTitle} removido de tu lista de deseos")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("¡Excelente! 📚\n\nEl libro \"${wishlistItem.bookTitle}\" que tenías en tu lista de deseos te ha sido asignado.\n\nYa no aparecerá en tu lista de deseos y lo encontrarás en \"Mis Libros\"."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(context.getColor(R.color.success_green))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            Log.i(TAG, "🎉 Notificación de asignación enviada: ${wishlistItem.bookTitle}")
        } catch (e: SecurityException) {
            Log.w(TAG, "⚠️ Permisos de notificación no otorgados: ${e.message}")
        }
    }

    /**
     * ⛔ Detener monitoreo
     */
    fun stopMonitoring() {
        Log.i(TAG, "⛔ Deteniendo monitoreo de lista de deseos")
        
        wishlistListener?.remove()
        wishlistListener = null
        
        activeListeners.values.forEach { it.remove() }
        activeListeners.clear()
        monitoredBooks.clear()
    }

    /**
     * 📺 Crear canal de notificaciones
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
            
            Log.d(TAG, "📺 Canal de notificaciones creado: $CHANNEL_ID_WISHLIST")
        }
    }
}
