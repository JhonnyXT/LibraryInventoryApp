package com.example.libraryinventoryapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.progressindicator.CircularProgressIndicator
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
                Toast.makeText(this, "Por favor, completa todos los campos.", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(this, "Error al obtener el UID del usuario.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    findViewById<CircularProgressIndicator>(R.id.progressBarRegister).visibility = View.GONE
                    Toast.makeText(this, "Error al registrar el usuario: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "Usuario registrado con éxito.", Toast.LENGTH_SHORT).show()
                // Redirigir a LoginActivity
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish() // Cierra la actividad actual
            }
            .addOnFailureListener { exception ->
                findViewById<CircularProgressIndicator>(R.id.progressBarRegister).visibility = View.GONE
                Toast.makeText(this, "Error al guardar el usuario: ${exception.message}", Toast.LENGTH_SHORT).show()
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
