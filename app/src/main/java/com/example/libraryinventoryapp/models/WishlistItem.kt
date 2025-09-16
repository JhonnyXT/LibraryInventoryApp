package com.example.libraryinventoryapp.models

import com.google.firebase.Timestamp

/**
 * 🌟 Modelo de datos para elementos de lista de deseos/favoritos
 * 
 * Representa un libro guardado como favorito por un usuario
 */
data class WishlistItem(
    var id: String = "", // ID único del item de wishlist
    val userId: String = "", // UID del usuario
    val bookId: String = "", // ID del libro
    val bookTitle: String = "", // Título del libro (para búsquedas rápidas)
    val bookAuthor: String = "", // Autor del libro
    val bookImageUrl: String? = null, // URL de la imagen
    val bookCategories: List<String> = emptyList(), // Categorías del libro
    val addedDate: Timestamp = Timestamp.now(), // Fecha cuando se añadió a favoritos
    var isAvailable: Boolean = true, // Si el libro está disponible actualmente
    val priority: Int = 0 // Prioridad del usuario (0 = normal, 1 = alta, etc.)
) {
    
    /**
     * 📅 Obtener fecha formateada
     */
    fun getFormattedAddedDate(): String {
        return try {
            val date = addedDate.toDate()
            val formatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            formatter.format(date)
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
}
