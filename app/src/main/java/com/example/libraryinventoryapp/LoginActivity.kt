package com.example.libraryinventoryapp

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        progressBar = findViewById(R.id.progressBarLogin)

        // Verificar si el usuario ya está autenticado
        val currentUser = auth.currentUser
        if (currentUser != null) {
            navigateToAppropriateScreen(currentUser)
            return
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        loginButton.setOnClickListener {
            // Ocultar el teclado
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)

            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // Validar el campo de email
            if (email.isEmpty()) {
                Toast.makeText(this, "Por favor ingresa un email.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Por favor ingresa un email válido.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validar el campo de contraseña
            if (password.isEmpty()) {
                Toast.makeText(this, "Por favor ingresa una contraseña.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = ProgressBar.VISIBLE

            // Autenticación con Firebase
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    progressBar.visibility = ProgressBar.GONE // Ocultar la barra de progreso

                    if (task.isSuccessful) {
                        // Autenticación exitosa
                        val user = auth.currentUser
                        navigateToAppropriateScreen(user)
                    } else {
                        // Manejo de errores específicos
                        val errorMessage = when (task.exception?.message) {
                            "The password is invalid or the user does not have a password." ->
                                "Correo o contraseña incorrectos. Inténtalo de nuevo."
                            "There is no user record corresponding to this identifier. The user may have been deleted." ->
                                "No existe una cuenta registrada con este correo electrónico."
                            "An internal error has occurred. [ INVALID_LOGIN_CREDENTIALS ]" ->
                                "Correo o contraseña incorrectos. Inténtalo de nuevo."
                            else ->
                                task.exception?.localizedMessage ?: "Error de autenticación. Inténtalo de nuevo."
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun navigateToAppropriateScreen(user: FirebaseUser?) {
        if (user == null) {
            // Handle user not logged in
            progressBar.visibility = ProgressBar.GONE
            Toast.makeText(this, "Usuario no autenticado.", Toast.LENGTH_SHORT).show()
            return
        }

        val firestore = FirebaseFirestore.getInstance()
        val userRef = firestore.collection("users").document(user.uid)

        userRef.get().addOnSuccessListener { document ->
            if (document != null) {
                val role = document.getString("role")
                val intent = if (role == "admin") {
                    Intent(this, AdminActivity::class.java)
                } else {
                    Intent(this, UserActivity::class.java)
                }
                progressBar.visibility = ProgressBar.GONE
                startActivity(intent)
                finish()
            } else {
                // Handle the case where the document does not exist
                progressBar.visibility = ProgressBar.GONE
                Toast.makeText(this, "El documento del usuario no existe.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            // Handle errors
            progressBar.visibility = ProgressBar.GONE
            Toast.makeText(this, "Error al obtener los datos del usuario: ${exception.message}", Toast.LENGTH_LONG).show()
        }
    }
}