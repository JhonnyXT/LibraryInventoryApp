package com.example.libraryinventoryapp.repositories

import com.example.libraryinventoryapp.models.User
import kotlinx.coroutines.flow.Flow

/**
 * 👥 UserRepository - Repository de usuarios multiplataforma
 * 
 * FUNCIONALIDADES:
 * ✅ Gestión completa de usuarios con Firebase
 * ✅ Búsquedas y filtros por roles
 * ✅ Gestión de perfiles y preferencias
 * ✅ Validación de emails y duplicados
 */
interface UserRepository {
    
    /**
     * 👤 Obtener usuario por ID
     */
    suspend fun getUserById(userId: String): Result<User?>
    
    /**
     * 📧 Obtener usuario por email
     */
    suspend fun getUserByEmail(email: String): Result<User?>
    
    /**
     * 💾 Crear/Actualizar usuario
     */
    suspend fun saveUser(user: User): Result<Unit>
    
    /**
     * 👥 Obtener todos los usuarios
     */
    suspend fun getAllUsers(): Result<List<User>>
    
    /**
     * 🔍 Buscar usuarios por criterios
     */
    suspend fun searchUsers(
        query: String,
        role: UserRole? = null,
        limit: Int = 50
    ): Result<List<User>>
    
    /**
     * 👑 Obtener todos los administradores
     */
    suspend fun getAllAdmins(): Result<List<User>>
    
    /**
     * 👤 Obtener todos los usuarios regulares
     */
    suspend fun getAllRegularUsers(): Result<List<User>>
    
    /**
     * ✅ Verificar si email existe
     */
    suspend fun emailExists(email: String): Result<Boolean>
    
    /**
     * 🗑️ Eliminar usuario
     */
    suspend fun deleteUser(userId: String): Result<Unit>
    
    /**
     * 🔄 Cambiar rol de usuario
     */
    suspend fun changeUserRole(userId: String, newRole: UserRole): Result<Unit>
    
    /**
     * 📊 Obtener estadísticas de usuarios
     */
    suspend fun getUserStatistics(): Result<UserStatistics>
    
    /**
     * 🔔 Obtener usuarios para notificaciones masivas
     */
    suspend fun getUsersForMassNotifications(): Result<List<User>>
}

/**
 * 👤 Roles de usuario
 */
enum class UserRole(val value: String) {
    ADMIN("admin"),
    USER("usuario");
    
    companion object {
        fun fromString(value: String): UserRole? {
            return values().find { it.value.equals(value, ignoreCase = true) }
        }
    }
}

/**
 * 📊 Estadísticas de usuarios
 */
data class UserStatistics(
    val totalUsers: Int,
    val adminUsers: Int,
    val regularUsers: Int,
    val activeUsers: Int, // Usuarios con libros asignados
    val newUsersThisMonth: Int
)
