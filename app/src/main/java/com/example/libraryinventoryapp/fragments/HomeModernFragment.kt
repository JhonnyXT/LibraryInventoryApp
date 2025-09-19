package com.example.libraryinventoryapp.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.libraryinventoryapp.R
import com.example.libraryinventoryapp.UserActivity
import com.example.libraryinventoryapp.adapters.BookHorizontalModernAdapter
import com.example.libraryinventoryapp.models.Book
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.view.MenuItem
import android.widget.PopupMenu
import com.example.libraryinventoryapp.LoginActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 🏠 HomeModernFragment - Pantalla principal moderna
 * 
 * Características principales:
 * - Header con perfil y menú hamburguesa
 * - Buscador en tiempo real
 * - Categorías horizontales con chips
 * - Lista horizontal de libros por categoría
 * - Diseño Material Design 3 premium
 */
class HomeModernFragment : Fragment() {

    companion object {
        private const val TAG = "HomeModernFragment"
        
        // Categorías disponibles en la app
        private val CATEGORIES = listOf(
            "Biblia", "Liderazgo", "Jóvenes", "Mujeres",
            "Profecía bíblica", "Familia", "Matrimonio",
            "Finanzas", "Estudio bíblico", "Evangelismo",
            "Navidad", "Emaus", "Misiones", "Devocionales",
            "Curso vida", "Iglesia", "Vida cristiana",
            "Libros de la Biblia", "Enciclopedia",
            "Religiones", "Inglés", "Infantil"
        )
    }

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    // Drawer Navigation - Temporalmente comentado
    // private lateinit var drawerLayout: DrawerLayout
    // private lateinit var navigationView: NavigationView
    
    // UI Components
    private lateinit var toolbarHome: MaterialToolbar
    
    // Header Actions - Solo Logout y Notificaciones
    private lateinit var btnLogoutHeader: ImageButton
    private lateinit var btnNotificationsHeader: ImageButton
    private lateinit var cardNotificationsBadge: MaterialCardView
    private lateinit var textNotificationsCount: TextView
    
    private lateinit var textGreeting: TextView
    private lateinit var textMainTitle: TextView
    
    // Search
    private lateinit var cardSearchContainer: MaterialCardView
    private lateinit var searchBooks: androidx.appcompat.widget.SearchView
    
    private lateinit var chipGroupCategories: ChipGroup
    private lateinit var chipShowMore: Chip
    private lateinit var textBooksSectionTitle: TextView
    private lateinit var btnSeeAllBooks: MaterialButton
    
    private lateinit var recyclerBooksHorizontal: RecyclerView
    private lateinit var recyclerPopularBooks: RecyclerView
    private lateinit var emptyStateHome: LinearLayout
    private lateinit var loadingOverlay: FrameLayout
    
    // Datos
    private val allBooks = mutableListOf<Book>()
    private val filteredBooks = mutableListOf<Book>()
    private var selectedCategory = "Todas"
    private var currentSearchQuery = ""
    
    // Adapters
    private lateinit var booksAdapter: BookHorizontalModernAdapter
    private lateinit var popularBooksAdapter: BookHorizontalModernAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home_modern, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Log.d(TAG, "🏠 Inicializando HomeModernFragment")
        
