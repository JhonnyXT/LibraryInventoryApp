package com.example.libraryinventoryapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.libraryinventoryapp.fragments.RegisterBookFragment
import com.example.libraryinventoryapp.fragments.ViewBooksFragment
import com.example.libraryinventoryapp.fragments.OverdueBooksFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class AdminActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin)

        bottomNav = findViewById(R.id.admin_bottom_nav)

        // 🔄 Configurar navegación mejorada (esto ya carga el fragmento por defecto)
        setupBottomNavigation()
    }
    
    /**
     * 🔄 Configurar navegación inferior mejorada (igual que UserActivity)
     */
    private fun setupBottomNavigation() {
        // 📚 Cargar fragmento por defecto y seleccionar tab
        loadFragment(ViewBooksFragment())
        bottomNav.selectedItemId = R.id.nav_view_books
        
        bottomNav.setOnItemSelectedListener { item ->
            try {
                when (item.itemId) {
                    R.id.nav_view_books -> {
                        loadFragment(ViewBooksFragment())
                        android.util.Log.d("AdminActivity", "📚 Navegando a Ver Libros")
                        true
                    }
                    R.id.nav_overdue_books -> {
                        loadFragment(OverdueBooksFragment())
                        android.util.Log.d("AdminActivity", "⏰ Navegando a Devoluciones")
                        true
                    }
                    else -> {
                        android.util.Log.w("AdminActivity", "⚠️ Item de menú no reconocido: ${item.itemId}")
                        false
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AdminActivity", "❌ Error en navegación: ${e.message}", e)
                false
            }
        }
    }
    
    /**
     * 🔄 Cambiar a tab específico programáticamente
     */
    fun switchToTab(tabIndex: Int) {
        val menuItem = when (tabIndex) {
            0 -> bottomNav.menu.findItem(R.id.nav_view_books)       // 📚 Ver Libros  
            1 -> bottomNav.menu.findItem(R.id.nav_overdue_books)    // ⏰ Devoluciones
            else -> return
        }
        
        menuItem?.let {
            bottomNav.selectedItemId = it.itemId
        }
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
            android.util.Log.e("AdminActivity", "❌ Error handling back press: ${e.message}", e)
            super.onBackPressed()
        }
    }

    /**
     * 🔄 Cargar fragmento con manejo de errores mejorado
     */
    private fun loadFragment(fragment: Fragment) {
        try {
            supportFragmentManager.beginTransaction()
                .replace(R.id.admin_fragment_container, fragment)
                .commit()
        } catch (e: Exception) {
            // Log del error en caso de que falle la transacción
            android.util.Log.e("AdminActivity", "❌ Error loading fragment: ${e.message}", e)
        }
    }
}