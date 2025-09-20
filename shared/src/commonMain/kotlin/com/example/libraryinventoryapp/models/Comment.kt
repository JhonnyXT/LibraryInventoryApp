package com.example.libraryinventoryapp.models

import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * 💬 Modelo de comentario para libros
 * Solo usuarios con libro asignado pueden comentar
 */
@Serializable
data class Comment(
    val id: String = "",
    val bookId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val comment: String = "",
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val isEdited: Boolean = false,
    val editedTimestamp: Long? = null
) {
    /**
     * 📅 Obtener texto de tiempo relativo
     */
    fun getRelativeTime(): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val commentTime = timestamp
        val diff = now - commentTime
        
        return when {
            diff < 60_000 -> "Hace un momento"
            diff < 3_600_000 -> "Hace ${diff / 60_000} min"
            diff < 86_400_000 -> "Hace ${diff / 3_600_000} h"
            diff < 604_800_000 -> "Hace ${diff / 86_400_000} días"
            else -> "Hace ${diff / 604_800_000} semanas"
        }
    }
    
    /**
     * ✏️ Obtener texto de tiempo de edición
     */
    fun getEditedRelativeTime(): String? {
        return editedTimestamp?.let { editTime ->
            val now = Clock.System.now().toEpochMilliseconds()
            val diff = now - editTime
            
            when {
                diff < 60_000 -> "Editado hace un momento"
                diff < 3_600_000 -> "Editado hace ${diff / 60_000} min"
                diff < 86_400_000 -> "Editado hace ${diff / 3_600_000} h"
                diff < 604_800_000 -> "Editado hace ${diff / 86_400_000} días"
                else -> "Editado hace ${diff / 604_800_000} semanas"
            }
        }
    }
    
    /**
     * 📝 Verificar si el comentario fue editado recientemente (últimas 24h)
     */
    fun wasRecentlyEdited(): Boolean {
        return editedTimestamp?.let { editTime ->
            val now = Clock.System.now().toEpochMilliseconds()
            val diff = now - editTime
            diff < 86_400_000 // 24 horas en milliseconds
        } ?: false
    }
    
    /**
     * 👤 Obtener iniciales del usuario para avatar
     */
    fun getUserInitials(): String {
        return if (userName.isNotBlank()) {
            userName.split(" ")
                .take(2)
                .map { it.first().uppercaseChar() }
                .joinToString("")
        } else {
            userEmail.take(2).uppercase()
        }
    }
    
    /**
     * 📊 Verificar si es un comentario largo (más de 200 caracteres)
     */
    fun isLongComment(): Boolean {
        return comment.length > 200
    }
    
    /**
     * ✂️ Obtener vista previa del comentario (primeras 100 caracteres)
     */
    fun getPreview(): String {
        return if (comment.length > 100) {
            "${comment.take(97)}..."
        } else {
            comment
        }
    }
    
    /**
     * 🔍 Verificar si contiene palabras clave
     */
    fun containsKeyword(keyword: String): Boolean {
        return comment.contains(keyword, ignoreCase = true)
    }
    
    /**
     * 🎨 Obtener color del avatar basado en el nombre del usuario
     */
    fun getAvatarColor(): String {
        val colors = listOf(
            "#F44336", "#E91E63", "#9C27B0", "#673AB7",
            "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
            "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
            "#FFC107", "#FF9800", "#FF5722", "#795548"
        )
        val hash = userName.hashCode()
        val index = kotlin.math.abs(hash) % colors.size
        return colors[index]
    }
}
