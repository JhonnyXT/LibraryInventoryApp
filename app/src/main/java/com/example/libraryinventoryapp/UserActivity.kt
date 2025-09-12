package com.example.libraryinventoryapp

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.libraryinventoryapp.fragments.AssignedBooksFragment
import com.example.libraryinventoryapp.fragments.BookListFragment
import com.example.libraryinventoryapp.fragments.NotificationsFragment
import com.example.libraryinventoryapp.fragments.ProfileFragment
import com.example.libraryinventoryapp.models.Book
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class UserActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "UserActivity"
    }
    
    // 🎯 Components
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var currentUserId: String? = null
    private var notificationBadge: BadgeDrawable? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user)

        // 🏗️ Inicializar componentes
        initializeComponents()
        setupBottomNavigation()
        
        // 🔄 Configurar badge de notificaciones
        setupNotificationBadge()
        
        // 📚 Fragmento por defecto
        loadFragment(BookListFragment())
        
        // 🔄 Actualizar badge periódicamente
        updateNotificationBadge()
    }
    
    /**
     * 🏗️ Inicializar todos los componentes
     */
    private fun initializeComponents() {
        bottomNav = findViewById(R.id.usuario_bottom_nav)
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid
    }
    
    /**
     * 🎯 Configurar navegación inferior
     */
    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_books_list -> {
                    loadFragment(BookListFragment())
                    true
                }
                R.id.nav_assigned_books -> {
                    loadFragment(AssignedBooksFragment())
                    true
                }
                R.id.nav_notifications -> {
                    loadFragment(NotificationsFragment())
                    // 🔴 Limpiar badge al abrir notificaciones
                    clearNotificationBadge()
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }
    
    /**
     * 🔔 Configurar badge de notificaciones
     */
    private fun setupNotificationBadge() {
        try {
            notificationBadge = bottomNav.getOrCreateBadge(R.id.nav_notifications)
            notificationBadge?.apply {
                backgroundColor = getColor(R.color.red_500)
                badgeTextColor = getColor(android.R.color.white)
                maxCharacterCount = 2 // Máximo 99
                isVisible = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error configurando badge: ${e.message}")
        }
    }
    
    /**
     * 🔄 Actualizar badge con número de notificaciones pendientes
     */
    fun updateNotificationBadge() {
        if (currentUserId == null) return
        
        firestore.collection("books")
            .whereArrayContains("assignedTo", currentUserId!!)
            .get()
            .addOnSuccessListener { documents ->
                var notificationCount = 0
                
                for (document in documents) {
                    val book = document.toObject(Book::class.java)
                    book.id = document.id
                    
                    // 🔍 Buscar asignación del usuario actual
                    val userIndex = book.assignedTo?.indexOf(currentUserId) ?: -1
                    if (userIndex >= 0 && book.loanExpirationDates != null && userIndex < book.loanExpirationDates!!.size) {
                        val expirationDate = book.loanExpirationDates!![userIndex]
                        val daysUntilDue = calculateDaysUntilDue(expirationDate)
                        
                        // 🎯 Contar notificaciones (próximos 5 días o vencidos)
                        if (daysUntilDue <= 5) {
                            notificationCount++
                        }
                    }
                }
                
                // 🔔 Actualizar badge
                updateBadgeCount(notificationCount)
                
                Log.d(TAG, "🔔 Notificaciones pendientes: $notificationCount")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error actualizando badge: ${e.message}")
            }
    }
    
    /**
     * 🔢 Actualizar contador del badge (ACCESO PÚBLICO PARA FRAGMENTOS)
     */
    fun updateBadgeCount(count: Int) {
        notificationBadge?.apply {
            if (count > 0) {
                number = count
                isVisible = true
                Log.d(TAG, "🔔 Badge actualizado: $count notificaciones")
            } else {
                isVisible = false
                Log.d(TAG, "🔔 Badge ocultado")
            }
        }
    }
    
    /**
     * 🗑️ Limpiar badge de notificaciones
     */
    private fun clearNotificationBadge() {
        notificationBadge?.isVisible = false
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
     * 🎯 Función pública para cambiar de tab (usada desde fragments)
     */
    fun switchToTab(tabIndex: Int) {
        val menuItem = when (tabIndex) {
            0 -> bottomNav.menu.findItem(R.id.nav_books_list)
            1 -> bottomNav.menu.findItem(R.id.nav_assigned_books)
            2 -> bottomNav.menu.findItem(R.id.nav_notifications)
            3 -> bottomNav.menu.findItem(R.id.nav_profile)
            else -> return
        }
        
        menuItem?.let {
            bottomNav.selectedItemId = it.itemId
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 🔄 Actualizar badge al volver a la activity
        updateNotificationBadge()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Manejar el botón atrás de forma segura
        try {
            // Si hay fragmentos en el stack, navegar hacia atrás
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                // Si no hay fragmentos en el stack, cerrar la app de forma segura
                finishAffinity()
            }
        } catch (e: Exception) {
            android.util.Log.e("UserActivity", "Error handling back press: ${e.message}", e)
            super.onBackPressed()
        }
    }

    private fun loadFragment(fragment: Fragment) {
        try {
            supportFragmentManager.beginTransaction()
                .replace(R.id.admin_fragment_container, fragment)
                .commit()
        } catch (e: Exception) {
            // Log del error en caso de que falle la transacción
            android.util.Log.e("UserActivity", "Error loading fragment: ${e.message}", e)
        }
    }
}