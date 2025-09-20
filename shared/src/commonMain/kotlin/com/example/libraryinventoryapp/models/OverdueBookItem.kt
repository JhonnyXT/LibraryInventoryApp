package com.example.libraryinventoryapp.models

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Serializable
data class OverdueBookItem(
    val book: Book,
    val userId: String,
    val userName: String,
    val userEmail: String,
    val expirationDate: Long, // Timestamp en milliseconds
    val assignedDate: Long?, // Timestamp en milliseconds
    val daysOverdue: Int
) {
    
    /**
     * 📅 Obtener fecha de vencimiento formateada
     */
    fun getFormattedExpirationDate(): String {
        return try {
            val instant = Instant.fromEpochMilliseconds(expirationDate)
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            
            val day = localDateTime.dayOfMonth.toString().padStart(2, '0')
            val month = localDateTime.monthNumber.toString().padStart(2, '0')
            val year = localDateTime.year.toString()
            
            "$day/$month/$year"
        } catch (e: Exception) {
            "Sin fecha"
        }
    }
    
    /**
     * 📅 Obtener fecha de asignación formateada
     */
    fun getFormattedAssignedDate(): String {
        return try {
            assignedDate?.let { timestamp ->
                val instant = Instant.fromEpochMilliseconds(timestamp)
                val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                
                val day = localDateTime.dayOfMonth.toString().padStart(2, '0')
                val month = localDateTime.monthNumber.toString().padStart(2, '0')
                val year = localDateTime.year.toString()
                
                "$day/$month/$year"
            } ?: "Sin fecha de asignación"
        } catch (e: Exception) {
            "Sin fecha de asignación"
        }
    }
    
    /**
     * 🚨 Obtener nivel de urgencia basado en días vencidos
     */
    fun getUrgencyLevel(): OverdueUrgencyLevel {
        return when {
            daysOverdue >= 30 -> OverdueUrgencyLevel.CRITICAL
            daysOverdue >= 14 -> OverdueUrgencyLevel.HIGH
            daysOverdue >= 7 -> OverdueUrgencyLevel.MEDIUM
            else -> OverdueUrgencyLevel.LOW
        }
    }
    
    /**
     * 🎨 Obtener color según urgencia
     */
    fun getUrgencyColor(): String {
        return when (getUrgencyLevel()) {
            OverdueUrgencyLevel.CRITICAL -> "#D32F2F"  // Rojo intenso
            OverdueUrgencyLevel.HIGH -> "#F57C00"      // Naranja
            OverdueUrgencyLevel.MEDIUM -> "#FBC02D"    // Amarillo
            OverdueUrgencyLevel.LOW -> "#388E3C"       // Verde
        }
    }
    
    /**
     * 📝 Obtener mensaje de urgencia
     */
    fun getUrgencyMessage(): String {
        return when (getUrgencyLevel()) {
            OverdueUrgencyLevel.CRITICAL -> "🚨 CRÍTICO - Contactar inmediatamente"
            OverdueUrgencyLevel.HIGH -> "⚠️ ALTO - Recordatorio urgente"
            OverdueUrgencyLevel.MEDIUM -> "📋 MEDIO - Enviar recordatorio"
            OverdueUrgencyLevel.LOW -> "ℹ️ BAJO - Recordatorio suave"
        }
    }
    
    /**
     * 📊 Calcular días de préstamo total
     */
    fun getTotalLoanDays(): Int? {
        return assignedDate?.let { assigned ->
            val diffMillis = expirationDate - assigned
            (diffMillis / (24 * 60 * 60 * 1000)).toInt()
        }
    }
    
    /**
     * 🎯 Obtener resumen completo
     */
    fun getSummary(): String {
        val urgency = getUrgencyMessage()
        val totalDays = getTotalLoanDays()?.let { " (préstamo de $it días)" } ?: ""
        return "$urgency - Vencido hace $daysOverdue días$totalDays"
    }
}

/**
 * 🚨 Niveles de urgencia para libros vencidos
 */
@Serializable
enum class OverdueUrgencyLevel {
    LOW,      // 1-6 días
    MEDIUM,   // 7-13 días  
    HIGH,     // 14-29 días
    CRITICAL  // 30+ días
}
