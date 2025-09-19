package com.example.libraryinventoryapp.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class EmailService {
    companion object {
        // Brevo Configuration - CONFIGURAR CON TUS CREDENCIALES REALES
        private const val BREVO_API_KEY = "TU_API_KEY_DE_BREVO_AQUI" // Tu API Key de Brevo (xkeysib-xxxxx)
        private const val BREVO_URL = "https://api.brevo.com/v3/smtp/email"
        private const val FROM_EMAIL = "hermanosencristobello@gmail.com" // Email verificado en Brevo
        private const val FROM_NAME = "Iglesia hermanos en Cristo Bello - Sistema de Biblioteca"
        
        private val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    suspend fun sendBookAssignmentEmail(
        adminEmail: String,
        userEmail: String,
        userName: String,
        bookTitle: String,
        bookAuthor: String,
        adminName: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Enviar email al usuario
                val userResult = sendEmailToUser(userEmail, userName, bookTitle, bookAuthor, adminName)
                
                // Enviar email al admin
                val adminResult = sendEmailToAdmin(adminEmail, adminName, userName, bookTitle, bookAuthor, userEmail)
                
                if (userResult.isSuccess && adminResult.isSuccess) {
                    Result.success("Correos enviados exitosamente")
                } else {
                    val errors = listOfNotNull(
                        userResult.exceptionOrNull()?.message,
                        adminResult.exceptionOrNull()?.message
                    ).joinToString("; ")
                    Result.failure(Exception("Errores al enviar correos: $errors"))
                }
            } catch (e: Exception) {
                Log.e("EmailService", "Error enviando correos: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    private suspend fun sendEmailToUser(
        userEmail: String,
        userName: String,
        bookTitle: String,
        bookAuthor: String,
        adminName: String
    ): Result<String> {
        val subject = "📚 Te han asignado un libro: $bookTitle"
        val content = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                <h2 style="color: #4CAF50; text-align: center;">¡Te han asignado un libro! 📚</h2>
                
                <p>Hola <strong>$userName</strong>,</p>
                
                <p>Te informamos que se te ha asignado el siguiente libro:</p>
                
                <div style="background-color: #f5f5f5; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #4CAF50;">
                    <p style="margin: 5px 0;"><strong>📖 Título:</strong> $bookTitle</p>
                    <p style="margin: 5px 0;"><strong>✍️ Autor:</strong> $bookAuthor</p>
                    <p style="margin: 5px 0;"><strong>👤 Asignado por:</strong> $adminName</p>
                </div>
                
                <p>¡Disfruta tu lectura!</p>
                
                <div style="margin-top: 30px; text-align: center; color: #666;">
                    <p>Saludos,<br>
                    <strong>Iglesia hermanos en Cristo Bello - Sistema de Biblioteca</strong></p>
                </div>
            </div>
        """.trimIndent()
        
        return sendBrevoEmail(userEmail, userName, subject, content)
    }

    private suspend fun sendEmailToAdmin(
        adminEmail: String,
        adminName: String,
        userName: String,
        bookTitle: String,
        bookAuthor: String,
        userEmail: String
    ): Result<String> {
        val subject = "✅ Libro asignado exitosamente: $bookTitle"
        val content = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                <h2 style="color: #2196F3; text-align: center;">Confirmación de Asignación ✅</h2>
                
                <p>Hola <strong>$adminName</strong>,</p>
                
                <p>Se ha asignado exitosamente el libro:</p>
                
                <div style="background-color: #e3f2fd; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #2196F3;">
                    <p style="margin: 5px 0;"><strong>📖 Título:</strong> $bookTitle</p>
                    <p style="margin: 5px 0;"><strong>✍️ Autor:</strong> $bookAuthor</p>
                    <p style="margin: 5px 0;"><strong>👤 Usuario asignado:</strong> $userName</p>
                    <p style="margin: 5px 0;"><strong>📧 Email del usuario:</strong> $userEmail</p>
                </div>
                
                <div style="margin-top: 30px; text-align: center; color: #666;">
                    <p><strong>Iglesia hermanos en Cristo Bello - Sistema de Biblioteca</strong></p>
                </div>
            </div>
        """.trimIndent()
        
        return sendBrevoEmail(adminEmail, adminName, subject, content)
    }

    private suspend fun sendBrevoEmail(
        toEmail: String,
        toName: String,
        subject: String,
        htmlContent: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Construir JSON para Brevo API v3
                val json = JSONObject().apply {
                    put("sender", JSONObject().apply {
                        put("email", FROM_EMAIL)
                        put("name", FROM_NAME)
                    })
                    put("to", JSONArray().apply {
                        put(JSONObject().apply {
                            put("email", toEmail)
                            put("name", toName)
                        })
                    })
                    put("subject", subject)
                    put("htmlContent", htmlContent)
                }

                val mediaType = "application/json".toMediaType()
                val requestBody = json.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(BREVO_URL)
                    .post(requestBody)
                    .addHeader("api-key", BREVO_API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    Log.d("EmailService", "✅ Brevo: Email enviado exitosamente a: $toEmail")
                    Result.success("Email enviado exitosamente")
                } else {
                    val errorBody = response.body?.string() ?: "Error desconocido"
                    Log.e("EmailService", "❌ Brevo Error: ${response.code} - $errorBody")
                    Result.failure(Exception("Brevo Error ${response.code}: $errorBody"))
                }
            } catch (e: Exception) {
                Log.e("EmailService", "❌ Excepción enviando email a $toEmail: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Versión DEMO que simula el envío de correos
     * Para desarrollo y pruebas
     */
    fun sendBookAssignmentEmailDemo(
        adminEmail: String,
        userEmail: String,
        userName: String,
        bookTitle: String,
        bookAuthor: String,
        adminName: String
    ) {
        Log.i("EmailService", """
            📧 EMAIL ENVIADO (DEMO MODE - Brevo) 📧
            
            === EMAIL AL USUARIO ===
            Para: $userEmail
            Asunto: 📚 Te han asignado un libro: $bookTitle
            
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
            
            🚀 Para activar correos REALES:
            1. Crear cuenta en Brevo (https://app.brevo.com)
            2. Obtener API Key (https://app.brevo.com/settings/keys/api)
            3. Verificar dominio de email
            4. Actualizar BREVO_API_KEY y FROM_EMAIL
            5. Cambiar sendBookAssignmentEmailDemo() por sendBookAssignmentEmail()
        """.trimIndent())
    }
    
    /**
     * Envío REAL de correos de recordatorio para libros vencidos/próximos a vencer
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
        Log.i("EmailService", """
            🚀 INICIANDO ENVÍO REAL DE RECORDATORIO 🚀
            Usuario: $userName ($userEmail)
            Libro: $bookTitle por $bookAuthor
            Vencimiento: $expirationDate
            Estado: $daysOverdue
            Admin: $adminName ($adminEmail)
        """.trimIndent())

        return withContext(Dispatchers.IO) {
            try {
                // Enviar email solo al usuario (no al admin - ya tiene la pantalla de devoluciones)
                Log.d("EmailService", "📧 Enviando recordatorio al usuario (sin notificar admin)...")
                val userResult = sendReminderToUser(userEmail, userName, bookTitle, bookAuthor, expirationDate, daysOverdue)
                
                if (userResult.isSuccess) {
                    Log.i("EmailService", "✅ Recordatorio enviado exitosamente al usuario")
                    Result.success("Recordatorio enviado exitosamente")
                } else {
                    val errorMsg = userResult.exceptionOrNull()?.message ?: "Error desconocido"
                    Log.e("EmailService", "❌ Error enviando recordatorio: $errorMsg")
                    Result.failure(Exception("Error enviando recordatorio: $errorMsg"))
                }
            } catch (e: Exception) {
                Log.e("EmailService", "❌ Excepción enviando recordatorio: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    private suspend fun sendReminderToUser(
        userEmail: String,
        userName: String,
        bookTitle: String,
        bookAuthor: String,
        expirationDate: String,
        daysOverdue: String
    ): Result<String> {
        Log.d("EmailService", "📤 Preparando email de recordatorio para usuario: $userEmail")
        
        val subject = "📚 Recordatorio: Devolución de libro - $bookTitle"
        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; padding: 30px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { text-align: center; color: #2c3e50; margin-bottom: 30px; }
                    .book-info { background-color: #ecf0f1; padding: 20px; border-radius: 8px; margin: 20px 0; }
                    .warning { background-color: #e74c3c; color: white; padding: 15px; border-radius: 8px; text-align: center; margin: 20px 0; }
                    .footer { text-align: center; color: #7f8c8d; margin-top: 30px; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>📚 Recordatorio de Devolución</h1>
                        <h2>$FROM_NAME</h2>
                    </div>
                    
                    <p>Hola <strong>$userName</strong>,</p>
                    
                    <p>Te recordamos sobre la devolución del siguiente libro:</p>
                    
                    <div class="book-info">
                        <h3>📖 $bookTitle</h3>
                        <p><strong>Autor:</strong> $bookAuthor</p>
                        <p><strong>Fecha de devolución:</strong> $expirationDate</p>
                    </div>
                    
                    <div class="warning">
                        <h3>⚠️ Estado: $daysOverdue</h3>
                    </div>
                    
                    <p>Por favor, devuelve el libro a la mayor brevedad posible.</p>
                    
                    <p>Si ya lo devolviste, puedes ignorar este mensaje.</p>
                    
                    <div class="footer">
                        <p>Gracias por tu colaboración</p>
                        <p><strong>$FROM_NAME</strong></p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        return sendBrevoEmail(userEmail, userName, subject, htmlContent)
    }

    private suspend fun sendReminderConfirmationToAdmin(
        adminEmail: String,
        adminName: String,
        userName: String,
        userEmail: String,
        bookTitle: String,
        bookAuthor: String,
        expirationDate: String,
        daysOverdue: String
    ): Result<String> {
        Log.d("EmailService", "📤 Preparando confirmación de recordatorio para admin: $adminEmail")
        
        val subject = "✅ Recordatorio enviado: $bookTitle - $userName"
        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; padding: 30px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { text-align: center; color: #2c3e50; margin-bottom: 30px; }
                    .info { background-color: #3498db; color: white; padding: 15px; border-radius: 8px; margin: 20px 0; }
                    .details { background-color: #ecf0f1; padding: 20px; border-radius: 8px; margin: 20px 0; }
                    .footer { text-align: center; color: #7f8c8d; margin-top: 30px; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>✅ Recordatorio Enviado</h1>
                        <h2>$FROM_NAME</h2>
                    </div>
                    
                    <p>Hola <strong>$adminName</strong>,</p>
                    
                    <div class="info">
                        <h3>📧 Recordatorio enviado exitosamente</h3>
                    </div>
                    
                    <div class="details">
                        <h3>📋 Detalles del recordatorio:</h3>
                        <p><strong>Usuario:</strong> $userName ($userEmail)</p>
                        <p><strong>Libro:</strong> $bookTitle</p>
                        <p><strong>Autor:</strong> $bookAuthor</p>
                        <p><strong>Fecha de vencimiento:</strong> $expirationDate</p>
                        <p><strong>Estado:</strong> $daysOverdue</p>
                    </div>
                    
                    <p>El usuario ha sido notificado sobre la devolución del libro.</p>
                    
                    <div class="footer">
                        <p>Sistema de Gestión de Biblioteca</p>
                        <p><strong>$FROM_NAME</strong></p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        return sendBrevoEmail(adminEmail, adminName, subject, htmlContent)
    }

    /**
     * Versión demo para recordatorios de libros vencidos - solo registra en el log
     */
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
        Log.i("EmailService", """
            📧 EMAIL DE RECORDATORIO ENVIADO (DEMO MODE - Brevo) 📧

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
            $FROM_NAME

            ===========================

            === EMAIL AL ADMINISTRADOR (CONFIRMACIÓN DE RECORDATORIO) ===
            Para: $adminEmail
            Asunto: 📨 Recordatorio enviado: $bookTitle

            Hola $adminName,

            Se ha enviado un recordatorio de devolución:

            📚 Libro: $bookTitle
            👤 Usuario: $userName ($userEmail)
            📅 Vencimiento: $expirationDate
            ⏰ Días de retraso: $daysOverdue

            Recordatorio enviado exitosamente.

            $FROM_NAME
            ===========================

            🚀 Para activar correos REALES de recordatorio:
            1. Implementar sendBookExpirationReminderEmail() con Brevo
            2. Cambiar sendBookExpirationReminderEmailDemo() por sendBookExpirationReminderEmail()
        """.trimIndent())
    }
}