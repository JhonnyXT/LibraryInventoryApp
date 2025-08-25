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
        try {
            // Cerrar sesión de Firebase
            auth.signOut()
            
            // Mostrar feedback al usuario
            Toast.makeText(context, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
            
            // Crear intent para ir a LoginActivity
            val intent = Intent(context, LoginActivity::class.java).apply {
                // Limpiar el stack de actividades y crear una nueva tarea
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                // Asegurar que no se pueda volver atrás
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            
            // Iniciar LoginActivity
            startActivity(intent)
            
            // Cerrar la actividad actual de forma segura
            activity?.let { activity ->
                activity.finishAffinity() // Cierra todas las actividades del stack
            }
            
        } catch (e: Exception) {
            // Manejo de errores en caso de que algo falle
            Toast.makeText(context, "Error al cerrar sesión: ${e.message}", Toast.LENGTH_SHORT).show()
            
            // Fallback: intentar logout básico
            try {
                val intent = Intent(context, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                activity?.finish()
            } catch (fallbackError: Exception) {
                Toast.makeText(context, "Error crítico en logout", Toast.LENGTH_LONG).show()
            }
        }
    }
}