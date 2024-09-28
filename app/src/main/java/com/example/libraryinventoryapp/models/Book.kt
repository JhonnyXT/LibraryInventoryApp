package com.example.libraryinventoryapp.models

data class Book(
    var id: String = "",
    val title: String = "",
    val author: String = "",
    val isbn: String = "",
    val categories: List<String> = emptyList(),
    val imageUrl: String? = null,
    val status: String = "",
    val assignedTo: String? = null,
    val assignedToEmail: String? = null,
    val assignedWithName: String? = null
)