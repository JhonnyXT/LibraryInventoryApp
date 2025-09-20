package com.example.libraryinventoryapp.services

import com.example.libraryinventoryapp.models.Book
import com.example.libraryinventoryapp.models.WishlistItem

/**
 * 🔔 NotificationService - Servicio de notificaciones multiplataforma
 * 
 * FUNCIONALIDADES:
 * ✅ Push notifications específicas de cada plataforma
 * ✅ Canales diferenciados por tipo de notificación
 * ✅ Programación inteligente de recordatorios
 * ✅ Gestión de permisos multiplataforma
 */
interface NotificationService {
    
    /**
     * 🔧 Inicializar servicio de notificaciones
     */
    suspend fun initialize(): Result<Unit>
    
    /**
     * 📚 Notificación de libro disponible en wishlist
     */
    suspend fun notifyBookAvailable(
        book: Book,
        availableCount: Int,
        targetUserId: String? = null
    ): Result<Unit>
    
    /**
     * 🎉 Notificación de libro asignado (removido de wishlist)
     */
    suspend fun notifyBookAssignedFromWishlist(
        wishlistItem: WishlistItem,
        targetUserId: String
    ): Result<Unit>
    
    /**
     * ⏰ Notificación de recordatorio de devolución
     */
    suspend fun notifyBookDueReminder(
        book: Book,
        userId: String,
        daysUntilDue: Int,
        urgencyLevel: ReminderUrgency
    ): Result<Unit>
    
    /**
     * 🚨 Notificación de libro vencido
     */
    suspend fun notifyBookOverdue(
        book: Book,
        userId: String,
        daysOverdue: Int,
        urgencyLevel: OverdueUrgency
    ): Result<Unit>
    
    /**
     * 🎯 Programar recordatorios automáticos
     */
    suspend fun scheduleBookDueReminders(
        book: Book,
        userId: String,
        loanExpirationDate: Long
    ): Result<Unit>
    
    /**
     * ❌ Cancelar recordatorios programados
     */
    suspend fun cancelBookReminders(bookId: String, userId: String): Result<Unit>
    
    /**
     * 🔔 Enviar notificación general
     */
    suspend fun sendGeneralNotification(
        title: String,
        message: String,
        targetUserId: String? = null,
        notificationType: NotificationType = NotificationType.INFO
    ): Result<Unit>
    
    /**
     * 📱 Verificar permisos de notificación
     */
    suspend fun checkNotificationPermissions(): NotificationPermissionStatus
    
    /**
     * 🔐 Solicitar permisos de notificación
     */
    suspend fun requestNotificationPermissions(): Result<Boolean>
    
    /**
     * 📊 Obtener estadísticas de notificaciones
     */
    suspend fun getNotificationStats(): Result<NotificationStats>
    
    /**
     * 🧹 Limpiar notificaciones antigas
     */
    suspend fun clearOldNotifications(olderThanDays: Int = 30): Result<Unit>
}

/**
 * ⚡ Niveles de urgencia para recordatorios
 */
enum class ReminderUrgency(val daysInAdvance: Int, val frequency: Int) {
    LOW(3, 1),      // 3 días antes, 1 vez al día
    MEDIUM(2, 2),   // 2 días antes, 2 veces al día
    HIGH(1, 3),     // 1 día antes, 3 veces al día
    CRITICAL(0, 6)  // Día del vencimiento, 6 veces al día
}

/**
 * 🚨 Niveles de urgencia para libros vencidos
 */
enum class OverdueUrgency(val daysOverdue: Int, val frequency: Int) {
    RECENT(1, 2),    // 1-3 días vencido, 2 veces al día
    MEDIUM(4, 3),    // 4-7 días vencido, 3 veces al día
    HIGH(8, 4),      // 8-14 días vencido, 4 veces al día
    CRITICAL(15, 6)  // +15 días vencido, 6 veces al día
}

/**
 * 🎯 Tipos de notificación
 */
enum class NotificationType {
    INFO,       // Información general
    REMINDER,   // Recordatorio de devolución
    OVERDUE,    // Libro vencido
    WISHLIST,   // Disponibilidad en wishlist
    ASSIGNMENT, // Asignación de libro
    SYSTEM      // Notificación del sistema
}

/**
 * 🔐 Estados de permisos de notificación
 */
enum class NotificationPermissionStatus {
    GRANTED,        // Concedido
    DENIED,         // Denegado
    NOT_REQUESTED,  // No solicitado aún
    RESTRICTED      // Restringido por el sistema
}

/**
 * 📊 Estadísticas de notificaciones
 */
data class NotificationStats(
    val totalSent: Int,
    val totalDelivered: Int,
    val totalClicked: Int,
    val pendingScheduled: Int,
    val lastNotificationSent: Long?, // timestamp
    val notificationsByType: Map<NotificationType, Int>
)

/**
 * 🏭 Factory para crear NotificationService específico de plataforma
 */
expect class NotificationServiceFactory() {
    fun create(): NotificationService
}
