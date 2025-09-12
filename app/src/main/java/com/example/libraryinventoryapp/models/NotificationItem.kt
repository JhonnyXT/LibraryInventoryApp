package com.example.libraryinventoryapp.models

import com.google.firebase.Timestamp
import java.util.Date

/**
 * 🔔 Modelo de notificación para bandeja de entrada del usuario
 * Diseño profesional basado en Material Design 3
 */
data class NotificationItem(
    val id: String = "",
    val bookId: String = "",
    val bookTitle: String = "",
    val bookAuthor: String = "",
    val userId: String = "",
    val userName: String = "",
    val daysUntilDue: Int = 0, // Positivo = próximo, negativo = vencido, 0 = hoy
    val expirationDate: Timestamp? = null,
    val type: NotificationType = NotificationType.UPCOMING,
    val isRead: Boolean = false,
    val timestamp: Timestamp = Timestamp.now()
) {
    /**
     * 🎨 Obtiene el color de fondo según urgencia (Material Design 3)
     */
    fun getBackgroundColor(): String {
        return when (type) {
            NotificationType.CRITICAL -> "#FFEBEE"      // Rojo muy claro
            NotificationType.URGENT -> "#FFF3E0"        // Naranja muy claro  
            NotificationType.DUE_TODAY -> "#E8F5E8"     // Verde muy claro
            NotificationType.UPCOMING -> "#E3F2FD"      // Azul muy claro
            NotificationType.INFO -> "#F5F5F5"          // Gris muy claro
        }
    }
    
    /**
     * 🎯 Obtiene el color del texto principal
     */
    fun getTextColor(): String {
        return when (type) {
            NotificationType.CRITICAL -> "#C62828"      // Rojo oscuro
            NotificationType.URGENT -> "#E65100"        // Naranja oscuro
            NotificationType.DUE_TODAY -> "#2E7D32"     // Verde oscuro
            NotificationType.UPCOMING -> "#1565C0"      // Azul oscuro
            NotificationType.INFO -> "#424242"          // Gris oscuro
        }
    }
    
    /**
     * 🔥 Obtiene el ícono según el tipo
     */
    fun getIconResource(): Int {
        return when (type) {
            NotificationType.CRITICAL -> android.R.drawable.ic_dialog_alert
            NotificationType.URGENT -> android.R.drawable.ic_dialog_info
            NotificationType.DUE_TODAY -> android.R.drawable.ic_menu_today
            NotificationType.UPCOMING -> android.R.drawable.ic_menu_recent_history
            NotificationType.INFO -> android.R.drawable.ic_dialog_info
        }
    }
    
    /**
     * ✨ Genera el mensaje personalizado - CONSISTENTE con AssignedBooksAdapter
     */
    fun getMessage(): String {
        return when {
            daysUntilDue > 0 -> "Tu préstamo vence en $daysUntilDue días - '$bookTitle'"
            daysUntilDue == 0 -> "⚠️ Tu préstamo VENCE HOY - '$bookTitle'"
            daysUntilDue < 0 -> {
                val overdueDays = kotlin.math.abs(daysUntilDue)
                "🔴 Tu préstamo está vencido hace $overdueDays días - '$bookTitle'"
            }
            else -> "Información sobre '$bookTitle'"
        }
    }
    
    /**
     * 🏷️ Genera el título de la notificación - Consistente con AssignedBooksAdapter
     */
    fun getTitle(): String {
        return when {
            daysUntilDue < -7 -> "🚨 CRÍTICO - Devolución Inmediata"
            daysUntilDue < 0 -> "🔴 Libro Vencido"
            daysUntilDue == 0 -> "🔥 Vence HOY"
            daysUntilDue <= 5 -> "⚠️ Próximo Vencimiento"
            else -> "ℹ️ Información"
        }
    }
}

/**
 * 🎨 Enum para tipos de notificación (ordenados por prioridad)
 */
enum class NotificationType(val priority: Int) {
    CRITICAL(5),      // +7 días vencido
    URGENT(4),        // 1-6 días vencido  
    DUE_TODAY(3),     // Vence hoy
    UPCOMING(2),      // 1-5 días próximos
    INFO(1)           // Información general
}
