package com.example.libraryinventoryapp.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.libraryinventoryapp.R
import com.example.libraryinventoryapp.UserActivity
import com.example.libraryinventoryapp.utils.NotificationHelper
import com.example.libraryinventoryapp.adapters.NotificationsAdapter
import com.example.libraryinventoryapp.models.Book
import com.example.libraryinventoryapp.models.NotificationItem
import com.example.libraryinventoryapp.models.NotificationType
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

/**
 * 🔔 Fragmento profesional de notificaciones para usuarios
 * Diseño moderno basado en Material Design 3 con mejores prácticas UX
 */
class NotificationsFragment : Fragment() {

    companion object {
        private const val TAG = "NotificationsFragment"
    }

    // 🏗️ UI Components
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var recyclerNotifications: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var loadingState: FrameLayout

    // 🔥 Logic Components
    private lateinit var adapter: NotificationsAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var currentUserId: String? = null

    private val notifications = mutableListOf<NotificationItem>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 🎯 Inicializar componentes
        initializeComponents(view)
        setupUI()
        setupClickListeners()
        
        // 🎯 Configuración simplificada - sin menú
        
        // 🔄 Cargar datos iniciales
        loadNotifications()
    }

    /**
     * 🏗️ Inicializar todos los componentes UI
     */
    private fun initializeComponents(view: View) {
        // Views
        swipeRefresh = view.findViewById(R.id.swipe_refresh_notifications)
        recyclerNotifications = view.findViewById(R.id.recycler_notifications)
        emptyState = view.findViewById(R.id.empty_state_notifications)
        loadingState = view.findViewById(R.id.loading_state_notifications)

        // Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid

        // Adapter
        adapter = NotificationsAdapter(notifications) { action, notification ->
            handleNotificationAction(action, notification)
        }
    }

    /**
     * 🎨 Configurar UI y comportamientos
     */
    private fun setupUI() {
        // 🔄 SwipeRefresh
        swipeRefresh.setOnRefreshListener {
            loadNotifications()
        }

        // 📋 RecyclerView
        recyclerNotifications.layoutManager = LinearLayoutManager(requireContext())
        recyclerNotifications.adapter = adapter

        // 🎯 Estados iniciales
        showLoadingState()
    }

    /**
     * 👆 Configurar listeners de clicks
     */
    private fun setupClickListeners() {
        // 🎯 No hay botones adicionales en la versión simplificada
        // Las acciones se manejan directamente en el adapter
    }

    // 🎯 Menú removido para simplicidad - acciones manejadas por adapter

    /**
     * 🔄 Cargar notificaciones del usuario actual
     */
    private fun loadNotifications() {
        if (currentUserId == null) {
            Log.e(TAG, "❌ Usuario no autenticado")
            showEmptyState()
            return
        }

        showLoadingState()
        
        firestore.collection("books")
            .whereArrayContains("assignedTo", currentUserId!!)
            .get()
            .addOnSuccessListener { documents ->
                notifications.clear()
                
                for (document in documents) {
                    val book = document.toObject(Book::class.java)
                    book.id = document.id
                    
                    // 🔍 Buscar asignación del usuario actual
                    val userIndex = book.assignedTo?.indexOf(currentUserId) ?: -1
                    if (userIndex >= 0 && book.loanExpirationDates != null && userIndex < book.loanExpirationDates!!.size) {
                        val expirationDate = book.loanExpirationDates!![userIndex]
                        val userName = book.assignedWithNames?.getOrNull(userIndex) ?: "Usuario"
                        
                        // 📅 Calcular días hasta vencimiento
                        val daysUntilDue = calculateDaysUntilDue(expirationDate)
                        
                        // 🎯 Solo crear notificación si está próximo a vencer o vencido
                        if (shouldCreateNotification(daysUntilDue)) {
                            val notificationItem = createNotificationItem(
                                book, userName, daysUntilDue, expirationDate
                            )
                            notifications.add(notificationItem)
                        }
                    }
                }
                
                // 📊 Ordenar por prioridad (más urgente primero)
                notifications.sortByDescending { it.type.priority }
                
                updateUI()
                swipeRefresh.isRefreshing = false
                
                Log.d(TAG, "✅ Notificaciones cargadas: ${notifications.size}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error cargando notificaciones: ${e.message}")
                showEmptyState()
                swipeRefresh.isRefreshing = false
                NotificationHelper.showError(
                    context = requireContext(),
                    title = "Error de Carga",
                    message = "No se pudieron cargar las notificaciones. Verifica tu conexión.",
                    view = view
                )
            }
    }

    /**
     * 📅 Calcular días hasta el vencimiento
     */
    private fun calculateDaysUntilDue(expirationDate: Timestamp): Int {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        
        val expiration = Calendar.getInstance()
        expiration.time = expirationDate.toDate()
        expiration.set(Calendar.HOUR_OF_DAY, 0)
        expiration.set(Calendar.MINUTE, 0)
        expiration.set(Calendar.SECOND, 0)
        expiration.set(Calendar.MILLISECOND, 0)
        
        val diffInMillis = expiration.timeInMillis - today.timeInMillis
        return (diffInMillis / (24 * 60 * 60 * 1000)).toInt()
    }

    /**
     * 🎯 Determinar si debe crear notificación - EXACTAMENTE igual que AssignedBooksAdapter
     * ✅ Lógica consistente: daysDiff == 0 (hoy) y daysDiff <= 5 (próximos)
     */
    private fun shouldCreateNotification(daysUntilDue: Int): Boolean {
        return daysUntilDue <= 5 // Incluye: próximos 5 días, hoy (0), y vencidos (negativos)
    }

    /**
     * 🔔 Crear item de notificación
     */
    private fun createNotificationItem(
        book: Book,
        userName: String,
        daysUntilDue: Int,
        expirationDate: Timestamp
    ): NotificationItem {
        val type = when {
            daysUntilDue < -7 -> NotificationType.CRITICAL
            daysUntilDue < 0 -> NotificationType.URGENT
            daysUntilDue == 0 -> NotificationType.DUE_TODAY
            daysUntilDue <= 5 -> NotificationType.UPCOMING
            else -> NotificationType.INFO
        }

        return NotificationItem(
            id = "${book.id}_${currentUserId}_${expirationDate.seconds}",
            bookId = book.id,
            bookTitle = book.title,
            bookAuthor = book.author,
            userId = currentUserId!!,
            userName = userName,
            daysUntilDue = daysUntilDue,
            expirationDate = expirationDate,
            type = type,
            isRead = false,
            timestamp = Timestamp.now()
        )
    }

    /**
     * 🎯 Manejar acciones en notificaciones
     */
    private fun handleNotificationAction(action: String, notification: NotificationItem) {
        when (action) {
            "mark_read" -> {
                markNotificationAsRead(notification)
            }
            "click" -> {
                // 👆 Click en la notificación - navegar a libros asignados
                (activity as? UserActivity)?.switchToTab(1) // Tab "Libros Asignados"
            }
        }
    }

    /**
     * ✅ Marcar notificación como leída - REMOVER de la lista
     */
    private fun markNotificationAsRead(notification: NotificationItem) {
        // 🗑️ Remover la notificación directamente de la lista
        adapter.removeNotification(notification)
        
        // 🔄 Actualizar UI inmediatamente
        updateUI()
        
        // 📱 Actualizar badge del menú INMEDIATAMENTE
        updateBottomMenuBadge()
        
        // ✅ Feedback al usuario
        NotificationHelper.showSuccess(
            context = requireContext(),
            title = "Notificación Leída",
            message = "La notificación ha sido marcada como leída.",
            view = view
        )
        
        Log.d(TAG, "📝 Notificación removida: ${notification.bookTitle}. Notificaciones restantes: ${notifications.size}")
    }

    /**
     * 📱 Actualizar badge del bottom menu en tiempo real
     */
    private fun updateBottomMenuBadge() {
        val remainingCount = adapter.getUnreadCount()
        (activity as? UserActivity)?.updateBadgeCount(remainingCount)
        Log.d(TAG, "🔔 Badge actualizado: $remainingCount notificaciones pendientes")
    }

    /**
     * ✅ Marcar todas las notificaciones como leídas
     */
    private fun markAllNotificationsAsRead() {
        for (i in notifications.indices) {
            notifications[i] = notifications[i].copy(isRead = true)
        }
        adapter.notifyDataSetChanged()
        updateUI()
        NotificationHelper.showSuccess(
            context = requireContext(),
            title = "Todas Leídas",
            message = "✅ Todas las notificaciones han sido marcadas como leídas.",
            view = view
        )
    }

    /**
     * 🗑️ Limpiar todas las notificaciones
     */
    private fun clearAllNotifications() {
        notifications.clear()
        adapter.notifyDataSetChanged()
        updateUI()
        NotificationHelper.showSuccess(
            context = requireContext(),
            title = "Notificaciones Limpiadas",
            message = "🗑️ Todas las notificaciones han sido eliminadas.",
            view = view
        )
    }

    /**
     * 🎨 Actualizar interfaz según estado
     */
    private fun updateUI() {
        val unreadCount = notifications.count { !it.isRead }
        
        when {
            notifications.isEmpty() -> showEmptyState()
            else -> showNotificationsList(unreadCount)
        }
        
        // 🔔 Actualizar badge del menú automáticamente
        updateBottomMenuBadge()
    }

    /**
     * ⏳ Mostrar estado de carga
     */
    private fun showLoadingState() {
        loadingState.visibility = View.VISIBLE
        recyclerNotifications.visibility = View.GONE
        emptyState.visibility = View.GONE
    }

    /**
     * 📋 Mostrar lista de notificaciones
     */
    private fun showNotificationsList(unreadCount: Int) {
        loadingState.visibility = View.GONE
        recyclerNotifications.visibility = View.VISIBLE
        emptyState.visibility = View.GONE
        
        adapter.notifyDataSetChanged()
    }

    /**
     * 🎯 Mostrar estado vacío
     */
    private fun showEmptyState() {
        loadingState.visibility = View.GONE
        recyclerNotifications.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
    }
}
