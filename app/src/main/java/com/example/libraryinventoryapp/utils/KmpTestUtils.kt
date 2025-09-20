package com.example.libraryinventoryapp.utils

import android.util.Log
import com.example.libraryinventoryapp.Greeting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 🧪 KmpTestUtils - Utilidades para probar la integración KMP
 */
object KmpTestUtils {
    
    private const val TAG = "KmpTestUtils"
    
    /**
     * 🚀 Test básico de conectividad KMP
     */
    fun testKmpConnectivity() {
        try {
            val greeting = Greeting().greet()
            Log.i(TAG, "✅ KMP Test: $greeting")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error KMP connectivity: ${e.message}", e)
        }
    }
    
    /**
     * 📧 Test del EmailServiceBridge (DEMO MODE)
     */
    fun testEmailServiceBridge() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.i(TAG, "🧪 Iniciando test de EmailServiceBridge...")
                
                val emailBridge = EmailServiceBridge()
                
                // Test 1: Email de asignación
                Log.i(TAG, "📚 Test 1: Email de asignación")
                val assignmentResult = emailBridge.sendBookAssignmentEmail(
                    adminEmail = "admin@test.com",
                    userEmail = "usuario@test.com", 
                    userName = "Usuario Test",
                    bookTitle = "Libro Test KMP",
                    bookAuthor = "Autor Test",
                    adminName = "Admin Test"
                )
                
                if (assignmentResult.isSuccess) {
                    Log.i(TAG, "✅ Test asignación: ${assignmentResult.getOrNull()}")
                } else {
                    Log.e(TAG, "❌ Test asignación falló: ${assignmentResult.exceptionOrNull()?.message}")
                }
                
                // Test 2: Email de recordatorio  
                Log.i(TAG, "⏰ Test 2: Email de recordatorio")
                val reminderResult = emailBridge.sendBookExpirationReminderEmail(
                    adminEmail = "admin@test.com",
                    userEmail = "usuario@test.com",
                    userName = "Usuario Test", 
                    bookTitle = "Libro Vencido Test",
                    bookAuthor = "Autor Test",
                    adminName = "Admin Test",
                    expirationDate = "20/09/2025",
                    daysOverdue = "Vencido hace 2 días"
                )
                
                if (reminderResult.isSuccess) {
                    Log.i(TAG, "✅ Test recordatorio: ${reminderResult.getOrNull()}")
                } else {
                    Log.e(TAG, "❌ Test recordatorio falló: ${reminderResult.exceptionOrNull()?.message}")
                }
                
                Log.i(TAG, "🎯 Tests de EmailServiceBridge completados")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en tests EmailServiceBridge: ${e.message}", e)
            }
        }
    }
}
