package com.example.libraryinventoryapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.libraryinventoryapp.R
import com.example.libraryinventoryapp.adapters.BookAdapter
import com.example.libraryinventoryapp.models.Book
import com.example.libraryinventoryapp.models.User
import com.google.firebase.firestore.FirebaseFirestore
import java.text.Normalizer

class ViewBooksFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var booksRecyclerView: RecyclerView
    private lateinit var booksAdapter: BookAdapter
    private lateinit var searchView: SearchView
    private var booksList: MutableList<Book> = mutableListOf()
    private var userNamesList: MutableList<String> = mutableListOf()
    private var userList: MutableList<User> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_view_books, container, false)

        booksRecyclerView = view.findViewById(R.id.recyclerViewBookList)
        searchView = view.findViewById(R.id.searchView)

        booksRecyclerView.layoutManager = LinearLayoutManager(context)
        firestore = FirebaseFirestore.getInstance()

        // Inicializar el adaptador vacío
        booksAdapter = BookAdapter(booksList, userNamesList, userList)
        booksRecyclerView.adapter = booksAdapter

        loadUsers()
        loadBooks()

        // Configurar el listener del SearchView
        setupSearchView()

        return view
    }

    private fun loadBooks() {
        firestore.collection("books")
            .get()
            .addOnSuccessListener { result ->
                booksList.clear()
                for (document in result) {
                    val book = document.toObject(Book::class.java)
                    book.id = document.id
                    booksList.add(book)
                }

                // Ordenar la lista de libros alfabéticamente por el título sin tildes
                booksList.sortBy { removeAccents(it.title ?: "") }

                // Actualizar el adaptador
                booksAdapter = BookAdapter(booksList, userNamesList, userList)
                booksRecyclerView.adapter = booksAdapter
            }
            .addOnFailureListener { e ->
                // Manejar el error de carga
                Toast.makeText(context, "Error al cargar los libros: $e", Toast.LENGTH_LONG).show()
            }
    }

    private fun loadUsers() {
        firestore.collection("users")
            .whereEqualTo("role", "usuario")
            .get()
            .addOnSuccessListener { result ->
                userList.clear() // Asegúrate de tener una lista de usuarios para almacenar los datos
                userNamesList.clear() // Esta lista puede seguir existiendo para el autocompletar si lo necesitas
                for (document in result) {
                    val user = document.toObject(User::class.java) // Convierte el documento a tu data class User
                    userList.add(user) // Agrega el usuario a la lista de usuarios
                    userNamesList.add(user.name) // También agrega solo el nombre si aún lo necesitas
                }

                // Actualizar el adaptador con la lista de usuarios
                booksAdapter.updateUserNames(userNamesList)

                // Si necesitas guardar la lista de usuarios en el adaptador, puedes crear un método para ello
                booksAdapter.updateUsers(userList)
            }
            .addOnFailureListener { e ->
                // Manejar el error de carga
                Toast.makeText(context, "Error al cargar los usuarios: $e", Toast.LENGTH_LONG).show()
            }
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterBooks(newText)
                return true
            }
        })
    }

    // Método para eliminar tildes y otros signos diacríticos
    private fun removeAccents(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }

    private fun filterBooks(query: String?) {
        val filteredList = if (!query.isNullOrEmpty()) {
            val normalizedQuery = removeAccents(query.lowercase())
            booksList.filter {
                val normalizedTitle = removeAccents(it.title?.lowercase() ?: "")
                val normalizedIsbn = removeAccents(it.isbn?.lowercase() ?: "")
                normalizedTitle.contains(normalizedQuery) || normalizedIsbn.contains(normalizedQuery)
            }
        } else {
            booksList
        }

        // Ordenar la lista filtrada alfabéticamente por el título sin tildes
        val sortedList = filteredList.sortedBy { removeAccents(it.title ?: "") }

        // Actualizar el adaptador con la lista filtrada y ordenada
        booksAdapter.updateBooks(sortedList)
    }
}