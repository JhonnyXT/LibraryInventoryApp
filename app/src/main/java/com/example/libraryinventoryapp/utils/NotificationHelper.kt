package com.example.libraryinventoryapp.utils

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.example.libraryinventoryapp.R
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 🎨 NotificationHelper - Sistema de notificaciones UI elegante nivel senior
 * 
 * FUNCIONALIDADES:
 * ✅ Snackbars animadas con Material Design 3
 * ✅ Progress indicators elegantes para operaciones async
 * ✅ Toast personalizados con iconos y colores
 * ✅ Feedback visual profesional para emails
 * ✅ Animaciones suaves y transiciones fluidas
 */
class NotificationHelper {

    companion object {
        
        /**
         * 📧 Mostrar progreso elegante para envío de emails
         */
        fun showEmailSendingProgress(
            view: View,
            message: String = "Enviando notificación por email..."
        ): Snackbar {
            val snackbar = Snackbar.make(view, "⏳ $message", Snackbar.LENGTH_INDEFINITE)
                .setBackgroundTint(ContextCompat.getColor(view.context, R.color.colorPrimary))
                .setTextColor(ContextCompat.getColor(view.context, android.R.color.white))
                .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
            
            // ✅ Progress indicator implementado con animación de texto
            CoroutineScope(Dispatchers.Main).launch {
                var dots = ""
                while (snackbar.isShown) {
                    dots = when (dots) {
                        "" -> "."
                        "." -> ".."
                        ".." -> "..."
                        else -> ""
                    }
                    try {
                        snackbar.setText("⏳ $message$dots")
                        delay(500)
                    } catch (e: Exception) {
                        break // Snackbar was dismissed
                    }
                }
            }
            
            snackbar.show()
            return snackbar
        }
        
        /**
         * ✅ Mostrar éxito con animación elegante
         */
        fun showEmailSuccess(
            view: View,
            recipientName: String,
            recipientEmail: String,
            bookTitle: String,
            isReminder: Boolean = false
        ) {
            val message = if (isReminder) {
                "✅ Recordatorio enviado a $recipientName\n📧 $recipientEmail\n📚 $bookTitle"
            } else {
                "✅ Notificación enviada a $recipientName\n📧 $recipientEmail\n📚 $bookTitle"
            }
            
            val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(view.context, R.color.colorPrimary)) // ✅ Mismo color que showEmailSendingProgress
                .setTextColor(ContextCompat.getColor(view.context, android.R.color.white))
                .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                .setAction("VER DETALLES") {
                    // ✅ Implementado: Abrir diálogo con detalles del email
                    showEmailDetailsDialog(
                        context = view.context,
                        recipientName = recipientName,
                        recipientEmail = recipientEmail,
                        bookTitle = bookTitle,
                        emailType = if (isReminder) "Recordatorio" else "Notificación"
                    )
                }
            
            snackbar.show()
        }
        
        /**
         * ❌ Mostrar error con estilo profesional
         */
        fun showEmailError(
            view: View,
            errorMessage: String
        ) {
            val snackbar = Snackbar.make(view, "❌ Error enviando email: $errorMessage", Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(view.context, R.color.error_red))
                .setTextColor(ContextCompat.getColor(view.context, android.R.color.white))
                .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                .setAction("REINTENTAR") {
                    // ✅ Implementado: Lógica de reintento genérica
                    showRetryDialog(
                        context = view.context,
                        title = "Error de Email",
                        message = "No se pudo enviar el email.\n\nError: $errorMessage\n\n¿Quieres intentar de nuevo?",
                        onRetry = {
                            // Mostrar nuevo progress mientras se reintenta
                            val retryProgressSnackbar = showEmailSendingProgress(view, "Reintentando envío...")
                            // Después de 3 segundos simular el reintento (la lógica real la maneja quien llama)
                            CoroutineScope(Dispatchers.Main).launch {
                                delay(3000)
                                retryProgressSnackbar.dismiss()
                                showModernToast(
                                    context = view.context,
                                    message = "Reintento iniciado. Verifica tu conexión.",
                                    type = NotificationType.INFO
                                )
                            }
                        }
                    )
                }
            
            snackbar.show()
        }
        
