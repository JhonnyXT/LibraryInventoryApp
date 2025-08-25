package com.example.libraryinventoryapp.models

import com.google.firebase.Timestamp

data class OverdueBookItem(
    val book: Book,
    val userId: String,
    val userName: String,
    val userEmail: String,
    val expirationDate: Timestamp,
    val assignedDate: Timestamp?,
    val daysOverdue: Int
)
