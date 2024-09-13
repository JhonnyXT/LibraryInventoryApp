package com.example.libraryinventoryapp.models

data class Book(
    val title: String = "",
    val author: String = "",
    val isbn: String = "",
    val imageUrl: String? = null,
    val status: String = "",
    val assignedTo: String? = null
)