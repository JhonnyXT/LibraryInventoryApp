package com.example.libraryinventoryapp.models

import com.google.firebase.Timestamp

/**
 * 💬 Modelo de comentario para libros
 * Solo usuarios con libro asignado pueden comentar
 */
data class Comment(
    val id: String = "",
    val bookId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val comment: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val isEdited: Boolean = false,
    val editedTimestamp: Timestamp? = null
) {
    /**
     * 📅 Obtener texto de tiempo relativo
     */
    fun getRelativeTime(): String {
        val now = System.currentTimeMillis()
        val commentTime = timestamp.toDate().time
        val diff = now - commentTime
        
        return when {
            diff < 60_000 -> "Hace un momento"
            diff < 3_600_000 -> "Hace ${diff / 60_000} min"
            diff < 86_400_000 -> "Hace ${diff / 3_600_000} h"
            diff < 604_800_000 -> "Hace ${diff / 86_400_000} días"
            else -> "Hace ${diff / 604_800_000} semanas"
        }
    }
}
