package com.example.libraryinventoryapp.utils

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import com.example.libraryinventoryapp.R

/**
 * 💬 DialogHelper - Popups personalizados profesionales
 * 
 * REEMPLAZA TODOS LOS TOAST CON POPUPS ELEGANTES:
 * ✅ showSuccess() - Para operaciones exitosas
 * ✅ showError() - Para errores y validaciones  
 * ✅ showInfo() - Para mensajes informativos
 * ✅ showConfirmation() - Para confirmar acciones
 * ✅ showWarning() - Para advertencias importantes
 * ✅ showProgress() - Para operaciones en progreso (futuro)
 */
class DialogHelper {

    companion object {
        
        /**
         * ✅ Popup de ÉXITO - Para operaciones completadas exitosamente
         */
        fun showSuccess(
            context: Context,
            title: String,
            message: String,
            onDismiss: (() -> Unit)? = null
        ) {
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
        
        /**
         * ❌ Popup de ERROR - Para errores y validaciones fallidas
         */
        fun showError(
            context: Context,
            title: String,
            message: String,
            onDismiss: (() -> Unit)? = null
        ) {
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
        
        /**
         * ℹ️ Popup INFORMATIVO - Para mensajes generales e información
         */
        fun showInfo(
            context: Context,
            title: String,
            message: String,
            onDismiss: (() -> Unit)? = null
        ) {
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
        
        /**
         * ⚠️ Popup de ADVERTENCIA - Para avisos importantes
         */
        fun showWarning(
            context: Context,
            title: String,
            message: String,
            onDismiss: (() -> Unit)? = null
        ) {
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
        
        /**
         * ❓ Popup de CONFIRMACIÓN - Para confirmar acciones importantes
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
        
        /**
         * 🎯 Popup de ACCIÓN COMPLETADA - Para feedback inmediato de operaciones
         */
        fun showActionCompleted(
            context: Context,
            action: String,
            details: String? = null,
            onDismiss: (() -> Unit)? = null
        ) {
            val message = if (details != null) {
                "$action\n\n$details"
            } else {
                action
            }
            
            AlertDialog.Builder(context)
                .setTitle("🎯 Operación Completada")
                .setMessage(message)
                .setPositiveButton("GENIAL") { dialog, _ ->
                    dialog.dismiss()
                    onDismiss?.invoke()
                }
                .setCancelable(false)
                .show()
        }
        
        /**
         * 📝 Popup con OPCIONES - Para seleccionar entre múltiples opciones
         */
        fun showOptions(
            context: Context,
            title: String,
            options: Array<String>,
            onOptionSelected: (Int, String) -> Unit,
            onCancel: (() -> Unit)? = null
        ) {
            AlertDialog.Builder(context)
                .setTitle("📝 $title")
                .setItems(options) { dialog, which ->
                    dialog.dismiss()
                    onOptionSelected.invoke(which, options[which])
                }
                .setNegativeButton("CANCELAR") { dialog, _ ->
                    dialog.dismiss()
                    onCancel?.invoke()
                }
                .show()
        }
        
        /**
         * 🔄 Popup con REINTENTAR - Para errores con opción de reintento
         */
        fun showRetry(
            context: Context,
            title: String,
            message: String,
            onRetry: () -> Unit,
            onCancel: (() -> Unit)? = null
        ) {
            AlertDialog.Builder(context)
                .setTitle("🔄 $title")
                .setMessage("$message\n\n¿Quieres intentar de nuevo?")
                .setPositiveButton("REINTENTAR") { dialog, _ ->
                    dialog.dismiss()
                    onRetry.invoke()
                }
                .setNegativeButton("CANCELAR") { dialog, _ ->
                    dialog.dismiss()
                    onCancel?.invoke()
                }
                .setCancelable(false)
                .show()
        }

        // ===============================================
        // 📱 CASOS ESPECÍFICOS DE LA APP
        // ===============================================
        
        /**
         * 📚 Popup para LIBRO ASIGNADO exitosamente
         */
        fun showBookAssigned(
            context: Context,
            bookTitle: String,
            userName: String,
            expirationDate: String? = null,
            onDismiss: (() -> Unit)? = null
        ) {
            val message = if (expirationDate != null) {
                "El libro \"$bookTitle\" ha sido asignado exitosamente a $userName.\n\nFecha de devolución: $expirationDate"
            } else {
                "El libro \"$bookTitle\" ha sido asignado exitosamente a $userName."
            }
            
            showSuccess(
                context = context,
                title = "Libro Asignado",
                message = message,
                onDismiss = onDismiss
            )
        }
        
        /**
         * 📚 Popup para LIBRO DESASIGNADO exitosamente
         */
        fun showBookUnassigned(
            context: Context,
            bookTitle: String,
            userName: String,
            onDismiss: (() -> Unit)? = null
        ) {
            showSuccess(
                context = context,
                title = "Libro Devuelto",
                message = "El libro \"$bookTitle\" ha sido devuelto por $userName exitosamente.",
                onDismiss = onDismiss
            )
        }
        
        /**
         * 📚 Popup para LIBRO CREADO exitosamente
         */
        fun showBookCreated(
            context: Context,
            bookTitle: String,
            onDismiss: (() -> Unit)? = null
        ) {
            showSuccess(
                context = context,
                title = "Libro Registrado",
                message = "El libro \"$bookTitle\" ha sido registrado exitosamente en el sistema.",
                onDismiss = onDismiss
            )
        }
        
        /**
         * 📚 Popup para LIBRO EDITADO exitosamente
         */
        fun showBookEdited(
            context: Context,
            bookTitle: String,
            onDismiss: (() -> Unit)? = null
        ) {
            showSuccess(
                context = context,
                title = "Libro Actualizado",
                message = "Los cambios en \"$bookTitle\" han sido guardados exitosamente.",
                onDismiss = onDismiss
            )
        }
        
        /**
         * 📚 Popup para LIBRO ELIMINADO exitosamente
         */
        fun showBookDeleted(
            context: Context,
            bookTitle: String,
            onDismiss: (() -> Unit)? = null
        ) {
            showSuccess(
                context = context,
                title = "Libro Eliminado",
                message = "El libro \"$bookTitle\" ha sido eliminado permanentemente del sistema.",
                onDismiss = onDismiss
            )
        }
        
        /**
         * 👤 Popup para CUENTA CREADA exitosamente
         */
        fun showAccountCreated(
            context: Context,
            userName: String,
            onDismiss: (() -> Unit)? = null
        ) {
            showSuccess(
                context = context,
                title = "Cuenta Creada",
                message = "¡Bienvenido/a $userName!\n\nTu cuenta ha sido creada exitosamente. Ya puedes explorar y solicitar libros.",
                onDismiss = onDismiss
            )
        }
        
        /**
         * 🔐 Popup para LOGIN exitoso
         */
        fun showLoginSuccess(
            context: Context,
            userName: String,
            onDismiss: (() -> Unit)? = null
        ) {
            showSuccess(
                context = context,
                title = "Acceso Autorizado",
                message = "¡Hola $userName!\n\nHas iniciado sesión exitosamente.",
                onDismiss = onDismiss
            )
        }
        
        /**
         * 🔐 Popup para LOGOUT exitoso
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
            
            showSuccess(
                context = context,
                title = "Sesión Cerrada",
                message = message,
                onDismiss = onDismiss
            )
        }
        
        /**
         * 📧 Popup para EMAIL ENVIADO exitosamente
         */
        fun showEmailSent(
            context: Context,
            emailType: String,
            recipient: String,
            onDismiss: (() -> Unit)? = null
        ) {
            showSuccess(
                context = context,
                title = "Email Enviado",
                message = "$emailType enviado exitosamente a $recipient.",
                onDismiss = onDismiss
            )
        }
        
        /**
         * 📅 Popup para FECHAS ACTUALIZADAS exitosamente
         */
        fun showDatesUpdated(
            context: Context,
            bookTitle: String,
            onDismiss: (() -> Unit)? = null
        ) {
            showSuccess(
                context = context,
                title = "Fechas Actualizadas",
                message = "Las fechas de préstamo de \"$bookTitle\" han sido actualizadas exitosamente.",
                onDismiss = onDismiss
            )
        }
        
        // ===============================================
        // ❌ CASOS DE ERROR ESPECÍFICOS
        // ===============================================
        
        /**
         * ❌ Error de VALIDACIÓN general
         */
        fun showValidationError(
            context: Context,
            field: String,
            requirement: String,
            onDismiss: (() -> Unit)? = null
        ) {
            showError(
                context = context,
                title = "Campo Requerido",
                message = "$field es requerido.\n\n$requirement",
                onDismiss = onDismiss
            )
        }
        
        /**
         * ❌ Error de CONEXIÓN
         */
        fun showConnectionError(
            context: Context,
            onRetry: (() -> Unit)? = null
        ) {
            if (onRetry != null) {
                showRetry(
                    context = context,
                    title = "Sin Conexión",
                    message = "No se pudo conectar al servidor. Verifica tu conexión a internet.",
                    onRetry = onRetry
                )
            } else {
                showError(
                    context = context,
                    title = "Sin Conexión",
                    message = "No se pudo conectar al servidor. Verifica tu conexión a internet e intenta de nuevo."
                )
            }
        }
        
        /**
         * ❌ Error de PERMISOS
         */
        fun showPermissionError(
            context: Context,
            permission: String,
            onDismiss: (() -> Unit)? = null
        ) {
            showError(
                context = context,
                title = "Permiso Requerido",
                message = "La aplicación necesita acceso a $permission para realizar esta acción.\n\nPor favor, otorga el permiso en la configuración de la app.",
                onDismiss = onDismiss
            )
        }
    }
}
