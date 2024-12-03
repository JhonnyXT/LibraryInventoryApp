package com.example.libraryinventoryapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private lateinit var searchView: SearchView
    private var assignedBooksList: MutableList<Book> = mutableListOf()
    private var filteredAssignedBooksList: MutableList<Book> = mutableListOf()

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
        searchView = view.findViewById(R.id.searchView)

        recyclerView.layoutManager = LinearLayoutManager(context)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        fetchAssignedBooks()

        // Configuración del SearchView para filtrar libros asignados
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterAssignedBooks(newText ?: "")
                return true
            }
        })
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

                filteredAssignedBooksList = assignedBooksList.toMutableList()
                assignedBooksAdapter = AssignedBooksAdapter(filteredAssignedBooksList)
                recyclerView.adapter = assignedBooksAdapter
                progressBar.visibility = View.GONE
            }
            .addOnFailureListener { exception ->
                progressBar.visibility = View.GONE
                Toast.makeText(context, "Error al cargar los libros: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    // Método para filtrar libros asignados
    private fun filterAssignedBooks(query: String) {
        val normalizedQuery = removeAccents(query.toLowerCase())

        filteredAssignedBooksList.clear()
        filteredAssignedBooksList.addAll(
            assignedBooksList.filter {
                val normalizedTitle = removeAccents(it.title.toLowerCase())
                val normalizedAuthor = removeAccents(it.author.toLowerCase())
                val normalizedDescription = removeAccents(it.description.toLowerCase())
                normalizedTitle.contains(normalizedQuery) ||
                        normalizedAuthor.contains(normalizedQuery) ||
                        normalizedDescription.contains(normalizedQuery)
            }
        )

        assignedBooksAdapter.notifyDataSetChanged()
    }

    // Método para eliminar tildes y otros signos diacríticos
    private fun removeAccents(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }
}