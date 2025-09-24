package com.example.libraryinventoryapp.models

import com.google.firebase.Timestamp

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
    val assignedDates: List<Timestamp>? = null, // Fechas de asignación por usuario
    val loanExpirationDates: List<Timestamp>? = null, // Fechas de vencimiento por usuario
    val createdDate: Timestamp? = null, // Fecha de creación del libro
    val lastEditedDate: Timestamp? = null // Fecha de última edición
) {
    // Función para verificar si el libro está vencido
    fun isOverdue(): Boolean {
        val currentTime = Timestamp.now()
        return loanExpirationDates?.any { it.toDate().before(currentTime.toDate()) } ?: false
    }

    // Función para obtener la cantidad disponible
    fun getAvailableQuantity(): Int {
        val assignedCount = assignedTo?.size ?: 0
        return quantity - assignedCount
    }

    // Función para verificar si hay préstamos próximos a vencer
    fun hasUpcomingDueLoans(): Boolean {
        val threeDaysFromNow = Timestamp(java.util.Date(System.currentTimeMillis() + 3 * 24 * 60 * 60 * 1000))
        return loanExpirationDates?.any { 
            it.toDate().after(Timestamp.now().toDate()) && it.toDate().before(threeDaysFromNow.toDate()) 
        } ?: false
    }
}
