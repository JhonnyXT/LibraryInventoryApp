package com.example.libraryinventoryapp.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.libraryinventoryapp.R
import com.example.libraryinventoryapp.models.WishlistItem
import com.google.android.material.card.MaterialCardView

/**
 * ⭐ Adapter para lista de wishlist/favoritos - Diseño limpio y profesional
 * 
 * Muestra los libros guardados como favoritos por el usuario.
 * Click en la tarjeta completa navega al detail del libro.
 */
class WishlistModernAdapter(
    private val wishlistItems: MutableList<WishlistItem>,
    private val onAction: (action: String, wishlistItem: WishlistItem) -> Unit
) : RecyclerView.Adapter<WishlistModernAdapter.WishlistViewHolder>() {

    companion object {
        private const val TAG = "WishlistModernAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WishlistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wishlist_modern, parent, false)
        return WishlistViewHolder(view)
    }

    override fun onBindViewHolder(holder: WishlistViewHolder, position: Int) {
        holder.bind(wishlistItems[position])
    }

    override fun getItemCount(): Int = wishlistItems.size

    /**
     * 🗑️ Remover item de la lista
     */
    fun removeItem(wishlistItem: WishlistItem) {
        val position = wishlistItems.indexOfFirst { it.id == wishlistItem.id }
        if (position != -1) {
            wishlistItems.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    /**
     * 🎯 ViewHolder para items de wishlist
     */
    inner class WishlistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        
        // UI Components
        private val cardWishlistItem: MaterialCardView = itemView.findViewById(R.id.card_wishlist_item)
        private val imgWishlistBookCover: ImageView = itemView.findViewById(R.id.img_wishlist_book_cover)
        private val textWishlistBookTitle: TextView = itemView.findViewById(R.id.text_wishlist_book_title)
        private val textWishlistBookAuthor: TextView = itemView.findViewById(R.id.text_wishlist_book_author)
        private val textWishlistCategory: TextView = itemView.findViewById(R.id.text_wishlist_category)
        private val textWishlistAddedDate: TextView = itemView.findViewById(R.id.text_wishlist_added_date)
        
        private val btnRemoveFavorite: ImageButton = itemView.findViewById(R.id.btn_remove_favorite)
        private val cardAvailabilityBadgeWishlist: MaterialCardView = itemView.findViewById(R.id.card_availability_badge_wishlist)
        private val textAvailabilityWishlist: TextView = itemView.findViewById(R.id.text_availability_wishlist)

        fun bind(wishlistItem: WishlistItem) {
            // 📖 Información básica
            textWishlistBookTitle.text = wishlistItem.bookTitle
            textWishlistBookAuthor.text = wishlistItem.bookAuthor
            
            // 🏷️ Categoría principal
            if (wishlistItem.bookCategories.isNotEmpty()) {
                textWishlistCategory.text = wishlistItem.getPrimaryCategory()
                textWishlistCategory.visibility = View.VISIBLE
            } else {
                textWishlistCategory.visibility = View.GONE
            }
            
            // 📅 Fecha añadido
            textWishlistAddedDate.text = "Añadido el ${wishlistItem.getFormattedAddedDate()}"
            
            // 📚 Cargar imagen
            loadBookCover(wishlistItem)
            
            // 🎯 Estado de disponibilidad
            updateAvailabilityStatus(wishlistItem)
            
            // 👆 Click listeners
            setupClickListeners(wishlistItem)
        }

        /**
         * 📚 Cargar imagen del libro
         */
        private fun loadBookCover(wishlistItem: WishlistItem) {
            if (wishlistItem.bookImageUrl.isNullOrEmpty()) {
                imgWishlistBookCover.setImageResource(R.drawable.book_placeholder)
            } else {
                Glide.with(itemView.context)
                    .load(wishlistItem.bookImageUrl)
                    .placeholder(R.drawable.book_placeholder)
                    .error(R.drawable.book_placeholder)
                    .centerCrop()
                    .into(imgWishlistBookCover)
            }
        }

        /**
         * 🎯 Actualizar estado de disponibilidad
         */
        private fun updateAvailabilityStatus(wishlistItem: WishlistItem) {
            if (wishlistItem.isAvailable) {
                textAvailabilityWishlist.text = "Disponible"
                cardAvailabilityBadgeWishlist.setCardBackgroundColor(
                    itemView.context.getColor(R.color.available_bg)
                )
                textAvailabilityWishlist.setTextColor(
                    itemView.context.getColor(R.color.available_text)
                )
            } else {
                textAvailabilityWishlist.text = "No disponible"
                cardAvailabilityBadgeWishlist.setCardBackgroundColor(
                    itemView.context.getColor(R.color.not_available_bg)
                )
                textAvailabilityWishlist.setTextColor(
                    itemView.context.getColor(R.color.not_available_text)
                )
            }
        }

        /**
         * 👆 Configurar click listeners
         */
        private fun setupClickListeners(wishlistItem: WishlistItem) {
            // Click en toda la card - navegar al detail del libro
            cardWishlistItem.setOnClickListener {
                onAction("view_details", wishlistItem)
            }
            
            // Remover de favoritos
            btnRemoveFavorite.setOnClickListener {
                onAction("remove_favorite", wishlistItem)
            }
        }
    }
}
