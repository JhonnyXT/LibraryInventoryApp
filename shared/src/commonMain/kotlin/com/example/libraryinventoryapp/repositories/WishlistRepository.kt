package com.example.libraryinventoryapp.repositories

import com.example.libraryinventoryapp.models.WishlistItem
import kotlinx.coroutines.flow.Flow

/**
 * 🌟 WishlistRepository - Repository de lista de deseos multiplataforma
 * 
 * FUNCIONALIDADES:
 * ✅ Gestión completa de wishlist con Firebase
 * ✅ Monitoreo de disponibilidad en tiempo real
 * ✅ Notificaciones automáticas
 * ✅ Limpieza automática al asignar libros
 */
interface WishlistRepository {
    
    /**
     * 📋 Obtener wishlist de usuario como Flow reactivo
     */
    fun getUserWishlist(userId: String): Flow<List<WishlistItem>>
    
    /**
     * ➕ Agregar libro a wishlist
     */
    suspend fun addToWishlist(
        userId: String, 
        bookId: String, 
        bookTitle: String, 
        bookAuthor: String,
        bookImageUrl: String? = null,
        bookCategories: List<String> = emptyList(),
        priority: Int = 0
    ): Result<String>
    
    /**
     * 🗑️ Remover libro de wishlist
     */
    suspend fun removeFromWishlist(userId: String, bookId: String): Result<Unit>
    
    /**
     * 🗑️ Remover item específico por ID
     */
    suspend fun removeWishlistItem(wishlistItemId: String): Result<Unit>
    
    /**
     * ✏️ Actualizar prioridad de item
     */
    suspend fun updateItemPriority(wishlistItemId: String, priority: Int): Result<Unit>
    
    /**
     * 🔄 Actualizar disponibilidad de item
     */
    suspend fun updateItemAvailability(wishlistItemId: String, isAvailable: Boolean): Result<Unit>
    
    /**
     * 📊 Verificar si libro está en wishlist del usuario
     */
    suspend fun isBookInWishlist(userId: String, bookId: String): Result<Boolean>
    
    /**
     * 🔍 Obtener item específico de wishlist
     */
    suspend fun getWishlistItem(userId: String, bookId: String): Result<WishlistItem?>
    
    /**
     * 📊 Obtener estadísticas de wishlist
     */
    suspend fun getWishlistStatistics(userId: String): Result<WishlistStatistics>
    
    /**
     * 🔔 Obtener todos los usuarios que tienen un libro específico en su wishlist
     */
    suspend fun getUsersWithBookInWishlist(bookId: String): Result<List<String>>
    
    /**
     * 🧹 Limpiar wishlist del usuario (remover libros ya asignados)
     */
    suspend fun cleanupUserWishlist(userId: String): Result<Int>
    
    /**
     * 🚨 Obtener libros de alta prioridad no disponibles
     */
    suspend fun getHighPriorityUnavailableBooks(userId: String): Result<List<WishlistItem>>
    
    /**
     * 📈 Obtener libros más deseados (con más apariciones en wishlists)
     */
    suspend fun getMostWantedBooks(limit: Int = 10): Result<List<WishlistBookStats>>
}

/**
 * 📊 Estadísticas de wishlist del usuario
 */
data class WishlistStatistics(
    val totalItems: Int,
    val availableItems: Int,
    val unavailableItems: Int,
    val highPriorityItems: Int,
    val oldestItemDate: Long?, // timestamp del item más antiguo
    val averagePriority: Double
)

/**
 * 📈 Estadísticas de popularidad de libro en wishlists
 */
data class WishlistBookStats(
    val bookId: String,
    val bookTitle: String,
    val bookAuthor: String,
    val totalWishes: Int,
    val averagePriority: Double,
    val isCurrentlyAvailable: Boolean
)
