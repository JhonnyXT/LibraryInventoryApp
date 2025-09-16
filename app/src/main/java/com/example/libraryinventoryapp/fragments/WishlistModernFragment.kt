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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.libraryinventoryapp.R
import com.example.libraryinventoryapp.UserActivity
import com.example.libraryinventoryapp.adapters.WishlistModernAdapter
import com.example.libraryinventoryapp.models.Book
import com.example.libraryinventoryapp.models.WishlistItem
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * ⭐ WishlistModernFragment - Lista de libros deseados/guardados
 * 
 * Permite a los usuarios ver y gestionar su lista de libros favoritos
 */
class WishlistModernFragment : Fragment() {

    companion object {
        private const val TAG = "WishlistModernFragment"
    }

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var currentUserId: String? = null

    // UI Components
    private lateinit var toolbarWishlist: MaterialToolbar
    private lateinit var textWishlistCount: TextView
    private lateinit var swipeRefreshWishlist: SwipeRefreshLayout
    private lateinit var recyclerWishlist: RecyclerView
    private lateinit var emptyStateWishlist: LinearLayout
    private lateinit var loadingStateWishlist: FrameLayout
    private lateinit var btnExploreCatalog: MaterialButton

    // Data
    private val wishlistItems = mutableListOf<WishlistItem>()
    private val booksMap = mutableMapOf<String, Book>() // Cache de libros

    // Adapter
    private lateinit var wishlistAdapter: WishlistModernAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wishlist_modern, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Log.d(TAG, "⭐ Inicializando WishlistModernFragment")
        
