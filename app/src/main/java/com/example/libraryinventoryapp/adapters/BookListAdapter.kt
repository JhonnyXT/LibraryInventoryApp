package com.example.libraryinventoryapp.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.libraryinventoryapp.R
import com.example.libraryinventoryapp.models.Book
import com.google.android.material.bottomsheet.BottomSheetDialog

class BookListAdapter(
    private val books: List<Book>,
    private val onAssignClick: (Book) -> Unit
) : RecyclerView.Adapter<BookListAdapter.BookListViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book_user, parent, false)
        return BookListViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookListViewHolder, position: Int) {
        val book = books[position]
        holder.bind(book)
    }

    override fun getItemCount(): Int = books.size

    inner class BookListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.book_title)
        private val descriptionView: TextView = itemView.findViewById(R.id.book_description)
        private val authorTextView: TextView = itemView.findViewById(R.id.book_author)
        private val categoriesTextView: TextView = itemView.findViewById(R.id.book_categories)
        private val statusTextView: TextView = itemView.findViewById(R.id.book_status)
        private val bookImageView: ImageView = itemView.findViewById(R.id.book_image)
        private val assignButton: Button = itemView.findViewById(R.id.assign_button)
        private val quantityTextView: TextView = itemView.findViewById(R.id.book_quantity)

        init {
            // Click en toda la tarjeta para mostrar detalles
            itemView.setOnClickListener {
                val book = books[adapterPosition]
                showBookDetailsBottomSheet(book, itemView.context)
            }
        }

        fun bind(book: Book) {
            titleTextView.text = book.title
            authorTextView.text = "por ${book.author}"
            statusTextView.text = book.status

            // Load book image with Glide
            Glide.with(itemView.context)
                .load(book.imageUrl)
                .placeholder(R.drawable.ic_book_default)
                .error(R.drawable.ic_book_default)
                .into(bookImageView)
        }
    }

    // BottomSheet para usuarios (sin opciones de admin)
    private fun showBookDetailsBottomSheet(book: Book, context: Context) {
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
        
        // Ocultar elementos admin para usuarios (BottomSheet simplificado - solo información)
        val bsButtonsContainer = bottomSheetView.findViewById<View>(R.id.bs_buttons_container)
        
        // Usuarios no ven botones admin
        bsButtonsContainer.visibility = View.GONE

        // Poblar datos
        populateUserBottomSheetData(
            book, bsBookImage, bsBookTitle, bsBookAuthor, bsBookStatus, 
            bsBookCategory, bsBookQuantity, bsBookIsbn, bsBookAssignedTo, 
            bsBookDescription, bsExpirationAlert, context
        )

        bottomSheetDialog.show()
    }

    private fun populateUserBottomSheetData(
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
        
        // Calcular disponibilidad real basada en stock vs usuarios asignados
        val totalCopies = book.quantity
        val assignedCopies = book.assignedTo?.size ?: 0
        val availableCopies = totalCopies - assignedCopies
        
        when {
            totalCopies == 0 -> {
                bsBookStatus.text = "❌ No disponible - Sin libros"
            }
            availableCopies <= 0 -> {
                bsBookStatus.text = "❌ No disponible - Todos asignados"
            }
            else -> {
                bsBookStatus.text = "✅ Disponible - $availableCopies de $totalCopies libres"
            }
        }
        
        bsBookCategory.text = if (book.categories.isNotEmpty()) {
            book.categories.joinToString(", ")
        } else {
            "Sin categoría"
        }
        bsBookQuantity.text = "$totalCopies libros (📖 $assignedCopies asignados, 📗 $availableCopies libres)"
        bsBookIsbn.text = book.isbn
        bsBookDescription.text = book.description.ifEmpty { "Sin descripción disponible" }
        
        // Para usuarios, mostrar información general de asignaciones
        bsBookAssignedTo.text = if (book.assignedWithNames.isNullOrEmpty()) {
            "Disponible para préstamo"
        } else {
            "${book.assignedWithNames.size} usuario(s) tienen este libro asignado"
        }

        // Los usuarios no ven alertas específicas de vencimiento
        bsExpirationAlert.visibility = View.GONE
    }
}