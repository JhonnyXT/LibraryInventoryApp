package com.example.libraryinventoryapp

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.progressindicator.CircularProgressIndicator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.libraryinventoryapp.utils.PermissionHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.activity.result.contract.ActivityResultContracts

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var permissionHelper: PermissionHelper
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerText: TextView
    private lateinit var googleSignInButton: Button
    
    // Variables para almacenar destino después de permisos
    private var pendingNavigationRole: String? = null
    
    // ActivityResultLauncher para Google Sign-In
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            findViewById<CircularProgressIndicator>(R.id.progressBarLogin).visibility = View.GONE
            // Mostrar error más detallado para debug
            val errorCode = e.statusCode
            val errorMessage = when (errorCode) {
                10 -> "Error de configuración: Verifica Web Client ID en strings.xml"
                12501 -> "Usuario canceló el login"
                7 -> "Error de red - Verifica conexión a internet"
                8 -> "Error interno - Verifica configuración Firebase"
                else -> "Error Google Sign-In: Código $errorCode - ${e.message}"
            }
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            android.util.Log.e("GoogleSignIn", "Error detallado: Código=$errorCode, Mensaje=${e.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        permissionHelper = PermissionHelper(this)
        
        // Configurar Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        registerText = findViewById(R.id.registerText)
        googleSignInButton = findViewById(R.id.googleSignInButton)

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

        registerText.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }
        
        // Listener para Google Sign-In
        googleSignInButton.setOnClickListener {
            signInWithGoogle()
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

            findViewById<CircularProgressIndicator>(R.id.progressBarLogin).visibility = View.VISIBLE

            // Autenticación con Firebase
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    findViewById<CircularProgressIndicator>(R.id.progressBarLogin).visibility = View.GONE

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
            findViewById<CircularProgressIndicator>(R.id.progressBarLogin).visibility = View.GONE
            Toast.makeText(this, "Usuario no autenticado.", Toast.LENGTH_SHORT).show()
            return
        }

        val firestore = FirebaseFirestore.getInstance()
        val userRef = firestore.collection("users").document(user.uid)

        userRef.get().addOnSuccessListener { document ->
            if (document != null) {
                val role = document.getString("role") ?: "usuario"
                
                findViewById<CircularProgressIndicator>(R.id.progressBarLogin).visibility = View.GONE
                
                // 🔒 Verificar permisos de notificación antes de navegar
                if (permissionHelper.checkAllNotificationPermissions()) {
                    // ✅ Todos los permisos están bien, navegar directamente
                    navigateToRoleScreen(role)
                } else {
                    // ❌ Faltan permisos, guardar destino y solicitarlos
                    pendingNavigationRole = role
                    permissionHelper.requestAllPermissions()
                }
            } else {
                // Handle the case where the document does not exist
                findViewById<CircularProgressIndicator>(R.id.progressBarLogin).visibility = View.GONE
                Toast.makeText(this, "El documento del usuario no existe.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            // Handle errors
            findViewById<CircularProgressIndicator>(R.id.progressBarLogin).visibility = View.GONE
            Toast.makeText(this, "Error al obtener los datos del usuario: ${exception.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * 🎯 Navegar a la pantalla correspondiente según el rol
     */
    private fun navigateToRoleScreen(role: String) {
        val intent = if (role == "admin") {
            Intent(this, AdminActivity::class.java)
        } else {
            Intent(this, UserActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
    
    /**
     * 🔄 Navegar después de obtener permisos
     */
    fun navigateAfterPermissions() {
        pendingNavigationRole?.let { role ->
            navigateToRoleScreen(role)
            pendingNavigationRole = null
        }
    }
    
    /**
     * 🔄 Manejar resultados de permisos
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
    
    /**
     * 🔄 Manejar resultados de actividades (configuración)
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        permissionHelper.onActivityResult(requestCode, resultCode)
        
        // Si regresamos de configuración y tenemos un rol pendiente, verificar permisos
        if (pendingNavigationRole != null && requestCode == PermissionHelper.REQUEST_NOTIFICATION_SETTINGS) {
            if (permissionHelper.checkAllNotificationPermissions()) {
                navigateAfterPermissions()
            }
        }
    }
    
    // 🌐 GOOGLE SIGN-IN FUNCTIONS
    
    private fun signInWithGoogle() {
        findViewById<CircularProgressIndicator>(R.id.progressBarLogin).visibility = View.VISIBLE
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }
    
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Google Sign-In exitoso
                    val user = auth.currentUser
                    user?.let { firebaseUser ->
                        // Verificar si el usuario ya existe en Firestore
                        checkAndCreateUserInFirestore(firebaseUser)
                    }
                } else {
                    findViewById<CircularProgressIndicator>(R.id.progressBarLogin).visibility = View.GONE
                    Toast.makeText(this, "Error en autenticación con Google: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
    
    private fun checkAndCreateUserInFirestore(user: FirebaseUser) {
        val db = FirebaseFirestore.getInstance()
        val userId = user.uid
        
        // Verificar si el usuario ya existe en Firestore
        db.collection("users").document(userId).get()
            .addOnCompleteListener { task ->
                findViewById<CircularProgressIndicator>(R.id.progressBarLogin).visibility = View.GONE
                
                if (task.isSuccessful) {
                    val document = task.result
                    if (document != null && document.exists()) {
                        // Usuario ya existe, navegar según su rol
                        val role = document.getString("role") ?: "usuario"
                        navigateBasedOnRole(role)
                    } else {
                        // Usuario nuevo, crear registro con rol por defecto "usuario"
                        createNewGoogleUser(user)
                    }
                } else {
                    Toast.makeText(this, "Error al verificar usuario: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
    
    private fun createNewGoogleUser(user: FirebaseUser) {
        val db = FirebaseFirestore.getInstance()
        val userId = user.uid
        
        val userData = hashMapOf(
            "name" to (user.displayName ?: "Usuario Google"),
            "email" to (user.email ?: ""),
            "role" to "usuario", // Rol por defecto para usuarios de Google
            "uid" to userId
        )
        
        findViewById<CircularProgressIndicator>(R.id.progressBarLogin).visibility = View.VISIBLE
        
        db.collection("users").document(userId).set(userData)
            .addOnCompleteListener { task ->
                findViewById<CircularProgressIndicator>(R.id.progressBarLogin).visibility = View.GONE
                
                if (task.isSuccessful) {
                    Toast.makeText(this, "¡Bienvenido! Tu cuenta ha sido creada exitosamente.", Toast.LENGTH_SHORT).show()
                    navigateBasedOnRole("usuario")
                } else {
                    Toast.makeText(this, "Error al crear usuario: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
    
    private fun navigateBasedOnRole(role: String) {
        when (role) {
            "admin" -> {
                val intent = Intent(this, AdminActivity::class.java)
                startActivity(intent)
            }
            "usuario" -> {
                val intent = Intent(this, UserActivity::class.java)
                startActivity(intent)
            }
            else -> {
                Toast.makeText(this, "Rol de usuario no reconocido", Toast.LENGTH_SHORT).show()
                return
            }
        }
        finish()
    }
}