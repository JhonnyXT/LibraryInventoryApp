package com.example.libraryinventoryapp.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.libraryinventoryapp.R
import com.example.libraryinventoryapp.adapters.BookListAdapter
import com.example.libraryinventoryapp.models.Book
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.Normalizer

class BookListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var bookListAdapter: BookListAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var progressBar: ProgressBar
    private lateinit var searchView: SearchView
    private lateinit var filterButton: ImageButton

    private var booksList: MutableList<Book> = mutableListOf()
    private var filteredBooksList: MutableList<Book> = mutableListOf()
    private var selectedCategoriesState: BooleanArray? = null

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
        searchView = view.findViewById(R.id.searchView)
        filterButton = view.findViewById(R.id.filter_button)

        recyclerView.layoutManager = LinearLayoutManager(context)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        fetchBooks()

        filterButton.setOnClickListener { showCategoryFilterDialog() }

        // Configuración del SearchView para filtrar libros
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterBooks(newText ?: "")
                return true
            }
        })
    }

    private fun fetchBooks() {
        progressBar.visibility = View.VISIBLE

        firestore.collection("books").get()
            .addOnSuccessListener { result ->
                booksList.clear()
                for (document in result) {
                    val book = document.toObject(Book::class.java)
                    book.id = document.id

                    // Filtrar libros con quantity > 0
                    if (book.quantity > 0) {
                        booksList.add(book)
                    }
                }

                // Ordena la lista de libros alfabéticamente por el título
                booksList.sortBy { it.title?.lowercase() }

                // Copia la lista original para usar en el filtrado
                filteredBooksList = booksList.toMutableList()

                bookListAdapter = BookListAdapter(filteredBooksList) { book ->
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

    private fun showCategoryFilterDialog() {
        val categories = resources.getStringArray(R.array.book_categories)
        if (selectedCategoriesState == null) {
            selectedCategoriesState = BooleanArray(categories.size) // Inicializa si es la primera vez
        }

        // Mostrar el diálogo
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Filtrar por categorías")
        builder.setMultiChoiceItems(categories, selectedCategoriesState) { _, which, isChecked ->
            selectedCategoriesState!![which] = isChecked
        }

        builder.setPositiveButton("Aplicar") { _, _ ->
            val selected = categories.filterIndexed { index, _ -> selectedCategoriesState!![index] }
            filterBooksByCategories(selected)
        }

        builder.setNegativeButton("Cancelar", null)
        builder.setNeutralButton("Limpiar filtros") { _, _ ->
            selectedCategoriesState = BooleanArray(categories.size) // Reinicia el estado
            filterBooksByCategories(emptyList())
        }

        builder.show()
    }

    private fun filterBooksByCategories(selectedCategories: List<String>) {
        if (selectedCategories.isEmpty()) {
            // Si no hay categorías seleccionadas, mostrar todos los libros
            filteredBooksList.clear()
            filteredBooksList.addAll(booksList)
        } else {
            // Filtrar los libros que contengan al menos una categoría seleccionada
            filteredBooksList.clear()
            filteredBooksList.addAll(
                booksList.filter { book ->
                    book.categories.any { it in selectedCategories }
                }
            )
        }

        // Actualizar el adaptador
        bookListAdapter.notifyDataSetChanged()
    }

    // Filtrar libros según el texto ingresado
    private fun filterBooks(query: String) {
        val normalizedQuery = removeAccents(query.lowercase())

        filteredBooksList.clear()
        filteredBooksList.addAll(
            booksList.filter {
                val normalizedTitle = removeAccents(it.title?.lowercase() ?: "")
                normalizedTitle.contains(normalizedQuery)
            }
        )

        // Notifica al adaptador para que actualice la vista
        bookListAdapter.notifyDataSetChanged()
    }

    private fun assignBook(book: Book) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Debes iniciar sesión para asignar un libro.", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid
        val userEmail = currentUser.email ?: "Correo desconocido"

        // Verificar si el libro ya está asignado al usuario actual
        if (book.assignedTo?.contains(userId) == true) {
            Toast.makeText(context, "Ya tienes este libro asignado.", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userName = document.getString("name") ?: "Usuario desconocido"

                    // Preparar las listas actualizadas
                    val updatedAssignedTo = (book.assignedTo ?: mutableListOf()).toMutableList()
                    val updatedAssignedWithNames = (book.assignedWithNames ?: mutableListOf()).toMutableList()
                    val updatedAssignedToEmails = (book.assignedToEmails ?: mutableListOf()).toMutableList()

                    updatedAssignedTo.add(userId)
                    updatedAssignedWithNames.add(userName)
                    updatedAssignedToEmails.add(userEmail)

                    val newQuantity = book.quantity - 1
                    val updateMap = mapOf(
                        "status" to if (newQuantity == 0) "No disponible" else "Disponible",
                        "quantity" to newQuantity,
                        "assignedTo" to updatedAssignedTo,
                        "assignedWithNames" to updatedAssignedWithNames,
                        "assignedToEmails" to updatedAssignedToEmails
                    )

                    firestore.collection("books").document(book.id).update(updateMap)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Libro asignado exitosamente.", Toast.LENGTH_SHORT).show()
                            fetchBooks() // Actualizar la lista de libros
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(context, "Error al asignar el libro: ${exception.message}", Toast.LENGTH_LONG).show()
                        }
                        .addOnCompleteListener {
                            progressBar.visibility = View.GONE
                        }
                } else {
                    progressBar.visibility = View.GONE
                    Toast.makeText(context, "No se encontró el nombre del usuario.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { exception ->
                progressBar.visibility = View.GONE
                Toast.makeText(context, "Error al obtener los datos del usuario: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    // Método para eliminar tildes y otros signos diacríticos
    private fun removeAccents(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }
}