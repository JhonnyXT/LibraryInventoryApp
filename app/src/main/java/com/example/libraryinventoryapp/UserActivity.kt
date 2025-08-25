package com.example.libraryinventoryapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.libraryinventoryapp.fragments.AssignedBooksFragment
import com.example.libraryinventoryapp.fragments.BookListFragment
import com.example.libraryinventoryapp.fragments.ProfileFragment
import com.example.libraryinventoryapp.fragments.RegisterBookFragment
import com.example.libraryinventoryapp.fragments.ViewBooksFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class UserActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user)

        val bottomNav = findViewById<BottomNavigationView>(R.id.usuario_bottom_nav)

        // Default fragment to load
        loadFragment(BookListFragment())

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_books_list -> loadFragment(BookListFragment())
                R.id.nav_assigned_books -> loadFragment(AssignedBooksFragment())
                R.id.nav_profile -> loadFragment(ProfileFragment())
            }
            true
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