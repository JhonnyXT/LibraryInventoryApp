package com.example.libraryinventoryapp.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.firebase.Timestamp
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 🔔 LibraryNotificationManager - Sistema Híbrido de Notificaciones
 * 
 * FUNCIONALIDADES:
 * ✅ Notificaciones inmediatas al cambiar fechas
 * ✅ Horarios escalados por urgencia del préstamo
 * ✅ Escalamiento inteligente para libros vencidos
 * ✅ Cancelación específica por libro/usuario
 * ✅ Reprogramación automática después de reinicio
 */
class LibraryNotificationManager(private val context: Context) {

    companion object {
        private const val TAG = "LibraryNotificationManager"
        
        // 📋 IDs de Canales de Notificación
        const val CHANNEL_ID_UPCOMING = "book_loans_upcoming"
        const val CHANNEL_ID_OVERDUE = "book_loans_overdue"
        const val CHANNEL_ID_CRITICAL = "book_loans_critical"
        
        // 🎯 Request Codes para identificar notificaciones únicas
        private const val REQUEST_CODE_BASE = 1000
        private const val REQUEST_CODE_UPCOMING_3DAY = 1
        private const val REQUEST_CODE_UPCOMING_1DAY = 2
        private const val REQUEST_CODE_DUE_TODAY_AM = 3
        private const val REQUEST_CODE_DUE_TODAY_PM = 4
        private const val REQUEST_CODE_OVERDUE_1DAY_AM = 5
        private const val REQUEST_CODE_OVERDUE_1DAY_PM = 6
        private const val REQUEST_CODE_OVERDUE_FREQUENT = 7
        private const val REQUEST_CODE_OVERDUE_CRITICAL = 8
        
        // ⏰ Horarios optimizados para notificaciones
        private const val HOUR_MORNING_REMINDER = 10      // 10:00 AM
        private const val HOUR_AFTERNOON_REMINDER = 18    // 6:00 PM
        private const val HOUR_DUE_MORNING = 9           // 9:00 AM 
        private const val HOUR_DUE_AFTERNOON = 18        // 6:00 PM
        private const val HOUR_OVERDUE_MORNING = 10      // 10:00 AM
        private const val HOUR_OVERDUE_AFTERNOON = 16    // 4:00 PM
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    /**
     * 📺 Crear canales de notificación
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            
            // 📅 Canal para próximos vencimientos
            val upcomingChannel = NotificationChannel(
                CHANNEL_ID_UPCOMING,
                "Próximos Vencimientos",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Recordatorios de libros próximos a vencer"
                enableVibration(true)
                setShowBadge(true)
            }

            // ⚠️ Canal para libros vencidos
            val overdueChannel = NotificationChannel(
                CHANNEL_ID_OVERDUE,
                "Libros Vencidos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de libros vencidos"
                enableVibration(true)
                setShowBadge(true)
            }

            // 🚨 Canal para casos críticos
            val criticalChannel = NotificationChannel(
                CHANNEL_ID_CRITICAL,
                "Crítico - Devolución Urgente",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Libros críticos - más de 7 días vencidos"
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(
                listOf(upcomingChannel, overdueChannel, criticalChannel)
            )
            
            Log.d(TAG, "🔔 Canales de notificación creados exitosamente")
        }
    }

    /**
     * 🎯 SISTEMA HÍBRIDO: Programar notificaciones con escalamiento inteligente
     */
    fun scheduleBookLoanNotifications(
        bookId: String,
        bookTitle: String,
        bookAuthor: String,
        userId: String,
        userName: String,
        expirationDate: Timestamp
    ) {
        Log.d(TAG, "🔔 === INICIANDO SISTEMA HÍBRIDO DE NOTIFICACIONES ===")
        Log.d(TAG, "📚 Libro: $bookTitle | 👤 Usuario: $userName")
        Log.d(TAG, "📅 Vencimiento: ${Date(expirationDate.seconds * 1000)}")

        val currentTime = System.currentTimeMillis()
        val expirationTime = expirationDate.seconds * 1000
        val daysUntilDue = TimeUnit.MILLISECONDS.toDays(expirationTime - currentTime).toInt()

        Log.d(TAG, "⏰ Días hasta vencimiento: $daysUntilDue")

        // 🎯 Escalamiento inteligente por urgencia
        when {
            daysUntilDue in 3..5 -> {
                Log.d(TAG, "📅 CATEGORÍA: Próximo (3-5 días) - 1 vez al día")
                scheduleUpcomingReminder(bookId, userId, bookTitle, bookAuthor, userName, expirationTime, 3)
            }
            
            daysUntilDue in 1..2 -> {
                Log.d(TAG, "⚠️ CATEGORÍA: Muy próximo (1-2 días) - 1 vez al día")
                scheduleUpcoming1DayReminder(bookId, userId, bookTitle, bookAuthor, userName, expirationTime)
            }
            
            daysUntilDue == 0 -> {
                Log.d(TAG, "🚨 CATEGORÍA: Vence HOY - 2 veces (mañana y tarde)")
                scheduleDueTodayReminders(bookId, userId, bookTitle, bookAuthor, userName, expirationTime)
            }
            
            daysUntilDue in -3..-1 -> {
                Log.d(TAG, "🔴 CATEGORÍA: Vencido reciente (1-3 días) - 2 veces al día")
                scheduleOverdueRecentReminders(bookId, userId, bookTitle, bookAuthor, userName, expirationTime, Math.abs(daysUntilDue))
            }
            
            daysUntilDue in -7..-4 -> {
                Log.d(TAG, "🔥 CATEGORÍA: Vencido medio (4-7 días) - Cada 8 horas")
                scheduleOverdueFrequentReminders(bookId, userId, bookTitle, bookAuthor, userName, expirationTime, Math.abs(daysUntilDue))
            }
            
            daysUntilDue < -7 -> {
                Log.d(TAG, "🚨 CATEGORÍA: CRÍTICO (+7 días vencido) - Cada 4 horas")
                scheduleOverdueCriticalReminders(bookId, userId, bookTitle, bookAuthor, userName, expirationTime, Math.abs(daysUntilDue))
            }
            
            else -> {
                Log.d(TAG, "📝 Libro muy lejano al vencimiento ($daysUntilDue días) - Sin notificaciones por ahora")
            }
        }

        // ⚡ NOTIFICACIÓN INMEDIATA si está próximo a vencer
        if (daysUntilDue <= 2) {
            scheduleImmediateNotification(bookId, userId, bookTitle, userName, daysUntilDue)
        }

        Log.d(TAG, "✅ Sistema híbrido configurado exitosamente para: $bookTitle")
    }

    /**
     * 📅 Recordatorio para libros próximos (3-5 días)
     */
    private fun scheduleUpcomingReminder(
        bookId: String, userId: String, bookTitle: String, bookAuthor: String, userName: String,
        expirationTime: Long, daysRemaining: Int
    ) {
        val reminderTime = calculateTimeAtHour(
            expirationTime - TimeUnit.DAYS.toMillis(daysRemaining.toLong()),
            HOUR_MORNING_REMINDER, 0
        )

        scheduleNotification(
            bookId, userId, REQUEST_CODE_UPCOMING_3DAY,
            reminderTime,
            "📅 Recordatorio de Biblioteca",
            "📚 \"$bookTitle\" vence en $daysRemaining días. ¡No olvides devolverlo!",
            CHANNEL_ID_UPCOMING,
            bookTitle, bookAuthor, userName, daysRemaining
        )
        
        Log.d(TAG, "📅 Programado recordatorio 3-5 días: ${Date(reminderTime)}")
    }

    /**
     * ⚠️ Recordatorio para libros muy próximos (1-2 días)
     */
    private fun scheduleUpcoming1DayReminder(
        bookId: String, userId: String, bookTitle: String, bookAuthor: String, userName: String,
        expirationTime: Long
    ) {
        val reminderTime = calculateTimeAtHour(
            expirationTime - TimeUnit.DAYS.toMillis(1),
            HOUR_AFTERNOON_REMINDER, 0
        )

        scheduleNotification(
            bookId, userId, REQUEST_CODE_UPCOMING_1DAY,
            reminderTime,
            "⚠️ Biblioteca - ¡Próximo Vencimiento!",
            "📚 \"$bookTitle\" vence MAÑANA. ¡Prepárate para devolverlo!",
            CHANNEL_ID_UPCOMING,
            bookTitle, bookAuthor, userName, 1
        )
        
        Log.d(TAG, "⚠️ Programado recordatorio 1-2 días: ${Date(reminderTime)}")
    }

    /**
     * 🚨 Recordatorios para día de vencimiento (2 veces)
     */
    private fun scheduleDueTodayReminders(
        bookId: String, userId: String, bookTitle: String, bookAuthor: String, userName: String,
        expirationTime: Long
    ) {
        // 🌅 Recordatorio matutino
        val morningTime = calculateTimeAtHour(expirationTime, HOUR_DUE_MORNING, 0)
        scheduleNotification(
            bookId, userId, REQUEST_CODE_DUE_TODAY_AM,
            morningTime,
            "🚨 ¡VENCE HOY! - Biblioteca",
            "📚 \"$bookTitle\" vence HOY. ¡Devuélvelo antes del cierre!",
            CHANNEL_ID_OVERDUE,
            bookTitle, bookAuthor, userName, 0
        )

        // 🌆 Recordatorio vespertino
        val afternoonTime = calculateTimeAtHour(expirationTime, HOUR_DUE_AFTERNOON, 0)
        scheduleNotification(
            bookId, userId, REQUEST_CODE_DUE_TODAY_PM,
            afternoonTime,
            "🚨 ¡ÚLTIMO RECORDATORIO! - Biblioteca",
            "📚 \"$bookTitle\" vence HOY. ¡Aún puedes devolverlo!",
            CHANNEL_ID_OVERDUE,
            bookTitle, bookAuthor, userName, 0
        )
        
        Log.d(TAG, "🚨 Programados 2 recordatorios para día de vencimiento")
    }

    /**
     * 🔴 Recordatorios para vencidos recientes (1-3 días) - 2 veces al día
     */
    private fun scheduleOverdueRecentReminders(
        bookId: String, userId: String, bookTitle: String, bookAuthor: String, userName: String,
        expirationTime: Long, daysOverdue: Int
    ) {
        val baseTime = System.currentTimeMillis()

        // 🌅 Recordatorio matutino
        val morningTime = calculateTimeAtHour(baseTime, HOUR_OVERDUE_MORNING, 0)
        scheduleNotification(
            bookId, userId, REQUEST_CODE_OVERDUE_1DAY_AM,
            morningTime,
            "🔴 Libro VENCIDO - Biblioteca",
            "📚 \"$bookTitle\" lleva $daysOverdue día(s) vencido. ¡Devuélvelo pronto!",
            CHANNEL_ID_OVERDUE,
            bookTitle, bookAuthor, userName, -daysOverdue
        )

        // 🌆 Recordatorio vespertino
        val afternoonTime = calculateTimeAtHour(baseTime, HOUR_OVERDUE_AFTERNOON, 0)
        scheduleNotification(
            bookId, userId, REQUEST_CODE_OVERDUE_1DAY_PM,
            afternoonTime,
            "🔴 Recordatorio VENCIDO - Biblioteca",
            "📚 \"$bookTitle\" está vencido hace $daysOverdue día(s). ¡Devuélvelo!",
            CHANNEL_ID_OVERDUE,
            bookTitle, bookAuthor, userName, -daysOverdue
        )
        
        Log.d(TAG, "🔴 Programados 2 recordatorios diarios para vencido reciente ($daysOverdue días)")
    }

    /**
     * 🔥 Recordatorios frecuentes para vencidos medios (4-7 días) - Cada 8 horas
     */
    private fun scheduleOverdueFrequentReminders(
        bookId: String, userId: String, bookTitle: String, bookAuthor: String, userName: String,
        expirationTime: Long, daysOverdue: Int
    ) {
        val baseTime = System.currentTimeMillis()
        val intervalMillis = TimeUnit.HOURS.toMillis(8) // Cada 8 horas

        // 🔄 Programar notificaciones cada 8 horas
        for (i in 0 until 3) { // 3 veces al día (cada 8 horas)
            val notificationTime = baseTime + (intervalMillis * i)
            
            scheduleNotification(
                bookId, userId, REQUEST_CODE_OVERDUE_FREQUENT + i,
                notificationTime,
                "🔥 URGENTE - Libro Vencido",
                "📚 \"$bookTitle\" lleva $daysOverdue días vencido. ¡DEVUÉLVELO YA!",
                CHANNEL_ID_OVERDUE,
                bookTitle, bookAuthor, userName, -daysOverdue
            )
        }
        
        Log.d(TAG, "🔥 Programadas 3 notificaciones cada 8 horas para vencido medio ($daysOverdue días)")
    }

    /**
     * 🚨 Recordatorios críticos para muy vencidos (+7 días) - Cada 4 horas
     */
    private fun scheduleOverdueCriticalReminders(
        bookId: String, userId: String, bookTitle: String, bookAuthor: String, userName: String,
        expirationTime: Long, daysOverdue: Int
    ) {
        val baseTime = System.currentTimeMillis()
        val intervalMillis = TimeUnit.HOURS.toMillis(4) // Cada 4 horas

        // 🚨 Programar notificaciones cada 4 horas
        for (i in 0 until 6) { // 6 veces al día (cada 4 horas)
            val notificationTime = baseTime + (intervalMillis * i)
            
            scheduleNotification(
                bookId, userId, REQUEST_CODE_OVERDUE_CRITICAL + i,
                notificationTime,
                "🚨 CRÍTICO - Devolución URGENTE",
                "📚 \"$bookTitle\" está CRÍTICO: $daysOverdue días vencido. ¡ACCIÓN INMEDIATA!",
                CHANNEL_ID_CRITICAL,
                bookTitle, bookAuthor, userName, -daysOverdue
            )
        }
        
        Log.d(TAG, "🚨 Programadas 6 notificaciones cada 4 horas para crítico ($daysOverdue días)")
    }

    /**
     * ⚡ Notificación inmediata para cambios de fecha próximos a vencer
     */
    fun scheduleImmediateNotification(
        bookId: String, userId: String, bookTitle: String, userName: String,
        daysUntilDue: Int
    ) {
        val message = when {
            daysUntilDue == 0 -> "📚 \"$bookTitle\" VENCE HOY. ¡Devuélvelo!"
            daysUntilDue == 1 -> "📚 \"$bookTitle\" vence MAÑANA. ¡Prepárate!"
            daysUntilDue == 2 -> "📚 \"$bookTitle\" vence en 2 días. ¡No olvides!"
            daysUntilDue < 0 -> "📚 \"$bookTitle\" está VENCIDO hace ${Math.abs(daysUntilDue)} día(s). ¡Devuélvelo YA!"
            else -> "📚 \"$bookTitle\" vence pronto. ¡Ten en cuenta!"
        }

        val channelId = if (daysUntilDue < 0) CHANNEL_ID_OVERDUE else CHANNEL_ID_UPCOMING
        val title = if (daysUntilDue < 0) "🚨 Libro VENCIDO" else "⚠️ Fecha Actualizada"

        // ⚡ Programar para 1 segundo después (inmediata)
        scheduleNotification(
            bookId, userId, 9999, // ID especial para inmediatas
            System.currentTimeMillis() + 1000,
            title,
            message,
            channelId,
            bookTitle, "", userName, daysUntilDue
        )

        Log.d(TAG, "⚡ Notificación inmediata programada: $message")
    }

    /**
     * 📚 Notificación inmediata al asignar libro
     */
    fun scheduleBookAssignmentNotification(
        bookId: String,
        bookTitle: String,
        bookAuthor: String,
        userId: String,
        userName: String,
        expirationDate: Timestamp
    ) {
        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val expirationDateStr = dateFormat.format(Date(expirationDate.seconds * 1000))
        
        val message = "📚 \"$bookTitle\" asignado a $userName hasta $expirationDateStr"
        val title = "✅ Libro Asignado"
        
        // ⚡ Programar para 1 segundo después (inmediata)
        scheduleNotification(
            bookId, userId, 8888, // ID especial para asignaciones
            System.currentTimeMillis() + 1000,
            title,
            message,
            CHANNEL_ID_UPCOMING,
            bookTitle, bookAuthor, userName, 999 // 999 = código especial para asignación
        )
        
        Log.d(TAG, "📚 Notificación de asignación programada: $message")
    }

    /**
     * 🗑️ Cancelar todas las notificaciones de un libro específico para un usuario
     */
    fun cancelBookNotifications(bookId: String, userId: String) {
        Log.d(TAG, "🗑️ Cancelando notificaciones para libro: $bookId, usuario: $userId")

        val requestCodes = listOf(
            REQUEST_CODE_UPCOMING_3DAY,
            REQUEST_CODE_UPCOMING_1DAY,
            REQUEST_CODE_DUE_TODAY_AM,
            REQUEST_CODE_DUE_TODAY_PM,
            REQUEST_CODE_OVERDUE_1DAY_AM,
            REQUEST_CODE_OVERDUE_1DAY_PM,
            REQUEST_CODE_OVERDUE_FREQUENT,
            REQUEST_CODE_OVERDUE_FREQUENT + 1,
            REQUEST_CODE_OVERDUE_FREQUENT + 2,
            REQUEST_CODE_OVERDUE_CRITICAL,
            REQUEST_CODE_OVERDUE_CRITICAL + 1,
            REQUEST_CODE_OVERDUE_CRITICAL + 2,
            REQUEST_CODE_OVERDUE_CRITICAL + 3,
            REQUEST_CODE_OVERDUE_CRITICAL + 4,
            REQUEST_CODE_OVERDUE_CRITICAL + 5,
            9999 // Inmediatas
        )

        requestCodes.forEach { requestCode ->
            val uniqueRequestCode = generateUniqueRequestCode(bookId, userId, requestCode)
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                uniqueRequestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            alarmManager.cancel(pendingIntent)
        }

        Log.d(TAG, "✅ Notificaciones canceladas exitosamente")
    }

    /**
     * 🔔 Programar una notificación específica
     */
    private fun scheduleNotification(
        bookId: String,
        userId: String,
        requestCode: Int,
        triggerTime: Long,
        title: String,
        message: String,
        channelId: String,
        bookTitle: String = "",
        bookAuthor: String = "",
        userName: String = "",
        daysUntilDue: Int = 0
    ) {
        if (triggerTime <= System.currentTimeMillis()) {
            Log.d(TAG, "⏰ Notificación programada para el pasado, omitiendo: $title")
            return
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("bookId", bookId)
            putExtra("bookTitle", bookTitle)
            putExtra("bookAuthor", bookAuthor)
            putExtra("userId", userId)
            putExtra("userName", userName)
            putExtra("title", title)
            putExtra("message", message)
            putExtra("channelId", channelId)
            putExtra("daysUntilDue", daysUntilDue)
        }

        val uniqueRequestCode = generateUniqueRequestCode(bookId, userId, requestCode)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            uniqueRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // 🔒 Verificar permisos y usar el método apropiado
            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.d(TAG, "⏰ Notificación EXACTA programada: $title -> ${Date(triggerTime)}")
            } else {
                // 📅 Fallback: usar alarma aproximada si no tenemos permisos exactos
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.d(TAG, "⏰ Notificación APROXIMADA programada: $title -> ${Date(triggerTime)}")
                Log.w(TAG, "⚠️ Sin permisos para alarmas exactas - usando aproximadas")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error programando notificación: ${e.message}")
            // 🔄 Último intento con alarma básica
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                Log.d(TAG, "⏰ Notificación BÁSICA programada como fallback")
            } catch (e2: Exception) {
                Log.e(TAG, "❌ Error crítico programando notificación: ${e2.message}")
            }
        }
    }

    /**
     * 🎯 Generar código único para cada notificación
     */
    private fun generateUniqueRequestCode(bookId: String, userId: String, baseCode: Int): Int {
        return (bookId.hashCode() + userId.hashCode() + baseCode + REQUEST_CODE_BASE)
    }

    /**
     * 🔒 Verificar si podemos programar alarmas exactas
     */
    private fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+) requiere verificación especial
            alarmManager.canScheduleExactAlarms()
        } else {
            // Android 11 y anteriores no requieren permisos especiales
            true
        }
    }

    /**
     * 🔔 Verificar permisos de notificación
     */
    fun hasNotificationPermissions(): Boolean {
        val hasPostNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        
        val canScheduleAlarms = canScheduleExactAlarms()
        
        Log.d(TAG, "📱 Permisos - Post Notifications: $hasPostNotifications, Schedule Alarms: $canScheduleAlarms")
        
        return hasPostNotifications && canScheduleAlarms
    }

    /**
     * 🎯 Solicitar permisos de alarmas exactas (si es necesario)
     */
    fun requestExactAlarmPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "⚠️ Sin permisos de alarmas exactas - Se usarán alarmas aproximadas")
            return false
        }
        return true
    }

    /**
     * ⏰ Calcular hora específica en un día
     */
    private fun calculateTimeAtHour(baseTime: Long, hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = baseTime
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}
