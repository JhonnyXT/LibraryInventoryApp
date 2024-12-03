package com.example.libraryinventoryapp.models

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
    val assignedToEmails: List<String>? = null // Arreglo de correos electrónicos
)
