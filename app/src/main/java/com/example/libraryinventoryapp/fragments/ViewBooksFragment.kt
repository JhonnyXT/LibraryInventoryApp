package com.example.libraryinventoryapp.fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.Intent
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.example.libraryinventoryapp.utils.NotificationHelper
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.libraryinventoryapp.LoginActivity
import com.example.libraryinventoryapp.R
import com.example.libraryinventoryapp.adapters.BookAdapter
import com.example.libraryinventoryapp.models.Book
import com.example.libraryinventoryapp.models.User
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.example.libraryinventoryapp.utils.AuthManager
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ViewBooksFragment : Fragment() {

    // 🔥 Componentes UI principales
    private lateinit var firestore: FirebaseFirestore
    private lateinit var booksRecyclerView: RecyclerView
    private lateinit var booksAdapter: BookAdapter
    private lateinit var searchView: SearchView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    
    // 🎯 Estados elegantes
    private lateinit var loadingState: LinearLayout
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var emptyStateTitle: TextView
    private lateinit var emptyStateText: TextView
    
    // 📊 Contadores y información
    private lateinit var textBooksCount: TextView
    private lateinit var textFilterCount: TextView
    
    // 🏷️ Filtros con chips
    private lateinit var chipAllBooks: Chip
    private lateinit var chipCategories: Chip
    private lateinit var chipUsers: Chip
    private lateinit var chipDates: Chip
    private lateinit var chipAvailable: Chip
    private lateinit var chipAssigned: Chip
    
    // 🎯 FAB para agregar libro
    private lateinit var fabAddBook: FloatingActionButton
    
    // 🚪 Botón de logout
    private lateinit var btnLogoutHeader: ImageButton

    // 📚 Datos y estado
    private var booksList: MutableList<Book> = mutableListOf()
    private var userNamesList: MutableList<String> = mutableListOf()
    private var userList: MutableList<User> = mutableListOf()
    private var filteredBooksList: MutableList<Book> = mutableListOf()
    private var selectedCategoriesState: BooleanArray? = null
    private var currentFilterType: FilterType = FilterType.ALL
    
    // 🎯 Enum para tipos de filtro
    private enum class FilterType {
        ALL, CATEGORIES, USERS, DATES, AVAILABLE, ASSIGNED
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_view_books, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔥 Inicializar componentes básicos
        initializeViews(view)
        
        // 🎯 Configurar funcionalidades
        setupRecyclerView()
        setupSwipeRefresh()
        setupSearchView()
        setupFilterChips()
        setupFAB()
        setupLogoutButton()
        
        // 📚 Cargar datos
        firestore = FirebaseFirestore.getInstance()
        loadUsers()
        loadBooks()
        
        Log.d("ViewBooksFragment", "🎨 Vista inicializada correctamente")
    }

    /**
     * 🔧 Inicializar todas las vistas UI
     */
    private fun initializeViews(view: View) {
        // Componentes principales
        booksRecyclerView = view.findViewById(R.id.recyclerViewBookList)
        searchView = view.findViewById(R.id.searchView)
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)
        
        // Estados elegantes
        loadingState = view.findViewById(R.id.loading_state)
        emptyStateLayout = view.findViewById(R.id.empty_state_layout)
        emptyStateTitle = view.findViewById(R.id.empty_state_title)
        emptyStateText = view.findViewById(R.id.empty_state_text)
        
        // Contadores
        textBooksCount = view.findViewById(R.id.text_books_count)
        textFilterCount = view.findViewById(R.id.text_filter_count)
        
        // Chips de filtros
        chipAllBooks = view.findViewById(R.id.chip_all_books)
        chipCategories = view.findViewById(R.id.chip_categories)
        chipUsers = view.findViewById(R.id.chip_users)
        chipDates = view.findViewById(R.id.chip_dates)
        chipAvailable = view.findViewById(R.id.chip_available)
        chipAssigned = view.findViewById(R.id.chip_assigned)
        
        // 🎯 FAB para agregar libro
        fabAddBook = view.findViewById(R.id.fab_add_book)
        
        // 🚪 Botón de logout
        btnLogoutHeader = view.findViewById(R.id.btn_logout_header)
    }

    /**
     * 📋 Configurar RecyclerView
     */
    private fun setupRecyclerView() {
        booksRecyclerView.layoutManager = LinearLayoutManager(context)
        
        // Inicializar el adaptador con callback para ir al detalle
        booksAdapter = BookAdapter(
            books = booksList, 
            userNames = userNamesList, 
            userList = userList, 
            onBookClick = { book ->
                navigateToBookDetail(book)
            }
        )
        booksRecyclerView.adapter = booksAdapter
    }

    /**
     * 🔄 Configurar SwipeRefresh
     */
    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
        loadBooks()
        }
    }

    /**
     * 🏷️ Configurar chips de filtros
     */
    private fun setupFilterChips() {
        // Configurar listeners para cada chip
        chipAllBooks.setOnClickListener { applyFilter(FilterType.ALL) }
        chipCategories.setOnClickListener { applyFilter(FilterType.CATEGORIES) }
        chipUsers.setOnClickListener { applyFilter(FilterType.USERS) }
        chipDates.setOnClickListener { applyFilter(FilterType.DATES) }
        chipAvailable.setOnClickListener { applyFilter(FilterType.AVAILABLE) }
        chipAssigned.setOnClickListener { applyFilter(FilterType.ASSIGNED) }
        
        // 🎯 Establecer "Todos" como filtro por defecto
        chipAllBooks.isChecked = true
        currentFilterType = FilterType.ALL
        updateFilterUI(FilterType.ALL, booksList.size)
        
        Log.d("ViewBooksFragment", "🏷️ Filtro 'Todos' establecido por defecto")
    }

    /**
     * 🎯 Aplicar filtro seleccionado - Compatible con singleSelection
     */
    private fun applyFilter(filterType: FilterType) {
        currentFilterType = filterType
        
        // Con singleSelection="true", solo necesitamos marcar el chip correspondiente
        when (filterType) {
            FilterType.ALL -> {
                chipAllBooks.isChecked = true
                showAllBooks()
            }
            FilterType.CATEGORIES -> {
                chipCategories.isChecked = true
                showCategoryMultiSelectDialog()
            }
            FilterType.USERS -> {
                chipUsers.isChecked = true
                showUserFilterDialog()
            }
            FilterType.DATES -> {
                chipDates.isChecked = true
                showDateFilterDialog()
            }
            FilterType.AVAILABLE -> {
                chipAvailable.isChecked = true
                filterByAvailability(true)
            }
            FilterType.ASSIGNED -> {
                chipAssigned.isChecked = true
                filterByAvailability(false)
            }
        }
    }

    /**
     * 📊 Mostrar todos los libros
     */
    private fun showAllBooks() {
        filteredBooksList.clear()
        filteredBooksList.addAll(booksList)
        booksAdapter.updateBooks(filteredBooksList)
        updateFilterUI(FilterType.ALL, filteredBooksList.size)
        updateEmptyState()
    }

    /**
     * ✅ Filtrar por disponibilidad
     */
    private fun filterByAvailability(showAvailable: Boolean) {
        filteredBooksList = if (showAvailable) {
            booksList.filter { book ->
                book.status == "Disponible" && (book.assignedTo?.size ?: 0) < (book.quantity ?: 0)
            }.toMutableList()
        } else {
            booksList.filter { book ->
                !book.assignedTo.isNullOrEmpty()
            }.toMutableList()
        }
        
        booksAdapter.updateBooks(filteredBooksList)
        val filterType = if (showAvailable) FilterType.AVAILABLE else FilterType.ASSIGNED
        updateFilterUI(filterType, filteredBooksList.size)
        updateEmptyState()
    }

    /**
     * 🎨 Actualizar UI de filtros
     */
    private fun updateFilterUI(filterType: FilterType, count: Int) {
        val filterName = when (filterType) {
            FilterType.ALL -> "Todos los libros"
            FilterType.CATEGORIES -> "Por categorías"
            FilterType.USERS -> "Por usuario"
            FilterType.DATES -> "Por fecha"
            FilterType.AVAILABLE -> "Disponibles"
            FilterType.ASSIGNED -> "Asignados"
        }
        
        textFilterCount.text = "$filterName ($count)"
        textBooksCount.text = "Mostrando $count de ${booksList.size} libros"
    }

    private fun loadBooks() {
        Log.i("ViewBooksFragment", "🔄 INICIANDO CARGA DE INVENTARIO")
        
        // 🔄 Mostrar estado de carga elegante
        loadingState.visibility = View.VISIBLE
        emptyStateLayout.visibility = View.GONE
        swipeRefreshLayout.visibility = View.GONE
        
        firestore.collection("books")
            .get()
            .addOnSuccessListener { result ->
                Log.d("ViewBooksFragment", "📚 Obtenidos ${result.size()} libros de Firestore")
                
                booksList.clear()
                for (document in result) {
                    val book = document.toObject(Book::class.java)
                    book.id = document.id
                    booksList.add(book)
                }

                // Ordenar la lista de libros alfabéticamente por el título sin acentos
                booksList.sortBy { normalizeText(it.title ?: "") }

                // Actualizar el adaptador
                booksAdapter = BookAdapter(
                    books = booksList, 
                    userNames = userNamesList, 
                    userList = userList, 
                    onBookClick = { book ->
                        navigateToBookDetail(book)
                    }
                )
                booksRecyclerView.adapter = booksAdapter
                
                // 🎯 Aplicar filtro por defecto "Todos" después de cargar los datos
                applyFilter(FilterType.ALL)
                
                // 🎯 Ocultar estado de carga
                loadingState.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
                swipeRefreshLayout.visibility = View.VISIBLE
            }
            .addOnFailureListener { exception ->
                // 🚨 Ocultar loading en caso de error
                loadingState.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
                NotificationHelper.showError(
                    context = requireContext(),
                    title = "Error de Carga",
                    message = "No se pudo cargar el inventario de libros: ${exception.message}",
                    view = view
                )
            }
    }

    /**
     * 📄 Actualizar estado vacío según filtros
     */
    private fun updateEmptyState() {
        if (filteredBooksList.isEmpty()) {
            // 🎯 Mostrar estado vacío centrado
            emptyStateLayout.visibility = View.VISIBLE
            swipeRefreshLayout.visibility = View.GONE
            
            val (title, message) = when (currentFilterType) {
                FilterType.ALL -> Pair("No hay libros", "No se han registrado libros en el inventario")
                FilterType.CATEGORIES -> Pair("Sin resultados", "No hay libros en las categorías seleccionadas")
                FilterType.USERS -> Pair("Sin asignaciones", "No hay libros asignados al usuario seleccionado")
                FilterType.DATES -> Pair("Sin coincidencias", "No hay libros asignados en el período seleccionado")
                FilterType.AVAILABLE -> Pair("Sin disponibles", "Todos los libros están asignados")
                FilterType.ASSIGNED -> Pair("Sin asignados", "No hay libros asignados actualmente")
            }
            
            emptyStateTitle.text = title
            emptyStateText.text = message
            
            Log.d("ViewBooksFragment", "📭 Estado vacío: $title - $message")
        } else {
            // 🎯 Mostrar lista con datos
            emptyStateLayout.visibility = View.GONE
            swipeRefreshLayout.visibility = View.VISIBLE
            
            Log.d("ViewBooksFragment", "📋 Lista mostrada con ${filteredBooksList.size} libros")
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
                NotificationHelper.showError(
                    context = requireContext(),
                    title = "Error de Usuarios",
                    message = "No se pudieron cargar los usuarios: $e",
                    view = view
                )
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
                    NotificationHelper.showInfo(
                        context = requireContext(),
                        title = "Sin Usuarios",
                        message = "No hay usuarios registrados en el sistema.",
                        view = view
                    )
                    return@addOnSuccessListener
                }

                // Ordenar alfabéticamente
                userList.sort()
                
                // Mostrar diálogo con lista de usuarios y búsqueda
                showUserSearchDialog(userList)
            }
            .addOnFailureListener { e ->
                NotificationHelper.showError(
                    context = requireContext(),
                    title = "Error de Carga",
                    message = "No se pudieron cargar los usuarios: ${e.message}",
                    view = view
                )
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
            NotificationHelper.showInfo(
                context = requireContext(),
                title = "Filtro Aplicado",
                message = "Filtrando libros asignados desde: ${dateFormat.format(selectedDate.time)}",
                view = view
            )
            
        }, currentYear, currentMonth, currentDay).show()
    }

    private fun clearAllFilters() {
        selectedCategoriesState = BooleanArray(resources.getStringArray(R.array.book_categories).size)
        currentFilterType = FilterType.ALL
        
        // Con singleSelection, solo marcamos "Todos"
        chipAllBooks.isChecked = true
        
        // Mostrar todos los libros
        filteredBooksList.clear()
        filteredBooksList.addAll(booksList)
        booksAdapter.updateBooks(filteredBooksList)
        
        updateFilterUI(FilterType.ALL, filteredBooksList.size)
        updateEmptyState()
        
        NotificationHelper.showSuccess(
            context = requireContext(),
            title = "Filtros Limpiados",
            message = "✅ Todos los filtros han sido eliminados.",
            view = view
        )
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
        updateFilterUI(FilterType.CATEGORIES, filteredBooksList.size)
        updateEmptyState()
    }

    private fun filterBooksByUser(userName: String) {
        filteredBooksList = booksList.filter { book ->
            book.assignedWithNames?.contains(userName) == true
        }.toMutableList()
        
        booksAdapter.updateBooks(filteredBooksList)
        updateFilterUI(FilterType.USERS, filteredBooksList.size)
        updateEmptyState()
        
        // Mostrar mensaje informativo
        val count = filteredBooksList.size
        val message = if (count == 0) {
            "El usuario '$userName' no tiene libros asignados"
        } else {
            "Mostrando $count libro${if (count == 1) "" else "s"} asignado${if (count == 1) "" else "s"} a '$userName'"
        }
        NotificationHelper.showInfo(
            context = requireContext(),
            title = "Filtro por Usuario",
            message = message,
            view = view
        )
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
        updateFilterUI(FilterType.DATES, filteredBooksList.size)
        updateEmptyState()
        
        val resultCount = filteredBooksList.size
        NotificationHelper.showInfo(
            context = requireContext(),
            title = "Resultados del Filtro",
            message = "Encontrados $resultCount libros asignados en los últimos $days días",
            view = view
        )
    }

    private fun filterBooksByDateTimestamp(fromDate: Timestamp) {
        filteredBooksList = booksList.filter { book ->
            book.assignedDates?.any { timestamp ->
                // Filtrar libros asignados desde la fecha seleccionada (inclusive)
                timestamp.toDate().time >= fromDate.toDate().time
            } == true
        }.toMutableList()

        booksAdapter.updateBooks(filteredBooksList)
        updateFilterUI(FilterType.DATES, filteredBooksList.size)
        updateEmptyState()
        
        val resultCount = filteredBooksList.size
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        NotificationHelper.showInfo(
            context = requireContext(),
            title = "Resultados del Filtro",
            message = "Encontrados $resultCount libros asignados desde ${dateFormat.format(fromDate.toDate())}",
            view = view
        )
    }

    /**
     * 🎯 Configurar FAB para agregar libro
     */
    private fun setupFAB() {
        fabAddBook.setOnClickListener {
            Log.d("ViewBooksFragment", "🎯 FAB clicked - Navegando a RegisterBookFragment")
            navigateToRegisterBook()
        }
    }
    
    /**
     * 📝 Navegar a la pantalla de registrar libro
     */
    private fun navigateToRegisterBook() {
        Log.d("ViewBooksFragment", "📝 Navegando a RegisterBookFragment")
        
        val registerFragment = RegisterBookFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.admin_fragment_container, registerFragment)
            .addToBackStack(null)
            .commit()
    }

    /**
     * 📖 Navegar al detalle del libro (Admin)
     */
    private fun navigateToBookDetail(book: Book) {
        Log.d("ViewBooksFragment", "🔍 Navegando al detalle del libro: ${book.title}")
        
        val detailFragment = BookDetailAdminFragment.newInstance(book.id)
        parentFragmentManager.beginTransaction()
            .replace(R.id.admin_fragment_container, detailFragment)
            .addToBackStack(null)
            .commit()
    }

    /**
     * 🚪 Configurar botón de logout
     */
    private fun setupLogoutButton() {
        btnLogoutHeader.setOnClickListener {
            performLogout()
        }
    }

    /**
     * 🚪 Realizar logout
     */
    private fun performLogout() {
        // 🔐 Usar AuthManager para logout completo (Firebase + Google Sign-In)
        val authManager = AuthManager.getInstance()
        authManager.performCompleteLogout(this, showSuccessMessage = true)
    }

    private fun normalizeText(text: String): String {
        return Normalizer.normalize(text.lowercase().trim(), Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }
}