package com.example.libraryinventoryapp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.libraryinventoryapp.LoginActivity
import com.example.libraryinventoryapp.R
import com.example.libraryinventoryapp.utils.AuthManager
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {

    private lateinit var logoutButton: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logoutButton = view.findViewById(R.id.logout_button)
        auth = FirebaseAuth.getInstance()

        logoutButton.setOnClickListener {
            logoutUser()
        }
    }

    private fun logoutUser() {
        // 🔐 Usar AuthManager para logout completo (Firebase + Google Sign-In)
        val authManager = AuthManager.getInstance()
        authManager.performCompleteLogout(this, showSuccessMessage = true)
    }
}