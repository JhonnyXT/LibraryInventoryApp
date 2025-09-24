package com.example.libraryinventoryapp.fragments

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.libraryinventoryapp.utils.NotificationHelper
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.libraryinventoryapp.R
import com.example.libraryinventoryapp.adapters.CommentsAdapter
import com.example.libraryinventoryapp.models.Book
import com.example.libraryinventoryapp.models.Comment
import com.example.libraryinventoryapp.models.WishlistItem
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 📖 BookDetailModernFragment - Pantalla de detalle de libro moderna
 * 
 * Características principales:
 * - Diseño premium con CollapsingToolbar
 * - Imagen de portada grande con parallax
 * - Información completa del libro
 * - Sistema de favoritos integrado
 * - Botón de solicitar libro
 * - Validaciones de disponibilidad
 */
class BookDetailModernFragment : Fragment() {

    companion object {
        private const val TAG = "BookDetailModern"
        private const val ARG_BOOK_ID = "book_id"
        
        fun newInstance(bookId: String): BookDetailModernFragment {
            val fragment = BookDetailModernFragment()
            val args = Bundle()
            args.putString(ARG_BOOK_ID, bookId)
            fragment.arguments = args
            return fragment
        }
    }

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    // Data
    private var bookId: String? = null
    private var currentBook: Book? = null
    private var currentUserId: String? = null
    private var isFavorite = false
    
    // UI Components
    private lateinit var collapsingToolbar: CollapsingToolbarLayout
    private lateinit var toolbarDetail: MaterialToolbar
    private lateinit var imgBookCoverLarge: ImageView
    private lateinit var btnFavoriteToolbar: MaterialButton
    
    private lateinit var textBookTitleContent: TextView
    private lateinit var textBookAuthorDetail: TextView
    private lateinit var cardAvailabilityStatus: MaterialCardView
    private lateinit var textAvailabilityStatus: TextView
    private lateinit var textBookDescription: TextView
    
    private lateinit var textIsbnDetail: TextView
    private lateinit var chipGroupCategoriesDetail: ChipGroup
    private lateinit var textAvailableCount: TextView
    
    private lateinit var cardAssignedUsers: MaterialCardView
    private lateinit var textAssignedUsersDetail: TextView
    
    // 💬 Components para comentarios
    private lateinit var cardAddComment: MaterialCardView
    private lateinit var editCommentText: com.google.android.material.textfield.TextInputEditText
    private lateinit var btnSendComment: MaterialButton
    private lateinit var cardCommentsList: MaterialCardView
    private lateinit var recyclerComments: androidx.recyclerview.widget.RecyclerView
    private lateinit var layoutNoComments: LinearLayout
    private lateinit var commentsAdapter: CommentsAdapter
    
    private lateinit var layoutOverdueWarning: LinearLayout
    private lateinit var textOverdueMessage: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_book_detail_modern, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        bookId = arguments?.getString(ARG_BOOK_ID)
        if (bookId == null) {
            Log.e(TAG, "❌ bookId es null")
            parentFragmentManager.popBackStack()
            return
        }
        
        Log.d(TAG, "📖 Inicializando BookDetail para: $bookId")
        
        initializeFirebase()
        initializeComponents(view)
        setupClickListeners()
        loadBookDetails()
        checkIfFavorite()
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
        // Header components
        collapsingToolbar = view.findViewById(R.id.collapsing_toolbar)
        toolbarDetail = view.findViewById(R.id.toolbar_detail)
        imgBookCoverLarge = view.findViewById(R.id.img_book_cover_large)
        btnFavoriteToolbar = view.findViewById(R.id.btn_favorite_toolbar)
        
        // 🎨 Configurar ícono de navegación Material Design
        toolbarDetail.setNavigationIcon(R.drawable.ic_arrow_back)
        
        // Content components
        textBookTitleContent = view.findViewById(R.id.text_book_title_content)
        textBookAuthorDetail = view.findViewById(R.id.text_book_author_detail)
        cardAvailabilityStatus = view.findViewById(R.id.card_availability_status)
        textAvailabilityStatus = view.findViewById(R.id.text_availability_status)
        textBookDescription = view.findViewById(R.id.text_book_description)
        
        // Details components
        textIsbnDetail = view.findViewById(R.id.text_isbn_detail)
        chipGroupCategoriesDetail = view.findViewById(R.id.chip_group_categories_detail)
        textAvailableCount = view.findViewById(R.id.text_available_count)
        
