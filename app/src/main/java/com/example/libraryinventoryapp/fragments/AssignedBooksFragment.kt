package com.example.libraryinventoryapp.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.example.libraryinventoryapp.utils.NotificationHelper
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.libraryinventoryapp.R
import com.example.libraryinventoryapp.adapters.AssignedBooksAdapter
import com.example.libraryinventoryapp.models.Book
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.Normalizer

class AssignedBooksFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var assignedBooksAdapter: AssignedBooksAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    
    // 🎯 Nuevas vistas para estado vacío
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var textBooksCount: TextView
    private lateinit var btnRefreshEmpty: MaterialButton

    private var assignedBooksList: MutableList<Book> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_assigned_books, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔧 Inicializar vistas
        initializeViews(view)
        setupRecyclerView()
        setupSwipeRefresh()
        setupEmptyState()

        // 🔥 Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // 📚 Cargar datos
        fetchAssignedBooks()

        Log.d("AssignedBooksFragment", "🎨 Vista inicializada correctamente")
    }

    /**
     * 🔧 Inicializar todas las vistas
     */
    private fun initializeViews(view: View) {
        recyclerView = view.findViewById(R.id.assigned_books_recycler_view)
        progressBar = view.findViewById(R.id.progress_bar)
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)
        
        // 🎯 Vistas del estado vacío
        emptyStateLayout = view.findViewById(R.id.empty_state_layout)
        textBooksCount = view.findViewById(R.id.text_books_count)
        btnRefreshEmpty = view.findViewById(R.id.btn_refresh_empty)
    }

    /**
     * 📋 Configurar RecyclerView
     */
    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(context)
    }

    /**
     * 🔄 Configurar SwipeRefresh
     */
    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            fetchAssignedBooks()
        }
    }

    /**
     * 🎯 Configurar estado vacío
     */
    private fun setupEmptyState() {
        btnRefreshEmpty.setOnClickListener {
            fetchAssignedBooks()
        }
    }

    private fun fetchAssignedBooks() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            NotificationHelper.showWarning(
                context = requireContext(),
                title = "Sesión Requerida",
                message = "Debes iniciar sesión para ver tus libros asignados.",
                view = view
            )
            updateEmptyState(true)
            return
        }

        // 🔄 Mostrar loading
        progressBar.visibility = View.VISIBLE
        emptyStateLayout.visibility = View.GONE
        swipeRefreshLayout.visibility = View.VISIBLE
        
        Log.d("AssignedBooksFragment", "🔄 Cargando libros asignados para usuario: ${currentUser.uid}")

        firestore.collection("books")
            .whereArrayContains("assignedTo", currentUser.uid)
            .get()
            .addOnSuccessListener { result ->
                Log.d("AssignedBooksFragment", "📚 Obtenidos ${result.size()} libros asignados")
                
                assignedBooksList.clear()
                for (document in result) {
                    val book = document.toObject(Book::class.java)
                    book.id = document.id
                    assignedBooksList.add(book)
                }

                // Ordenar la lista de libros alfabéticamente por el título sin acentos
                assignedBooksList.sortBy { normalizeText(it.title ?: "") }

                // 🎯 Configurar adaptador
                assignedBooksAdapter = AssignedBooksAdapter(assignedBooksList) { book ->
                    openBookDetail(book.id)
                }
                recyclerView.adapter = assignedBooksAdapter
                
                // 🎯 Actualizar UI
                updateBooksCount()
                updateEmptyState(assignedBooksList.isEmpty())
                
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
                
                Log.d("AssignedBooksFragment", "✅ Libros cargados exitosamente")
            }
            .addOnFailureListener { exception ->
                Log.e("AssignedBooksFragment", "❌ Error cargando libros: ${exception.message}", exception)
                
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
                updateEmptyState(true)
                
                NotificationHelper.showError(
                    context = requireContext(),
                    title = "Error de Carga",
                    message = "No se pudieron cargar los libros asignados: ${exception.message}",
                    view = view
                )
            }
    }

    /**
     * 📊 Actualizar contador de libros
     */
    private fun updateBooksCount() {
        val count = assignedBooksList.size
        textBooksCount.text = when (count) {
            0 -> "Sin libros asignados"
            1 -> "1 libro asignado"
            else -> "$count libros asignados"
        }
    }

    /**
     * 🎯 Mostrar/ocultar estado vacío
     */
    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            emptyStateLayout.visibility = View.VISIBLE
            swipeRefreshLayout.visibility = View.GONE
        } else {
            emptyStateLayout.visibility = View.GONE
            swipeRefreshLayout.visibility = View.VISIBLE
        }
    }

    // Método para normalizar texto (eliminar acentos, convertir a minúsculas)
    private fun normalizeText(text: String): String {
        return java.text.Normalizer.normalize(text.lowercase().trim(), java.text.Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }

    /**
     * 📖 Abrir detalle del libro
     */
    private fun openBookDetail(bookId: String) {
        val fragment = BookDetailModernFragment.newInstance(bookId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.admin_fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}