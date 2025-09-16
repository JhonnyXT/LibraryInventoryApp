package com.example.libraryinventoryapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.libraryinventoryapp.R
import com.example.libraryinventoryapp.models.Book
import com.google.android.material.card.MaterialCardView

/**
 * 📚 Adapter para grid de libros (3 columnas)
 * 
 * Diseño minimalista como en la primera imagen
 */
class BooksGridAdapter(
    private val books: MutableList<Book>,
    private val onBookClick: (Book) -> Unit
) : RecyclerView.Adapter<BooksGridAdapter.BookGridViewHolder>() {

    companion object {
        private const val TAG = "BooksGridAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookGridViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book_grid, parent, false)
        return BookGridViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookGridViewHolder, position: Int) {
        holder.bind(books[position])
    }

    override fun getItemCount(): Int = books.size

    /**
     * 🎯 ViewHolder para items de grid
     */
    inner class BookGridViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        
        // UI Components
        private val cardBookGrid: MaterialCardView = itemView.findViewById(R.id.card_book_grid)
        private val imgBookCoverGrid: ImageView = itemView.findViewById(R.id.img_book_cover_grid)
        private val cardAvailabilityBadgeGrid: MaterialCardView = itemView.findViewById(R.id.card_availability_badge_grid)
        private val textAvailabilityGrid: TextView = itemView.findViewById(R.id.text_availability_grid)
        
        private val textBookTitleGrid: TextView = itemView.findViewById(R.id.text_book_title_grid)
        private val textBookAuthorGrid: TextView = itemView.findViewById(R.id.text_book_author_grid)

        fun bind(book: Book) {
            // 📖 Información básica
            textBookTitleGrid.text = book.title
            textBookAuthorGrid.text = book.author
            
            // 📚 Cargar imagen
            loadBookCover(book)
            
            // 🎯 Estado de disponibilidad
            updateAvailabilityStatus(book)
            
            // 👆 Click listener
            cardBookGrid.setOnClickListener {
                onBookClick(book)
            }
        }

        /**
         * 📚 Cargar imagen del libro
         */
        private fun loadBookCover(book: Book) {
            if (book.imageUrl.isNullOrEmpty()) {
                imgBookCoverGrid.setImageResource(R.drawable.book_placeholder)
            } else {
                Glide.with(itemView.context)
                    .load(book.imageUrl)
                    .placeholder(R.drawable.book_placeholder)
                    .error(R.drawable.book_placeholder)
                    .centerCrop()
                    .into(imgBookCoverGrid)
            }
        }

        /**
         * 🎯 Actualizar estado de disponibilidad
         */
        private fun updateAvailabilityStatus(book: Book) {
            val availableCount = calculateAvailableBooks(book)
            val isAvailable = availableCount > 0
            
            if (isAvailable) {
                textAvailabilityGrid.text = "Disponible"
                cardAvailabilityBadgeGrid.setCardBackgroundColor(
                    itemView.context.getColor(R.color.available_bg)
                )
                textAvailabilityGrid.setTextColor(
                    itemView.context.getColor(R.color.available_text)
                )
            } else {
                textAvailabilityGrid.text = "No disponible"
                cardAvailabilityBadgeGrid.setCardBackgroundColor(
                    itemView.context.getColor(R.color.not_available_bg)
                )
                textAvailabilityGrid.setTextColor(
                    itemView.context.getColor(R.color.not_available_text)
                )
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
    }
}
