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

class BookAdapter(private var books: List<Book>) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    inner class BookViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val bookTitle: TextView = view.findViewById(R.id.book_title)
        val bookAuthor: TextView = view.findViewById(R.id.book_author)
        val bookIsbn: TextView = view.findViewById(R.id.book_isbn)
        val bookStatus: TextView = view.findViewById(R.id.book_status)
        val bookAssignedTo: TextView = view.findViewById(R.id.book_assigned_to)
        val bookImage: ImageView = view.findViewById(R.id.book_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = books[position]
        holder.bookTitle.text = book.title
        holder.bookAuthor.text = "Autor: ${book.author}"
        holder.bookIsbn.text = "ISBN: ${book.isbn}"
        holder.bookStatus.text = "Estado: ${book.status}"
        holder.bookAssignedTo.text = "Asignado a: ${book.assignedWithName ?: "Nadie"}"

        // Cargar la imagen usando Glide
        if (book.imageUrl != null) {
            Glide.with(holder.itemView.context)
                .load(book.imageUrl)
                .placeholder(R.drawable.icon_load) // Reemplaza con tu imagen de marcador de posición
                .error(R.drawable.icon_error) // Reemplaza con tu imagen de error
                .into(holder.bookImage)
        } else {
            holder.bookImage.setImageResource(R.drawable.icon_load) // Reemplaza con tu imagen de marcador de posición
        }
    }

    override fun getItemCount(): Int = books.size

    // Método para actualizar los libros en el adaptador
    fun updateBooks(newBooks: List<Book>) {
        books = newBooks
        notifyDataSetChanged()
    }
}