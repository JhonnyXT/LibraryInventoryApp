package com.example.libraryinventoryapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.libraryinventoryapp.fragments.BookListFragment
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
                R.id.nav_assigned_books -> loadFragment(ViewBooksFragment())
                R.id.nav_profile -> loadFragment(RegisterBookFragment())
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.admin_fragment_container, fragment)
            .commit()
    }
}