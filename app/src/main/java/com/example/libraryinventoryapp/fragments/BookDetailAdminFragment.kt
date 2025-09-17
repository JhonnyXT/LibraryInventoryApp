package com.example.libraryinventoryapp.fragments

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.libraryinventoryapp.R
import com.example.libraryinventoryapp.models.Book
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore

/**
 * 📖 BookDetailAdminFragment - Detalle de libro para administradores
 * 
 * Características:
 * - Vista detallada del libro con imagen grande
 * - FAB con menú de opciones (editar/eliminar)
 * - Material Design 3 completo
 * - Navegación a edición y confirmación de eliminación
 */
class BookDetailAdminFragment : Fragment() {

    companion object {
        private const val TAG = "BookDetailAdminFragment"
        private const val ARG_BOOK_ID = "book_id"
        
        fun newInstance(bookId: String): BookDetailAdminFragment {
            val fragment = BookDetailAdminFragment()
            val args = Bundle()
            args.putString(ARG_BOOK_ID, bookId)
            fragment.arguments = args
            return fragment
        }
    }

    // 🔥 Componentes UI
    private lateinit var toolbar: MaterialToolbar
    private lateinit var imgBookCover: ImageView
    private lateinit var textBookTitle: TextView
    private lateinit var textBookAuthor: TextView
    private lateinit var textBookStatus: TextView
    private lateinit var textBookQuantity: TextView
    private lateinit var textBookDescription: TextView
    private lateinit var textBookIsbn: TextView
    private lateinit var chipGroupCategories: ChipGroup
    private lateinit var cardAssignmentInfo: MaterialCardView
    private lateinit var textAssignedUsers: TextView
    
    // 🎯 Menú flotante moderno
    private var fabMain: FloatingActionButton? = null
    private var menuEditBook: View? = null
    private var menuDeleteBook: View? = null
    private var overlayBackground: View? = null
    private var isMenuOpen = false
    
    // 📚 Datos
    private lateinit var firestore: FirebaseFirestore
    private var currentBook: Book? = null
    private var bookId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_book_detail_admin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔧 Inicializar componentes
        initializeViews(view)
        
        // 🔥 Configurar funcionalidades
        setupToolbar()
        setupFABMenu()
        
        // 📚 Cargar datos
        firestore = FirebaseFirestore.getInstance()
        bookId = arguments?.getString(ARG_BOOK_ID)
        
        if (bookId != null) {
            loadBookDetails(bookId!!)
        } else {
            Log.e(TAG, "❌ No se proporcionó ID de libro")
            Toast.makeText(context, "Error: Libro no encontrado", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
        
        Log.d(TAG, "🎨 Vista inicializada correctamente")
    }

    /**
     * 🔧 Inicializar todas las vistas
     */
    private fun initializeViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar_detail)
        imgBookCover = view.findViewById(R.id.img_book_cover_large)
        textBookTitle = view.findViewById(R.id.text_book_title)
        textBookAuthor = view.findViewById(R.id.text_book_author)
        textBookStatus = view.findViewById(R.id.text_book_status)
        textBookQuantity = view.findViewById(R.id.text_book_quantity)
        textBookDescription = view.findViewById(R.id.text_book_description)
        textBookIsbn = view.findViewById(R.id.text_book_isbn)
        chipGroupCategories = view.findViewById(R.id.chip_group_categories)
        cardAssignmentInfo = view.findViewById(R.id.card_assignment_info)
        textAssignedUsers = view.findViewById(R.id.text_assigned_users)
        
        // 🎯 Menú flotante moderno
        Log.d(TAG, "🔍 Buscando componentes del menú flotante...")
        
