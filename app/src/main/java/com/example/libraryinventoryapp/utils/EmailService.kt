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
        // SendGrid Configuration - Reemplazar con tus credenciales reales
        private const val SENDGRID_API_KEY = "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" // Tu API Key de SendGrid (SG.xxxxx)
        private const val SENDGRID_URL = "https://api.sendgrid.com/v3/mail/send"
        private const val FROM_EMAIL = "jonathanblandon1017@gmail.com" // Email verificado en SendGrid
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
        
        return sendSendGridEmail(userEmail, userName, subject, content)
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
        
        return sendSendGridEmail(adminEmail, adminName, subject, content)
    }

    private suspend fun sendSendGridEmail(
        toEmail: String,
        toName: String,
        subject: String,
        htmlContent: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Construir JSON para SendGrid API v3
                val json = JSONObject().apply {
                    put("personalizations", JSONArray().apply {
                        put(JSONObject().apply {
                            put("to", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("email", toEmail)
                                    put("name", toName)
                                })
                            })
                        })
                    })
                    put("from", JSONObject().apply {
                        put("email", FROM_EMAIL)
                        put("name", FROM_NAME)
                    })
                    put("subject", subject)
                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "text/html")
                            put("value", htmlContent)
                        })
                    })
                }

                val mediaType = "application/json".toMediaType()
                val requestBody = json.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(SENDGRID_URL)
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer $SENDGRID_API_KEY")
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    Log.d("EmailService", "✅ SendGrid: Email enviado exitosamente a: $toEmail")
                    Result.success("Email enviado exitosamente")
                } else {
                    val errorBody = response.body?.string() ?: "Error desconocido"
                    Log.e("EmailService", "❌ SendGrid Error: ${response.code} - $errorBody")
                    Result.failure(Exception("SendGrid Error ${response.code}: $errorBody"))
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
            📧 EMAIL ENVIADO (DEMO MODE - SendGrid) 📧
            
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
            1. Crear cuenta en SendGrid (https://sendgrid.com)
            2. Obtener API Key
            3. Verificar dominio de email
            4. Actualizar SENDGRID_API_KEY y FROM_EMAIL
            5. Cambiar sendBookAssignmentEmailDemo() por sendBookAssignmentEmail()
        """.trimIndent())
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
            📧 EMAIL DE RECORDATORIO ENVIADO (DEMO MODE - SendGrid) 📧

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
            1. Implementar sendBookExpirationReminderEmail() con SendGrid
            2. Cambiar sendBookExpirationReminderEmailDemo() por sendBookExpirationReminderEmail()
        """.trimIndent())
    }
}