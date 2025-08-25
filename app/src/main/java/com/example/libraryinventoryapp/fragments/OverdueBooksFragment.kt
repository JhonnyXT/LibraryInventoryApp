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
        overdueAdapter = OverdueBooksAdapter(overdueBooksList) { overdueItem ->
            sendReminderEmail(overdueItem)
        }
        recyclerView.adapter = overdueAdapter

        loadOverdueBooks()
    }

    private fun loadOverdueBooks() {
        progressBar.visibility = View.VISIBLE
        
        firestore.collection("books")
            .get()
            .addOnSuccessListener { result ->
                overdueBooksList.clear()
                val currentTime = System.currentTimeMillis()

                for (document in result) {
                    val book = document.toObject(Book::class.java)
                    book.id = document.id

                    // Verificar si el libro tiene asignaciones y fechas de vencimiento
                    book.loanExpirationDates?.forEachIndexed { index, expirationDate ->
                        if (expirationDate.toDate().time < currentTime) {
                            // Este préstamo está vencido
                            val userId = book.assignedTo?.getOrNull(index)
                            val userName = book.assignedWithNames?.getOrNull(index)
                            val userEmail = book.assignedToEmails?.getOrNull(index)
                            val assignedDate = book.assignedDates?.getOrNull(index)

                            if (userId != null && userName != null) {
                                val daysOverdue = TimeUnit.MILLISECONDS.toDays(
                                    currentTime - expirationDate.toDate().time
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
                                
                                overdueBooksList.add(overdueItem)
                            }
                        }
                    }
                }

                // Ordenar por días de retraso (mayor primero)
                overdueBooksList.sortByDescending { it.daysOverdue }

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
            emptyStateText.text = "🎉 ¡No hay devoluciones pendientes!\n\nTodos los préstamos están al día."
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyStateText.visibility = View.GONE
            overdueAdapter.notifyDataSetChanged()
        }
    }

    private fun sendReminderEmail(overdueItem: OverdueBookItem) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        
        // Obtener información del admin actual
        getCurrentAdminInfo { adminName, adminEmail ->
            
            // Crear mensaje personalizado para libro vencido
            val daysText = if (overdueItem.daysOverdue == 1) "1 día" else "${overdueItem.daysOverdue} días"
            
            // Por ahora usar modo demo - en producción cambiar a sendBookExpirationReminderEmail
            emailService.sendBookExpirationReminderEmailDemo(
                adminEmail = adminEmail,
                userEmail = overdueItem.userEmail,
                userName = overdueItem.userName,
                bookTitle = overdueItem.book.title,
                bookAuthor = overdueItem.book.author,
                adminName = adminName,
                expirationDate = dateFormat.format(overdueItem.expirationDate.toDate()),
                daysOverdue = daysText
            )
            
            Toast.makeText(
                context,
                "📧 Recordatorio enviado a ${overdueItem.userName}\n" +
                "Libro: ${overdueItem.book.title}\n" +
                "Vencido hace: $daysText",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun getCurrentAdminInfo(callback: (String, String) -> Unit) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            callback("Administrador", "admin@biblioteca.com")
            return
        }

        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val adminName = document.getString("name") ?: "Administrador"
                    val adminEmail = currentUser.email ?: "admin@biblioteca.com"
                    callback(adminName, adminEmail)
                } else {
                    callback("Administrador", currentUser.email ?: "admin@biblioteca.com")
                }
            }
            .addOnFailureListener {
                callback("Administrador", currentUser.email ?: "admin@biblioteca.com")
            }
    }
}
