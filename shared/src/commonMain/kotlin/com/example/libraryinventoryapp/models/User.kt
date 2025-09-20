package com.example.libraryinventoryapp.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val uid: String = ""
) {
    
    /**
     * 👤 Verificar si el usuario es administrador
     */
    fun isAdmin(): Boolean {
        return role.equals("admin", ignoreCase = true)
    }
    
    /**
     * 👥 Verificar si el usuario es usuario regular
     */
    fun isRegularUser(): Boolean {
        return role.equals("usuario", ignoreCase = true)
    }
    
    /**
     * 📧 Verificar si tiene email válido
     */
    fun hasValidEmail(): Boolean {
        return email.isNotBlank() && email.contains("@")
    }
    
    /**
     * 🏷️ Obtener nombre para mostrar (fallback a email si no hay nombre)
     */
    fun getDisplayName(): String {
        return if (name.isNotBlank()) name else email.substringBefore("@")
    }
    
    /**
     * 🎯 Obtener iniciales para avatar
     */
    fun getInitials(): String {
        return if (name.isNotBlank()) {
            name.split(" ")
                .take(2)
                .map { it.first().uppercaseChar() }
                .joinToString("")
        } else {
            email.take(2).uppercase()
        }
    }
}
