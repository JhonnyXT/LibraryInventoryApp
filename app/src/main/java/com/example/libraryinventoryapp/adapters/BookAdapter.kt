package com.example.libraryinventoryapp.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.libraryinventoryapp.R
import com.example.libraryinventoryapp.models.Book
import com.example.libraryinventoryapp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BookAdapter(
    private var books: List<Book>,
    private var userNames: List<String>,
    private var userList: List<User>
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    inner class BookViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val bookTitle: TextView = view.findViewById(R.id.book_title)
        val bookAuthor: TextView = view.findViewById(R.id.book_author)
        val bookCategory: TextView = view.findViewById(R.id.book_category)
        val bookIsbn: TextView = view.findViewById(R.id.book_isbn)
        val bookStatus: TextView = view.findViewById(R.id.book_status)
        val bookAssignedTo: TextView = view.findViewById(R.id.book_assigned_to)
        val bookImage: ImageView = view.findViewById(R.id.book_image)
        val bookUserSearch: AutoCompleteTextView = view.findViewById(R.id.autoCompleteTextView)
        val assignUserButton: TextView = view.findViewById(R.id.assignButton)

        init {
            firestore = FirebaseFirestore.getInstance()
            auth = FirebaseAuth.getInstance()
            assignUserButton.setOnClickListener {
                val book = books[adapterPosition]
                val userName = bookUserSearch.text.toString()
                assignUserToBook(book, userName, this, itemView.context)
            }
        }

        fun setupUserSearch(userNames: List<String>) {
            val adapter = ArrayAdapter(itemView.context, android.R.layout.simple_dropdown_item_1line, userNames)
            bookUserSearch.setAdapter(adapter)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = books[position]
        holder.bookTitle.text = book.title
        holder.bookAuthor.text = "Autor: ${book.author}"
        // Verifica si hay categorías y únelas con comas
        holder.bookCategory.text = if (book.categories.isNotEmpty()) {
            "Categoría: ${book.categories.joinToString(", ")}"
        } else {
            "Categoría: Ninguna"
        }
        holder.bookIsbn.text = "ISBN: ${book.isbn}"
        holder.bookStatus.text = "Estado: ${book.status}"
        holder.bookAssignedTo.text = "Asignado a: ${book.assignedWithName ?: "Nadie"}"

        // Mostrar el botón de asignar usuario si el libro está disponible
        if (book.status == "Disponible") {
            holder.assignUserButton.visibility = View.VISIBLE
            holder.bookUserSearch.visibility = View.VISIBLE
        } else {
            holder.assignUserButton.visibility = View.GONE
            holder.bookUserSearch.visibility = View.GONE
        }

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

        // Configurar la lista de nombres de usuario
        holder.setupUserSearch(userNames)
    }

    override fun getItemCount(): Int = books.size

    fun updateBooks(newBooks: List<Book>) {
        books = newBooks
        notifyDataSetChanged()
    }

    fun updateUsers(newUsers: List<User>) {
        this.userList = newUsers
        notifyDataSetChanged()
    }

    fun updateUserNames(userNames: List<String>) {
        this.userNames = userNames
        notifyDataSetChanged()
    }

    private fun assignUserToBook(
        book: Book,
        userName: String,
        holder: BookViewHolder,
        context: Context
    ) {
        if (userName.isNotBlank()) {
            val user = userList.find { it.name.equals(userName, ignoreCase = true) }
            if (user != null) {
                val bookRef = firestore.collection("books").document(book.id)
                bookRef.update("assignedTo", user.uid,
                    "assignedToEmail", user.email,
                    "assignedWithName", user.name,
                    "status", "Asignado")
                    .addOnSuccessListener {
                        Toast.makeText(context, "Libro asignado a ${user.name}", Toast.LENGTH_SHORT).show()
                        holder.bookUserSearch.setText("")
                        updateBookList(context)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error al asignar libro: $e", Toast.LENGTH_LONG).show()
                    }
            } else {
                Toast.makeText(context, "Usuario no encontrado.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Por favor, ingrese un nombre de usuario.", Toast.LENGTH_SHORT).show()
        }
    }

    // Nueva función para recuperar libros de Firestore
    private fun updateBookList(context: Context) {
        firestore.collection("books").get()
            .addOnSuccessListener { documents ->
                val updatedBooks = documents.map { document ->
                    document.toObject(Book::class.java)
                }
                // Actualiza la lista en el adaptador
                updateBooks(updatedBooks)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error al recuperar la lista de libros: $e", Toast.LENGTH_LONG).show()
            }
    }
}