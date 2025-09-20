package com.example.libraryinventoryapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.example.libraryinventoryapp.utils.NotificationHelper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Get UI elements
        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        // Progress bar se controla directamente

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                NotificationHelper.showValidationError(
                    context = this,
                    field = "Campos Obligatorios",
                    requirement = "Por favor, completa todos los campos (nombre, email y contraseña)."
                )
                return@setOnClickListener
            }

            registerUser(name, email, password)
        }
    }

    private fun registerUser(name: String, email: String, password: String) {
        findViewById<CircularProgressIndicator>(R.id.progressBarRegister).visibility = View.VISIBLE
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        saveUserToFirestore(name, email, userId)
                    } else {
                        findViewById<CircularProgressIndicator>(R.id.progressBarRegister).visibility = View.GONE
                        NotificationHelper.showError(
                            context = this,
                            title = "Error de Usuario",
                            message = "No se pudo obtener el identificador único del usuario."
                        )
                    }
                } else {
                    findViewById<CircularProgressIndicator>(R.id.progressBarRegister).visibility = View.GONE
                    NotificationHelper.showError(
                        context = this,
                        title = "Error de Registro",
                        message = "No se pudo registrar el usuario: ${task.exception?.message}"
                    )
                }
            }
    }

    private fun saveUserToFirestore(name: String, email: String, uid: String) {
        val userMap = hashMapOf(
            "name" to name,
            "email" to email,
            "uid" to uid,
            "role" to "usuario" // Rol por defecto
        )

        firestore.collection("users").document(uid)
            .set(userMap)
            .addOnSuccessListener {
                findViewById<CircularProgressIndicator>(R.id.progressBarRegister).visibility = View.GONE
                NotificationHelper.showAccountCreated(
                    context = this,
                    userName = name
                )
                // Redirigir a LoginActivity
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish() // Cierra la actividad actual
            }
            .addOnFailureListener { exception ->
                findViewById<CircularProgressIndicator>(R.id.progressBarRegister).visibility = View.GONE
                NotificationHelper.showError(
                    context = this,
                    title = "Error de Base de Datos",
                    message = "No se pudo guardar la información del usuario: ${exception.message}"
                )
            }
    }

    override fun onBackPressed() {
        // Redirigir a LoginActivity al presionar el botón de retroceso
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // Cierra la actividad actual

        // Llama a la implementación por defecto para asegurarte de que la actividad se maneje adecuadamente
        super.onBackPressed()
    }
}
