package com.example.libraryinventoryapp.adapters

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
        private val authorTextView: TextView = itemView.findViewById(R.id.book_author)
        private val statusTextView: TextView = itemView.findViewById(R.id.book_status)
        private val bookImageView: ImageView = itemView.findViewById(R.id.book_image)
        private val assignButton: Button = itemView.findViewById(R.id.assign_button)

        fun bind(book: Book) {
            titleTextView.text = book.title
            authorTextView.text = "Autor: ${book.author}"
            statusTextView.text = "Estado: ${book.status}"

            // Load book image with Glide
            Glide.with(itemView.context)
                .load(book.imageUrl)
                .placeholder(R.drawable.icon_load)
                .error(R.drawable.icon_error)
                .into(bookImageView)

            assignButton.setOnClickListener {
                onAssignClick(book)
            }
        }
    }
}