package com.example.libraryinventoryapp.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 🔄 BootReceiver - Receptor de Reinicio del Sistema
 * 
 * FUNCIONALIDADES:
 * ✅ Reprogramar notificaciones después de reinicio del dispositivo
 * ✅ Reprogramar notificaciones después de actualización de la app
 * ✅ Recuperar datos de préstamos activos desde Firebase
 * ✅ Integración con sistema híbrido de escalamiento
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            Log.e(TAG, "❌ Context o Intent nulos en BootReceiver")
            return
        }

        val action = intent.action
        Log.d(TAG, "🔄 === BOOT RECEIVER ACTIVADO ===")
        Log.d(TAG, "🎯 Acción recibida: $action")

        when (action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "📱 Dispositivo reiniciado - Reprogramando notificaciones")
                rescheduleAllNotifications(context)
            }
            
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.d(TAG, "📦 App actualizada - Reprogramando notificaciones")
                rescheduleAllNotifications(context)
            }
            
            else -> {
                Log.d(TAG, "❓ Acción no reconocida: $action")
            }
        }
    }

    /**
     * 🔄 Reprogramar todas las notificaciones activas
     */
    private fun rescheduleAllNotifications(context: Context) {
        Log.d(TAG, "🔄 Iniciando reprogramación de notificaciones...")

        // 🌐 Usar corrutina para operaciones asíncronas con Firebase
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val notificationManager = LibraryNotificationManager(context)
                
                Log.d(TAG, "📚 Consultando libros con préstamos activos...")

                // 📋 Consultar todos los libros con asignaciones activas
                firestore.collection("books")
                    .whereNotEqualTo("assignedTo", null)
                    .get()
                    .addOnSuccessListener { documents ->
                        Log.d(TAG, "✅ Encontrados ${documents.size()} libros con asignaciones")
                        
                        var totalNotificationsScheduled = 0

                        for (document in documents) {
                            try {
                                val bookId = document.id
                                val bookTitle = document.getString("title") ?: "Libro sin título"
                                val bookAuthor = document.getString("author") ?: "Autor desconocido"
                                
                                val assignedTo = document.get("assignedTo") as? List<String> ?: emptyList()
                                val assignedWithNames = document.get("assignedWithNames") as? List<String> ?: emptyList()
                                val loanExpirationDates = document.get("loanExpirationDates") as? List<Timestamp> ?: emptyList()

                                Log.d(TAG, "📖 Procesando libro: $bookTitle")
                                Log.d(TAG, "👥 Usuarios asignados: ${assignedTo.size}")

                                // 🔄 Reprogramar notificaciones para cada usuario asignado
                                assignedTo.forEachIndexed { index, userId ->
                                    try {
                                        val userName = if (index < assignedWithNames.size) {
                                            assignedWithNames[index]
                                        } else {
                                            "Usuario desconocido"
                                        }
                                        
                                        val expirationDate = if (index < loanExpirationDates.size) {
                                            loanExpirationDates[index]
                                        } else {
                                            // 📅 Fecha por defecto si no hay fecha de vencimiento
                                            Timestamp(System.currentTimeMillis() / 1000 + (15 * 24 * 60 * 60), 0)
                                        }

                                        Log.d(TAG, "🔔 Reprogramando para: $userName")
                                        
                                        // 🔔 Programar notificaciones con sistema híbrido
                                        notificationManager.scheduleBookLoanNotifications(
                                            bookId = bookId,
                                            bookTitle = bookTitle,
                                            bookAuthor = bookAuthor,
                                            userId = userId,
                                            userName = userName,
                                            expirationDate = expirationDate
                                        )
                                        
                                        totalNotificationsScheduled++

                                    } catch (e: Exception) {
                                        Log.e(TAG, "❌ Error reprogramando notificación para usuario $index: ${e.message}")
                                    }
                                }

                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error procesando libro ${document.id}: ${e.message}")
                            }
                        }

                        Log.d(TAG, "✅ Reprogramación completada: $totalNotificationsScheduled notificaciones programadas")
                        
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "❌ Error consultando Firebase: ${exception.message}")
                    }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error general en reprogramación: ${e.message}")
            }
        }
    }
}