        initializeFirebase()
        initializeComponents(view)
        setupUI()
        setupClickListeners()
        loadWishlist()
    }

    /**
     * 🔥 Inicializar Firebase
     */
    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        currentUserId = auth.currentUser?.uid
    }

    /**
     * 🏗️ Inicializar componentes UI
     */
    private fun initializeComponents(view: View) {
        toolbarWishlist = view.findViewById(R.id.toolbar_wishlist)
        textWishlistCount = view.findViewById(R.id.text_wishlist_count)
        swipeRefreshWishlist = view.findViewById(R.id.swipe_refresh_wishlist)
        recyclerWishlist = view.findViewById(R.id.recycler_wishlist)
        emptyStateWishlist = view.findViewById(R.id.empty_state_wishlist)
        loadingStateWishlist = view.findViewById(R.id.loading_state_wishlist)
        btnExploreCatalog = view.findViewById(R.id.btn_explore_catalog)
    }

    /**
     * 🎨 Configurar UI
     */
    private fun setupUI() {
        // Setup RecyclerView
        wishlistAdapter = WishlistModernAdapter(wishlistItems) { action, wishlistItem ->
            handleWishlistAction(action, wishlistItem)
        }
        recyclerWishlist.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = wishlistAdapter
        }

        // Setup SwipeRefresh
        swipeRefreshWishlist.setOnRefreshListener {
            loadWishlist()
        }

        // Estado inicial
        showLoadingState()
    }

    /**
     * 👆 Configurar click listeners
     */
    private fun setupClickListeners() {
        btnExploreCatalog.setOnClickListener {
            // Navegar al Home
            (activity as? UserActivity)?.switchToTab(0)
        }
    }

    /**
     * ⭐ Cargar lista de deseos
     */
    private fun loadWishlist() {
        if (currentUserId == null) {
            Log.e(TAG, "❌ Usuario no autenticado")
            showEmptyState()
            return
        }

        showLoadingState()

        firestore.collection("wishlist")
            .whereEqualTo("userId", currentUserId)
            .get()
            .addOnSuccessListener { documents ->
                wishlistItems.clear()
                booksMap.clear()

                if (documents.isEmpty) {
                    Log.d(TAG, "📋 Lista de deseos vacía")
                    showEmptyState()
                    return@addOnSuccessListener
                }

                // Cargar wishlist items
                val bookIds = mutableSetOf<String>()
                for (document in documents) {
                    val wishlistItem = document.toObject(WishlistItem::class.java)
                    wishlistItem.id = document.id
                    wishlistItems.add(wishlistItem)
                    bookIds.add(wishlistItem.bookId)
                }

                // Cargar información completa de los libros
                loadBooksDetails(bookIds.toList())
                
                Log.d(TAG, "✅ Wishlist items cargados: ${wishlistItems.size}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error cargando wishlist: ${e.message}")
                showError("Error cargando lista de deseos")
                showEmptyState()
            }
    }

    /**
     * 📚 Cargar detalles de los libros
     */
    private fun loadBooksDetails(bookIds: List<String>) {
        if (bookIds.isEmpty()) {
            updateUI()
            return
        }

        // Dividir en chunks para consultas múltiples (Firestore limit: 10)
        val chunks = bookIds.chunked(10)
        var completedChunks = 0

        for (chunk in chunks) {
            firestore.collection("books")
                .whereIn("id", chunk)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val book = document.toObject(Book::class.java)
                        book.id = document.id
                        booksMap[book.id] = book
                    }

                    completedChunks++
                    if (completedChunks == chunks.size) {
                        // Todas las consultas completadas
                        updateWishlistWithBookDetails()
                        updateUI()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Error cargando detalles de libros: ${e.message}")
                    completedChunks++
                    if (completedChunks == chunks.size) {
                        updateUI()
                    }
                }
        }
    }

    /**
     * 🔄 Actualizar wishlist con detalles de libros
     */
    private fun updateWishlistWithBookDetails() {
        for (wishlistItem in wishlistItems) {
            val book = booksMap[wishlistItem.bookId]
            if (book != null) {
                // Actualizar disponibilidad
                val availableCount = calculateAvailableBooks(book)
                wishlistItem.isAvailable = availableCount > 0
            }
        }

        // Ordenar por fecha añadido (más reciente primero)
        wishlistItems.sortByDescending { it.addedDate }
    }

    /**
     * 🎯 Manejar acciones de wishlist
     */
    private fun handleWishlistAction(action: String, wishlistItem: WishlistItem) {
        when (action) {
            "view_details" -> {
                openBookDetail(wishlistItem.bookId)
            }
            "remove_favorite" -> {
                removeFromWishlist(wishlistItem)
            }
        }
    }

    /**
     * 📖 Abrir detalle del libro
     */
    private fun openBookDetail(bookId: String) {
        Log.d(TAG, "📖 Abriendo detalle: $bookId")
        
        val fragment = BookDetailModernFragment.newInstance(bookId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.admin_fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    /**
     * ❌ Remover de wishlist
     */
    private fun removeFromWishlist(wishlistItem: WishlistItem) {
        firestore.collection("wishlist").document(wishlistItem.id)
            .delete()
            .addOnSuccessListener {
                // Usar el método del adapter para actualización inmediata de UI
                wishlistAdapter.removeItem(wishlistItem)
                updateUI()
                Toast.makeText(requireContext(), "Removido de favoritos", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "✅ Libro removido de wishlist: ${wishlistItem.bookTitle}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error removiendo de wishlist: ${e.message}")
                showError("Error removiendo de favoritos")
            }
    }


    /**
     * 📊 Calcular libros disponibles
     */
    private fun calculateAvailableBooks(book: Book): Int {
        val totalBooks = book.quantity
        val assignedBooks = book.assignedTo?.size ?: 0
        return maxOf(0, totalBooks - assignedBooks)
    }

    /**
     * 🎨 Estados de UI
     */
    private fun showLoadingState() {
        loadingStateWishlist.visibility = View.VISIBLE
        recyclerWishlist.visibility = View.GONE
        emptyStateWishlist.visibility = View.GONE
    }

    private fun showEmptyState() {
        loadingStateWishlist.visibility = View.GONE
        recyclerWishlist.visibility = View.GONE
        emptyStateWishlist.visibility = View.VISIBLE
        textWishlistCount.text = "0 libros guardados"
        swipeRefreshWishlist.isRefreshing = false
    }

    private fun updateUI() {
        loadingStateWishlist.visibility = View.GONE
        swipeRefreshWishlist.isRefreshing = false
        
        if (wishlistItems.isEmpty()) {
            showEmptyState()
        } else {
            recyclerWishlist.visibility = View.VISIBLE
            emptyStateWishlist.visibility = View.GONE
            
            val count = wishlistItems.size
            textWishlistCount.text = if (count == 1) {
                "1 libro guardado"
            } else {
                "$count libros guardados"
            }
            
            wishlistAdapter.notifyDataSetChanged()
            
            Log.d(TAG, "✅ UI actualizada: $count libros en wishlist")
        }
    }

    /**
     * ❌ Mostrar error
     */
    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }
}
