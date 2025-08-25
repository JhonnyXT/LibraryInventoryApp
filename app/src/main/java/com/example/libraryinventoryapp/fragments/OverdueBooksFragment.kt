package com.example.libraryinventoryapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.libraryinventoryapp.R
import com.example.libraryinventoryapp.adapters.OverdueBooksAdapter
import com.example.libraryinventoryapp.models.Book
import com.example.libraryinventoryapp.models.OverdueBookItem
import com.example.libraryinventoryapp.utils.EmailService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class OverdueBooksFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var overdueAdapter: OverdueBooksAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var emptyStateText: TextView
    
    private val emailService = EmailService()
    private var overdueBooksList: MutableList<OverdueBookItem> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_overdue_books, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.overdue_books_recycler_view)
        progressBar = view.findViewById(R.id.progress_bar)
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)
        emptyStateText = view.findViewById(R.id.empty_state_text)

        recyclerView.layoutManager = LinearLayoutManager(context)
        firestore = FirebaseFirestore.getInstance()

        // Configurar SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            loadOverdueBooks()
        }

        // Configurar el adaptador
        overdueAdapter = OverdueBooksAdapter(
            overdueBooksList,
            onSendReminderClick = { overdueItem, hideProgress ->
                sendReminderEmail(overdueItem, hideProgress)
            },
            onBookReturned = { overdueItem ->
                removeBookFromList(overdueItem)
            }
        )
        recyclerView.adapter = overdueAdapter

        loadOverdueBooks()
    }

    private fun loadOverdueBooks() {
        Log.i("OverdueBooksFragment", "🔄 INICIANDO CARGA DE LIBROS VENCIDOS/PRÓXIMOS A VENCER")
        progressBar.visibility = View.VISIBLE
        
        firestore.collection("books")
            .get()
            .addOnSuccessListener { result ->
                Log.d("OverdueBooksFragment", "📚 Obtenidos ${result.size()} libros de Firestore")
                overdueBooksList.clear()
                val currentTime = System.currentTimeMillis()
                val fiveDaysFromNow = currentTime + (5 * 24 * 60 * 60 * 1000L) // 5 días en milisegundos

                for (document in result) {
                    val book = document.toObject(Book::class.java)
                    book.id = document.id

                    // Verificar si el libro tiene asignaciones y fechas de vencimiento
                    book.loanExpirationDates?.forEachIndexed { index, expirationDate ->
                        val expirationTime = expirationDate.toDate().time
                        
                        // Incluir libros vencidos Y próximos a vencer (dentro de 5 días)
                        if (expirationTime <= fiveDaysFromNow) {
                            val userId = book.assignedTo?.getOrNull(index)
                            val userName = book.assignedWithNames?.getOrNull(index)
                            val userEmail = book.assignedToEmails?.getOrNull(index)
                            val assignedDate = book.assignedDates?.getOrNull(index)

                            if (userId != null && userName != null) {
                                // Calcular días: positivo = vencido, negativo = próximo a vencer, 0 = vence hoy
                                val daysOverdue = TimeUnit.MILLISECONDS.toDays(
                                    currentTime - expirationTime
                                ).toInt()

                                val overdueItem = OverdueBookItem(
                                    book = book,
                                    userId = userId,
                                    userName = userName,
                                    userEmail = userEmail ?: "",
                                    expirationDate = expirationDate,
                                    assignedDate = assignedDate,
                                    daysOverdue = daysOverdue
                                )
                                
                                // Log detallado de cada item cargado
                                Log.d("OverdueBooksFragment", """
                                    📖 Libro cargado para alertas:
                                    - Título: ${book.title}
                                    - Usuario: $userName
                                    - Email: ${userEmail ?: "NO DISPONIBLE ⚠️"}
                                    - Días diferencia: $daysOverdue
                                    - Fecha vencimiento: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(expirationDate.toDate())}
                                """.trimIndent())
                                
                                overdueBooksList.add(overdueItem)
                            } else {
                                Log.w("OverdueBooksFragment", "⚠️ Usuario incompleto para libro ${book.title}: userId=$userId, userName=$userName")
                            }
                        }
                    }
                }

                // Ordenar: primero los más vencidos, luego los próximos a vencer
                overdueBooksList.sortByDescending { it.daysOverdue }

                // Log resumen de la carga
                Log.i("OverdueBooksFragment", """
                    📊 RESUMEN DE CARGA DE LIBROS:
                    - Total libros cargados: ${overdueBooksList.size}
                    - Vencidos: ${overdueBooksList.count { it.daysOverdue > 0 }}
                    - Vencen hoy: ${overdueBooksList.count { it.daysOverdue == 0 }}
                    - Próximos a vencer: ${overdueBooksList.count { it.daysOverdue < 0 }}
                    - Con email: ${overdueBooksList.count { it.userEmail.isNotBlank() }}
                    - Sin email: ${overdueBooksList.count { it.userEmail.isBlank() }}
                """.trimIndent())

                // Actualizar UI
                updateUI()
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
            }
            .addOnFailureListener { exception ->
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
                Toast.makeText(
                    context, 
                    "Error al cargar devoluciones pendientes: ${exception.message}", 
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun updateUI() {
        if (overdueBooksList.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyStateText.visibility = View.VISIBLE
            emptyStateText.text = "🎉 ¡Todo está al día!\n\nNo hay libros vencidos ni próximos a vencer en los próximos 5 días."
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyStateText.visibility = View.GONE
            overdueAdapter.notifyDataSetChanged()
        }
    }

    private fun sendReminderEmail(overdueItem: OverdueBookItem, hideProgress: () -> Unit) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        
        // Logs detallados del inicio del proceso
        Log.i("OverdueBooksFragment", """
            🎯 INICIANDO PROCESO DE RECORDATORIO 🎯
            Usuario: ${overdueItem.userName}
            Email usuario: ${overdueItem.userEmail}
            Libro: ${overdueItem.book.title}
            Autor: ${overdueItem.book.author}
            Fecha vencimiento: ${dateFormat.format(overdueItem.expirationDate.toDate())}
            Días de diferencia: ${overdueItem.daysOverdue}
        """.trimIndent())
        
        // Verificar que el email del usuario no esté vacío
        if (overdueItem.userEmail.isBlank()) {
            Log.e("OverdueBooksFragment", "❌ ERROR: Email del usuario está vacío para ${overdueItem.userName}")
            hideProgress()
            Toast.makeText(context, "❌ Error: No se encontró email para ${overdueItem.userName}", Toast.LENGTH_LONG).show()
            return
        }
        
        // Obtener información del admin actual
        Log.d("OverdueBooksFragment", "🔍 Obteniendo información del admin...")
        getCurrentAdminInfo { adminName, adminEmail ->
            
            Log.d("OverdueBooksFragment", "✅ Info admin obtenida: $adminName ($adminEmail)")
            
            // Crear mensaje personalizado según el estado del libro
            val daysText = when {
                overdueItem.daysOverdue > 0 -> {
                    if (overdueItem.daysOverdue == 1) "Vencido hace 1 día" else "Vencido hace ${overdueItem.daysOverdue} días"
                }
                overdueItem.daysOverdue == 0 -> "Vence hoy"
                else -> {
                    val daysUntil = kotlin.math.abs(overdueItem.daysOverdue)
                    if (daysUntil == 1) "Vence mañana" else "Vence en $daysUntil días"
                }
            }
            
            Log.i("OverdueBooksFragment", "📧 Enviando recordatorio REAL via SendGrid...")
            
            // CAMBIO: Usar la versión REAL de SendGrid (no demo)
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val result = emailService.sendBookExpirationReminderEmail(
                        adminEmail = adminEmail,
                        userEmail = overdueItem.userEmail,
                        userName = overdueItem.userName,
                        bookTitle = overdueItem.book.title,
                        bookAuthor = overdueItem.book.author,
                        adminName = adminName,
                        expirationDate = dateFormat.format(overdueItem.expirationDate.toDate()),
                        daysOverdue = daysText
                    )
                    
                    if (result.isSuccess) {
                        hideProgress()
                        Log.i("OverdueBooksFragment", "✅ Recordatorio enviado exitosamente!")
                        
                        // Toast personalizado según el estado (sin admin)
                        val toastMessage = when {
                            overdueItem.daysOverdue > 0 -> {
                                "✅ Recordatorio enviado a ${overdueItem.userName}\n" +
                                "📧 Email: ${overdueItem.userEmail}\n" +
                                "📚 Libro: ${overdueItem.book.title}\n" +
                                "⚠️ $daysText"
                            }
                            overdueItem.daysOverdue == 0 -> {
                                "✅ Recordatorio enviado a ${overdueItem.userName}\n" +
                                "📧 Email: ${overdueItem.userEmail}\n" +
                                "📚 Libro: ${overdueItem.book.title}\n" +
                                "🔥 Vence HOY"
                            }
                            else -> {
                                "✅ Recordatorio enviado a ${overdueItem.userName}\n" +
                                "📧 Email: ${overdueItem.userEmail}\n" +
                                "📚 Libro: ${overdueItem.book.title}\n" +
                                "⏳ $daysText"
                            }
                        }
                        
                        Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()
                        
                    } else {
                        hideProgress()
                        val errorMsg = result.exceptionOrNull()?.message ?: "Error desconocido"
                        Log.e("OverdueBooksFragment", "❌ Error enviando recordatorio: $errorMsg")
                        Toast.makeText(context, "❌ Error enviando recordatorio: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                    
                } catch (e: Exception) {
                    hideProgress()
                    Log.e("OverdueBooksFragment", "❌ Excepción enviando recordatorio: ${e.message}", e)
                    Toast.makeText(context, "❌ Error inesperado: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun removeBookFromList(overdueItem: OverdueBookItem) {
        Log.i("OverdueBooksFragment", """
            🗑️ REMOVIENDO LIBRO DE LA LISTA:
            Usuario: ${overdueItem.userName}
            Libro: ${overdueItem.book.title}
            Lista antes: ${overdueBooksList.size} items
        """.trimIndent())
        
        // Buscar y remover el item específico de la lista
        val position = overdueBooksList.indexOfFirst { 
            it.book.id == overdueItem.book.id && it.userId == overdueItem.userId 
        }
        
        if (position != -1) {
            overdueBooksList.removeAt(position)
            overdueAdapter.notifyItemRemoved(position)
            
            Log.i("OverdueBooksFragment", """
                ✅ LIBRO REMOVIDO EXITOSAMENTE:
                Posición removida: $position
                Lista después: ${overdueBooksList.size} items
            """.trimIndent())
            
            // Actualizar UI si la lista quedó vacía
            updateUI()
            
            // Log resumen actualizado
            Log.i("OverdueBooksFragment", """
                📊 RESUMEN ACTUALIZADO:
                - Total libros: ${overdueBooksList.size}
                - Vencidos: ${overdueBooksList.count { it.daysOverdue > 0 }}
                - Vencen hoy: ${overdueBooksList.count { it.daysOverdue == 0 }}
                - Próximos a vencer: ${overdueBooksList.count { it.daysOverdue < 0 }}
            """.trimIndent())
            
        } else {
            Log.w("OverdueBooksFragment", "⚠️ No se encontró el libro para remover de la lista")
        }
    }

    private fun getCurrentAdminInfo(callback: (String, String) -> Unit) {
        Log.d("OverdueBooksFragment", "🔍 Iniciando obtención de información del admin...")
        
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.w("OverdueBooksFragment", "⚠️ No hay usuario autenticado, usando datos por defecto")
            callback("Administrador", "admin@biblioteca.com")
            return
        }

        Log.d("OverdueBooksFragment", "👤 Usuario autenticado: ${currentUser.uid}, email: ${currentUser.email}")
        
        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val adminName = document.getString("name") ?: "Administrador"
                    val adminEmail = currentUser.email ?: "admin@biblioteca.com"
                    val userRole = document.getString("role") ?: "desconocido"
                    
                    Log.i("OverdueBooksFragment", """
                        ✅ Información del admin obtenida exitosamente:
                        Nombre: $adminName
                        Email: $adminEmail
                        Rol: $userRole
                        UID: ${currentUser.uid}
                    """.trimIndent())
                    
                    callback(adminName, adminEmail)
                } else {
                    Log.w("OverdueBooksFragment", "⚠️ Documento de usuario no existe, usando email de Firebase Auth")
                    val fallbackEmail = currentUser.email ?: "admin@biblioteca.com"
                    Log.d("OverdueBooksFragment", "📧 Email de fallback: $fallbackEmail")
                    callback("Administrador", fallbackEmail)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("OverdueBooksFragment", "❌ Error obteniendo datos del admin: ${exception.message}", exception)
                val fallbackEmail = currentUser.email ?: "admin@biblioteca.com"
                Log.d("OverdueBooksFragment", "📧 Email de fallback por error: $fallbackEmail")
                callback("Administrador", fallbackEmail)
            }
    }
}
