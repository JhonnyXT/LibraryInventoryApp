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
        private val authorTextView: TextView = itemView.findViewById(R.id.book_author)
        private val isbnTextView: TextView = itemView.findViewById(R.id.book_isbn)
        private val bookImageView: ImageView = itemView.findViewById(R.id.book_image)

        fun bind(book: Book) {
            titleTextView.text = book.title
            authorTextView.text = "Autor: ${book.author}"
            isbnTextView.text = "ISBN: ${book.isbn}"

            // Load book image using Glide
            Glide.with(itemView.context)
                .load(book.imageUrl)
                .placeholder(R.drawable.icon_load)
                .error(R.drawable.icon_error)
                .into(bookImageView)
        }
    }
}