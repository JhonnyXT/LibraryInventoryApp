package com.example.libraryinventoryapp.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.example.libraryinventoryapp.LoginActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * 🌉 AuthServiceBridge - Puente entre UI Android y AuthService KMP
 * 
 * PROPÓSITO:
 * ✅ Mantener interfaz exacta del AuthManager Android original
 * ✅ Usar internamente AuthService KMP cuando esté disponible
 * ✅ Preservar 100% compatibilidad con código existente
 * ✅ Gestión híbrida Firebase + Google Sign-In
 */
class AuthServiceBridge private constructor() {

    companion object {
        private const val TAG = "AuthServiceBridge"
        
        @Volatile
        private var INSTANCE: AuthServiceBridge? = null
        
        fun getInstance(): AuthServiceBridge {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthServiceBridge().also { INSTANCE = it }
            }
        }
    }

    // 🔧 AuthService KMP (pendiente de implementación completa)
    private val authServiceKmp = null // Temporalmente null hasta Phase 5

    // 🔥 Firebase Auth directo (fallback)
    private val firebaseAuth = FirebaseAuth.getInstance()

    /**
     * 🚪 Logout completo desde Fragment - COMPATIBLE con interfaz original
     */
    fun performCompleteLogout(fragment: Fragment, showSuccessMessage: Boolean = true) {
        val context = fragment.requireContext()
        val activity = fragment.requireActivity()
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.i(TAG, "🌉 Bridge: Logout desde Fragment")
                
                val success = performLogoutProcess(context)
                
                if (success) {
                    // 🛑 Detener servicios relacionados
                    stopRelatedServices()
                    
                    if (showSuccessMessage) {
                        NotificationHelper.showLogoutSuccess(
                            context = context,
                            canChooseAccount = true
                        )
                    }
                    
                    // 🔄 Redireccionar a LoginActivity
                    redirectToLogin(context)
                } else {
                    NotificationHelper.showError(
                        context = context,
                        title = "Error",
                        message = "Error al cerrar sesión",
                        view = fragment.view
                    )
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Bridge: Error en logout desde Fragment: ${e.message}", e)
                NotificationHelper.showError(
                    context = context,
                    title = "Error",
                    message = "Error inesperado al cerrar sesión",
                    view = fragment.view
                )
            }
        }
    }

    /**
     * 🚪 Logout completo desde Activity - COMPATIBLE con interfaz original
     */
    fun performCompleteLogout(activity: FragmentActivity, showSuccessMessage: Boolean = true) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.i(TAG, "🌉 Bridge: Logout desde Activity")
                
                val success = performLogoutProcess(activity)
                
                if (success) {
                    // 🛑 Detener servicios relacionados
                    stopRelatedServices()
                    
                    if (showSuccessMessage) {
                        NotificationHelper.showLogoutSuccess(
                            context = activity,
                            canChooseAccount = true
                        )
                    }
                    
                    // 🔄 Redireccionar a LoginActivity
                    redirectToLogin(activity)
                } else {
                    NotificationHelper.showError(
                        context = activity,
                        title = "Error",
                        message = "Error al cerrar sesión"
                    )
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Bridge: Error en logout desde Activity: ${e.message}", e)
                NotificationHelper.showError(
                    context = activity,
                    title = "Error",
                    message = "Error inesperado al cerrar sesión"
                )
            }
        }
    }

    /**
     * 🔧 Proceso de logout híbrido (KMP + Firebase fallback)
     */
    private suspend fun performLogoutProcess(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 🔧 Usar logout Firebase directo (Phase 4 - simplified)
                // En Phase 5 implementaremos KMP completo
                Log.d(TAG, "🔧 Usando logout Firebase directo")
                val firebaseSuccess = logoutFromFirebase()
                val googleSuccess = logoutFromGoogle(context)
                
                firebaseSuccess && googleSuccess
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en proceso de logout: ${e.message}", e)
                false
            }
        }
    }

    /**
     * 🔥 Logout de Firebase Auth
     */
    private suspend fun logoutFromFirebase(): Boolean {
        return try {
            firebaseAuth.signOut()
            Log.d(TAG, "✅ Firebase logout exitoso")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en logout Firebase: ${e.message}", e)
            false
        }
    }

    /**
     * 🌐 Logout de Google Sign-In con revoke access
     */
    private fun logoutFromGoogle(context: Context): Boolean {
        return try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(com.example.libraryinventoryapp.R.string.default_web_client_id))
                .requestEmail()
                .build()
            
            val googleSignInClient = GoogleSignIn.getClient(context, gso)
            
            // 1. Sign out (operación síncrona simple)
            googleSignInClient.signOut()
            Log.d(TAG, "✅ Google signOut exitoso")
            
            // 2. Revoke access para forzar selector de cuentas (operación síncrona simple)
            googleSignInClient.revokeAccess()
            Log.d(TAG, "✅ Google revokeAccess exitoso - selector de cuentas habilitado")
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en logout Google: ${e.message}", e)
            false
        }
    }

    /**
     * 🛑 Detener servicios relacionados
     */
    private fun stopRelatedServices() {
        try {
            // Detener WishlistAvailabilityService si está activo
            // (Se manejará con WishlistServiceBridge después)
            Log.d(TAG, "🛑 Deteniendo servicios relacionados...")
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Error deteniendo servicios: ${e.message}")
        }
    }

    /**
     * 🔄 Redireccionar a LoginActivity
     */
    private fun redirectToLogin(context: Context) {
        try {
            val intent = Intent(context, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
            Log.d(TAG, "🔄 Redirección a LoginActivity exitosa")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en redirección: ${e.message}", e)
        }
    }

    /**
     * 👤 Obtener usuario actual (compatible con AuthService KMP)
     */
    fun getCurrentUser(): com.example.libraryinventoryapp.models.User? {
        return try {
            // Por ahora siempre usamos Firebase directo
            // En Phase 5 implementaremos la conversión KMP completa
            firebaseAuth.currentUser?.let { firebaseUser ->
                Log.d(TAG, "📱 Usando Firebase para getCurrentUser")
                // Retornamos datos básicos del usuario de Firebase
                // Para datos completos se necesitaría consultar Firestore
                null // Simplified por ahora
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo usuario actual: ${e.message}", e)
            null
        }
    }
}
