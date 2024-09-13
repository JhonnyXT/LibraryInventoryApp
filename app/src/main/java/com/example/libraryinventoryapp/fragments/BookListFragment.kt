package com.example.libraryinventoryapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.libraryinventoryapp.R
import com.example.libraryinventoryapp.adapters.BookListAdapter
import com.example.libraryinventoryapp.models.Book
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BookListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var bookListAdapter: BookListAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_book_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.books_recycler_view)
        progressBar = view.findViewById(R.id.progress_bar)
        recyclerView.layoutManager = LinearLayoutManager(context)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        fetchBooks()
    }

    private fun fetchBooks() {
        progressBar.visibility = View.VISIBLE

        firestore.collection("books").get()
            .addOnSuccessListener { result ->
                val books = mutableListOf<Book>()
                for (document in result) {
                    val book = document.toObject(Book::class.java)
                    book.id = document.id
                    books.add(book)
                }

                bookListAdapter = BookListAdapter(books) { book ->
                    assignBook(book)
                }
                recyclerView.adapter = bookListAdapter
                progressBar.visibility = View.GONE
            }
            .addOnFailureListener { exception ->
                progressBar.visibility = View.GONE
                Toast.makeText(context, "Error al cargar los libros: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun assignBook(book: Book) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Debes iniciar sesión para asignar un libro.", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid
        val userEmail = currentUser.email

        if (book.status == "Asignado") {
            Toast.makeText(context, "El libro ya está asignado.", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE

        firestore.collection("books").document(book.id).update(
            mapOf(
                "status" to "Asignado",
                "assignedTo" to userId,
                "assignedToEmail" to userEmail
            )
        ).addOnSuccessListener {
            Toast.makeText(context, "Libro asignado exitosamente.", Toast.LENGTH_SHORT).show()
            fetchBooks()  // Refresh the list
        }.addOnFailureListener { exception ->
            Toast.makeText(context, "Error al asignar el libro: ${exception.message}", Toast.LENGTH_LONG).show()
        }.addOnCompleteListener {
            progressBar.visibility = View.GONE
        }
    }
}