        try {
            val floatingMenu = view.findViewById<View>(R.id.floating_action_menu)
            
            if (floatingMenu != null) {
                Log.d(TAG, "✅ Floating menu container encontrado")
                
                fabMain = floatingMenu.findViewById(R.id.fab_main)
                menuEditBook = floatingMenu.findViewById(R.id.menu_edit_book)
                menuDeleteBook = floatingMenu.findViewById(R.id.menu_delete_book)
                overlayBackground = floatingMenu.findViewById(R.id.overlay_background)
                
                Log.d(TAG, "FAB: ${if (fabMain != null) "✅ OK" else "❌ NULL"}")
                Log.d(TAG, "Edit: ${if (menuEditBook != null) "✅ OK" else "❌ NULL"}")
                Log.d(TAG, "Delete: ${if (menuDeleteBook != null) "✅ OK" else "❌ NULL"}")
                Log.d(TAG, "Overlay: ${if (overlayBackground != null) "✅ OK" else "❌ NULL"}")
                
                if (fabMain != null && menuEditBook != null && menuDeleteBook != null && overlayBackground != null) {
                    Log.d(TAG, "✅ Todos los componentes del menú inicializados correctamente")
                    
                    // 🎯 Configurar estado inicial de los elementos del menú
                    initializeMenuItemsState()
                } else {
                    Log.w(TAG, "⚠️ Algunos componentes del menú no se pudieron inicializar")
                }
            } else {
                Log.e(TAG, "❌ ERROR: Floating menu container NO encontrado")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ ERROR inicializando menú flotante: ${e.message}")
        }
    }

    /**
     * 🎯 Configurar estado inicial de elementos del menú
     */
    private fun initializeMenuItemsState() {
        Log.d(TAG, "🔧 Configurando estado inicial del menú...")
        
        // Configurar estado inicial: invisible pero listo para animar
        listOfNotNull(menuEditBook, menuDeleteBook).forEach { item ->
            item.alpha = 0f
            item.scaleX = 0.7f
            item.scaleY = 0.7f
            item.translationY = 30f
        }
        
        Log.d(TAG, "✅ Estado inicial del menú configurado")
    }

    /**
     * 🔙 Configurar toolbar
     */
    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    /**
     * 🎯 Configurar menú flotante moderno
     */
    private fun setupFABMenu() {
        Log.d(TAG, "🔧 Configurando FAB menu...")
        
        // Verificar que todos los componentes estén inicializados
        if (fabMain == null || menuEditBook == null || menuDeleteBook == null || overlayBackground == null) {
            Log.w(TAG, "⚠️ No se puede configurar FAB menu - componentes no inicializados")
            return
        }
        
        // Click en FAB principal para mostrar/ocultar menú
        fabMain?.setOnClickListener {
            Log.d(TAG, "🔥 FAB principal clickeado")
            toggleFloatingMenu()
        }
        
        // Click en overlay para cerrar menú
        overlayBackground?.setOnClickListener {
            Log.d(TAG, "🎯 Overlay clickeado - cerrando menú")
            closeFloatingMenu()
        }
        
        // Click en opción "Editar libro"
        menuEditBook?.setOnClickListener {
            Log.d(TAG, "✏️ Opción editar clickeada")
            closeFloatingMenu()
            editBook()
        }
        
        // Click en opción "Eliminar libro"
        menuDeleteBook?.setOnClickListener {
            Log.d(TAG, "🗑️ Opción eliminar clickeada")
            closeFloatingMenu()
            confirmDeleteBook()
        }
        
        Log.d(TAG, "✅ FAB menu configurado correctamente")
    }

