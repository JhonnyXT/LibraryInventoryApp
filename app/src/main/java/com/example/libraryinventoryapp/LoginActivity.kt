package com.example.libraryinventoryapp

import com.example.libraryinventoryapp.Greeting

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.progressindicator.CircularProgressIndicator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.libraryinventoryapp.utils.PermissionHelper
import com.example.libraryinventoryapp.utils.NotificationHelper
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
            NotificationHelper.showError(
                context = this,
                title = "Error de Autenticación",
                message = errorMessage
            )
            android.util.Log.e("GoogleSignIn", "Error detallado: Código=$errorCode, Mensaje=${e.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        
        // 🧪 Test KMP Integration - Todos los bridges
        com.example.libraryinventoryapp.utils.KmpTestUtils.testAllKmpBridges(this)

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
                NotificationHelper.showValidationError(
                    context = this,
                    field = "Email",
                    requirement = "Por favor ingresa tu correo electrónico."
                )
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                NotificationHelper.showValidationError(
                    context = this,
                    field = "Email",
                    requirement = "Por favor ingresa un email válido con formato correcto (ejemplo@dominio.com)."
                )
                return@setOnClickListener
            }

            // Validar el campo de contraseña
            if (password.isEmpty()) {
                NotificationHelper.showValidationError(
                    context = this,
                    field = "Contraseña",
                    requirement = "Por favor ingresa tu contraseña."
                )
                return@setOnClickListener
            }

            if (password.length < 6) {
                NotificationHelper.showValidationError(
                    context = this,
                    field = "Contraseña",
                    requirement = "La contraseña debe tener al menos 6 caracteres para mayor seguridad."
                )
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
                        NotificationHelper.showError(
                context = this,
                title = "Error de Autenticación",
                message = errorMessage
            )
                    }
                }
        }
    }

    private fun navigateToAppropriateScreen(user: FirebaseUser?) {
        if (user == null) {
            // Handle user not logged in
            findViewById<CircularProgressIndicator>(R.id.progressBarLogin).visibility = View.GONE
            NotificationHelper.showError(
                context = this,
                title = "Error de Autenticación",
                message = "No se pudo autenticar al usuario.\n\nIntenta iniciar sesión de nuevo."
            )
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
                NotificationHelper.showError(
                context = this,
                title = "Error de Usuario",
                message = "No se encontraron datos del usuario.\n\nContacta al administrador."
            )
            }
        }.addOnFailureListener { exception ->
            // Handle errors
            findViewById<CircularProgressIndicator>(R.id.progressBarLogin).visibility = View.GONE
            NotificationHelper.showError(
                context = this,
                title = "Error de Conexión",
                message = "No se pudieron obtener los datos del usuario:\n\n${exception.message}"
            )
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
                    NotificationHelper.showError(
                    context = this,
                    title = "Error Google Sign-In",
                    message = "No se pudo autenticar con Google:\n\n${task.exception?.message}"
                )
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
                    NotificationHelper.showError(
                    context = this,
                    title = "Error de Verificación",
                    message = "No se pudo verificar el usuario:\n\n${task.exception?.message}"
                )
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
                    NotificationHelper.showAccountCreated(
                    context = this,
                    userName = user.displayName ?: "Usuario"
                )
                    navigateBasedOnRole("usuario")
                } else {
                    NotificationHelper.showError(
                    context = this,
                    title = "Error al Crear Usuario",
                    message = "No se pudo crear el usuario en el sistema:\n\n${task.exception?.message}"
                )
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
                NotificationHelper.showError(
                context = this,
                title = "Error de Rol",
                message = "El rol del usuario no es reconocido por el sistema.\n\nContacta al administrador."
            )
                return
            }
        }
        finish()
    }
}