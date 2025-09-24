package com.example.libraryinventoryapp.services

import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * 🤖 Implementación Android de AuthService
 * Delega completamente a AuthServiceBridge existente
 */
actual class AuthService {
    
    /**
     * 🔄 Logout completo - implementación Android
     */
    actual suspend fun performLogout(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // Delegar al AuthServiceBridge existente usando reflection
                val bridgeClass = Class.forName("com.example.libraryinventoryapp.utils.AuthServiceBridge")
                val getInstance = bridgeClass.getMethod("getInstance")
                val bridge = getInstance.invoke(null)
                
                // Usar método que ya funciona perfecto
                val logoutMethod = bridgeClass.getMethod("logoutFromFirebase")
                val success = logoutMethod.invoke(bridge) as Boolean
                
                Result.success(success)
            } catch (e: Exception) {
                // Si falla reflection, retornar fallo
                Result.failure(e)
            }
        }
    }
    
    /**
     * 👤 Obtener usuario actual - implementación Android
     */
    actual suspend fun getCurrentUser(): Result<AuthUser?> {
        return withContext(Dispatchers.IO) {
            try {
                // Por simplicidad, usar reflection para acceder a Firebase Auth
                val authClass = Class.forName("com.google.firebase.auth.FirebaseAuth")
                val getInstance = authClass.getMethod("getInstance")
                val auth = getInstance.invoke(null)
                
                val getCurrentUser = authClass.getMethod("getCurrentUser")
                val firebaseUser = getCurrentUser.invoke(auth)
                
                if (firebaseUser != null) {
                    // Obtener datos básicos usando reflection
                    val userClass = firebaseUser.javaClass
                    val getUid = userClass.getMethod("getUid")
                    val getEmail = userClass.getMethod("getEmail")
                    val getDisplayName = userClass.getMethod("getDisplayName")
                    
                    val uid = getUid.invoke(firebaseUser) as String
                    val email = getEmail.invoke(firebaseUser) as? String ?: ""
                    val displayName = getDisplayName.invoke(firebaseUser) as? String ?: ""
                    
                    // Por simplicidad, asumimos usuario regular (no admin)
                    val authUser = AuthUser(
                        uid = uid,
                        email = email,
                        displayName = displayName,
                        isAdmin = false
                    )
                    
                    Result.success(authUser)
                } else {
                    Result.success(null)
                }
            } catch (e: Exception) {
                Result.success(null) // Si falla, no hay usuario
            }
        }
    }
    
    /**
     * 🔒 Verificar sesión activa - implementación Android
     */
    actual fun isUserLoggedIn(): Boolean {
        return try {
            val authClass = Class.forName("com.google.firebase.auth.FirebaseAuth")
            val getInstance = authClass.getMethod("getInstance")
            val auth = getInstance.invoke(null)
            
            val getCurrentUser = authClass.getMethod("getCurrentUser")
            val firebaseUser = getCurrentUser.invoke(auth)
            
            firebaseUser != null
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * 🏭 Factory para Android
 */
actual object AuthServiceFactory {
    actual fun create(): AuthService = AuthService()
}
