package com.example.libraryinventoryapp.services

/**
 * 🏭 Factory para crear EmailService con configuración específica de plataforma
 * Patrón expect/actual para manejo de configuración por plataforma
 */
expect class EmailServiceFactory() {
    /**
     * 🔧 Crear EmailService configurado con credenciales de plataforma
     */
    fun create(): EmailService?
}

/**
 * 📱 Configuración base de email (común a todas las plataformas)
 */
object EmailConfig {
    const val BREVO_URL = "https://api.brevo.com/v3/smtp/email"
    const val FROM_NAME = "Sistema de Biblioteca"
    const val REQUEST_TIMEOUT_MS = 30_000L
    const val CONNECT_TIMEOUT_MS = 30_000L
}
