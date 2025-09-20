package com.example.libraryinventoryapp.models

import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * 🌟 Modelo de datos para elementos de lista de deseos/favoritos
 * 
 * Representa un libro guardado como favorito por un usuario
 */
@Serializable
data class WishlistItem(
    var id: String = "", // ID único del item de wishlist
    val userId: String = "", // UID del usuario
    val bookId: String = "", // ID del libro
    val bookTitle: String = "", // Título del libro (para búsquedas rápidas)
    val bookAuthor: String = "", // Autor del libro
    val bookImageUrl: String? = null, // URL de la imagen
    val bookCategories: List<String> = emptyList(), // Categorías del libro
    val addedDate: Long = Clock.System.now().toEpochMilliseconds(), // Fecha cuando se añadió a favoritos
    var isAvailable: Boolean = true, // Si el libro está disponible actualmente
    val priority: Int = 0 // Prioridad del usuario (0 = normal, 1 = alta, etc.)
) {
    
    /**
     * 📅 Obtener fecha formateada
     */
    fun getFormattedAddedDate(): String {
        return try {
            val instant = Instant.fromEpochMilliseconds(addedDate)
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            
            // Formato dd/MM/yyyy
            val day = localDateTime.dayOfMonth.toString().padStart(2, '0')
            val month = localDateTime.monthNumber.toString().padStart(2, '0')
            val year = localDateTime.year.toString()
            
            "$day/$month/$year"
        } catch (e: Exception) {
            "Sin fecha"
        }
    }
    
    /**
     * 🏷️ Obtener primera categoría (para mostrar)
     */
    fun getPrimaryCategory(): String {
        return if (bookCategories.isNotEmpty()) {
            bookCategories.first()
        } else {
            "Sin categoría"
        }
    }
    
    /**
     * 📚 Estado de disponibilidad para UI
     */
    fun getAvailabilityText(): String {
        return if (isAvailable) "Disponible" else "No disponible"
    }
    
    fun getAvailabilityColor(): String {
        return if (isAvailable) "#4CAF50" else "#F44336"
    }
    
    /**
     * ⭐ Obtener texto de prioridad
     */
    fun getPriorityText(): String {
        return when (priority) {
            0 -> "Normal"
            1 -> "Alta"
            2 -> "Muy alta"
            else -> "Normal"
        }
    }
    
    /**
     * 🔢 Obtener color de prioridad
     */
    fun getPriorityColor(): String {
        return when (priority) {
            0 -> "#757575"  // Gris
            1 -> "#FF9800"  // Naranja
            2 -> "#F44336"  // Rojo
            else -> "#757575"
        }
    }
    
    /**
     * 📊 Verificar si es de alta prioridad
     */
    fun isHighPriority(): Boolean {
        return priority > 0
    }
    
    /**
     * 🎯 Generar resumen para mostrar
     */
    fun getSummary(): String {
        val availabilityText = if (isAvailable) "disponible" else "no disponible"
        val categoryText = getPrimaryCategory()
        return "Por $bookAuthor • $categoryText • $availabilityText"
    }
}