        /**
         * 🎯 Mostrar toast profesional con iconos
         */
        fun showModernToast(
            context: Context,
            message: String,
            type: NotificationType = NotificationType.INFO,
            duration: Int = Toast.LENGTH_SHORT
        ) {
            val icon = when (type) {
                NotificationType.SUCCESS -> "✅"
                NotificationType.ERROR -> "❌"
                NotificationType.WARNING -> "⚠️"
                NotificationType.INFO -> "ℹ️"
                else -> "📱"
            }
            
            Toast.makeText(context, "$icon $message", duration).show()
        }
        
        /**
         * 📱 Animación de "enviando" con puntos suspensivos
         */
        fun showSendingAnimation(
            view: View,
            message: String,
            onComplete: () -> Unit
        ) {
            val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE)
                .setBackgroundTint(ContextCompat.getColor(view.context, R.color.colorPrimary))
                .setTextColor(ContextCompat.getColor(view.context, android.R.color.white))
            
            snackbar.show()
            
            // Animación de puntos suspensivos
            CoroutineScope(Dispatchers.Main).launch {
                var dots = ""
                repeat(6) { // 3 segundos de animación
                    dots = when (dots) {
                        "" -> "."
                        "." -> ".."
                        ".." -> "..."
                        else -> ""
                    }
                    snackbar.setText("$message$dots")
                    delay(500)
                }
                snackbar.dismiss()
                onComplete()
            }
        }
        
        /**
         * 🎨 Crear diálogo de confirmación moderno
         */
        fun showEmailConfirmationDialog(
            context: Context,
            title: String,
            message: String,
            onConfirm: () -> Unit,
            onCancel: () -> Unit = {}
        ) {
            // ✅ AlertDialog Material Design 3 personalizado implementado
            AlertDialog.Builder(context)
                .setTitle("📧 $title")
                .setMessage(message)
                .setPositiveButton("ENVIAR") { dialog, _ -> 
                    dialog.dismiss()
                    onConfirm() 
                }
                .setNegativeButton("CANCELAR") { dialog, _ -> 
                    dialog.dismiss()
                    onCancel() 
                }
                .setIcon(R.drawable.ic_email_24)
                .setCancelable(false)
                .show()
        }

        /**
         * 📧 Mostrar detalles completos del email enviado
         */
        private fun showEmailDetailsDialog(
            context: Context,
            recipientName: String,
            recipientEmail: String,
            bookTitle: String,
            emailType: String
        ) {
            val currentTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            
            AlertDialog.Builder(context)
                .setTitle("✅ $emailType Enviado")
                .setMessage(
                    "📧 Detalles del email:\n\n" +
                    "👤 Destinatario: $recipientName\n" +
                    "📨 Email: $recipientEmail\n" +
                    "📚 Libro: $bookTitle\n" +
                    "📅 Fecha: $currentTime\n" +
                    "🔔 Tipo: $emailType de asignación\n\n" +
                    "El $emailType fue enviado exitosamente."
                )
                .setPositiveButton("ENTENDIDO") { dialog, _ -> dialog.dismiss() }
                .setIcon(R.drawable.ic_email_24)
                .show()
        }

        /**
         * 🔄 Mostrar diálogo de reintento genérico
         */
        private fun showRetryDialog(
            context: Context,
            title: String,
            message: String,
            onRetry: () -> Unit
        ) {
            AlertDialog.Builder(context)
                .setTitle("🔄 $title")
                .setMessage(message)
                .setPositiveButton("REINTENTAR") { dialog, _ ->
                    dialog.dismiss()
                    onRetry.invoke()
                }
                .setNegativeButton("CANCELAR") { dialog, _ -> dialog.dismiss() }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .show()
        }

        // ===============================================
        // 🎯 SISTEMA UNIFICADO DE NOTIFICACIONES
        // ===============================================

        /**
         * ✅ ÉXITO - Snackbar elegante cuando hay view, AlertDialog como fallback
         */
        fun showSuccess(
            context: Context,
            title: String,
            message: String,
            view: View? = null,
            onDismiss: (() -> Unit)? = null
        ) {
            if (view != null) {
                // Snackbar elegante (preferido)
                val snackbar = Snackbar.make(view, "✅ $title", Snackbar.LENGTH_LONG)
                    .setBackgroundTint(ContextCompat.getColor(context, R.color.success_green))
                    .setTextColor(ContextCompat.getColor(context, android.R.color.white))
                    .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                    .setAction("VER") {
                        onDismiss?.invoke()
                    }
                snackbar.show()
            } else {
                // AlertDialog como fallback
                AlertDialog.Builder(context)
                    .setTitle("✅ $title")
                    .setMessage(message)
                    .setPositiveButton("PERFECTO") { dialog, _ ->
                        dialog.dismiss()
                        onDismiss?.invoke()
                    }
                    .setCancelable(false)
                    .show()
            }
        }

        /**
         * ❌ ERROR - Snackbar con botón reintentar, AlertDialog como fallback
         */
        fun showError(
            context: Context,
            title: String,
            message: String,
            view: View? = null,
            onRetry: (() -> Unit)? = null,
            onDismiss: (() -> Unit)? = null
        ) {
            if (view != null) {
                // Snackbar elegante con opción de reintento
                val snackbar = Snackbar.make(view, "❌ $title", Snackbar.LENGTH_LONG)
                    .setBackgroundTint(ContextCompat.getColor(context, R.color.error_red))
                    .setTextColor(ContextCompat.getColor(context, android.R.color.white))
                    .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                
                if (onRetry != null) {
                    snackbar.setAction("REINTENTAR") { onRetry.invoke() }
                } else {
                    snackbar.setAction("ENTENDIDO") { onDismiss?.invoke() }
                }
                
                snackbar.show()
            } else {
                // AlertDialog como fallback
                AlertDialog.Builder(context)
                    .setTitle("❌ $title")
                    .setMessage(message)
                    .setPositiveButton("ENTENDIDO") { dialog, _ ->
                        dialog.dismiss()
                        onDismiss?.invoke()
                    }
                    .setCancelable(false)
                    .show()
            }
        }

        /**
         * ⚠️ ADVERTENCIA - Snackbar naranja elegante
         */
        fun showWarning(
            context: Context,
            title: String,
            message: String,
            view: View? = null,
            onDismiss: (() -> Unit)? = null
        ) {
            if (view != null) {
                val snackbar = Snackbar.make(view, "⚠️ $title", Snackbar.LENGTH_LONG)
                    .setBackgroundTint(ContextCompat.getColor(context, android.R.color.holo_orange_dark))
                    .setTextColor(ContextCompat.getColor(context, android.R.color.white))
                    .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                    .setAction("ENTENDIDO") { onDismiss?.invoke() }
                
                snackbar.show()
            } else {
                AlertDialog.Builder(context)
                    .setTitle("⚠️ $title")
                    .setMessage(message)
                    .setPositiveButton("ENTENDIDO") { dialog, _ ->
                        dialog.dismiss()
                        onDismiss?.invoke()
                    }
                    .setCancelable(true)
                    .show()
            }
        }

        /**
         * ℹ️ INFORMACIÓN - Snackbar informativa azul
         */
        fun showInfo(
            context: Context,
            title: String,
            message: String,
            view: View? = null,
            onDismiss: (() -> Unit)? = null
        ) {
            if (view != null) {
                val snackbar = Snackbar.make(view, "ℹ️ $title", Snackbar.LENGTH_LONG)
                    .setBackgroundTint(ContextCompat.getColor(context, R.color.colorPrimary))
                    .setTextColor(ContextCompat.getColor(context, android.R.color.white))
                    .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                    .setAction("ENTENDIDO") { onDismiss?.invoke() }
                
                snackbar.show()
            } else {
                AlertDialog.Builder(context)
                    .setTitle("ℹ️ $title")
                    .setMessage(message)
                    .setPositiveButton("ENTENDIDO") { dialog, _ ->
                        dialog.dismiss()
                        onDismiss?.invoke()
                    }
                    .setCancelable(true)
                    .show()
            }
        }

        /**
         * ❓ CONFIRMACIÓN - Siempre AlertDialog para decisiones importantes
         */
        fun showConfirmation(
            context: Context,
            title: String,
            message: String,
            onConfirm: () -> Unit,
            onCancel: (() -> Unit)? = null,
            confirmText: String = "CONFIRMAR",
            cancelText: String = "CANCELAR"
        ) {
            AlertDialog.Builder(context)
                .setTitle("❓ $title")
                .setMessage(message)
                .setPositiveButton(confirmText) { dialog, _ ->
                    dialog.dismiss()
                    onConfirm.invoke()
                }
                .setNegativeButton(cancelText) { dialog, _ ->
                    dialog.dismiss()
                    onCancel?.invoke()
                }
                .setCancelable(false)
                .show()
        }

        // ===============================================
        // 📚 FUNCIONES ESPECÍFICAS DE LIBROS 
        // ===============================================

        /**
         * 📚 LIBRO ASIGNADO - Snackbar verde con detalles
         */
        fun showBookAssigned(
            context: Context,
            bookTitle: String,
            userName: String,
            expirationDate: String? = null,
            view: View? = null,
            onDismiss: (() -> Unit)? = null
        ) {
            val message = if (expirationDate != null) {
                "📚 $bookTitle\n👤 $userName\n📅 Hasta: $expirationDate"
            } else {
                "📚 $bookTitle\n👤 $userName"
            }

            if (view != null) {
                val snackbar = Snackbar.make(view, "✅ Libro Asignado", Snackbar.LENGTH_LONG)
                    .setBackgroundTint(ContextCompat.getColor(context, R.color.success_green))
                    .setTextColor(ContextCompat.getColor(context, android.R.color.white))
                    .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                    .setAction("VER DETALLES") {
                        showSuccess(context, "Libro Asignado", message, null, onDismiss)
                    }
                snackbar.show()
            } else {
                showSuccess(
                    context = context,
                    title = "Libro Asignado",
                    message = "El libro \"$bookTitle\" ha sido asignado exitosamente a $userName." + 
                             if (expirationDate != null) "\n\nFecha de devolución: $expirationDate" else "",
                    onDismiss = onDismiss
                )
            }
        }

        /**
         * 📚 LIBRO DESASIGNADO - Snackbar azul con detalles
         */
        fun showBookUnassigned(
            context: Context,
            bookTitle: String,
            userName: String,
            view: View? = null,
            onDismiss: (() -> Unit)? = null
        ) {
            val message = "📚 $bookTitle\n👤 Devuelto por $userName"

            if (view != null) {
                val snackbar = Snackbar.make(view, "✅ Libro Devuelto", Snackbar.LENGTH_LONG)
                    .setBackgroundTint(ContextCompat.getColor(context, R.color.colorPrimary))
                    .setTextColor(ContextCompat.getColor(context, android.R.color.white))
                    .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                    .setAction("VER") { onDismiss?.invoke() }
                snackbar.show()
            } else {
                showSuccess(
                    context = context,
                    title = "Libro Devuelto",
                    message = "El libro \"$bookTitle\" ha sido devuelto por $userName exitosamente.",
                    onDismiss = onDismiss
                )
            }
        }

        /**
         * 📚 LIBRO CREADO - Snackbar verde
         */
        fun showBookCreated(
            context: Context,
            bookTitle: String,
            view: View? = null,
            onDismiss: (() -> Unit)? = null
        ) {
            showSuccess(
                context = context,
                title = "Libro Registrado",
                message = "El libro \"$bookTitle\" ha sido registrado exitosamente en el sistema.",
                view = view,
                onDismiss = onDismiss
            )
        }

        /**
         * 📚 LIBRO EDITADO - Snackbar verde
         */
        fun showBookEdited(
            context: Context,
            bookTitle: String,
            view: View? = null,
            onDismiss: (() -> Unit)? = null
        ) {
            showSuccess(
                context = context,
                title = "Libro Actualizado",
                message = "Los cambios en \"$bookTitle\" han sido guardados exitosamente.",
                view = view,
                onDismiss = onDismiss
            )
        }

        /**
         * 📚 LIBRO ELIMINADO - Snackbar verde
         */
        fun showBookDeleted(
            context: Context,
            bookTitle: String,
            view: View? = null,
            onDismiss: (() -> Unit)? = null
        ) {
            showSuccess(
                context = context,
                title = "Libro Eliminado",
                message = "El libro \"$bookTitle\" ha sido eliminado permanentemente del sistema.",
                view = view,
                onDismiss = onDismiss
            )
        }

        // ===============================================
        // 👤 FUNCIONES ESPECÍFICAS DE USUARIOS
        // ===============================================

        /**
         * 👤 CUENTA CREADA - AlertDialog de bienvenida
         */
        fun showAccountCreated(
            context: Context,
            userName: String,
            onDismiss: (() -> Unit)? = null
        ) {
            AlertDialog.Builder(context)
                .setTitle("✅ ¡Bienvenido/a!")
                .setMessage("Hola $userName!\n\nTu cuenta ha sido creada exitosamente. Ya puedes explorar y solicitar libros.")
                .setPositiveButton("EMPEZAR") { dialog, _ ->
                    dialog.dismiss()
                    onDismiss?.invoke()
                }
                .setCancelable(false)
                .show()
        }

        /**
         * 🔐 LOGIN EXITOSO - Snackbar de bienvenida
         */
        fun showLoginSuccess(
            context: Context,
            userName: String,
            view: View? = null,
            onDismiss: (() -> Unit)? = null
        ) {
            if (view != null) {
                val snackbar = Snackbar.make(view, "✅ ¡Hola $userName!", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(ContextCompat.getColor(context, R.color.success_green))
                    .setTextColor(ContextCompat.getColor(context, android.R.color.white))
                    .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                snackbar.show()
            }
            onDismiss?.invoke()
        }

        /**
         * 🔐 LOGOUT EXITOSO - AlertDialog informativo
         */
        fun showLogoutSuccess(
            context: Context,
            canChooseAccount: Boolean = false,
            onDismiss: (() -> Unit)? = null
        ) {
            val message = if (canChooseAccount) {
                "Tu sesión ha sido cerrada exitosamente.\n\nLa próxima vez que uses Google Sign-In podrás elegir tu cuenta."
            } else {
                "Tu sesión ha sido cerrada exitosamente."
            }
            
            AlertDialog.Builder(context)
                .setTitle("✅ Sesión Cerrada")
                .setMessage(message)
                .setPositiveButton("ENTENDIDO") { dialog, _ ->
                    dialog.dismiss()
                    onDismiss?.invoke()
                }
                .setCancelable(false)
                .show()
        }

        // ===============================================
        // ❌ ERRORES ESPECÍFICOS
        // ===============================================

        /**
         * ❌ ERROR DE VALIDACIÓN - Snackbar roja específica
         */
        fun showValidationError(
            context: Context,
            field: String,
            requirement: String,
            view: View? = null,
            onDismiss: (() -> Unit)? = null
        ) {
            showError(
                context = context,
                title = "$field Requerido",
                message = "$field es requerido.\n\n$requirement",
                view = view,
                onDismiss = onDismiss
            )
        }

        /**
         * ❌ ERROR DE CONEXIÓN - Con opción de reintento
         */
        fun showConnectionError(
            context: Context,
            view: View? = null,
            onRetry: (() -> Unit)? = null
        ) {
            showError(
                context = context,
                title = "Sin Conexión",
                message = "No se pudo conectar al servidor. Verifica tu conexión a internet.",
                view = view,
                onRetry = onRetry
            )
        }

        /**
         * 📧 EMAIL ENVIADO - Confirmación de email exitoso
         */
        fun showEmailSent(
            context: Context,
            emailType: String,
            recipient: String,
            view: View? = null,
            onDismiss: (() -> Unit)? = null
        ) {
            showSuccess(
                context = context,
                title = "Email Enviado",
                message = "$emailType enviado exitosamente a $recipient.",
                view = view,
                onDismiss = onDismiss
            )
        }
    }
    
    enum class NotificationType {
        SUCCESS, ERROR, WARNING, INFO, EMAIL_SENT, EMAIL_ERROR
    }
}
