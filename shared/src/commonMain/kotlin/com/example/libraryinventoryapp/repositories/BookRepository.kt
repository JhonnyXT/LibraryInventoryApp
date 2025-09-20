package com.example.libraryinventoryapp.repositories

import com.example.libraryinventoryapp.models.Book
import kotlinx.coroutines.flow.Flow

/**
 * 📚 BookRepository - Repository de libros multiplataforma
 * 
 * FUNCIONALIDADES:
 * ✅ CRUD completo de libros con Flow reactivo
 * ✅ Búsquedas y filtros avanzados
 * ✅ Gestión de asignaciones y disponibilidad
 * ✅ Soporte para imágenes y metadatos
 */
interface BookRepository {
    
    /**
     * 📖 Obtener todos los libros como Flow reactivo
     */
    fun getAllBooks(): Flow<List<Book>>
    
    /**
     * 🔍 Obtener libro por ID
     */
    suspend fun getBookById(bookId: String): Result<Book?>
    
    /**
     * 💾 Crear nuevo libro
     */
    suspend fun createBook(book: Book): Result<String>
    
    /**
     * ✏️ Actualizar libro existente
     */
    suspend fun updateBook(book: Book): Result<Unit>
    
    /**
     * 🗑️ Eliminar libro
     */
    suspend fun deleteBook(bookId: String): Result<Unit>
    
    /**
     * 🔍 Buscar libros por criterios
     */
    suspend fun searchBooks(
        query: String,
        categories: List<String> = emptyList(),
        availability: BookAvailability? = null
    ): Result<List<Book>>
    
    /**
     * 📋 Obtener libros asignados a un usuario
     */
    fun getBooksAssignedToUser(userId: String): Flow<List<Book>>
    
    /**
     * ⏰ Obtener libros vencidos
     */
    suspend fun getOverdueBooks(): Result<List<Book>>
    
    /**
     * 👤 Asignar libro a usuario
     */
    suspend fun assignBookToUser(bookId: String, userId: String, userEmail: String, userName: String): Result<Unit>
    
    /**
     * 🔄 Desasignar libro de usuario
     */
    suspend fun unassignBookFromUser(bookId: String, userId: String): Result<Unit>
    
    /**
     * 📊 Obtener estadísticas de biblioteca
     */
    suspend fun getLibraryStatistics(): Result<LibraryStatistics>
    
    /**
     * 🏷️ Obtener todas las categorías disponibles
     */
    suspend fun getAllCategories(): Result<List<String>>
    
    /**
     * 🔄 Renovar préstamo de libro
     */
    suspend fun renewBookLoan(bookId: String, userId: String, additionalDays: Int): Result<Unit>
}

/**
 * 📊 Estadísticas de la biblioteca
 */
data class LibraryStatistics(
    val totalBooks: Int,
    val availableBooks: Int,
    val assignedBooks: Int,
    val overdueBooks: Int,
    val totalUsers: Int,
    val booksPerCategory: Map<String, Int>
)

/**
 * 📋 Estados de disponibilidad para filtros
 */
enum class BookAvailability {
    AVAILABLE,      // Disponible
    ASSIGNED,       // Asignado/Prestado
    OVERDUE,        // Vencido
    ALL             // Todos
}
