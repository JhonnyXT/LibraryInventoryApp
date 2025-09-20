package com.example.libraryinventoryapp.services

import com.example.libraryinventoryapp.models.User
import kotlinx.coroutines.flow.Flow

/**
 * 🔐 AuthService - Servicio de autenticación multiplataforma
 * 
 * FUNCIONALIDADES:
 * ✅ Abstrae Firebase Auth + Google Sign-In para ambas plataformas
 * ✅ Manejo de estado de autenticación con Flow
 * ✅ Login/logout multiplataforma
 */
interface AuthService {
    
    /**
     * 👤 Estado actual del usuario autenticado
     */
    val currentUser: Flow<User?>
    
    /**
     * ✅ Verificar si hay un usuario autenticado
     */
    val isAuthenticated: Flow<Boolean>
    
    /**
     * 🔑 Login con email y contraseña
     */
    suspend fun signInWithEmail(email: String, password: String): Result<User>
    
    /**
     * 🌐 Login con Google Sign-In
     */
    suspend fun signInWithGoogle(): Result<User>
    
    /**
     * 📝 Registrar nuevo usuario
     */
    suspend fun signUp(email: String, password: String, name: String): Result<User>
    
    /**
     * 🚪 Logout completo (Firebase + Google)
     */
    suspend fun signOut(): Result<Unit>
    
    /**
     * 👤 Obtener usuario actual sincronamente
     */
    fun getCurrentUser(): User?
    
    /**
     * 📧 Enviar email de recuperación de contraseña
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    
    /**
     * 🔄 Refrescar token de autenticación
     */
    suspend fun refreshToken(): Result<Unit>
}

/**
 * 🏭 Factory para crear AuthService específico de plataforma
 */
expect class AuthServiceFactory() {
    fun create(): AuthService
}
