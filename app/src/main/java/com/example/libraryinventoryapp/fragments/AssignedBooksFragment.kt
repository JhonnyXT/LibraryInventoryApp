package com.example.libraryinventoryapp.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.libraryinventoryapp.R
import com.example.libraryinventoryapp.adapters.AssignedBooksAdapter
import com.example.libraryinventoryapp.models.Book
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

    private var assignedBooksList: MutableList<Book> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_assigned_books, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.assigned_books_recycler_view)
        progressBar = view.findViewById(R.id.progress_bar)
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)

        recyclerView.layoutManager = LinearLayoutManager(context)
        
        // Configurar SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            fetchAssignedBooks()
        }

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        fetchAssignedBooks()

    }

    private fun fetchAssignedBooks() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Debes iniciar sesión para ver tus libros asignados.", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE

        firestore.collection("books")
            .whereArrayContains("assignedTo", currentUser.uid)
            .get()
            .addOnSuccessListener { result ->
                assignedBooksList.clear()
                for (document in result) {
                    val book = document.toObject(Book::class.java)
                    book.id = document.id
                    assignedBooksList.add(book)
                }

                // Ordenar la lista de libros alfabéticamente por el título sin acentos
                assignedBooksList.sortBy { normalizeText(it.title ?: "") }

                assignedBooksAdapter = AssignedBooksAdapter(assignedBooksList) { book ->
                    openBookDetail(book.id)
                }
                recyclerView.adapter = assignedBooksAdapter
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
            }
            .addOnFailureListener { exception ->
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
                Toast.makeText(context, "Error al cargar los libros: ${exception.message}", Toast.LENGTH_LONG).show()
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