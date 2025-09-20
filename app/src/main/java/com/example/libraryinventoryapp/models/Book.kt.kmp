package com.example.libraryinventoryapp.models

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class Book(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val author: String = "",
    val isbn: String = "",
    val categories: List<String> = emptyList(),
    val imageUrl: String? = null,
    val quantity: Int = 0,
    val status: String = "Disponible",
    val assignedTo: List<String>? = null,  // Arreglo de IDs de usuarios
    val assignedWithNames: List<String>? = null, // Arreglo de nombres de usuarios
    val assignedToEmails: List<String>? = null, // Arreglo de correos electrónicos
    val assignedDates: List<Long>? = null, // Fechas de asignación como timestamps (epoch milliseconds)
    val loanExpirationDates: List<Long>? = null, // Fechas de vencimiento como timestamps
    val createdDate: Long? = null, // Fecha de creación como timestamp
    val lastEditedDate: Long? = null // Fecha de última edición como timestamp
) {
    
    /**
     * ✅ Verificar si el libro está vencido
     */
    fun hasOverdueLoans(): Boolean {
        val currentTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        return loanExpirationDates?.any { it < currentTime } ?: false
    }
    
    /**
     * 📊 Obtener cantidad disponible
     */
    fun getAvailableQuantity(): Int {
        return quantity - (assignedTo?.size ?: 0)
    }
    
    /**
     * 📅 Verificar si hay préstamos que vencen pronto (próximos 5 días)
     */
    fun hasUpcomingDueLoans(daysAhead: Int = 5): Boolean {
        val currentTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        val futureTime = currentTime + (daysAhead * 24 * 60 * 60 * 1000) // días en milisegundos
        
        return loanExpirationDates?.any { expirationTime ->
            expirationTime in currentTime..futureTime
        } ?: false
    }
    
    /**
     * 🏷️ Obtener categoría principal (primera de la lista)
     */
    fun getPrimaryCategory(): String {
        return categories.firstOrNull() ?: "Sin categoría"
    }
    
    /**
     * ✨ Verificar si el libro está disponible para préstamo
     */
    fun isAvailableForLoan(): Boolean {
        return status == "Disponible" && getAvailableQuantity() > 0
    }
    
    /**
     * 📚 Obtener estado extendido del libro
     */
    fun getExtendedStatus(): String {
        return when {
            !isAvailableForLoan() -> "No disponible"
            hasOverdueLoans() -> "Con préstamos vencidos"
            hasUpcomingDueLoans() -> "Préstamos próximos a vencer"
            assignedTo?.isNotEmpty() == true -> "Parcialmente prestado"
            else -> "Disponible"
        }
    }
}