        // Assigned users
        cardAssignedUsers = view.findViewById(R.id.card_assigned_users)
        textAssignedUsersDetail = view.findViewById(R.id.text_assigned_users_detail)
        
        // 💬 Comments components
        cardAddComment = view.findViewById(R.id.card_add_comment)
        editCommentText = view.findViewById(R.id.edit_comment_text)
        btnSendComment = view.findViewById(R.id.btn_send_comment)
        cardCommentsList = view.findViewById(R.id.card_comments_list)
        recyclerComments = view.findViewById(R.id.recycler_comments)
        layoutNoComments = view.findViewById(R.id.layout_no_comments)
        
        // Overdue warning
        layoutOverdueWarning = view.findViewById(R.id.layout_overdue_warning)
        textOverdueMessage = view.findViewById(R.id.text_overdue_message)
    }

    /**
     * 👆 Configurar click listeners
     */
    private fun setupClickListeners() {
        // Back button
        toolbarDetail.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        // Botón favorito en toolbar (único botón de favorito)
        btnFavoriteToolbar.setOnClickListener {
            toggleFavorite()
        }
        
        // 💬 Configurar sistema de comentarios
        setupCommentsSystem()
        
        // Setup AppBarLayout collapse listener para cambiar colores dinámicamente
        setupAppBarCollapseListener()
    }

    /**
     * 🎨 Configurar listener para cambios de colapso del AppBarLayout
     */
    private fun setupAppBarCollapseListener() {
        val appBarLayout = view?.findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.app_bar_detail)
        appBarLayout?.addOnOffsetChangedListener { _, verticalOffset ->
            val totalScrollRange = appBarLayout.totalScrollRange
            val percentage = Math.abs(verticalOffset).toFloat() / totalScrollRange.toFloat()
            
            // Cuando está completamente colapsado (percentage >= 0.8)
            if (percentage >= 0.8f) {
                // Usar colores para fondo sólido (negro para contraste)
                val darkColor = requireContext().getColor(android.R.color.black)
                toolbarDetail.navigationIcon?.setTint(darkColor)
                btnFavoriteToolbar.iconTint = requireContext().getColorStateList(android.R.color.black)
            } else {
                // Usar colores para fondo con imagen (iconos blancos)
                val whiteColor = requireContext().getColor(android.R.color.white)
                toolbarDetail.navigationIcon?.setTint(whiteColor)
                btnFavoriteToolbar.iconTint = requireContext().getColorStateList(android.R.color.white)
            }
        }
    }

    /**
     * 📚 Cargar detalles del libro
     */
    private fun loadBookDetails() {
        firestore.collection("books").document(bookId!!)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    try {
                        val book = document.toObject(Book::class.java)
                        book?.id = document.id
                        currentBook = book
                        
                        if (book != null) {
                            populateBookDetails(book)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error deserializando libro ${document.id}: ${e.message}")
                        showError("Error cargando detalles del libro - Problema de compatibilidad")
                        // Regresar al listado
                        parentFragmentManager.popBackStack()
                    }
                } else {
                    Log.e(TAG, "❌ Libro no encontrado")
                    showError("Libro no encontrado")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error cargando libro: ${e.message}")
                showError("Error cargando libro")
            }
    }

    /**
     * 🎨 Llenar UI con datos del libro
     */
    private fun populateBookDetails(book: Book) {
        // Título solo en contenido (más limpio)
        collapsingToolbar.title = ""  // Sin título en toolbar
        textBookTitleContent.text = book.title
        textBookAuthorDetail.text = book.author
        
        // Imagen de portada
        loadBookCover(book)
        
        // Estado de disponibilidad
        updateAvailabilityStatus(book)
        
        // Descripción
        textBookDescription.text = if (book.description.isNotEmpty()) {
            book.description
        } else {
            "Sin descripción disponible."
        }
        
        // Detalles técnicos
        textIsbnDetail.text = book.isbn.ifEmpty { "Sin ISBN" }
        
        // Categorías
        populateCategories(book.categories)
        
        // Copias disponibles
        updateAvailableCount(book)
        
        // Usuarios asignados
        updateAssignedUsers(book)
        
        // Validar alertas de vencimiento
        checkOverdueWarning(book)
        
        // 💬 Configurar sistema de comentarios
        setupCommentsForBook(book)
        
        Log.d(TAG, "✅ Detalles del libro cargados: ${book.title}")
    }

    /**
     * 📚 Cargar imagen de portada
     */
    private fun loadBookCover(book: Book) {
        if (book.imageUrl.isNullOrEmpty()) {
            imgBookCoverLarge.setImageResource(R.drawable.book_placeholder)
        } else {
            Glide.with(this)
                .load(book.imageUrl)
                .placeholder(R.drawable.book_placeholder)
                .error(R.drawable.book_placeholder)
                .centerCrop()
                .into(imgBookCoverLarge)
        }
        
        // 🖼️ Click para expandir imagen
        imgBookCoverLarge.setOnClickListener {
            showImageFullscreen(book.imageUrl)
        }
    }
    
    /**
     * 🖼️ Mostrar imagen en pantalla completa
     */
    private fun showImageFullscreen(imageUrl: String?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_image_fullscreen, null)
        val dialog = android.app.Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        
        dialog.setContentView(dialogView)
        dialog.window?.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        
        val imgFullscreen = dialogView.findViewById<ImageView>(R.id.img_fullscreen)
        val btnClose = dialogView.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.btn_close_fullscreen)
        
        // Cargar imagen con zoom
        if (imageUrl.isNullOrEmpty()) {
            imgFullscreen.setImageResource(R.drawable.book_placeholder)
        } else {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.book_placeholder)
                .error(R.drawable.book_placeholder)
                .fitCenter()
                .into(imgFullscreen)
        }
        
        // Configurar zoom y pan
        setupImageZoom(imgFullscreen)
        
        // Cerrar dialog
        btnClose.setOnClickListener { dialog.dismiss() }
        
        // Cerrar con click fuera de la imagen
        dialogView.setOnClickListener { dialog.dismiss() }
        
        dialog.show()
    }
    
    /**
     * 🔍 Configurar zoom y pan para imagen
     */
    private fun setupImageZoom(imageView: ImageView) {
        val scaleGestureDetector = android.view.ScaleGestureDetector(
            requireContext(),
            object : android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
                private var scaleFactor = 1.0f
                
                override fun onScale(detector: android.view.ScaleGestureDetector): Boolean {
                    scaleFactor *= detector.scaleFactor
                    scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 3.0f))
                    
                    imageView.scaleX = scaleFactor
                    imageView.scaleY = scaleFactor
                    return true
                }
            }
        )
        
        imageView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            true
        }
    }

    /**
     * 🎯 Actualizar estado de disponibilidad
     */
    private fun updateAvailabilityStatus(book: Book) {
        val availableCount = calculateAvailableBooks(book)
        val isAvailable = availableCount > 0
        
        if (isAvailable) {
            textAvailabilityStatus.text = "Disponible"
            cardAvailabilityStatus.setCardBackgroundColor(
                requireContext().getColor(R.color.available_bg)
            )
            textAvailabilityStatus.setTextColor(
                requireContext().getColor(R.color.available_text)
            )
        } else {
            textAvailabilityStatus.text = "No disponible"
            cardAvailabilityStatus.setCardBackgroundColor(
                requireContext().getColor(R.color.not_available_bg)
            )
            textAvailabilityStatus.setTextColor(
                requireContext().getColor(R.color.not_available_text)
            )
        }
    }

    /**
     * 🏷️ Llenar chips de categorías
     */
    private fun populateCategories(categories: List<String>) {
        chipGroupCategoriesDetail.removeAllViews()
        
        if (categories.isEmpty()) {
            val chip = Chip(requireContext())
            chip.text = "Sin categoría"
            chip.isEnabled = false
            chipGroupCategoriesDetail.addView(chip)
        } else {
            categories.forEach { category ->
                val chip = Chip(requireContext())
                chip.text = category
                chip.isClickable = false
                chipGroupCategoriesDetail.addView(chip)
            }
        }
    }

    /**
     * 📊 Actualizar conteo de libros disponibles
     */
    private fun updateAvailableCount(book: Book) {
        val availableCount = calculateAvailableBooks(book)
        val totalCount = book.quantity
        textAvailableCount.text = "$availableCount de $totalCount"
    }

    /**
     * 👥 Actualizar usuarios asignados
     */
    private fun updateAssignedUsers(book: Book) {
        if (book.assignedWithNames.isNullOrEmpty()) {
            cardAssignedUsers.visibility = View.GONE
        } else {
            cardAssignedUsers.visibility = View.VISIBLE
            val assignedText = book.assignedWithNames!!.joinToString(", ")
            textAssignedUsersDetail.text = assignedText
        }
    }

    /**
     * ⚠️ Verificar alertas de vencimiento (como en AssignedBooksAdapter)
     */
    private fun checkOverdueWarning(book: Book) {
        if (currentUserId == null) return
        
        // Verificar si el usuario actual tiene este libro asignado
        val userIndex = book.assignedTo?.indexOf(currentUserId) ?: -1
        if (userIndex < 0 || book.loanExpirationDates.isNullOrEmpty() || userIndex >= book.loanExpirationDates!!.size) {
            layoutOverdueWarning.visibility = View.GONE
            return
        }
        
        // Calcular días hasta vencimiento
        val expirationDate = book.loanExpirationDates!![userIndex]
        val daysUntilDue = calculateDaysUntilDue(expirationDate)
        
        // Mostrar alerta según la misma lógica que AssignedBooksAdapter
        if (daysUntilDue <= 5) { // Próximos 5 días, hoy, o vencidos
            layoutOverdueWarning.visibility = View.VISIBLE
            
            val message = when {
                daysUntilDue > 0 -> "Tienes este libro próximo a vencer en $daysUntilDue días"
                daysUntilDue == 0 -> "⚠️ Este libro vence HOY - Debes devolverlo"
                daysUntilDue < 0 -> {
                    val overdueDays = kotlin.math.abs(daysUntilDue)
                    "🔴 Este libro está vencido hace $overdueDays días - Debes devolverlo URGENTE"
                }
                else -> "Información sobre el vencimiento de este libro"
            }
            
            textOverdueMessage.text = message
            
            Log.d(TAG, "⚠️ Alerta de vencimiento mostrada: $daysUntilDue días")
        } else {
            layoutOverdueWarning.visibility = View.GONE
        }
    }

    /**
     * 📅 Calcular días hasta vencimiento (igual que en UserActivity)
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
     * ⭐ Verificar si está en favoritos
     */
    private fun checkIfFavorite() {
        if (currentUserId == null || bookId == null) return
        
        firestore.collection("wishlist")
            .whereEqualTo("userId", currentUserId)
            .whereEqualTo("bookId", bookId)
            .get()
            .addOnSuccessListener { documents ->
                isFavorite = !documents.isEmpty
                updateFavoriteButtons()
            }
    }

    /**
     * 💖 Toggle favorito
     */
    private fun toggleFavorite() {
        if (currentUserId == null || bookId == null || currentBook == null) {
            showError("Error: datos del usuario o libro no disponibles")
            return
        }
        
        if (isFavorite) {
            removeFromWishlist()
        } else {
            addToWishlist()
        }
    }

    /**
     * ➕ Añadir a favoritos
     */
    private fun addToWishlist() {
        val wishlistItem = WishlistItem(
            userId = currentUserId!!,
            bookId = bookId!!,
            bookTitle = currentBook!!.title,
            bookAuthor = currentBook!!.author,
            bookImageUrl = currentBook!!.imageUrl,
            bookCategories = currentBook!!.categories,
            addedDate = Timestamp.now(),
            isAvailable = calculateAvailableBooks(currentBook!!) > 0
        )
        
        firestore.collection("wishlist")
            .add(wishlistItem)
            .addOnSuccessListener {
                isFavorite = true
                updateFavoriteButtons()
                NotificationHelper.showSuccess(
                    context = requireContext(),
                    title = "Añadido a Favoritos",
                    message = "⭐ El libro ha sido agregado a tu lista de deseos.",
                    view = view
                )
                Log.d(TAG, "✅ Libro añadido a wishlist: ${currentBook!!.title}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error añadiendo a wishlist: ${e.message}")
                showError("Error añadiendo a favoritos")
            }
    }

    /**
     * ➖ Remover de favoritos
     */
    private fun removeFromWishlist() {
        firestore.collection("wishlist")
            .whereEqualTo("userId", currentUserId)
            .whereEqualTo("bookId", bookId)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.delete()
                }
                isFavorite = false
                updateFavoriteButtons()
                NotificationHelper.showSuccess(
                    context = requireContext(),
                    title = "Removido de Favoritos",
                    message = "El libro ha sido eliminado de tu lista de deseos.",
                    view = view
                )
                Log.d(TAG, "✅ Libro removido de wishlist: ${currentBook!!.title}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error removiendo de wishlist: ${e.message}")
                showError("Error removiendo de favoritos")
            }
    }

    /**
     * 🎨 Actualizar botón de favorito (solo toolbar)
     */
    private fun updateFavoriteButtons() {
        if (isFavorite) {
            // Favorito activo - estrella llena
            btnFavoriteToolbar.setIconResource(android.R.drawable.btn_star_big_on)
        } else {
            // Favorito inactivo - estrella vacía
            btnFavoriteToolbar.setIconResource(android.R.drawable.btn_star_big_off)
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
     * ❌ Mostrar error
     */
    private fun showError(message: String) {
        NotificationHelper.showError(
            context = requireContext(),
            title = "Error",
            message = message,
            view = view
        )
    }
    
    // ═══════════════════════════════════════════════════════════
    // 💬 SISTEMA DE COMENTARIOS EN TIEMPO REAL
    // ═══════════════════════════════════════════════════════════
    
    /**
     * 💬 Configurar sistema de comentarios
     */
    private fun setupCommentsSystem() {
        // Configurar RecyclerView
        commentsAdapter = CommentsAdapter(mutableListOf())
        recyclerComments.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            adapter = commentsAdapter
        }
        
        // Configurar listener para enviar comentario
        btnSendComment.setOnClickListener {
            val commentText = editCommentText.text.toString().trim()
            if (commentText.isNotEmpty()) {
                sendComment(commentText)
            } else {
                editCommentText.error = "Escribe algo para comentar"
            }
        }
        
        // Cargar comentarios en tiempo real
        loadCommentsRealTime()
    }
    
    /**
     * 🔍 Verificar si el usuario puede comentar (tiene libro asignado)
     */
    private fun checkCanUserComment(book: Book) {
        val currentUserId = auth.currentUser?.uid
        val canComment = currentUserId != null && 
                        book.assignedTo?.contains(currentUserId) == true
        
        if (canComment) {
            cardAddComment.visibility = View.VISIBLE
            Log.d(TAG, "✅ Usuario puede comentar: tiene libro asignado")
        } else {
            cardAddComment.visibility = View.GONE
            Log.d(TAG, "❌ Usuario NO puede comentar: no tiene libro asignado")
        }
    }
    
    /**
     * 📝 Enviar nuevo comentario
     */
    private fun sendComment(commentText: String) {
        val currentUser = auth.currentUser
        val safeBookId = this.bookId
        
        if (currentUser == null) {
            showError("Error: Usuario no autenticado")
            return
        }
        
        if (safeBookId == null) {
            showError("Error: ID de libro no válido")
            return
        }
        
        // Mostrar loading en el botón
        btnSendComment.isEnabled = false
        btnSendComment.text = "Enviando..."
        
        // 👤 Obtener nombre real del usuario desde Firestore
        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { userDoc ->
                val userName = if (userDoc.exists()) {
                    userDoc.getString("name") ?: currentUser.displayName ?: "Usuario Anónimo"
                } else {
                    currentUser.displayName ?: "Usuario Anónimo"
                }
                
                // Crear comentario con nombre correcto
                val commentId = firestore.collection("comments").document().id
                val comment = Comment(
                    id = commentId,
                    bookId = safeBookId,
                    userId = currentUser.uid,
                    userName = userName,
                    userEmail = currentUser.email ?: "",
                    comment = commentText,
                    timestamp = com.google.firebase.Timestamp.now()
                )
                
                // Guardar en Firestore
                firestore.collection("comments")
                    .document(commentId)
                    .set(comment)
                    .addOnSuccessListener {
                        // Limpiar campo y restaurar botón
                        editCommentText.text?.clear()
                        btnSendComment.isEnabled = true
                        btnSendComment.text = "Enviar comentario"
                        
                        NotificationHelper.showSuccess(
                            context = requireContext(),
                            title = "Comentario Enviado",
                            message = "✅ Tu comentario ha sido publicado correctamente.",
                            view = view
                        )
                        Log.d(TAG, "✅ Comentario guardado: $commentId")
                    }
                    .addOnFailureListener { e ->
                        btnSendComment.isEnabled = true
                        btnSendComment.text = "Enviar comentario"
                        showError("❌ Error al enviar comentario")
                        Log.e(TAG, "❌ Error al guardar comentario", e)
                    }
            }
            .addOnFailureListener { e ->
                // Usar nombre por defecto si falla la consulta
                val userName = currentUser.displayName ?: "Usuario"
                
                val commentId = firestore.collection("comments").document().id
                val comment = Comment(
                    id = commentId,
                    bookId = safeBookId,
                    userId = currentUser.uid,
                    userName = userName,
                    userEmail = currentUser.email ?: "",
                    comment = commentText,
                    timestamp = com.google.firebase.Timestamp.now()
                )
                
                firestore.collection("comments")
                    .document(commentId)
                    .set(comment)
                    .addOnSuccessListener {
                        editCommentText.text?.clear()
                        btnSendComment.isEnabled = true
                        btnSendComment.text = "Enviar comentario"
                        NotificationHelper.showSuccess(
                            context = requireContext(),
                            title = "Comentario Enviado",
                            message = "✅ Tu comentario ha sido publicado correctamente.",
                            view = view
                        )
                    }
                    .addOnFailureListener { 
                        btnSendComment.isEnabled = true
                        btnSendComment.text = "Enviar comentario"
                        showError("❌ Error al enviar comentario")
                    }
                
                Log.w(TAG, "⚠️ Error al obtener datos de usuario, usando nombre por defecto", e)
            }
    }
    
    /**
     * 🔄 Cargar comentarios en tiempo real con listener
     */
    private fun loadCommentsRealTime() {
        val safeBookId = this.bookId
        if (safeBookId == null) {
            Log.e(TAG, "❌ No se puede cargar comentarios: bookId es null")
            return
        }
        
        Log.d(TAG, "🔄 Configurando listener de comentarios para libro: $safeBookId")
        
        // 📝 Query simplificado sin orderBy para evitar problemas de índices
        firestore.collection("comments")
            .whereEqualTo("bookId", safeBookId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "❌ Error en listener de comentarios", e)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val comments = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Comment::class.java)?.copy(id = doc.id)
                        } catch (ex: Exception) {
                            Log.e(TAG, "❌ Error al parsear comentario: ${doc.id}", ex)
                            null
                        }
                    }.sortedByDescending { it.timestamp } // Ordenar en el cliente
                    
                    Log.d(TAG, "📝 Comentarios recibidos en tiempo real: ${comments.size}")
                    
                    // 🔄 Actualizar en el hilo principal
                    if (isAdded && !isDetached) { // Verificar que el fragmento esté activo
                        updateCommentsUI(comments)
                    }
                }
            }
    }
    
    /**
     * 🎨 Actualizar UI de comentarios
     */
    private fun updateCommentsUI(comments: List<Comment>) {
        try {
            if (comments.isEmpty()) {
                // Sin comentarios - mostrar estado vacío
                cardCommentsList.visibility = View.VISIBLE
                recyclerComments.visibility = View.GONE
                layoutNoComments.visibility = View.VISIBLE
                Log.d(TAG, "📄 Mostrar estado vacío de comentarios")
            } else {
                // Hay comentarios - mostrar lista
                cardCommentsList.visibility = View.VISIBLE
                recyclerComments.visibility = View.VISIBLE
                layoutNoComments.visibility = View.GONE
                
                // 🔄 Actualizar adapter con animación
                commentsAdapter.updateComments(comments)
                
                Log.d(TAG, "📝 Comentarios actualizados en tiempo real: ${comments.size}")
                
                // 📜 Opcional: scroll al último comentario si se agregó uno nuevo
                if (comments.isNotEmpty()) {
                    recyclerComments.post {
                        recyclerComments.smoothScrollToPosition(0)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al actualizar UI de comentarios", e)
        }
    }
    
    /**
     * 📋 Configurar comentarios para un libro específico
     */
    private fun setupCommentsForBook(book: Book) {
        // Verificar si usuario puede comentar
        checkCanUserComment(book)
        
        // Los comentarios se cargan automáticamente con el listener en tiempo real
        Log.d(TAG, "💬 Sistema de comentarios configurado para: ${book.title}")
    }
}
