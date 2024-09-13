package com.example.libraryinventoryapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.libraryinventoryapp.R
import com.example.libraryinventoryapp.adapters.BookAdapter
import com.example.libraryinventoryapp.models.Book
import com.google.firebase.firestore.FirebaseFirestore

class ViewBooksFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var booksRecyclerView: RecyclerView
    private lateinit var booksAdapter: BookAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_view_books, container, false)

        booksRecyclerView = view.findViewById(R.id.recyclerViewBookList)
        booksRecyclerView.layoutManager = LinearLayoutManager(context)
        firestore = FirebaseFirestore.getInstance()

        loadBooks()

        return view
    }

    private fun loadBooks() {
        firestore.collection("books")
            .get()
            .addOnSuccessListener { result ->
                val books = mutableListOf<Book>()
                for (document in result) {
                    val book = document.toObject(Book::class.java)
                    book.id = document.id
                    books.add(book)
                }
                booksAdapter = BookAdapter(books)
                booksRecyclerView.adapter = booksAdapter
            }
            .addOnFailureListener { e ->
                // Manejar el error de carga
                Toast.makeText(context, "Error al cargar los libros: $e", Toast.LENGTH_LONG).show()
            }
    }
}