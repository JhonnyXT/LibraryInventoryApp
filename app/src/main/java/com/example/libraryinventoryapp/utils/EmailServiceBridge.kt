package com.example.libraryinventoryapp.utils

import android.util.Log
import com.example.libraryinventoryapp.BuildConfig
import com.example.libraryinventoryapp.services.EmailService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext

/**
 * 🌉 EmailServiceBridge - Puente entre UI Android y EmailService KMP
 * 
 * PROPÓSITO:
 * ✅ Mantener la interfaz exacta del EmailService Android original
 * ✅ Usar internamente el EmailService KMP del módulo shared
 * ✅ Preservar 100% compatibilidad con código existente
 * ✅ Facilitar migración gradual sin romper funcionalidad
 */
class EmailServiceBridge {
    
    companion object {
        private const val TAG = "EmailServiceBridge"
    }
    
    // 🔧 EmailService KMP inicializado con credenciales de Android
    private val emailService by lazy {
        EmailService(
            apiKey = BuildConfig.BREVO_API_KEY,
            fromEmail = BuildConfig.BREVO_FROM_EMAIL
        )
    }

    /**
     * 📚 Envío de correos de asignación de libros - COMPATIBLE con interfaz original
     * Usa GlobalScope para evitar cancelaciones por navegación del usuario
     */
    suspend fun sendBookAssignmentEmail(
        adminEmail: String,
        userEmail: String,
        userName: String,
        bookTitle: String,
        bookAuthor: String,
        adminName: String
    ): Result<String> {
        return try {
            Log.i(TAG, "🌉 Bridge: Delegando al EmailService KMP")
            
            // Usar GlobalScope para que el email se complete independientemente de la UI
            val result = withContext(Dispatchers.IO + SupervisorJob()) {
                emailService.sendBookAssignmentEmail(
                    adminEmail = adminEmail,
                    userEmail = userEmail,
                    userName = userName,
                    bookTitle = bookTitle,
                    bookAuthor = bookAuthor,
                    adminName = adminName
                )
            }
            
            if (result.isSuccess) {
                Log.i(TAG, "✅ Bridge: Correos enviados exitosamente via KMP")
            } else {
                Log.e(TAG, "❌ Bridge: Error en EmailService KMP: ${result.exceptionOrNull()?.message}")
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "❌ Bridge: Excepción: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * ⏰ Envío de correos de recordatorio - COMPATIBLE con interfaz original
     */
    suspend fun sendBookExpirationReminderEmail(
        adminEmail: String,
        userEmail: String,
        userName: String,
        bookTitle: String,
        bookAuthor: String,
        adminName: String,
        expirationDate: String,
        daysOverdue: String
    ): Result<String> {
        return try {
            Log.i(TAG, "🌉 Bridge: Delegando recordatorio al EmailService KMP")
            
            // Usar scope independiente para que el email se complete sin cancelaciones
            val result = withContext(Dispatchers.IO + SupervisorJob()) {
                emailService.sendBookExpirationReminderEmail(
                    adminEmail = adminEmail,
                    userEmail = userEmail,
                    userName = userName,
                    bookTitle = bookTitle,
                    bookAuthor = bookAuthor,
                    adminName = adminName,
                    expirationDate = expirationDate,
                    daysOverdue = daysOverdue
                )
            }
            
            if (result.isSuccess) {
                Log.i(TAG, "✅ Bridge: Recordatorio enviado exitosamente via KMP")
            } else {
                Log.e(TAG, "❌ Bridge: Error en recordatorio KMP: ${result.exceptionOrNull()?.message}")
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "❌ Bridge: Excepción en recordatorio: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 🎭 Versiones DEMO - Mantener compatibilidad con funciones demo originales
     */
    fun sendBookAssignmentEmailDemo(
        adminEmail: String,
        userEmail: String,
        userName: String,
        bookTitle: String,
        bookAuthor: String,
        adminName: String
    ) {
        Log.i(TAG, """
            📧 EMAIL ENVIADO (DEMO MODE - KMP Bridge) 📧
            
            === EMAIL AL USUARIO ===
            Para: $userEmail
            Asunto: Te han asignado un libro: $bookTitle
            
            Hola $userName,
            
            Te informamos que se te ha asignado el siguiente libro:
            
            📚 Título: $bookTitle
            ✍️ Autor: $bookAuthor
            👤 Asignado por: $adminName
            
            ¡Disfruta tu lectura!
            
            Saludos,
            Iglesia hermanos en Cristo Bello - Sistema de Biblioteca
            
            ===========================
            
            === EMAIL AL ADMINISTRADOR ===
            Para: $adminEmail
            Asunto: ✅ Libro asignado exitosamente: $bookTitle
            
            Hola $adminName,
            
            Se ha confirmado la asignación del libro:
            
            📚 Título: $bookTitle
            ✍️ Autor: $bookAuthor
            👤 Asignado a: $userName ($userEmail)
            
            La asignación se ha registrado correctamente en el sistema.
            
            Saludos,
            Iglesia hermanos en Cristo Bello - Sistema de Biblioteca
            ===========================
            
            🌉 NOTA: Usando EmailService KMP a través de Bridge
            🚀 Para activar correos REALES: Ya están configurados con KMP
        """.trimIndent())
    }

    fun sendBookExpirationReminderEmailDemo(
        adminEmail: String,
        userEmail: String,
        userName: String,
        bookTitle: String,
        bookAuthor: String,
        adminName: String,
        expirationDate: String,
        daysOverdue: String
    ) {
        Log.i(TAG, """
            📧 EMAIL DE RECORDATORIO ENVIADO (DEMO MODE - KMP Bridge) 📧

            === EMAIL AL USUARIO (RECORDATORIO DE DEVOLUCIÓN) ===
            Para: $userEmail
            Asunto: ⚠️ RECORDATORIO: Libro vencido - $bookTitle

            Hola $userName,

            Te recordamos que tienes un libro VENCIDO que debe ser devuelto:

            📚 Título: $bookTitle
            ✍️ Autor: $bookAuthor
            📅 Fecha de vencimiento: $expirationDate
            ⏰ Tiempo vencido: $daysOverdue

            Por favor, devuelve el libro lo antes posible.

            Si ya lo devolviste, contacta al administrador.

            Gracias por tu comprensión,
            Sistema de Biblioteca

            ===========================

            🌉 NOTA: Usando EmailService KMP a través de Bridge
            🚀 Para activar correos REALES de recordatorio: Ya están configurados con KMP
        """.trimIndent())
    }

    /**
     * 🔄 Cerrar recursos del EmailService KMP
     */
    fun close() {
        try {
            emailService.close()
            Log.d(TAG, "✅ Bridge: EmailService KMP cerrado correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Bridge: Error cerrando EmailService KMP: ${e.message}")
        }
    }
}