    /**
     * 📚 Cargar detalles del libro
     */
    private fun loadBookDetails(bookId: String) {
        Log.i(TAG, "📚 Cargando detalles del libro: $bookId")
        
        firestore.collection("books")
            .document(bookId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val book = document.toObject(Book::class.java)
                    if (book != null) {
                        book.id = document.id
                        currentBook = book
                        displayBookDetails(book)
                        Log.d(TAG, "✅ Libro cargado: ${book.title}")
                    } else {
                        showError("Error al procesar datos del libro")
                    }
                } else {
                    showError("Libro no encontrado")
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "❌ Error al cargar libro", exception)
                showError("Error al cargar libro: ${exception.message}")
            }
    }

    /**
     * 🎨 Mostrar detalles del libro en la UI
     */
    private fun displayBookDetails(book: Book) {
        // 📖 Información básica
        textBookTitle.text = book.title
        textBookAuthor.text = "por ${book.author}"
        textBookDescription.text = book.description.ifEmpty { "Sin descripción disponible" }
        textBookIsbn.text = book.isbn.ifEmpty { "No especificado" }
        
        // 📊 Estado y cantidad
        textBookStatus.text = book.status
        val availableCount = book.quantity - (book.assignedTo?.size ?: 0)
        textBookQuantity.text = "$availableCount de ${book.quantity} disponibles"
        
        // 🎨 Imagen de portada
        if (!book.imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(book.imageUrl)
                .placeholder(R.drawable.ic_book_library)
                .error(R.drawable.ic_book_library)
                .into(imgBookCover)
        }
        
        // 🏷️ Categorías
        displayCategories(book.categories)
        
        // 👥 Información de asignación
        displayAssignmentInfo(book)
    }

    /**
     * 🏷️ Mostrar categorías como chips
     */
    private fun displayCategories(categories: List<String>) {
        chipGroupCategories.removeAllViews()
        
        if (categories.isNotEmpty()) {
            categories.forEach { category ->
                val chip = Chip(requireContext())
                chip.text = category
                chip.isEnabled = false
                chipGroupCategories.addView(chip)
            }
        } else {
            val chip = Chip(requireContext())
            chip.text = "Sin categoría"
            chip.isEnabled = false
            chipGroupCategories.addView(chip)
        }
    }

    /**
     * 👥 Mostrar información de asignación
     */
    private fun displayAssignmentInfo(book: Book) {
        if (!book.assignedWithNames.isNullOrEmpty()) {
            cardAssignmentInfo.visibility = View.VISIBLE
            textAssignedUsers.text = book.assignedWithNames!!.joinToString(", ")
        } else {
            cardAssignmentInfo.visibility = View.GONE
        }
    }

    /**
     * 🎯 Alternar visibilidad del menú flotante
     */
    private fun toggleFloatingMenu() {
        if (isMenuOpen) {
            closeFloatingMenu()
        } else {
            openFloatingMenu()
        }
    }
    
    /**
     * 📤 Abrir menú flotante con animación
     */
    private fun openFloatingMenu() {
        Log.d(TAG, "🔄 Abriendo menú flotante...")
        
        // Verificar que los componentes estén disponibles
        if (overlayBackground == null || fabMain == null) {
            Log.w(TAG, "⚠️ No se puede abrir menú - componentes no disponibles")
            return
        }
        
        isMenuOpen = true
        
        // Mostrar overlay
        overlayBackground?.let { overlay ->
            overlay.visibility = View.VISIBLE
            overlay.alpha = 0f
            overlay.animate()
                .alpha(1f)
                .setDuration(200)
                .start()
        }
        
        // Rotar FAB principal (+ se convierte en X)
        fabMain?.animate()
            ?.rotation(45f)
            ?.setDuration(300)
            ?.setInterpolator(android.view.animation.OvershootInterpolator())
            ?.start()
        
        // Animar opciones del menú
        animateMenuItems(true)
        Log.d(TAG, "✅ Menú flotante abierto")
    }
    
    /**
     * 📥 Cerrar menú flotante con animación
     */
    private fun closeFloatingMenu() {
        Log.d(TAG, "🔄 Cerrando menú flotante...")
        
        // Verificar que los componentes estén disponibles
        if (overlayBackground == null || fabMain == null) {
            Log.w(TAG, "⚠️ No se puede cerrar menú - componentes no disponibles")
            return
        }
        
        isMenuOpen = false
        
        // Ocultar overlay
        overlayBackground?.animate()
            ?.alpha(0f)
            ?.setDuration(200)
            ?.setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    overlayBackground?.visibility = View.GONE
                }
            })
            ?.start()
        
        // Rotar FAB principal de vuelta (X se convierte en +)
        fabMain?.animate()
            ?.rotation(0f)
            ?.setDuration(300)
            ?.setInterpolator(android.view.animation.OvershootInterpolator())
            ?.start()
        
        // Animar opciones del menú
        animateMenuItems(false)
        Log.d(TAG, "✅ Menú flotante cerrado")
    }
    
    /**
     * 🎬 Animar elementos del menú (versión simplificada)
     */
    private fun animateMenuItems(show: Boolean) {
        val items = listOfNotNull(menuEditBook, menuDeleteBook)
        
        if (items.isEmpty()) {
            Log.w(TAG, "⚠️ No hay elementos de menú para animar")
            return
        }
        
        Log.d(TAG, "🎬 Animando ${items.size} elementos del menú. Mostrar: $show")
        
        items.forEachIndexed { index, item ->
            Log.d(TAG, "🎯 Animando elemento $index - Estado actual: visibility=${item.visibility}, alpha=${item.alpha}")
            
            if (show) {
                // Mostrar elemento
                Log.d(TAG, "📤 Mostrando elemento $index")
                
                // NO cambiar visibility aquí - los elementos ya están VISIBLE en el layout
                item.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationY(0f)
                    .setStartDelay((index * 80).toLong())
                    .setDuration(350)
                    .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator) {
                            Log.d(TAG, "🎬 Iniciando animación mostrar elemento $index")
                        }
                        override fun onAnimationEnd(animation: Animator) {
                            Log.d(TAG, "✅ Elemento $index mostrado completamente")
                        }
                    })
                    .start()
            } else {
                // Ocultar elemento
                Log.d(TAG, "📥 Ocultando elemento $index")
                
                item.animate()
                    .alpha(0f)
                    .scaleX(0.7f)
                    .scaleY(0.7f)
                    .translationY(30f)
                    .setStartDelay((index * 50).toLong())
                    .setDuration(250)
                    .setInterpolator(android.view.animation.AccelerateInterpolator())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator) {
                            Log.d(TAG, "🎬 Iniciando animación ocultar elemento $index")
                        }
                        override fun onAnimationEnd(animation: Animator) {
                            Log.d(TAG, "✅ Elemento $index ocultado completamente")
                        }
                    })
                    .start()
            }
        }
    }

    /**
     * ✏️ Navegar a edición de libro
     */
    private fun editBook() {
        currentBook?.let { book ->
            Log.d(TAG, "✏️ Navegando a edición: ${book.title}")
            
            val editFragment = EditBookFragment.newInstance(book)
            parentFragmentManager.beginTransaction()
                .replace(R.id.admin_fragment_container, editFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    /**
     * 🗑️ Confirmar eliminación de libro
     */
    private fun confirmDeleteBook() {
        currentBook?.let { book ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Eliminar libro")
                .setMessage("¿Estás seguro de que deseas eliminar \"${book.title}\"?\n\nEsta acción no se puede deshacer.")
                .setPositiveButton("Eliminar") { _, _ ->
                    deleteBook(book)
                }
                .setNegativeButton("Cancelar", null)
                .setIcon(R.drawable.ic_delete_warning)
                .show()
        }
    }

    /**
     * 🗑️ Eliminar libro de Firebase
     */
    private fun deleteBook(book: Book) {
        Log.i(TAG, "🗑️ Eliminando libro: ${book.title}")
        
        firestore.collection("books")
            .document(book.id)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "✅ Libro eliminado exitosamente")
                Toast.makeText(context, "Libro eliminado correctamente", Toast.LENGTH_SHORT).show()
                
                // Regresar a la lista
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "❌ Error al eliminar libro", exception)
                Toast.makeText(
                    context,
                    "Error al eliminar libro: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    /**
     * ❌ Mostrar error al usuario
     */
    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        Log.e(TAG, "❌ Error: $message")
    }
}
