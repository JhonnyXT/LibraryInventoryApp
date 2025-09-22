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
    
    /**
     * 🔐 Test del AuthServiceBridge (DEMO MODE)
     */
    fun testAuthServiceBridge() {
        try {
            Log.i(TAG, "🧪 Iniciando test de AuthServiceBridge...")
            
            val authBridge = AuthServiceBridge.getInstance()
            
            // Test 1: Verificar instancia
            Log.i(TAG, "🔐 Test 1: Verificar instancia de AuthServiceBridge")
            Log.i(TAG, "✅ AuthServiceBridge instanciado correctamente")
            
            // Test 2: getCurrentUser (sin autenticación)
            Log.i(TAG, "👤 Test 2: getCurrentUser (sin autenticación)")
            val currentUser = authBridge.getCurrentUser()
            if (currentUser != null) {
                Log.i(TAG, "✅ Usuario actual: ${currentUser.name} (${currentUser.email})")
            } else {
                Log.i(TAG, "ℹ️ No hay usuario autenticado (esperado)")
            }
            
            Log.i(TAG, "🎯 Tests de AuthServiceBridge completados")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en tests AuthServiceBridge: ${e.message}", e)
        }
    }
    
    /**
     * 🌟 Test del WishlistServiceBridge (DEMO MODE)
     */
    fun testWishlistServiceBridge(context: android.content.Context) {
        try {
            Log.i(TAG, "🧪 Iniciando test de WishlistServiceBridge...")
            
            val wishlistBridge = WishlistServiceBridge.getInstance(context)
            
            // Test 1: Verificar instancia
            Log.i(TAG, "🌟 Test 1: Verificar instancia de WishlistServiceBridge")
            Log.i(TAG, "✅ WishlistServiceBridge instanciado correctamente")
            
            // Test 2: startMonitoring (sin usuario autenticado - debería advertir)
            Log.i(TAG, "🚀 Test 2: startMonitoring (sin autenticación)")
            wishlistBridge.startMonitoring()
            
            // Test 3: stopMonitoring
            Log.i(TAG, "🛑 Test 3: stopMonitoring")
            wishlistBridge.stopMonitoring()
            
            Log.i(TAG, "🎯 Tests de WishlistServiceBridge completados")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en tests WishlistServiceBridge: ${e.message}", e)
        }
    }
    
    /**
     * 🚀 Test completo de todos los bridges KMP
     */
    fun testAllKmpBridges(context: android.content.Context) {
        Log.i(TAG, "🚀 Iniciando tests completos de KMP Bridges...")
        
        testKmpConnectivity()
        testEmailServiceBridge()
        testAuthServiceBridge()
        testWishlistServiceBridge(context)
        
        Log.i(TAG, "✅ Tests completos de KMP Bridges finalizados")
    }
}
