package com.example.libraryinventoryapp.services

import android.util.Log

/**
 * 🤖 Implementación Android de EmailServiceFactory
 */
actual class EmailServiceFactory {
    
    companion object {
        private const val TAG = "EmailServiceFactory"
    }
    
    /**
     * 🔧 Crear EmailService para Android (usando BuildConfig)
     * En Android obtenemos las credenciales desde BuildConfig
     */
    actual fun create(): EmailService? {
        return try {
            // TODO: Obtener credenciales desde BuildConfig de la app Android
            // Por ahora retornamos null - se implementará en la próxima fase
            Log.d(TAG, "⚠️ EmailService factory pendiente de configuración con BuildConfig")
            null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creando EmailService: ${e.message}", e)
            null
        }
    }
}
