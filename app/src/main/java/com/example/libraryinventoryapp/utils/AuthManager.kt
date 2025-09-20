package com.example.libraryinventoryapp.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.Fragment
import com.example.libraryinventoryapp.LoginActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * 🔐 AuthManager - Gestor centralizado de autenticación
 * 
 * FUNCIONALIDADES:
 * ✅ Logout completo de Firebase + Google Sign-In
 * ✅ Limpieza de caché de Google para mostrar selector de cuentas
 * ✅ Redirección segura a LoginActivity
 * ✅ Manejo de errores robusto
 */
class AuthManager private constructor() {

    companion object {
        private const val TAG = "AuthManager"
        
        @Volatile
        private var INSTANCE: AuthManager? = null
        
        fun getInstance(): AuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthManager().also { INSTANCE = it }
            }
        }
    }

    /**
     * 🚪 Logout completo desde Fragment
     */
    fun performCompleteLogout(fragment: Fragment, showSuccessMessage: Boolean = true) {
        val context = fragment.requireContext()
        val activity = fragment.requireActivity()
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.i(TAG, "🚪 Iniciando logout completo desde Fragment...")
                
                // 1. Logout de Google Sign-In
                val success = logoutFromGoogle(context)
                
                // 2. Logout de Firebase
                FirebaseAuth.getInstance().signOut()
                Log.d(TAG, "✅ Firebase logout completado")
                
                // 3. Detener servicio de lista de deseos
                try {
                    val wishlistService = WishlistAvailabilityService.getInstance(context)
                    wishlistService.stopMonitoring()
                    Log.d(TAG, "✅ Servicio de lista de deseos detenido")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Error deteniendo servicio de lista de deseos: ${e.message}")
                }
                
                // 4. Mostrar mensaje de éxito
                if (showSuccessMessage) {
                    if (success) {
                        Toast.makeText(context, "🔐 Sesión cerrada correctamente - Podrás elegir cuenta", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "🔐 Sesión cerrada - Logout Google parcial", Toast.LENGTH_LONG).show()
                    }
                }
                
                // 5. Navegar a LoginActivity
                navigateToLogin(activity)
                
                Log.i(TAG, "✅ Logout completo finalizado exitosamente")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en logout completo: ${e.message}", e)
                
                // Fallback: logout básico
                try {
                    FirebaseAuth.getInstance().signOut()
                    Toast.makeText(context, "⚠️ Logout parcial - Error con Google Sign-In", Toast.LENGTH_LONG).show()
                    navigateToLogin(activity)
                } catch (fallbackError: Exception) {
                    Log.e(TAG, "❌ Error crítico en logout: ${fallbackError.message}")
                    Toast.makeText(context, "❌ Error crítico al cerrar sesión", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    /**
     * 🚪 Logout completo desde Activity
     */
    fun performCompleteLogout(activity: FragmentActivity, showSuccessMessage: Boolean = true) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.i(TAG, "🚪 Iniciando logout completo desde Activity...")
                
                // 1. Logout de Google Sign-In
                val success = logoutFromGoogle(activity)
                
                // 2. Logout de Firebase
                FirebaseAuth.getInstance().signOut()
                Log.d(TAG, "✅ Firebase logout completado")
                
                // 3. Detener servicio de lista de deseos
                try {
                    val wishlistService = WishlistAvailabilityService.getInstance(activity)
                    wishlistService.stopMonitoring()
                    Log.d(TAG, "✅ Servicio de lista de deseos detenido")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Error deteniendo servicio de lista de deseos: ${e.message}")
                }
                
                // 4. Mostrar mensaje de éxito
                if (showSuccessMessage) {
                    if (success) {
                        Toast.makeText(activity, "🔐 Sesión cerrada correctamente - Podrás elegir cuenta", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(activity, "🔐 Sesión cerrada - Logout Google parcial", Toast.LENGTH_LONG).show()
                    }
                }
                
                // 5. Navegar a LoginActivity
                navigateToLogin(activity)
                
                Log.i(TAG, "✅ Logout completo finalizado exitosamente")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en logout completo: ${e.message}", e)
                
                // Fallback: logout básico
                try {
                    FirebaseAuth.getInstance().signOut()
                    Toast.makeText(activity, "⚠️ Logout parcial - Error con Google Sign-In", Toast.LENGTH_LONG).show()
                    navigateToLogin(activity)
                } catch (fallbackError: Exception) {
                    Log.e(TAG, "❌ Error crítico en logout: ${fallbackError.message}")
                    Toast.makeText(activity, "❌ Error crítico al cerrar sesión", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    /**
     * 🔓 Logout de Google Sign-In con limpieza completa
     */
    private suspend fun logoutFromGoogle(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Configurar Google Sign-In Client igual que en LoginActivity
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(com.example.libraryinventoryapp.R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                
                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                
                Log.d(TAG, "🔓 Haciendo logout de Google Sign-In...")
                
                // Método 1: signOut() - Desconecta la cuenta actual
                googleSignInClient.signOut().await()
                Log.d(TAG, "✅ Google Sign-In signOut() completado")
                
                // Método 2: revokeAccess() - Limpia completamente la caché y tokens
                // Esto fuerza que aparezca el selector de cuentas la próxima vez
                googleSignInClient.revokeAccess().await()
                Log.d(TAG, "✅ Google Sign-In revokeAccess() completado")
                
                Log.i(TAG, "🎉 Google Sign-In logout completo exitoso")
                true
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en Google Sign-In logout: ${e.message}", e)
                
                // Intentar solo signOut() como fallback
                try {
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(context.getString(com.example.libraryinventoryapp.R.string.default_web_client_id))
                        .requestEmail()
                        .build()
                    
                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                    googleSignInClient.signOut().await()
                    Log.d(TAG, "⚠️ Fallback: Solo signOut() completado")
                    true
                } catch (fallbackError: Exception) {
                    Log.e(TAG, "❌ Error también en fallback Google logout: ${fallbackError.message}")
                    false
                }
            }
        }
    }
    
    /**
     * 🏠 Navegar a LoginActivity con limpieza de stack
     */
    private fun navigateToLogin(activity: FragmentActivity) {
        try {
            val intent = Intent(activity, LoginActivity::class.java).apply {
                // Limpiar completamente el stack de actividades
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or 
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            
            activity.startActivity(intent)
            
            // Cerrar todas las actividades de la aplicación
            activity.finishAffinity()
            
            Log.d(TAG, "🏠 Navegación a LoginActivity completada")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error navegando a LoginActivity: ${e.message}", e)
            
            // Fallback básico
            try {
                val basicIntent = Intent(activity, LoginActivity::class.java)
                activity.startActivity(basicIntent)
                activity.finish()
            } catch (fallbackError: Exception) {
                Log.e(TAG, "❌ Error crítico en navegación: ${fallbackError.message}")
            }
        }
    }
}
