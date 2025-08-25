package com.example.libraryinventoryapp.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.libraryinventoryapp.R
import com.example.libraryinventoryapp.models.Book
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.button.MaterialButton
import android.app.AlertDialog
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

class AssignedBooksAdapter(
    private val assignedBooks: List<Book>
) : RecyclerView.Adapter<AssignedBooksAdapter.AssignedBookViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssignedBookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_assigned_book, parent, false)
        return AssignedBookViewHolder(view)
    }

    override fun onBindViewHolder(holder: AssignedBookViewHolder, position: Int) {
        val book = assignedBooks[position]
        holder.bind(book)
    }

    override fun getItemCount(): Int = assignedBooks.size

    inner class AssignedBookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.book_title)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.book_description)
        private val authorTextView: TextView = itemView.findViewById(R.id.book_author)
        private val categoriesTextView: TextView = itemView.findViewById(R.id.book_cetegories)
        private val isbnTextView: TextView = itemView.findViewById(R.id.book_isbn)
        private val bookImageView: ImageView = itemView.findViewById(R.id.book_image)

        init {
            // Click en toda la tarjeta para mostrar detalles
            itemView.setOnClickListener {
                val book = assignedBooks[adapterPosition]
                showAssignedBookDetailsBottomSheet(book, itemView.context)
            }
        }

        fun bind(book: Book) {
            titleTextView.text = book.title
            authorTextView.text = "por ${book.author}"

            // Verificar si el usuario actual tiene un préstamo vencido
            checkUserExpirationStatus(book)

            // Load book image using Glide
            Glide.with(itemView.context)
                .load(book.imageUrl)
                .placeholder(R.drawable.ic_book_default)
                .error(R.drawable.ic_book_default)
                .into(bookImageView)
        }

        private fun checkUserExpirationStatus(book: Book) {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            val currentTime = System.currentTimeMillis()
            val expirationAlert = itemView.findViewById<TextView>(R.id.expiration_alert)

            if (currentUserId != null) {
                // Buscar el índice del usuario actual en la lista de asignados
                val userIndex = book.assignedTo?.indexOf(currentUserId)
                if (userIndex != null && userIndex >= 0 && 
                    book.loanExpirationDates != null && userIndex < book.loanExpirationDates.size) {
                    
                    val userExpirationDate = book.loanExpirationDates[userIndex]
                    val expirationTime = userExpirationDate.toDate().time
                    val daysDiff = ((expirationTime - currentTime) / (24 * 60 * 60 * 1000)).toInt()
                    
                    val statusIndicator = itemView.findViewById<View>(R.id.status_indicator)
                    
                    when {
                        daysDiff < 0 -> {
                            // Préstamo vencido
                            val daysOverdue = (-daysDiff)
                            expirationAlert.apply {
                                visibility = View.VISIBLE
                                text = "⚠️ Vencido hace $daysOverdue días"
                                setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
                            }
                            statusIndicator.setBackgroundColor(itemView.context.getColor(android.R.color.holo_red_dark))
                        }
                        daysDiff <= 5 -> {
                            // Por vencer (5 días o menos)
                            expirationAlert.apply {
                                visibility = View.VISIBLE
                                text = if (daysDiff == 0) "⏰ Vence HOY" else "⏰ Vence en $daysDiff días"
                                setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                            }
                            statusIndicator.setBackgroundColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                        }
                        else -> {
                            // Préstamo vigente sin alertas
                            expirationAlert.visibility = View.GONE
                            statusIndicator.setBackgroundColor(itemView.context.getColor(android.R.color.holo_green_dark))
                        }
                    }
                } else {
                    // No hay fecha de vencimiento o error
                    expirationAlert.visibility = View.GONE
                }
            } else {
                expirationAlert.visibility = View.GONE
            }
        }
    }

    // BottomSheet para libros asignados al usuario
    private fun showAssignedBookDetailsBottomSheet(book: Book, context: Context) {
        val bottomSheetDialog = BottomSheetDialog(context)
        val bottomSheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_book_details, null)
        bottomSheetDialog.setContentView(bottomSheetView)

        // Referencias a los elementos del BottomSheet
        val bsBookImage = bottomSheetView.findViewById<ImageView>(R.id.bs_book_image)
        val bsBookTitle = bottomSheetView.findViewById<TextView>(R.id.bs_book_title)
        val bsBookAuthor = bottomSheetView.findViewById<TextView>(R.id.bs_book_author)
        val bsBookStatus = bottomSheetView.findViewById<TextView>(R.id.bs_book_status)
        val bsBookCategory = bottomSheetView.findViewById<TextView>(R.id.bs_book_category)
        val bsBookQuantity = bottomSheetView.findViewById<TextView>(R.id.bs_book_quantity)
        val bsBookIsbn = bottomSheetView.findViewById<TextView>(R.id.bs_book_isbn)
        val bsBookAssignedTo = bottomSheetView.findViewById<TextView>(R.id.bs_book_assigned_to)
        val bsBookDescription = bottomSheetView.findViewById<TextView>(R.id.bs_book_description)
        val bsExpirationAlert = bottomSheetView.findViewById<TextView>(R.id.bs_expiration_alert)
        // bs_alert_container es un View vacío ahora, usamos directamente bs_expiration_alert
        
        // Ocultar elementos admin para usuarios (BottomSheet simplificado)
        val bsButtonsContainer = bottomSheetView.findViewById<View>(R.id.bs_buttons_container)
        val bsEditButton = bottomSheetView.findViewById<MaterialButton>(R.id.bs_editBookButton)
        val bsDeleteButton = bottomSheetView.findViewById<MaterialButton>(R.id.bs_deleteBookButton)
        
        // Ocultar botones de admin para usuarios
        bsButtonsContainer.visibility = View.GONE
        
        // Ocultar específicamente los botones de admin
        bsEditButton?.visibility = View.GONE
        bsDeleteButton?.visibility = View.GONE

        // Poblar datos y configurar botón de devolución si es necesario
        populateAssignedBookData(
            book, bsBookImage, bsBookTitle, bsBookAuthor, bsBookStatus, 
            bsBookCategory, bsBookQuantity, bsBookIsbn, bsBookAssignedTo, 
            bsBookDescription, bsExpirationAlert, bsButtonsContainer, 
            context
        )

        bottomSheetDialog.show()
    }

    private fun populateAssignedBookData(
        book: Book,
        bsBookImage: ImageView,
        bsBookTitle: TextView,
        bsBookAuthor: TextView,
        bsBookStatus: TextView,
        bsBookCategory: TextView,
        bsBookQuantity: TextView,
        bsBookIsbn: TextView,
        bsBookAssignedTo: TextView,
        bsBookDescription: TextView,
        bsExpirationAlert: TextView,
        bsButtonsContainer: View,
        context: Context
    ) {
        // Imagen
        if (book.imageUrl.isNullOrEmpty()) {
            bsBookImage.setImageResource(R.drawable.ic_book_default)
        } else {
            Glide.with(context).load(book.imageUrl).into(bsBookImage)
        }

        // Textos
        bsBookTitle.text = book.title
        bsBookAuthor.text = "👤 por ${book.author}"
        bsBookStatus.text = "📖 Asignado a ti"  // Para libros asignados
        bsBookCategory.text = if (book.categories.isNotEmpty()) {
            book.categories.joinToString(", ")
        } else {
            "Sin categoría"
        }
        bsBookQuantity.text = "${book.quantity} libros totales"
        bsBookIsbn.text = book.isbn
        bsBookDescription.text = book.description.ifEmpty { "Sin descripción disponible" }
        
        // Mostrar estado de asignación personal
        bsBookAssignedTo.text = "Tienes este libro asignado"

        // Verificar fecha de vencimiento del préstamo del usuario actual
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val currentTime = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        var userLoanExpired = false
        var expirationDateText = ""
        var dueDateText = ""

        if (currentUserId != null) {
            // Buscar el índice del usuario actual en la lista de asignados
            val userIndex = book.assignedTo?.indexOf(currentUserId)
            if (userIndex != null && userIndex >= 0 && book.loanExpirationDates != null && userIndex < book.loanExpirationDates.size) {
                val userExpirationDate = book.loanExpirationDates[userIndex]
                val expirationTime = userExpirationDate.toDate().time
                val daysDiff = ((expirationTime - currentTime) / (24 * 60 * 60 * 1000)).toInt()
                
                // Formato de la fecha de vencimiento para mostrar
                dueDateText = dateFormat.format(userExpirationDate.toDate())
                
                when {
                    daysDiff < 0 -> {
                        // Préstamo vencido
                        userLoanExpired = true
                        val daysOverdue = (-daysDiff)
                        expirationDateText = "Tu préstamo venció hace $daysOverdue día(s) - Fecha límite: $dueDateText"
                    }
                    daysDiff <= 5 -> {
                        // Por vencer (alerta urgente)
                        expirationDateText = if (daysDiff == 0) {
                            "⚠️ Tu préstamo VENCE HOY - Fecha límite: $dueDateText"
                        } else {
                            "⚠️ Tu préstamo vence en $daysDiff días - Fecha límite: $dueDateText"
                        }
                    }
                    else -> {
                        // Préstamo vigente
                        expirationDateText = "Fecha de entrega: $dueDateText (en $daysDiff días)"
                    }
                }
            }
        }

        if (userLoanExpired) {
            bsExpirationAlert.visibility = View.VISIBLE
            bsExpirationAlert.text = "🔴 PRÉSTAMO VENCIDO\n$expirationDateText"
            bsExpirationAlert.setTextColor(context.getColor(android.R.color.holo_red_dark))
            
            // Botón "Ya devolví" eliminado - gestión completa ahora en otras pantallas
        } else if (expirationDateText.isNotEmpty()) {
            bsExpirationAlert.visibility = View.VISIBLE
            bsExpirationAlert.text = "📅 $expirationDateText"
            
            // Color según proximidad de vencimiento
            val currentUserId2 = FirebaseAuth.getInstance().currentUser?.uid
            val userIndex2 = book.assignedTo?.indexOf(currentUserId2)
            if (userIndex2 != null && userIndex2 >= 0 && book.loanExpirationDates != null && userIndex2 < book.loanExpirationDates.size) {
                val userExpirationDate2 = book.loanExpirationDates[userIndex2]
                val daysDiff2 = ((userExpirationDate2.toDate().time - currentTime) / (24 * 60 * 60 * 1000)).toInt()
                
                val alertColor = when {
                    daysDiff2 <= 5 -> android.R.color.holo_orange_dark
                    else -> android.R.color.holo_blue_dark
                }
                bsExpirationAlert.setTextColor(context.getColor(alertColor))
            } else {
                bsExpirationAlert.setTextColor(context.getColor(android.R.color.holo_blue_dark))
            }
            
            bsButtonsContainer.visibility = View.GONE
        } else {
            bsExpirationAlert.visibility = View.GONE
            bsButtonsContainer.visibility = View.GONE
        }
    }
    
    private fun showReturnConfirmationDialog(book: Book, context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Confirmar Devolución")
        builder.setMessage("¿Confirmas que ya devolviste el libro '${book.title}'?")
        
        builder.setPositiveButton("SÍ, DEVOLVÍ") { _, _ ->
            markBookAsReturned(book, context)
        }
        
        builder.setNegativeButton("Cancelar", null)
        
        val dialog = builder.create()
        dialog.show()
    }
    
    private fun markBookAsReturned(book: Book, context: Context) {
        // Implementar lógica para marcar el libro como devuelto
        // Esto podría ser similar a la funcionalidad en OverdueBooksAdapter
        Toast.makeText(context, "¡Libro marcado como devuelto!", Toast.LENGTH_SHORT).show()
        
        // Aquí iríamos a Firebase para actualizar el libro
        // Remover al usuario de la lista de asignados, restaurar cantidad, etc.
    }
}