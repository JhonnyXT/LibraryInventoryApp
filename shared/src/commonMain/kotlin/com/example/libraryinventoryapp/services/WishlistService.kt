package com.example.libraryinventoryapp.services

/**
 * 🌟 WishlistService KMP - Servicio de lista de deseos multiplataforma
 * 
 * Este servicio proporciona funcionalidades de wishlist que serán
 * implementadas específicamente para cada plataforma.
 */
expect class WishlistService {
    
    /**
     * 🔔 Iniciar monitoreo de disponibilidad de libros
     * Envía notificaciones cuando un libro deseado se vuelve disponible
     */
    suspend fun startMonitoring(userId: String): Result<Boolean>
    
    /**
     * 🛑 Detener monitoreo de disponibilidad
     */
    suspend fun stopMonitoring(): Result<Boolean>
    
    /**
     * ➕ Agregar libro a lista de deseos
     */
    suspend fun addToWishlist(userId: String, bookId: String, bookTitle: String): Result<Boolean>
    
    /**
     * ➖ Remover libro de lista de deseos
     */
    suspend fun removeFromWishlist(userId: String, bookId: String): Result<Boolean>
    
    /**
     * 📋 Obtener lista de deseos del usuario
     */
    suspend fun getUserWishlist(userId: String): Result<List<WishlistBook>>
}

/**
 * 📚 Modelo simple de libro para wishlist KMP
 */
data class WishlistBook(
    val id: String,
    val title: String,
    val author: String,
    val isAvailable: Boolean,
    val addedDate: Long // epoch milliseconds
)

/**
 * 🏭 Factory para crear instancias de WishlistService según la plataforma
 */
expect object WishlistServiceFactory {
    fun create(): WishlistService
}
