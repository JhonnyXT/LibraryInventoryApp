package com.example.libraryinventoryapp.utils

import kotlinx.datetime.*

/**
 * 📅 Utilidades de fecha y hora compartidas para KMP
 * Reemplaza las funciones específicas de Android/Firebase
 */
object DateUtils {
    
    /**
     * 🔄 Convertir timestamp (epoch milliseconds) a Instant
     */
    fun timestampToInstant(timestamp: Long): Instant {
        return Instant.fromEpochMilliseconds(timestamp)
    }
    
    /**
     * 🔄 Obtener timestamp actual
     */
    fun currentTimestamp(): Long {
        return Clock.System.now().toEpochMilliseconds()
    }
    
    /**
     * 📅 Formatear fecha en formato dd/MM/yyyy
     */
    fun formatDate(timestamp: Long): String {
        return try {
            val instant = Instant.fromEpochMilliseconds(timestamp)
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
     * 📅 Formatear fecha y hora en formato dd/MM/yyyy HH:mm
     */
    fun formatDateTime(timestamp: Long): String {
        return try {
            val instant = Instant.fromEpochMilliseconds(timestamp)
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            
            val day = localDateTime.dayOfMonth.toString().padStart(2, '0')
            val month = localDateTime.monthNumber.toString().padStart(2, '0')
            val year = localDateTime.year.toString()
            val hour = localDateTime.hour.toString().padStart(2, '0')
            val minute = localDateTime.minute.toString().padStart(2, '0')
            
            "$day/$month/$year $hour:$minute"
        } catch (e: Exception) {
            "Sin fecha"
        }
    }
    
    /**
     * ⏱️ Calcular diferencia en días entre dos timestamps
     */
    fun daysBetween(startTimestamp: Long, endTimestamp: Long): Int {
        return try {
            val startInstant = Instant.fromEpochMilliseconds(startTimestamp)
            val endInstant = Instant.fromEpochMilliseconds(endTimestamp)
            
            val startDate = startInstant.toLocalDateTime(TimeZone.currentSystemDefault()).date
            val endDate = endInstant.toLocalDateTime(TimeZone.currentSystemDefault()).date
            
            endDate.toEpochDays() - startDate.toEpochDays()
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * 🔍 Verificar si una fecha está en el pasado
     */
    fun isInPast(timestamp: Long): Boolean {
        return timestamp < currentTimestamp()
    }
    
    /**
     * 🔮 Verificar si una fecha está en el futuro
     */
    fun isInFuture(timestamp: Long): Boolean {
        return timestamp > currentTimestamp()
    }
    
    /**
     * 📊 Obtener días hasta el vencimiento (negativo si ya venció)
     */
    fun daysUntilExpiration(expirationTimestamp: Long): Int {
        val current = currentTimestamp()
        return daysBetween(current, expirationTimestamp)
    }
    
    /**
     * ⭐ Obtener tiempo relativo legible (hace X tiempo)
     */
    fun getRelativeTime(timestamp: Long): String {
        val now = currentTimestamp()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "Hace un momento"
            diff < 3_600_000 -> "Hace ${diff / 60_000} min"
            diff < 86_400_000 -> "Hace ${diff / 3_600_000} h"
            diff < 604_800_000 -> "Hace ${diff / 86_400_000} días"
            else -> "Hace ${diff / 604_800_000} semanas"
        }
    }
    
    /**
     * 🕐 Agregar días a un timestamp
     */
    fun addDays(timestamp: Long, days: Int): Long {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val newDate = localDateTime.date.plus(DatePeriod(days = days))
        val newDateTime = newDate.atTime(localDateTime.time)
        return newDateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    }
}
