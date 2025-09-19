package com.example.libraryinventoryapp.utils

import android.util.Log
import com.example.libraryinventoryapp.BuildConfig
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
        // Brevo Configuration - Cargada desde local.properties via BuildConfig
        private val BREVO_API_KEY = BuildConfig.BREVO_API_KEY
        private const val BREVO_URL = "https://api.brevo.com/v3/smtp/email"
        private val FROM_EMAIL = BuildConfig.BREVO_FROM_EMAIL
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
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Libro Asignado - Sistema de Biblioteca</title>
                <style>
                    @media screen and (max-width: 600px) {
                        .container { width: 100% !important; padding: 15px !important; }
                        .book-card { padding: 15px !important; }
                        .header h1 { font-size: 24px !important; }
                    }
                </style>
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f8fafc; line-height: 1.6;">
                <div class="container" style="max-width: 600px; margin: 0 auto; padding: 20px; background-color: #ffffff;">
                    
                    <!-- Header Moderno -->
                    <div class="header" style="text-align: center; padding: 30px 0; background: linear-gradient(135deg, #4CAF50 0%, #66BB6A 100%); border-radius: 12px; margin-bottom: 30px; color: white;">
                        <h1 style="margin: 0; font-size: 28px; font-weight: 600;">📚 ¡Nuevo Libro Asignado!</h1>
                        <p style="margin: 10px 0 0 0; opacity: 0.9; font-size: 16px;">Sistema de Biblioteca Digital</p>
                    </div>
                    
                    <!-- Saludo Personal -->
                    <div style="margin-bottom: 25px;">
                        <h2 style="color: #2d3748; margin: 0 0 10px 0; font-size: 24px; font-weight: 500;">Hola $userName 👋</h2>
                        <p style="color: #4a5568; margin: 0; font-size: 16px;">Te informamos que tienes un nuevo libro esperándote:</p>
                    </div>
                    
                    <!-- Card del Libro - Diseño Material -->
                    <div class="book-card" style="background-color: #f7fafc; padding: 25px; border-radius: 12px; border: 1px solid #e2e8f0; box-shadow: 0 2px 4px rgba(0,0,0,0.05); margin: 25px 0;">
                        <div style="display: flex; align-items: center; margin-bottom: 20px;">
                            <div style="width: 50px; height: 50px; background: linear-gradient(135deg, #4CAF50, #66BB6A); border-radius: 50%; display: flex; align-items: center; justify-content: center; margin-right: 15px;">
                                <span style="color: white; font-size: 24px;">📖</span>
                            </div>
                            <div>
                                <h3 style="margin: 0; color: #2d3748; font-size: 20px; font-weight: 600;">$bookTitle</h3>
                                <p style="margin: 5px 0 0 0; color: #666; font-size: 14px;">Información del libro</p>
                            </div>
                        </div>
                        
                        <div style="background-color: white; padding: 20px; border-radius: 8px; border-left: 4px solid #4CAF50;">
                            <div style="margin-bottom: 15px;">
                                <span style="display: inline-block; background-color: #e3f2fd; color: #1976d2; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 500; margin-bottom: 8px;">TÍTULO</span>
                                <p style="margin: 0; color: #2d3748; font-size: 16px; font-weight: 500;">$bookTitle</p>
                            </div>
                            
                            <div style="margin-bottom: 15px;">
                                <span style="display: inline-block; background-color: #f3e5f5; color: #7b1fa2; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 500; margin-bottom: 8px;">AUTOR</span>
                                <p style="margin: 0; color: #2d3748; font-size: 16px;">$bookAuthor</p>
                            </div>
                            
                            <div>
                                <span style="display: inline-block; background-color: #e8f5e8; color: #388e3c; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 500; margin-bottom: 8px;">ASIGNADO POR</span>
                                <p style="margin: 0; color: #2d3748; font-size: 16px;">$adminName</p>
                            </div>
                        </div>
                    </div>
                    
                    <!-- Mensaje Motivacional -->
                    <div style="background: linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%); padding: 20px; border-radius: 12px; text-align: center; margin: 25px 0;">
                        <h3 style="margin: 0 0 10px 0; color: #e65100; font-size: 18px;">🌟 ¡Disfruta tu lectura! 🌟</h3>
                        <p style="margin: 0; color: #bf360c; font-size: 14px; font-style: italic;">Que este libro enriquezca tu conocimiento y fortalezca tu fe</p>
                </div>
                
                    <!-- Footer Profesional -->
                    <div style="margin-top: 40px; padding-top: 30px; border-top: 2px solid #e2e8f0; text-align: center;">
                        <div style="margin-bottom: 20px;">
                            <img src="https://via.placeholder.com/60x60/4CAF50/FFFFFF?text=📚" alt="Logo" style="width: 60px; height: 60px; border-radius: 50%; margin-bottom: 10px;">
                        </div>
                        <p style="margin: 0 0 5px 0; color: #2d3748; font-weight: 600; font-size: 16px;">Iglesia Hermanos en Cristo Bello</p>
                        <p style="margin: 0 0 15px 0; color: #718096; font-size: 14px;">Sistema de Biblioteca Digital</p>
                        
                        <div style="background-color: #f7fafc; padding: 15px; border-radius: 8px; margin: 20px 0;">
                            <p style="margin: 0; color: #4a5568; font-size: 12px;">
                                📧 Este es un email automático del sistema de biblioteca.<br>
                                Para cualquier consulta, contacta con el administrador.
                            </p>
                        </div>
                </div>
            </div>
            </body>
            </html>
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
            4. Actualizar BREVO_API_KEY en local.properties con tu clave real
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
            1. Configurar BREVO_API_KEY en local.properties con tu clave real
            2. Cambiar sendBookExpirationReminderEmailDemo() por sendBookExpirationReminderEmail()
        """.trimIndent())
    }
}