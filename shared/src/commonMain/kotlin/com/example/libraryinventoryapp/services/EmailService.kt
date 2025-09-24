package com.example.libraryinventoryapp.services

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 📧 EmailService KMP - Servicio de correos multiplataforma
 * 
 * FUNCIONALIDADES:
 * ✅ Envío con Brevo API usando Ktor HTTP client
 * ✅ Templates HTML responsive Material Design 3
 * ✅ Soporte para asignación de libros y recordatorios
 * ✅ Logs detallados y manejo de errores
 */
class EmailService(
    private val apiKey: String,
    private val fromEmail: String
) {
    
    companion object {
        private const val BREVO_URL = "https://api.brevo.com/v3/smtp/email"
        private const val FROM_NAME = "Sistema de Biblioteca"
        private const val TAG = "EmailService"
    }

    // 🌐 Cliente HTTP Ktor con configuración JSON
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    /**
     * 📚 Envío de correos de asignación de libros
     */
    suspend fun sendBookAssignmentEmail(
        adminEmail: String,
        userEmail: String,
        userName: String,
        bookTitle: String,
        bookAuthor: String,
        adminName: String
    ): Result<String> {
        return withContext(Dispatchers.Default) {
            try {
                logInfo("🚀 Iniciando envío de correos de asignación")
                logInfo("Usuario: $userName ($userEmail)")
                logInfo("Libro: $bookTitle por $bookAuthor")
                logInfo("Admin: $adminName ($adminEmail)")
                
                // Enviar email al usuario
                val userResult = sendEmailToUser(userEmail, userName, bookTitle, bookAuthor, adminName)
                
                // Enviar email al admin
                val adminResult = sendEmailToAdmin(adminEmail, adminName, userName, bookTitle, bookAuthor, userEmail)
                
                if (userResult.isSuccess && adminResult.isSuccess) {
                    logInfo("✅ Ambos correos enviados exitosamente")
                    Result.success("Correos enviados exitosamente")
                } else {
                    val errors = listOfNotNull(
                        userResult.exceptionOrNull()?.message,
                        adminResult.exceptionOrNull()?.message
                    ).joinToString("; ")
                    logError("❌ Errores al enviar correos: $errors")
                    Result.failure(Exception("Errores al enviar correos: $errors"))
                }
            } catch (e: Exception) {
                logError("❌ Excepción enviando correos: ${e.message}")
                Result.failure(e)
            }
        }
    }

    /**
     * 📧 Email al usuario sobre asignación
     */
    private suspend fun sendEmailToUser(
        userEmail: String,
        userName: String,
        bookTitle: String,
        bookAuthor: String,
        adminName: String
    ): Result<String> {
        val subject = "Te han asignado un libro: $bookTitle"
        val content = buildUserAssignmentEmailHtml(userName, bookTitle, bookAuthor, adminName)
        
        return sendBrevoEmail(userEmail, userName, subject, content)
    }

    /**
     * 📧 Email al admin sobre confirmación
     */
    private suspend fun sendEmailToAdmin(
        adminEmail: String,
        adminName: String,
        userName: String,
        bookTitle: String,
        bookAuthor: String,
        userEmail: String
    ): Result<String> {
        val subject = "✅ Libro asignado exitosamente: $bookTitle"
        val content = buildAdminConfirmationEmailHtml(adminName, userName, bookTitle, bookAuthor, userEmail)
        
        return sendBrevoEmail(adminEmail, adminName, subject, content)
    }

    /**
     * ⏰ Envío de correos de recordatorio
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
        logInfo("🚀 Iniciando envío de recordatorio")
        logInfo("Usuario: $userName ($userEmail)")
        logInfo("Libro: $bookTitle por $bookAuthor")
        logInfo("Vencimiento: $expirationDate")
        logInfo("Estado: $daysOverdue")

        return withContext(Dispatchers.Default) {
            try {
                // Enviar email solo al usuario
                logDebug("📧 Enviando recordatorio al usuario...")
                val userResult = sendReminderToUser(userEmail, userName, bookTitle, bookAuthor, expirationDate, daysOverdue)
                
                if (userResult.isSuccess) {
                    logInfo("✅ Recordatorio enviado exitosamente al usuario")
                    Result.success("Recordatorio enviado exitosamente")
                } else {
                    val errorMsg = userResult.exceptionOrNull()?.message ?: "Error desconocido"
                    logError("❌ Error enviando recordatorio: $errorMsg")
                    Result.failure(Exception("Error enviando recordatorio: $errorMsg"))
                }
            } catch (e: Exception) {
                logError("❌ Excepción enviando recordatorio: ${e.message}")
                Result.failure(e)
            }
        }
    }

    /**
     * 📧 Email de recordatorio al usuario
     */
    private suspend fun sendReminderToUser(
        userEmail: String,
        userName: String,
        bookTitle: String,
        bookAuthor: String,
        expirationDate: String,
        daysOverdue: String
    ): Result<String> {
        logDebug("📤 Preparando email de recordatorio profesional para: $userEmail")
        
        val subject = "Recordatorio: Devolución de libro - $bookTitle"
        val htmlContent = buildReminderEmailHtml(userName, bookTitle, bookAuthor, expirationDate, daysOverdue)

        return sendBrevoEmail(userEmail, userName, subject, htmlContent)
    }

    /**
     * 🌐 Envío real con Brevo API usando Ktor
     */
    private suspend fun sendBrevoEmail(
        toEmail: String,
        toName: String,
        subject: String,
        htmlContent: String
    ): Result<String> {
        return withContext(Dispatchers.Default) {
            try {
                val requestPayload = BrevoEmailRequest(
                    sender = BrevoSender(fromEmail, FROM_NAME),
                    to = listOf(BrevoRecipient(toEmail, toName)),
                    subject = subject,
                    htmlContent = htmlContent
                )

                val response = httpClient.post(BREVO_URL) {
                    header("api-key", apiKey)
                    contentType(ContentType.Application.Json)
                    setBody(requestPayload)
                }

                if (response.status.value in 200..299) {
                    logDebug("✅ Brevo: Email enviado exitosamente a: $toEmail")
                    Result.success("Email enviado exitosamente")
                } else {
                    val errorBody = response.body<String>()
                    logError("❌ Brevo Error: ${response.status.value} - $errorBody")
                    Result.failure(Exception("Brevo Error ${response.status.value}: $errorBody"))
                }
            } catch (e: Exception) {
                logError("❌ Excepción enviando email a $toEmail: ${e.message}")
                Result.failure(e)
            }
        }
    }

    /**
     * 🎨 HTML Builder para email de asignación al usuario
     */
    private fun buildUserAssignmentEmailHtml(
        userName: String,
        bookTitle: String,
        bookAuthor: String,
        adminName: String
    ): String = """
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
                <div style="text-align: center; margin-bottom: 25px;">
                    <h2 style="color: #2d3748; margin: 0 0 10px 0; font-size: 24px; font-weight: 500;">Hola $userName 👋</h2>
                    <p style="color: #4a5568; margin: 0; font-size: 16px;">Te informamos que tienes un nuevo libro esperándote:</p>
                </div>
                
                <!-- Card del Libro - Diseño Material -->
                <div class="book-card" style="background-color: #f7fafc; padding: 25px; border-radius: 12px; border: 1px solid #e2e8f0; box-shadow: 0 2px 4px rgba(0,0,0,0.05); margin: 25px 0;">
                    <div style="text-align: center; margin-bottom: 20px;">
                        <h3 style="margin: 0; color: #2d3748; font-size: 20px; font-weight: 600;">$bookTitle</h3>
                        <p style="margin: 5px 0 0 0; color: #666; font-size: 14px;">Información del libro</p>
                    </div>
                    
                    <div style="background-color: white; padding: 20px; border-radius: 8px; border-left: 4px solid #4CAF50;">
                        <div style="text-align: center; margin-bottom: 15px;">
                            <span style="display: inline-block; background-color: #e3f2fd; color: #1976d2; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 500; margin-bottom: 8px;">TÍTULO</span>
                            <p style="margin: 0; color: #2d3748; font-size: 16px; font-weight: 500;">$bookTitle</p>
                        </div>
                        
                        <div style="text-align: center; margin-bottom: 15px;">
                            <span style="display: inline-block; background-color: #f3e5f5; color: #7b1fa2; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 500; margin-bottom: 8px;">AUTOR</span>
                            <p style="margin: 0; color: #2d3748; font-size: 16px;">$bookAuthor</p>
                        </div>
                        
                        <div style="text-align: center;">
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
                    <p style="margin: 0 0 5px 0; color: #2d3748; font-weight: 600; font-size: 16px;">Iglesia Hermanos en Cristo Bello</p>
                    <p style="margin: 0 0 15px 0; color: #718096; font-size: 14px;">Sistema de Biblioteca Digital</p>
                    
                    <div style="background-color: #f7fafc; padding: 15px; border-radius: 8px; margin: 20px 0; text-align: center;">
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

    /**
     * 🎨 HTML Builder para email de confirmación al admin
     */
    private fun buildAdminConfirmationEmailHtml(
        adminName: String,
        userName: String,
        bookTitle: String,
        bookAuthor: String,
        userEmail: String
    ): String = """
        <!DOCTYPE html>
        <html lang="es">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Confirmación de Asignación - Sistema de Biblioteca</title>
        </head>
        <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f8fafc; line-height: 1.6;">
            <div style="max-width: 600px; margin: 0 auto; padding: 20px; background-color: #ffffff;">
                
                <!-- Header -->
                <div style="text-align: center; padding: 30px 0; background: linear-gradient(135deg, #2196F3 0%, #42A5F5 100%); border-radius: 12px; margin-bottom: 30px; color: white;">
                    <h1 style="margin: 0; font-size: 28px; font-weight: 600;">✅ Confirmación de Asignación</h1>
                    <p style="margin: 10px 0 0 0; opacity: 0.9; font-size: 16px;">Sistema de Biblioteca Digital</p>
                </div>
                
                <!-- Saludo -->
                <div style="text-align: center; margin-bottom: 25px;">
                    <h2 style="color: #2d3748; margin: 0 0 10px 0; font-size: 24px; font-weight: 500;">Hola $adminName 👋</h2>
                    <p style="color: #4a5568; margin: 0; font-size: 16px;">Se ha asignado exitosamente el libro:</p>
                </div>
                
                <!-- Información del Libro -->
                <div style="background-color: #e3f2fd; padding: 25px; border-radius: 12px; margin: 20px 0; border-left: 4px solid #2196F3; text-align: center;">
                    <div style="margin-bottom: 15px;">
                        <span style="display: inline-block; background-color: #1976d2; color: white; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 500; margin-bottom: 8px;">TÍTULO</span>
                        <p style="margin: 0; color: #2d3748; font-size: 16px; font-weight: 500;">📖 $bookTitle</p>
                    </div>
                    
                    <div style="margin-bottom: 15px;">
                        <span style="display: inline-block; background-color: #7b1fa2; color: white; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 500; margin-bottom: 8px;">AUTOR</span>
                        <p style="margin: 0; color: #2d3748; font-size: 16px;">✍️ $bookAuthor</p>
                    </div>
                    
                    <div style="margin-bottom: 15px;">
                        <span style="display: inline-block; background-color: #388e3c; color: white; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 500; margin-bottom: 8px;">USUARIO ASIGNADO</span>
                        <p style="margin: 0; color: #2d3748; font-size: 16px;">👤 $userName</p>
                    </div>
                    
                    <div>
                        <span style="display: inline-block; background-color: #e65100; color: white; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 500; margin-bottom: 8px;">EMAIL DEL USUARIO</span>
                        <p style="margin: 0; color: #2d3748; font-size: 16px;">📧 $userEmail</p>
                    </div>
                </div>
                
                <!-- Footer -->
                <div style="margin-top: 40px; padding-top: 30px; border-top: 2px solid #e2e8f0; text-align: center;">
                    <p style="margin: 0 0 5px 0; color: #2d3748; font-weight: 600; font-size: 16px;">Iglesia Hermanos en Cristo Bello</p>
                    <p style="margin: 0 0 15px 0; color: #718096; font-size: 14px;">Sistema de Biblioteca Digital</p>
                    
                    <div style="background-color: #f7fafc; padding: 15px; border-radius: 8px; margin: 20px 0; text-align: center;">
                        <p style="margin: 0; color: #4a5568; font-size: 12px;">
                            📧 Este es un email automático del sistema de biblioteca.<br>
                            La asignación se ha registrado correctamente.
                        </p>
                    </div>
                </div>
            </div>
        </body>
        </html>
    """.trimIndent()

    /**
     * 🎨 HTML Builder para email de recordatorio con colores dinámicos
     */
    private fun buildReminderEmailHtml(
        userName: String,
        bookTitle: String,
        bookAuthor: String,
        expirationDate: String,
        daysOverdue: String
    ): String {
        // Determinar colores y estado según urgencia
        val (urgencyLevel, headerColor, statusColor, statusIcon) = when {
            daysOverdue.contains("Vencido") -> arrayOf("URGENTE", "#f44336", "#d32f2f", "🚨")
            daysOverdue.contains("Vence hoy") -> arrayOf("HOY", "#ff9800", "#f57c00", "⚠️")
            daysOverdue.contains("mañana") -> arrayOf("PRÓXIMO", "#2196f3", "#1976d2", "📅")
            else -> arrayOf("RECORDATORIO", "#4caf50", "#388e3c", "📚")
        }
        
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Recordatorio Devolución - Sistema de Biblioteca</title>
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
                    
                    <!-- Header Moderno con Gradiente Dinámico -->
                    <div class="header" style="text-align: center; padding: 30px 0; background: linear-gradient(135deg, $headerColor 0%, ${headerColor}cc 100%); border-radius: 12px; margin-bottom: 30px; color: white;">
                        <h1 style="margin: 0; font-size: 28px; font-weight: 600;">$statusIcon Recordatorio de Devolución</h1>
                        <p style="margin: 10px 0 0 0; opacity: 0.9; font-size: 16px;">Sistema de Biblioteca Digital</p>
                    </div>
                    
                    <!-- Saludo Personal -->
                    <div style="text-align: center; margin-bottom: 25px;">
                        <h2 style="color: #2d3748; margin: 0 0 10px 0; font-size: 24px; font-weight: 500;">Hola $userName 👋</h2>
                        <p style="color: #4a5568; margin: 0; font-size: 16px;">Te recordamos sobre la devolución del siguiente libro:</p>
                    </div>
                    
                    <!-- Card del Libro - Diseño Material -->
                    <div class="book-card" style="background-color: #f7fafc; padding: 25px; border-radius: 12px; border: 1px solid #e2e8f0; box-shadow: 0 2px 4px rgba(0,0,0,0.05); margin: 25px 0;">
                        <div style="text-align: center; margin-bottom: 20px;">
                            <h3 style="margin: 0; color: #2d3748; font-size: 20px; font-weight: 600;">$bookTitle</h3>
                            <p style="margin: 5px 0 0 0; color: #666; font-size: 14px;">Información del libro</p>
                        </div>
                        
                        <div style="background-color: white; padding: 20px; border-radius: 8px; border-left: 4px solid $headerColor;">
                            <div style="text-align: center; margin-bottom: 15px;">
                                <span style="display: inline-block; background-color: #e3f2fd; color: #1976d2; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 500; margin-bottom: 8px;">TÍTULO</span>
                                <p style="margin: 0; color: #2d3748; font-size: 16px; font-weight: 500;">$bookTitle</p>
                            </div>
                            
                            <div style="text-align: center; margin-bottom: 15px;">
                                <span style="display: inline-block; background-color: #f3e5f5; color: #7b1fa2; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 500; margin-bottom: 8px;">AUTOR</span>
                                <p style="margin: 0; color: #2d3748; font-size: 16px;">$bookAuthor</p>
                            </div>
                            
                            <div style="text-align: center;">
                                <span style="display: inline-block; background-color: #fff3e0; color: #ef6c00; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 500; margin-bottom: 8px;">FECHA DEVOLUCIÓN</span>
                                <p style="margin: 0; color: #2d3748; font-size: 16px; font-weight: 500;">$expirationDate</p>
                            </div>
                        </div>
                    </div>
                    
                    <!-- Estado de Urgencia -->
                    <div style="background: linear-gradient(135deg, ${statusColor}22 0%, ${statusColor}11 100%); padding: 20px; border-radius: 12px; text-align: center; margin: 25px 0; border: 2px solid ${statusColor}44;">
                        <h3 style="margin: 0 0 10px 0; color: $statusColor; font-size: 18px;">$statusIcon Estado: $daysOverdue</h3>
                        <p style="margin: 0; color: #555; font-size: 14px; font-weight: 500;">Nivel de prioridad: $urgencyLevel</p>
                    </div>
                    
                    <!-- Mensaje de Acción -->
                    <div style="background-color: #f0f4f8; padding: 20px; border-radius: 12px; text-align: center; margin: 25px 0;">
                        <p style="margin: 0 0 10px 0; color: #2d3748; font-size: 16px; font-weight: 500;">Por favor, devuelve el libro a la mayor brevedad posible.</p>
                        <p style="margin: 0; color: #4a5568; font-size: 14px;">Si ya lo devolviste, puedes ignorar este mensaje.</p>
                    </div>
                    
                    <!-- Mensaje Motivacional -->
                    <div style="background: linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%); padding: 20px; border-radius: 12px; text-align: center; margin: 25px 0;">
                        <h3 style="margin: 0 0 10px 0; color: #e65100; font-size: 18px;">📖 Gracias por cuidar nuestros libros</h3>
                        <p style="margin: 0; color: #bf360c; font-size: 14px; font-style: italic;">Tu responsabilidad permite que otros también disfruten de la lectura</p>
                    </div>
                
                    <!-- Footer Profesional -->
                    <div style="margin-top: 40px; padding-top: 30px; border-top: 2px solid #e2e8f0; text-align: center;">
                        <p style="margin: 0 0 5px 0; color: #2d3748; font-weight: 600; font-size: 16px;">Iglesia Hermanos en Cristo Bello</p>
                        <p style="margin: 0 0 15px 0; color: #718096; font-size: 14px;">Sistema de Biblioteca Digital</p>
                        
                        <div style="background-color: #f7fafc; padding: 15px; border-radius: 8px; margin: 15px 0;">
                            <p style="margin: 0; color: #4a5568; font-size: 12px; line-height: 1.5;">
                                Este es un recordatorio automático generado por nuestro sistema de biblioteca.
                                <br>Para cualquier consulta, contacta a la administración.
                            </p>
                        </div>
                        
                        <p style="margin: 15px 0 0 0; color: #a0aec0; font-size: 11px;">© 2024 Sistema de Biblioteca - Iglesia Hermanos en Cristo Bello</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    // 📝 Funciones de logging (expect/actual para cada plataforma)
    private fun logInfo(message: String) = println("ℹ️ $TAG: $message")
    private fun logDebug(message: String) = println("🐛 $TAG: $message")
    private fun logError(message: String) = println("❌ $TAG: $message")

    /**
     * 🔄 Limpiar recursos
     */
    fun close() {
        httpClient.close()
    }
}

// 📧 Modelos de datos para Brevo API
@Serializable
data class BrevoEmailRequest(
    val sender: BrevoSender,
    val to: List<BrevoRecipient>,
    val subject: String,
    val htmlContent: String
)

@Serializable
data class BrevoSender(
    val email: String,
    val name: String
)

@Serializable
data class BrevoRecipient(
    val email: String,
    val name: String
)
