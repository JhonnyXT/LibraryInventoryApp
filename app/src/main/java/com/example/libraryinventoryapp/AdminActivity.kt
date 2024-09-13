package com.example.libraryinventoryapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.libraryinventoryapp.fragments.RegisterBookFragment
import com.example.libraryinventoryapp.fragments.ViewBooksFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class AdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin)

        val bottomNav = findViewById<BottomNavigationView>(R.id.admin_bottom_nav)

        // Default fragment to load
        loadFragment(RegisterBookFragment())

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_register_book -> loadFragment(RegisterBookFragment())
                R.id.nav_view_books -> loadFragment(ViewBooksFragment())
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