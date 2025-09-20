package com.example.libraryinventoryapp.services

/**
 * 🍎 Implementación iOS de EmailServiceFactory
 */
actual class EmailServiceFactory {
    
    /**
     * 🔧 Crear EmailService para iOS (usando Info.plist)
     * En iOS obtenemos las credenciales desde Info.plist
     */
    actual fun create(): EmailService? {
        return try {
            // TODO: Implementar obtención de credenciales desde Info.plist
            // Por ahora retornamos null - se implementará en la Fase 5
            println("⚠️ EmailService factory pendiente para iOS")
            null
        } catch (e: Exception) {
            println("❌ Error creando EmailService iOS: ${e.message}")
            null
        }
    }
}