        initializeFirebase()
        initializeComponents(view)
        setupUI()
        setupClickListeners()
        loadUserData()
        loadBooks()
    }

    override fun onResume() {
        super.onResume()
        // 🔄 Actualizar contador de notificaciones al regresar a la pantalla
        loadNotificationsCount()
    }

    /**
     * 🔥 Inicializar Firebase
     */
    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
    }

    /**
     * 🏗️ Inicializar todos los componentes UI
     */
    private fun initializeComponents(view: View) {
        // Drawer Navigation - Temporalmente comentado
        // drawerLayout = view.findViewById(R.id.drawer_layout)
        // navigationView = view.findViewById(R.id.nav_view)
        
        // Header components
        toolbarHome = view.findViewById(R.id.toolbar_home)
        
        // Header Actions - Solo Logout y Notificaciones
        btnLogoutHeader = view.findViewById(R.id.btn_logout_header)
        btnNotificationsHeader = view.findViewById(R.id.btn_notifications_header)
        cardNotificationsBadge = view.findViewById(R.id.card_notifications_badge)
        textNotificationsCount = view.findViewById(R.id.text_notifications_count)
        
        // Content components
        textGreeting = view.findViewById(R.id.text_greeting)
        textMainTitle = view.findViewById(R.id.text_main_title)
        
        // Search components
        cardSearchContainer = view.findViewById(R.id.card_search_container)
        searchBooks = view.findViewById(R.id.search_books)
        
        // Categories
        chipGroupCategories = view.findViewById(R.id.chip_group_categories)
        chipShowMore = view.findViewById(R.id.chip_show_more)
        
        // Books section
        textBooksSectionTitle = view.findViewById(R.id.text_books_section_title)
        btnSeeAllBooks = view.findViewById(R.id.btn_see_all_books)
        recyclerBooksHorizontal = view.findViewById(R.id.recycler_books_horizontal)
        recyclerPopularBooks = view.findViewById(R.id.recycler_popular_books)
        
        // States
        emptyStateHome = view.findViewById(R.id.empty_state_home)
        loadingOverlay = view.findViewById(R.id.loading_overlay)
    }

    /**
     * 🎨 Configurar UI y comportamientos
     */
    private fun setupUI() {
        // Setup RecyclerViews
        setupRecyclerViews()
        
        // Setup Search
        setupSearch()
        
        // Cargar contador real de notificaciones del usuario
        loadNotificationsCount()
        
        // Setup Categories
        setupCategories()
        
        // Initial loading state
        showLoadingState()
    }

    /**
     * 📱 Configurar RecyclerViews horizontales
     */
    private fun setupRecyclerViews() {
        // Main books RecyclerView
        booksAdapter = BookHorizontalModernAdapter(filteredBooks) { book ->
            openBookDetail(book)
        }
        recyclerBooksHorizontal.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = booksAdapter
        }
        
        // Popular books RecyclerView
        popularBooksAdapter = BookHorizontalModernAdapter(mutableListOf()) { book ->
            openBookDetail(book)
        }
        recyclerPopularBooks.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = popularBooksAdapter
        }
    }

    /**
     * 🔍 Configurar búsqueda en tiempo real
     */
    /**
     * 🔍 Configurar búsqueda avanzada - Título, Autor y Categorías
     */
    private fun setupSearch() {
        searchBooks.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText?.trim() ?: ""
                Log.d(TAG, "🔍 Búsqueda: '$currentSearchQuery'")
                filterBooks()
                return true
            }
        })

        // Limpiar el texto al inicializar
        searchBooks.setQuery("", false)
    }

    /**
     * 🏷️ Configurar chips de categorías
     */
    private fun setupCategories() {
        // Configurar chip "Todas" que ya existe en el XML
        val todasChip = chipGroupCategories.findViewById<Chip>(R.id.chip_todas_categorias)
        todasChip?.setOnClickListener {
            selectedCategory = "Todas"
            updateSectionTitle()
            filterBooks()
            Log.d(TAG, "🏷️ Todas las categorías seleccionadas")
        }
        
        // 🎯 Marcar "Todas" como seleccionado por defecto
        todasChip?.isChecked = true
        updateSectionTitle()
        Log.d(TAG, "🏷️ Chip 'Todas' marcado por defecto")
        
        // Mostrar primeras 6 categorías
        val visibleCategories = CATEGORIES.take(6)
        val showMoreChip = chipGroupCategories.findViewById<Chip>(R.id.chip_show_more)
        val showMoreIndex = chipGroupCategories.indexOfChild(showMoreChip)
        
        visibleCategories.forEach { category ->
            val chip = layoutInflater.inflate(R.layout.chip_category_item, chipGroupCategories, false) as Chip
            chip.text = category
            chip.isCheckable = true
            chip.setOnClickListener {
                selectedCategory = category
                updateSectionTitle()
                filterBooks()
                Log.d(TAG, "🏷️ Categoría seleccionada: $selectedCategory")
            }
            // Insertar antes del chip "Mostrar más"
            chipGroupCategories.addView(chip, showMoreIndex)
        }
        
        // Botón "Mostrar más"
        chipShowMore.setOnClickListener {
            showAllCategoriesDialog()
        }
        
        // Configurar listener de selección del ChipGroup
        chipGroupCategories.setOnCheckedStateChangeListener { group, checkedIds ->
            // El ChipGroup maneja automáticamente la selección visual
        }
    }


    /**
     * 👆 Configurar click listeners
     */
    private fun setupClickListeners() {
        // 🚪 Botón de logout en header
        btnLogoutHeader.setOnClickListener {
            performLogout()
        }
        
        // 🔔 Botón de notificaciones en header - Abre el fragment de notificaciones
        btnNotificationsHeader.setOnClickListener {
            openNotificationsFragment()
        }
        
        // Ver todos los libros en grid 3x3
        btnSeeAllBooks.setOnClickListener {
            openBooksGridFragment()
        }
        
        // Configurar NavigationView - Temporalmente comentado
        // setupNavigationView()
    }

    /**
     * 🍔 Configurar Navigation Drawer - TEMPORALMENTE COMENTADO
     */
    /*
    private fun setupNavigationView() {
        // Configurar listener de navegación
        navigationView.setNavigationItemSelectedListener { menuItem ->
            handleNavigationItemSelected(menuItem)
        }
        
        // Configurar información del usuario en el header
        updateNavigationHeader()
        
        // Marcar item actual como seleccionado
        navigationView.setCheckedItem(R.id.nav_home)
    }
    
    /**
     * 🎯 Manejar selección de items del drawer
     */
    private fun handleNavigationItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.nav_home -> {
                // Ya estamos en Home
                drawerLayout.closeDrawer(GravityCompat.START)
                return true
            }
            R.id.nav_wishlist -> {
                (activity as? UserActivity)?.switchToTab(1) // Deseados
            }
            R.id.nav_assigned_books -> {
                (activity as? UserActivity)?.switchToTab(2) // Mis Libros
            }
            R.id.nav_notifications -> {
                (activity as? UserActivity)?.switchToTab(3) // Notificaciones
            }
            R.id.nav_logout -> {
                showLogoutDialog()
                return true
            }
        }
        
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
    
    /**
     * 🔄 Actualizar información del header del drawer
     */
    private fun updateNavigationHeader() {
        val headerView = navigationView.getHeaderView(0)
        val navHeaderName = headerView.findViewById<TextView>(R.id.nav_header_name)
        val navHeaderEmail = headerView.findViewById<TextView>(R.id.nav_header_email)
        
        val currentUser = auth.currentUser
        if (currentUser != null) {
            navHeaderEmail.text = currentUser.email
            
            firestore.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    val userName = document.getString("name") ?: "Usuario"
                    navHeaderName.text = userName
                }
                .addOnFailureListener {
                    navHeaderName.text = "Usuario"
                }
        }
    }
    
    /**
     * 🚪 Mostrar diálogo de confirmación de logout
     */
    private fun showLogoutDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    */

    /**
     * 🔔 Abrir fragment de notificaciones
     */
    private fun openNotificationsFragment() {
        val notificationsFragment = NotificationsFragment()
        
        parentFragmentManager.beginTransaction()
            .replace(R.id.admin_fragment_container, notificationsFragment)
            .addToBackStack("notifications")
            .commit()
    }

    /**
     * 🚪 Realizar logout
     */
    private fun performLogout() {
        try {
            // Cerrar sesión en Firebase
            FirebaseAuth.getInstance().signOut()
            
            // Redirigir al login
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al cerrar sesión: ${e.message}", e)
            Toast.makeText(requireContext(), "Error al cerrar sesión", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 📊 Cargar contador real de notificaciones del usuario
     */
    private fun loadNotificationsCount() {
        val currentUserId = auth.currentUser?.uid ?: return
        
        firestore.collection("books")
            .whereArrayContains("assignedTo", currentUserId)
            .get()
            .addOnSuccessListener { documents ->
                var notificationCount = 0
                
                for (document in documents) {
                    val book = document.toObject(Book::class.java)
                    book.id = document.id
                    
                    // 🔍 Buscar asignación del usuario actual
                    val userIndex = book.assignedTo?.indexOf(currentUserId) ?: -1
                    if (userIndex >= 0 && book.loanExpirationDates != null && userIndex < book.loanExpirationDates!!.size) {
                        val expirationDate = book.loanExpirationDates!![userIndex]
                        val daysUntilDue = calculateDaysUntilDue(expirationDate)
                        
                        // 🎯 Contar notificaciones (próximos 5 días o vencidos)
                        if (daysUntilDue <= 5) {
                            notificationCount++
                        }
                    }
                }
                
                // 🔔 Actualizar badge con contador real
                updateNotificationsBadge(notificationCount)
                
                Log.d(TAG, "🔔 Notificaciones pendientes: $notificationCount")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error cargando notificaciones: ${e.message}")
                // En caso de error, ocultar el badge
                updateNotificationsBadge(0)
            }
    }
    
    /**
     * 📅 Calcular días hasta el vencimiento
     */
    private fun calculateDaysUntilDue(expirationDate: com.google.firebase.Timestamp): Int {
        val today = java.util.Calendar.getInstance()
        today.set(java.util.Calendar.HOUR_OF_DAY, 0)
        today.set(java.util.Calendar.MINUTE, 0)
        today.set(java.util.Calendar.SECOND, 0)
        today.set(java.util.Calendar.MILLISECOND, 0)
        
        val expiration = java.util.Calendar.getInstance()
        expiration.time = expirationDate.toDate()
        expiration.set(java.util.Calendar.HOUR_OF_DAY, 0)
        expiration.set(java.util.Calendar.MINUTE, 0)
        expiration.set(java.util.Calendar.SECOND, 0)
        expiration.set(java.util.Calendar.MILLISECOND, 0)
        
        val diffInMillis = expiration.timeInMillis - today.timeInMillis
        return (diffInMillis / (24 * 60 * 60 * 1000)).toInt()
    }

    /**
     * 🔔 Actualizar contador de notificaciones
     */
    private fun updateNotificationsBadge(count: Int) {
        if (count > 0) {
            cardNotificationsBadge.visibility = View.VISIBLE
            textNotificationsCount.text = if (count > 99) "99+" else count.toString()
        } else {
            cardNotificationsBadge.visibility = View.GONE
        }
    }

    // Función showHamburgerMenu removida - ya no se usa el menú hamburguesa

    /**
     * 🚪 Mostrar diálogo de confirmación de logout
     */
    private fun showLogoutDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    /**
     * 👤 Cargar datos del usuario
     */
    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    val userName = document.getString("name") ?: "Usuario"
                    updateGreeting(userName)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Error cargando datos del usuario: ${e.message}")
                    updateGreeting("Usuario")
                }
        }
    }

    /**
     * 📚 Cargar libros desde Firebase
     */
    private fun loadBooks() {
        showLoadingState()
        
        firestore.collection("books")
            .get()
            .addOnSuccessListener { documents ->
                allBooks.clear()
                
                for (document in documents) {
                    val book = document.toObject(Book::class.java)
                    book.id = document.id
                    allBooks.add(book)
                }
                
                Log.d(TAG, "✅ Libros cargados: ${allBooks.size}")
                
                filterBooks()
                loadPopularBooks()
                hideLoadingState()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error cargando libros: ${e.message}")
                showEmptyState()
                hideLoadingState()
            }
    }

    /**
     * 🔍 Filtrar libros por categoría y búsqueda
     */
    private fun filterBooks() {
        lifecycleScope.launch(Dispatchers.Default) {
            val filtered = allBooks.filter { book ->
                // Filtro por categoría
                val categoryMatch = if (selectedCategory == "Todas") {
                    true
                } else {
                    book.categories.contains(selectedCategory)
                }
                
                // Filtro por búsqueda - Título, Autor y Categorías
                val searchMatch = if (currentSearchQuery.isEmpty()) {
                    true
                } else {
                    // Normalizar texto de búsqueda
                    val searchQuery = normalizeText(currentSearchQuery)
                    
                    // Buscar en título
                    val titleMatch = normalizeText(book.title).contains(searchQuery, ignoreCase = true)
                    
                    // Buscar en autor  
                    val authorMatch = normalizeText(book.author).contains(searchQuery, ignoreCase = true)
                    
                    // Buscar en categorías
                    val categoryMatch = book.categories.any { category ->
                        normalizeText(category).contains(searchQuery, ignoreCase = true)
                    }
                    
                    titleMatch || authorMatch || categoryMatch
                }
                
                categoryMatch && searchMatch
            }
            
            withContext(Dispatchers.Main) {
                filteredBooks.clear()
                filteredBooks.addAll(filtered)
                booksAdapter.notifyDataSetChanged()
                
                updateUI()
                
                Log.d(TAG, "🔍 Libros filtrados: ${filteredBooks.size} de ${allBooks.size}")
            }
        }
    }

    /**
     * 🔥 Cargar libros populares (más asignados)
     */
    private fun loadPopularBooks() {
        val popularBooks = allBooks
            .filter { it.assignedTo?.isNotEmpty() == true }
            .sortedByDescending { it.assignedTo?.size ?: 0 }
            .take(10)
        
        popularBooksAdapter.updateBooks(popularBooks)
        
        Log.d(TAG, "🔥 Libros populares: ${popularBooks.size}")
    }

    /**
     * 🎯 Abrir detalle del libro
     */
    private fun openBookDetail(book: Book) {
        Log.d(TAG, "📖 Abriendo detalle: ${book.title}")
        
        val fragment = BookDetailModernFragment.newInstance(book.id)
        parentFragmentManager.beginTransaction()
            .replace(R.id.admin_fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    /**
     * 📚 Abrir catálogo completo de libros 
     */
    private fun openBooksGridFragment() {
        Log.d(TAG, "📚 Abriendo catálogo completo - Categoría: $selectedCategory")
        
        val fragment = BookCatalogFragment.newInstance(selectedCategory)
        parentFragmentManager.beginTransaction()
            .replace(R.id.admin_fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    /**
     * 📱 Actualizar saludo personalizado
     */
    private fun updateGreeting(userName: String) {
        val greeting = getTimeBasedGreeting()
        textGreeting.text = "$greeting, $userName!"
    }

    /**
     * 🌅 Obtener saludo según hora del día
     */
    private fun getTimeBasedGreeting(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 0..11 -> "Buenos días"
            in 12..17 -> "Buenas tardes"
            else -> "Buenas noches"
        }
    }

    /**
     * 🏷️ Actualizar título de sección según categoría
     */
    private fun updateSectionTitle() {
        textBooksSectionTitle.text = if (selectedCategory == "Todas") {
            "Libros"
        } else {
            "Libros de $selectedCategory"
        }
    }

    // Función updateNotificationsBadge movida arriba - esta es duplicada

    // Función showProfileMenu eliminada - ya no se usa el botón de perfil

    /**
     * 🏷️ Mostrar diálogo con todas las categorías
     */
    private fun showAllCategoriesDialog() {
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Seleccionar Categoría")
        
        // Incluir "Todas" + todas las categorías
        val allCategories = listOf("Todas") + CATEGORIES
        val categoriesArray = allCategories.toTypedArray()
        
        builder.setItems(categoriesArray) { _, which ->
            val selectedCat = allCategories[which]
            
            // Actualizar categoría seleccionada
            selectedCategory = selectedCat
            
            // Si es una categoría visible, seleccionarla
            updateSelectedChip(selectedCat)
            
            // Si no está en los chips visibles, asegurar que "Todas" esté seleccionado
            if (selectedCat != "Todas" && !CATEGORIES.take(6).contains(selectedCat)) {
                val todasChip = chipGroupCategories.findViewById<Chip>(R.id.chip_todas_categorias)
                chipGroupCategories.check(todasChip.id)
            }
            
            // Filtrar libros
            updateSectionTitle()
            filterBooks()
            
            Log.d(TAG, "🏷️ Categoría seleccionada: $selectedCat")
        }
        
        builder.setNegativeButton("Cancelar", null)
        builder.create().show()
    }

    /**
     * 🎯 Actualizar chip seleccionado visualmente
     */
    private fun updateSelectedChip(category: String) {
        // Buscar el chip con la categoría seleccionada
        for (i in 0 until chipGroupCategories.childCount) {
            val chip = chipGroupCategories.getChildAt(i) as? Chip
            if (chip != null && chip.text == category && chip.id != R.id.chip_show_more) {
                chipGroupCategories.check(chip.id)
                return
            }
        }
    }

    /**
     * 🚪 Cerrar sesión
     */
    private fun logout() {
        auth.signOut()
        val intent = Intent(requireContext(), com.example.libraryinventoryapp.LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    /**
     * ⏳ Estados de UI
     */
    private fun showLoadingState() {
        loadingOverlay.visibility = View.VISIBLE
        emptyStateHome.visibility = View.GONE
    }

    private fun hideLoadingState() {
        loadingOverlay.visibility = View.GONE
    }

    private fun showEmptyState() {
        emptyStateHome.visibility = View.VISIBLE
    }

    private fun updateUI() {
        if (filteredBooks.isEmpty()) {
            showEmptyState()
        } else {
            emptyStateHome.visibility = View.GONE
        }
    }

    /**
     * 🔤 Normalizar texto para búsqueda (sin acentos)
     */
    private fun normalizeText(text: String): String {
        return java.text.Normalizer.normalize(text.lowercase().trim(), java.text.Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }
}
