package com.example.libraryinventoryapp.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.libraryinventoryapp.R
import com.example.libraryinventoryapp.adapters.BooksGridAdapter
import com.example.libraryinventoryapp.models.Book
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 📚 BooksGridFragment - Grid completo 3x3 como en la imagen
 * 
 * Muestra todos los libros en formato grid de 3 columnas
 */
class BooksGridFragment : Fragment() {

    companion object {
        private const val TAG = "BooksGridFragment"
        
        fun newInstance(category: String? = null): BooksGridFragment {
            val fragment = BooksGridFragment()
            val args = Bundle()
            args.putString("category", category)
            fragment.arguments = args
            return fragment
        }
    }

    // Firebase
    private lateinit var firestore: FirebaseFirestore
    
    // UI Components
    private lateinit var toolbarBooksGrid: MaterialToolbar
    private lateinit var textGridTitle: TextView
    private lateinit var textGridSubtitle: TextView
    private lateinit var searchBooksGrid: SearchView
    private lateinit var swipeRefreshGrid: SwipeRefreshLayout
    private lateinit var recyclerBooksGrid: RecyclerView
    private lateinit var emptyStateGrid: LinearLayout
    private lateinit var loadingStateGrid: FrameLayout

    // Data
    private val allBooks = mutableListOf<Book>()
    private val filteredBooks = mutableListOf<Book>()
    private var selectedCategory: String? = null
    private var currentSearchQuery = ""

    // Adapter
    private lateinit var booksGridAdapter: BooksGridAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_books_grid, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        selectedCategory = arguments?.getString("category")
        
        Log.d(TAG, "📚 Inicializando BooksGridFragment - Categoría: $selectedCategory")
        
        initializeFirebase()
        initializeComponents(view)
        setupUI()
        setupClickListeners()
        loadBooks()
    }

    /**
     * 🔥 Inicializar Firebase
     */
    private fun initializeFirebase() {
        firestore = FirebaseFirestore.getInstance()
    }

    /**
     * 🏗️ Inicializar componentes UI
     */
    private fun initializeComponents(view: View) {
        toolbarBooksGrid = view.findViewById(R.id.toolbar_books_grid)
        textGridTitle = view.findViewById(R.id.text_grid_title)
        textGridSubtitle = view.findViewById(R.id.text_grid_subtitle)
        searchBooksGrid = view.findViewById(R.id.search_books_grid)
        swipeRefreshGrid = view.findViewById(R.id.swipe_refresh_grid)
        recyclerBooksGrid = view.findViewById(R.id.recycler_books_grid)
        emptyStateGrid = view.findViewById(R.id.empty_state_grid)
        loadingStateGrid = view.findViewById(R.id.loading_state_grid)
    }

    /**
     * 🎨 Configurar UI
     */
    private fun setupUI() {
        // Setup título según categoría
        updateTitle()
        
        // Setup RecyclerView con GridLayoutManager (3 columnas)
        booksGridAdapter = BooksGridAdapter(filteredBooks) { book ->
            openBookDetail(book)
        }
        
        recyclerBooksGrid.apply {
            layoutManager = GridLayoutManager(requireContext(), 3) // 3 columnas
            adapter = booksGridAdapter
        }

        // Setup Search
        searchBooksGrid.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            
            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText?.trim() ?: ""
                filterBooks()
                return true
            }
        })

        // Setup SwipeRefresh
        swipeRefreshGrid.setOnRefreshListener {
            loadBooks()
        }

        // Estado inicial
        showLoadingState()
    }

    /**
     * 👆 Configurar click listeners
     */
    private fun setupClickListeners() {
        // Botón de regreso
        toolbarBooksGrid.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    /**
     * 🏷️ Actualizar título según categoría
     */
    private fun updateTitle() {
        if (selectedCategory.isNullOrEmpty() || selectedCategory == "Todas") {
            textGridTitle.text = "📚 Todos los Libros"
        } else {
            textGridTitle.text = "📚 Libros de $selectedCategory"
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
                hideLoadingState()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error cargando libros: ${e.message}")
                showError("Error cargando libros")
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
                val categoryMatch = if (selectedCategory.isNullOrEmpty() || selectedCategory == "Todas") {
                    true
                } else {
                    book.categories.contains(selectedCategory)
                }
                
                // Filtro por búsqueda
                val searchMatch = if (currentSearchQuery.isEmpty()) {
                    true
                } else {
                    book.title.contains(currentSearchQuery, ignoreCase = true) ||
                    book.author.contains(currentSearchQuery, ignoreCase = true)
                }
                
                categoryMatch && searchMatch
            }
            
            withContext(Dispatchers.Main) {
                filteredBooks.clear()
                filteredBooks.addAll(filtered)
                booksGridAdapter.notifyDataSetChanged()
                
                updateUI()
                updateSubtitle()
                
                Log.d(TAG, "🔍 Libros filtrados: ${filteredBooks.size} de ${allBooks.size}")
            }
        }
    }

    /**
     * 📊 Actualizar subtítulo con conteo
     */
    private fun updateSubtitle() {
        val count = filteredBooks.size
        textGridSubtitle.text = if (count == 0) {
            "No se encontraron libros"
        } else if (count == 1) {
            "1 libro disponible"
        } else {
            "$count libros disponibles"
        }
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
     * 🎨 Estados de UI
     */
    private fun showLoadingState() {
        loadingStateGrid.visibility = View.VISIBLE
        recyclerBooksGrid.visibility = View.GONE
        emptyStateGrid.visibility = View.GONE
    }

    private fun hideLoadingState() {
        loadingStateGrid.visibility = View.GONE
        swipeRefreshGrid.isRefreshing = false
    }

    private fun showEmptyState() {
        recyclerBooksGrid.visibility = View.GONE
        emptyStateGrid.visibility = View.VISIBLE
    }

    private fun updateUI() {
        if (filteredBooks.isEmpty()) {
            showEmptyState()
        } else {
            recyclerBooksGrid.visibility = View.VISIBLE
            emptyStateGrid.visibility = View.GONE
        }
    }

    /**
     * ❌ Mostrar error
     */
    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }
}
