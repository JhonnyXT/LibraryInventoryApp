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
    val loanExpirationDates: List<Timestamp>? = null, // NUEVO: Fechas de vencimiento por usuario
    val createdDate: Timestamp? = null, // NUEVO: Fecha de creación del libro
    val lastEditedDate: Timestamp? = null // NUEVO: Fecha de última edición
)
