package com.example.libraryinventoryapp.services

/**
 * 🍎 AuthServiceFactory para iOS
 */
actual class AuthServiceFactory {
    
    /**
     * 🔧 Crear AuthService para iOS
     */
    actual fun create(): AuthService {
        println("🚀 Creando AuthService para iOS")
        return IOSAuthService()
    }
}

/**
 * 🍎 Implementación iOS de AuthService (placeholder)
 */
class IOSAuthService : AuthService {
    
    // TODO: Implementar con Firebase Auth iOS
    // Por ahora, implementación básica para compilación
    
    override val currentUser = kotlinx.coroutines.flow.flowOf(null)
    override val isAuthenticated = kotlinx.coroutines.flow.flowOf(false)
    
    override suspend fun signInWithEmail(email: String, password: String) = 
        Result.failure<com.example.libraryinventoryapp.models.User>(Exception("TODO: Implementar Firebase Auth iOS"))
        
    override suspend fun signInWithGoogle() = 
        Result.failure<com.example.libraryinventoryapp.models.User>(Exception("TODO: Implementar Google Sign-In iOS"))
        
    override suspend fun signUp(email: String, password: String, name: String) = 
        Result.failure<com.example.libraryinventoryapp.models.User>(Exception("TODO: Implementar registro iOS"))
        
    override suspend fun signOut() = 
        Result.success(Unit)
        
    override fun getCurrentUser() = null
    
    override suspend fun sendPasswordResetEmail(email: String) = 
        Result.success(Unit)
        
    override suspend fun refreshToken() = 
        Result.success(Unit)
}
