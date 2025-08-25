package com.example.libraryinventoryapp.fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.libraryinventoryapp.R
import com.example.libraryinventoryapp.adapters.BookAdapter
import com.example.libraryinventoryapp.models.Book
import com.example.libraryinventoryapp.models.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ViewBooksFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var booksRecyclerView: RecyclerView
    private lateinit var booksAdapter: BookAdapter
    private lateinit var searchView: SearchView
    private lateinit var filterButton: ImageButton
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar

    private var booksList: MutableList<Book> = mutableListOf()
    private var userNamesList: MutableList<String> = mutableListOf()
    private var userList: MutableList<User> = mutableListOf()
    private var filteredBooksList: MutableList<Book> = mutableListOf()
    private var selectedCategoriesState: BooleanArray? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_view_books, container, false)

        booksRecyclerView = view.findViewById(R.id.recyclerViewBookList)
        searchView = view.findViewById(R.id.searchView)
        filterButton = view.findViewById(R.id.filterButton)
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)
        progressBar = view.findViewById(R.id.progress_bar)

        booksRecyclerView.layoutManager = LinearLayoutManager(context)
        
        // Configurar SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            loadBooks()
        }
        firestore = FirebaseFirestore.getInstance()

        // Inicializar el adaptador vacío con callback para editar
        booksAdapter = BookAdapter(booksList, userNamesList, userList) { book ->
            navigateToEditBook(book)
        }
        booksRecyclerView.adapter = booksAdapter

        filterButton.setOnClickListener { showCategoryFilterDialog() }
        loadUsers()
        loadBooks()

        // Configurar el listener del SearchView
        setupSearchView()

        return view
    }

    private fun loadBooks() {
        progressBar.visibility = View.VISIBLE
        
        firestore.collection("books")
            .get()
            .addOnSuccessListener { result ->
                booksList.clear()
                for (document in result) {
                    val book = document.toObject(Book::class.java)
                    book.id = document.id
                    booksList.add(book)
                }

                // Ordenar la lista de libros alfabéticamente por el título sin acentos
                booksList.sortBy { normalizeText(it.title ?: "") }

                // Actualizar el adaptador
                booksAdapter = BookAdapter(booksList, userNamesList, userList) { book ->
                    navigateToEditBook(book)
                }
                booksRecyclerView.adapter = booksAdapter
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
            }
            .addOnFailureListener { e ->
                // Manejar el error de carga
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
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

        searchView.setOnCloseListener {
            loadBooks()
            false
        }
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

    private fun showCategoryFilterDialog() {
        val filterOptions = arrayOf(
            "Filtrar por Categorías",
            "Filtrar por Usuario Asignado", 
            "Filtrar por Fecha de Asignación",
            "Limpiar todos los filtros"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Opciones de Filtrado")
            .setItems(filterOptions) { _, which ->
                when (which) {
                    0 -> showCategoryMultiSelectDialog()
                    1 -> showUserFilterDialog()
                    2 -> showDateFilterDialog()
                    3 -> clearAllFilters()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showCategoryMultiSelectDialog() {
        val categories = resources.getStringArray(R.array.book_categories)
        if (selectedCategoriesState == null) {
            selectedCategoriesState = BooleanArray(categories.size)
        }

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
        builder.setNeutralButton("Limpiar filtro") { _, _ ->
            selectedCategoriesState = BooleanArray(categories.size)
            filterBooksByCategories(emptyList())
        }
        builder.show()
    }

    private fun showUserFilterDialog() {
        // Consultar todos los usuarios con rol "usuario" desde Firestore
        firestore.collection("users")
            .whereEqualTo("role", "usuario")
            .get()
            .addOnSuccessListener { documents ->
                val userList = mutableListOf<String>()
                for (document in documents) {
                    val userName = document.getString("name")
                    if (!userName.isNullOrEmpty()) {
                        userList.add(userName)
                    }
                }
                
                if (userList.isEmpty()) {
                    Toast.makeText(context, "No hay usuarios registrados", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Ordenar alfabéticamente
                userList.sort()
                
                // Mostrar diálogo con lista de usuarios y búsqueda
                showUserSearchDialog(userList)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error al cargar usuarios: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showUserSearchDialog(userList: List<String>) {
        try {
            // Crear vista personalizada con SearchView
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_user_search, null)
            val searchView = dialogView.findViewById<androidx.appcompat.widget.SearchView>(R.id.search_view_users)
            val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recycler_view_users)
            
            // Setup RecyclerView
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            
            var filteredUsers = userList.toMutableList()
            
            // Crear diálogo primero
            val dialog = AlertDialog.Builder(requireContext())
                .setTitle("Filtrar por Usuario")
                .setView(dialogView)
                .setNegativeButton("Cancelar", null)
                .create()
            
            // Adaptador simple
            val userAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                
                inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
                    private val textView: TextView = itemView.findViewById(android.R.id.text1)
                    
                    fun bind(userName: String) {
                        textView.text = userName
                        itemView.setOnClickListener {
                            filterBooksByUser(userName)
                            dialog.dismiss()
                        }
                    }
                }
                
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(android.R.layout.simple_list_item_1, parent, false)
                    return UserViewHolder(view)
                }
                
                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    (holder as UserViewHolder).bind(filteredUsers[position])
                }
                
                override fun getItemCount(): Int = filteredUsers.size
                
                fun updateUsers(newUsers: List<String>) {
                    filteredUsers.clear()
                    filteredUsers.addAll(newUsers)
                    notifyDataSetChanged()
                }
            }
            
            recyclerView.adapter = userAdapter
            
            // Configurar SearchView
            searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean = false
                
                override fun onQueryTextChange(newText: String?): Boolean {
                    val query = newText?.trim()?.lowercase() ?: ""
                    val filtered = if (query.isEmpty()) {
                        userList
                    } else {
                        userList.filter { user ->
                            removeAccents(user.lowercase()).contains(removeAccents(query))
                        }
                    }
                    userAdapter.updateUsers(filtered)
                    return true
                }
            })
            
            dialog.show()
            
        } catch (e: Exception) {
            // Si hay error con la vista personalizada, mostrar diálogo simple
            val userArray = userList.toTypedArray()
            AlertDialog.Builder(requireContext())
                .setTitle("Filtrar por Usuario")
                .setItems(userArray) { _, which ->
                    val selectedUser = userArray[which]
                    filterBooksByUser(selectedUser)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun showDateFilterDialog() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        // DatePicker para seleccionar "desde" qué fecha filtrar
        DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(year, month, dayOfMonth, 0, 0, 0)
            selectedDate.set(Calendar.MILLISECOND, 0)
            
            // Convertir a Timestamp de Firebase
            val filterTimestamp = Timestamp(selectedDate.time)
            
            // Filtrar libros asignados desde esa fecha
            filterBooksByDateTimestamp(filterTimestamp)
            
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            Toast.makeText(
                context, 
                "Filtrando libros asignados desde: ${dateFormat.format(selectedDate.time)}", 
                Toast.LENGTH_SHORT
            ).show()
            
        }, currentYear, currentMonth, currentDay).show()
    }

    private fun clearAllFilters() {
        selectedCategoriesState = BooleanArray(resources.getStringArray(R.array.book_categories).size)
        filteredBooksList.clear()
        filteredBooksList.addAll(booksList)
        booksAdapter.updateBooks(filteredBooksList)
        Toast.makeText(context, "Filtros limpiados", Toast.LENGTH_SHORT).show()
    }

    private fun filterBooksByCategories(selectedCategories: List<String>) {
        filteredBooksList = if (selectedCategories.isEmpty()) {
            booksList
        } else {
            booksList.filter { book ->
                book.categories.any { it in selectedCategories }
            }
        }.toMutableList()

        booksAdapter.updateBooks(filteredBooksList)
    }

    private fun filterBooksByUser(userName: String) {
        filteredBooksList = booksList.filter { book ->
            book.assignedWithNames?.contains(userName) == true
        }.toMutableList()
        
        booksAdapter.updateBooks(filteredBooksList)
        
        // Mostrar mensaje informativo
        val count = filteredBooksList.size
        val message = if (count == 0) {
            "El usuario '$userName' no tiene libros asignados"
        } else {
            "Mostrando $count libro${if (count == 1) "" else "s"} asignado${if (count == 1) "" else "s"} a '$userName'"
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun filterBooksByDate(days: Int) {
        val cutoffDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -days)
        }.time

        filteredBooksList = booksList.filter { book ->
            book.assignedDates?.any { timestamp ->
                timestamp.toDate().after(cutoffDate)
            } == true
        }.toMutableList()

        booksAdapter.updateBooks(filteredBooksList)
        
        val resultCount = filteredBooksList.size
        Toast.makeText(
            context, 
            "Encontrados $resultCount libros asignados en los últimos $days días", 
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun filterBooksByDateTimestamp(fromDate: Timestamp) {
        filteredBooksList = booksList.filter { book ->
            book.assignedDates?.any { timestamp ->
                // Filtrar libros asignados desde la fecha seleccionada (inclusive)
                timestamp.toDate().time >= fromDate.toDate().time
            } == true
        }.toMutableList()

        booksAdapter.updateBooks(filteredBooksList)
        
        val resultCount = filteredBooksList.size
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        Toast.makeText(
            context, 
            "Encontrados $resultCount libros asignados desde ${dateFormat.format(fromDate.toDate())}", 
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun navigateToEditBook(book: Book) {
        val editBookFragment = EditBookFragment.newInstance(book)
        
        parentFragmentManager.beginTransaction()
            .replace(R.id.admin_fragment_container, editBookFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun normalizeText(text: String): String {
        return Normalizer.normalize(text.lowercase().trim(), Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }
}