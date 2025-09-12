package com.example.libraryinventoryapp.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat

/**
 * 🔒 PermissionHelper - Gestor de Permisos para Notificaciones
 * 
 * FUNCIONALIDADES:
 * ✅ Verificar permisos de notificación
 * ✅ Solicitar permisos automáticamente 
 * ✅ Guiar al usuario a configuración si es necesario
 * ✅ Verificar alarmas exactas (Android 12+)
 * ✅ Diálogos explicativos amigables
 */
class PermissionHelper(private val activity: Activity) {

    companion object {
        private const val TAG = "PermissionHelper"
        const val REQUEST_POST_NOTIFICATIONS = 1001
        const val REQUEST_NOTIFICATION_SETTINGS = 1002
    }

    /**
     * 🎯 Verificar todos los permisos necesarios para notificaciones
     */
    fun checkAllNotificationPermissions(): Boolean {
        val hasPostNotifications = hasPostNotificationPermission()
        val canScheduleAlarms = canScheduleExactAlarms()
        val areNotificationsEnabled = areNotificationsEnabled()

        Log.d(TAG, "📱 Estado de permisos:")
        Log.d(TAG, "  - POST_NOTIFICATIONS: $hasPostNotifications")
        Log.d(TAG, "  - SCHEDULE_EXACT_ALARM: $canScheduleAlarms") 
        Log.d(TAG, "  - Notificaciones habilitadas: $areNotificationsEnabled")

        return hasPostNotifications && canScheduleAlarms && areNotificationsEnabled
    }

    /**
     * 🔔 Verificar si se pueden enviar notificaciones (Android 13+)
     */
    private fun hasPostNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // No se requiere en versiones anteriores
        }
    }

    /**
     * ⏰ Verificar si se pueden programar alarmas exactas (Android 12+)
     */
    private fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true // No se requiere en versiones anteriores
        }
    }

    /**
     * 📱 Verificar si las notificaciones están habilitadas en configuración
     */
    private fun areNotificationsEnabled(): Boolean {
        return NotificationManagerCompat.from(activity).areNotificationsEnabled()
    }

    /**
     * 🚀 Solicitar todos los permisos necesarios de forma directa
     */
    fun requestAllPermissions() {
        Log.d(TAG, "🔒 Iniciando solicitud de permisos...")

        when {
            // 1️⃣ Verificar permisos de notificación primero - SOLICITAR DIRECTAMENTE
            !hasPostNotificationPermission() -> {
                Log.d(TAG, "📱 Solicitando permiso POST_NOTIFICATIONS directamente...")
                requestPostNotificationPermission()
            }

            // 2️⃣ Verificar si están habilitadas en configuración
            !areNotificationsEnabled() -> {
                showNotificationsDisabledDialog()
            }

            // 3️⃣ Verificar alarmas exactas (Android 12+)
            !canScheduleExactAlarms() -> {
                Log.d(TAG, "⏰ Alarmas exactas no disponibles, continuando con aproximadas...")
                navigateAfterPermissions()
            }

            // ✅ Todo está bien
            else -> {
                Log.d(TAG, "✅ Todos los permisos están correctos")
                navigateAfterPermissions()
            }
        }
    }

    /**
     * 📋 Solicitar permiso POST_NOTIFICATIONS
     */
    private fun requestPostNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_POST_NOTIFICATIONS
            )
            Log.d(TAG, "🔔 Solicitando permiso POST_NOTIFICATIONS...")
        }
    }

    /**
     * ⚠️ Mostrar diálogo cuando las notificaciones están desactivadas
     */
    private fun showNotificationsDisabledDialog() {
        AlertDialog.Builder(activity)
            .setTitle("Notificaciones Desactivadas")
            .setMessage(
                "Las notificaciones están desactivadas.\n\n" +
                "Para recibir recordatorios de libros, actívalas en configuración."
            )
            .setPositiveButton("Ir a Configuración") { _, _ ->
                openNotificationSettings()
            }
            .setNegativeButton("Continuar") { _, _ ->
                navigateAfterPermissions()
            }
            .setCancelable(false)
            .show()
    }


    /**
     * 🔧 Abrir configuración de notificaciones de la app
     */
    private fun openNotificationSettings() {
        try {
            val intent = Intent().apply {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
            }
            activity.startActivityForResult(intent, REQUEST_NOTIFICATION_SETTINGS)
            Log.d(TAG, "🔧 Abriendo configuración de notificaciones")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error abriendo configuración: ${e.message}")
            // Fallback: abrir configuración general de la app
            try {
                val fallbackIntent = Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.parse("package:${activity.packageName}")
                }
                activity.startActivityForResult(fallbackIntent, REQUEST_NOTIFICATION_SETTINGS)
            } catch (e2: Exception) {
                Log.e(TAG, "❌ Error abriendo configuración fallback: ${e2.message}")
            }
        }
    }


    /**
     * 🚀 Navegar después de obtener permisos
     */
    private fun navigateAfterPermissions() {
        if (activity is com.example.libraryinventoryapp.LoginActivity) {
            (activity as com.example.libraryinventoryapp.LoginActivity).navigateAfterPermissions()
        }
    }

    /**
     * 🔄 Manejar resultado de permisos
     */
    fun onRequestPermissionsResult(
        requestCode: Int, 
        permissions: Array<out String>, 
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_POST_NOTIFICATIONS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "✅ Permiso POST_NOTIFICATIONS concedido")
                    // ✅ Navegar directamente sin más diálogos
                    navigateAfterPermissions()
                } else {
                    Log.w(TAG, "❌ Permiso POST_NOTIFICATIONS denegado")
                    // Solo mostrar diálogo si el usuario denegó el permiso
                    showNotificationsDisabledDialog()
                }
            }
        }
    }

    /**
     * 🔄 Manejar resultado de configuración
     */
    fun onActivityResult(requestCode: Int, resultCode: Int) {
        when (requestCode) {
            REQUEST_NOTIFICATION_SETTINGS -> {
                Log.d(TAG, "🔄 Usuario regresó de configuración, verificando permisos...")
                // Dar tiempo al sistema para aplicar cambios
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (areNotificationsEnabled()) {
                        navigateAfterPermissions()
                    } else {
                        Log.w(TAG, "⚠️ Notificaciones aún desactivadas, continuando sin ellas")
                        navigateAfterPermissions()
                    }
                }, 1000)
            }
        }
    }
}
