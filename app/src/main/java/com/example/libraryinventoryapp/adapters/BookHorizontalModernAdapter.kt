package com.example.libraryinventoryapp.adapters

import android.graphics.Color
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
 * 📖 Adapter para lista horizontal de libros - Diseño Premium
 * 
 * Características:
 * - Cards horizontales con imagen grande
 * - Badge de favorito
 * - Badge de disponibilidad dinámico
 * - Información completa del libro
 * - Click listener para navegación
 */
class BookHorizontalModernAdapter(
    private val books: MutableList<Book>,
    private val onBookClick: (Book) -> Unit
) : RecyclerView.Adapter<BookHorizontalModernAdapter.BookHorizontalViewHolder>() {

    companion object {
        private const val TAG = "BookHorizontalAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookHorizontalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book_horizontal_modern, parent, false)
        return BookHorizontalViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookHorizontalViewHolder, position: Int) {
        holder.bind(books[position])
    }

    override fun getItemCount(): Int = books.size

    /**
     * 🔄 Actualizar lista de libros
     */
    fun updateBooks(newBooks: List<Book>) {
        books.clear()
        books.addAll(newBooks)
        notifyDataSetChanged()
    }

    /**
     * 🎯 ViewHolder para items de libro horizontal
     */
    inner class BookHorizontalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        
        // UI Components
        private val cardBookHorizontal: MaterialCardView = itemView.findViewById(R.id.card_book_horizontal)
        private val imgBookCover: ImageView = itemView.findViewById(R.id.img_book_cover)
        private val cardFavoriteBadge: MaterialCardView = itemView.findViewById(R.id.card_favorite_badge)
        private val imgFavoriteIcon: ImageView = itemView.findViewById(R.id.img_favorite_icon)
        private val cardAvailabilityBadge: MaterialCardView = itemView.findViewById(R.id.card_availability_badge)
        private val textAvailability: TextView = itemView.findViewById(R.id.text_availability)
        
        private val textBookTitle: TextView = itemView.findViewById(R.id.text_book_title)
        private val textBookAuthor: TextView = itemView.findViewById(R.id.text_book_author)
        private val textAvailableCopies: TextView = itemView.findViewById(R.id.text_available_copies)
        private val imgPopularityStar: ImageView = itemView.findViewById(R.id.img_popularity_star)

        fun bind(book: Book) {
            // 📖 Información básica del libro
            textBookTitle.text = book.title
            textBookAuthor.text = book.author
            
            
            // 📚 Cargar imagen con Glide
            loadBookImage(book)
            
            // 🎯 Estado de disponibilidad
            updateAvailabilityStatus(book)
            
            // 📊 Información de copias disponibles
            updateCopiesInfo(book)
            
            // ⭐ Badge de popularidad
            updatePopularityBadge(book)
            
            // TODO: Badge de favorito (requiere sistema de wishlist)
            updateFavoriteBadge(book)
            
            // 👆 Click listener
            cardBookHorizontal.setOnClickListener {
                onBookClick(book)
            }
        }

        /**
         * 📚 Cargar imagen del libro con Glide
         */
        private fun loadBookImage(book: Book) {
            if (book.imageUrl.isNullOrEmpty()) {
                imgBookCover.setImageResource(R.drawable.book_placeholder)
            } else {
                Glide.with(itemView.context)
                    .load(book.imageUrl)
                    .placeholder(R.drawable.book_placeholder)
                    .error(R.drawable.book_placeholder)
                    .centerCrop()
                    .into(imgBookCover)
            }
        }

        /**
         * 🎯 Actualizar estado de disponibilidad
         */
        private fun updateAvailabilityStatus(book: Book) {
            val availableCount = calculateAvailableBooks(book)
            val isAvailable = availableCount > 0
            
            if (isAvailable) {
                textAvailability.text = "Disponible"
                cardAvailabilityBadge.setCardBackgroundColor(
                    itemView.context.getColor(R.color.available_bg)
                )
                textAvailability.setTextColor(
                    itemView.context.getColor(R.color.available_text)
                )
            } else {
                textAvailability.text = "No disponible"
                cardAvailabilityBadge.setCardBackgroundColor(
                    itemView.context.getColor(R.color.not_available_bg)
                )
                textAvailability.setTextColor(
                    itemView.context.getColor(R.color.not_available_text)
                )
            }
        }

        /**
         * 📊 Actualizar información de copias
         */
        private fun updateCopiesInfo(book: Book) {
            val availableCount = calculateAvailableBooks(book)
            val totalCount = book.quantity
            
            textAvailableCopies.text = "$availableCount de $totalCount libros"
        }

        /**
         * ⭐ Actualizar badge de popularidad
         */
        private fun updatePopularityBadge(book: Book) {
            val assignedCount = book.assignedTo?.size ?: 0
            
            // Mostrar estrella si tiene más de 2 asignaciones
            if (assignedCount >= 2) {
                imgPopularityStar.visibility = View.VISIBLE
            } else {
                imgPopularityStar.visibility = View.GONE
            }
        }

        /**
         * 💖 Actualizar badge de favorito
         */
        private fun updateFavoriteBadge(book: Book) {
            // TODO: Implementar lógica de favoritos
            // Por ahora oculto hasta implementar el sistema de wishlist
            cardFavoriteBadge.visibility = View.GONE
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
