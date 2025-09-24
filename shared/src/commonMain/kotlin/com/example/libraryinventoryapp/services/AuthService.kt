package com.example.libraryinventoryapp.services

/**
 * 🔐 AuthService KMP - Servicio de autenticación multiplataforma
 * 
 * Este servicio proporciona funcionalidades de autenticación básicas
 * que serán implementadas específicamente para cada plataforma.
 */
expect class AuthService {
    
    /**
     * 🔄 Logout completo de la sesión actual
     * Incluye Firebase Auth y Google Sign-In si aplica
     */
    suspend fun performLogout(): Result<Boolean>
    
    /**
     * 👤 Obtener usuario actual autenticado
     * Retorna información básica del usuario o null si no hay sesión
     */
    suspend fun getCurrentUser(): Result<AuthUser?>
    
    /**
     * 🔒 Verificar si hay una sesión activa
     */
    fun isUserLoggedIn(): Boolean
}

/**
 * 👤 Modelo simple de usuario para KMP
 * Usado solo para transferencia de datos entre plataformas
 */
data class AuthUser(
    val uid: String,
    val email: String,
    val displayName: String,
    val isAdmin: Boolean
)

/**
 * 🏭 Factory para crear instancias de AuthService según la plataforma
 */
expect object AuthServiceFactory {
    fun create(): AuthService
}
