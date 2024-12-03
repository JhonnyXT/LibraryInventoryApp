package com.example.libraryinventoryapp.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.libraryinventoryapp.R
import com.example.libraryinventoryapp.models.Book
import com.example.libraryinventoryapp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class BookAdapter(
    private var books: List<Book>,
    private var userNames: List<String>,
    private var userList: List<User>
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage

    inner class BookViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val bookTitle: TextView = view.findViewById(R.id.book_title)
        val bookDescription: TextView = view.findViewById(R.id.book_description)
        val bookAuthor: TextView = view.findViewById(R.id.book_author)
        val bookCategory: TextView = view.findViewById(R.id.book_category)
        val bookIsbn: TextView = view.findViewById(R.id.book_isbn)
        val bookStatus: TextView = view.findViewById(R.id.book_status)
        val bookAssignedTo: TextView = view.findViewById(R.id.book_assigned_to)
        val bookImage: ImageView = view.findViewById(R.id.book_image)
        val bookUserSearch: AutoCompleteTextView = view.findViewById(R.id.autoCompleteTextView)
        val bookQuantity: TextView = view.findViewById(R.id.book_quantity)
        val assignUserButton: TextView = view.findViewById(R.id.assignButton)
        val deleteBookButton: Button = view.findViewById(R.id.deleteBookButton)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBarAssignBook)

        init {
            firestore = FirebaseFirestore.getInstance()
            auth = FirebaseAuth.getInstance()
            storage = FirebaseStorage.getInstance()

            assignUserButton.setOnClickListener {
                val book = books[adapterPosition]
                val userName = bookUserSearch.text.toString()
                assignUserToBook(book, userName, this, itemView.context)
            }

            deleteBookButton.setOnClickListener {
                val book = books[adapterPosition]
                deleteBook(book, this, itemView.context)
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
        holder.bookDescription.text = "Descripción: ${book.description}"
        holder.bookAuthor.text = "Autor: ${book.author}"
        // Verifica si hay categorías y únelas con comas
        holder.bookCategory.text = if (book.categories.isNotEmpty()) {
            "Categoría: ${book.categories.joinToString(", ")}"
        } else {
            "Categoría: Ninguna"
        }
        holder.bookIsbn.text = "ISBN: ${book.isbn}"
        holder.bookStatus.text = "Estado: ${book.status}"
        holder.bookAssignedTo.text = if (!book.assignedWithNames.isNullOrEmpty()) {
            "Asignado a: ${book.assignedWithNames.joinToString(", ")}"
        } else {
            "Asignado a: Nadie"
        }
        holder.bookQuantity.text = "Cantidad: ${book.quantity}"

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

    private fun deleteBook(book: Book, holder: BookViewHolder, context: Context) {
        showProgressBar(holder, true)
        Toast.makeText(context, "Eliminando libro...", Toast.LENGTH_SHORT).show()

        // Eliminar el libro de Firestore
        val bookRef = firestore.collection("books").document(book.id)
        bookRef.delete()
            .addOnSuccessListener {
                // Eliminar la imagen de Firebase Storage si existe
                book.imageUrl?.let { imageUrl ->
                    val storageRef = storage.getReferenceFromUrl(imageUrl)
                    storageRef.delete()
                        .addOnSuccessListener {
                            showProgressBar(holder, false)
                            Toast.makeText(context, "Libro e imagen eliminados correctamente", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            showProgressBar(holder, false)
                            Toast.makeText(context, "Error al eliminar la imagen: $e", Toast.LENGTH_LONG).show()
                        }
                }

                // Actualizar la lista de libros en el adaptador después de eliminar
                updateBookList(context)
            }
            .addOnFailureListener { e ->
                showProgressBar(holder, false)
                Toast.makeText(context, "Error al eliminar el libro: $e", Toast.LENGTH_LONG).show()
            }
    }

    private fun assignUserToBook(
        book: Book,
        userName: String,
        holder: BookViewHolder,
        context: Context
    ) {
        showProgressBar(holder, true)

        // Validar que el nombre del usuario no esté en blanco
        if (userName.isBlank()) {
            showProgressBar(holder, false)
            Toast.makeText(context, "Por favor, selecciona un usuario para asignar el libro.", Toast.LENGTH_SHORT).show()
            return
        }

        // Buscar el usuario en la lista por su nombre
        val user = userList.find { it.name.equals(userName, ignoreCase = true) }
        if (user == null) {
            showProgressBar(holder, false)
            Toast.makeText(context, "Usuario no encontrado. Intenta de nuevo.", Toast.LENGTH_SHORT).show()
            return
        }

        // Verificar si el usuario ya tiene asignado este libro
        if (book.assignedTo?.contains(user.uid) == true) {
            showProgressBar(holder, false)
            holder.bookUserSearch.setText("")
            Toast.makeText(context, "El usuario ya tiene asignado este libro.", Toast.LENGTH_SHORT).show()
            return
        }

        // Verificar si hay cantidad suficiente para asignar
        if (book.quantity <= 0) {
            showProgressBar(holder, false)
            Toast.makeText(context, "No hay más copias disponibles de este libro.", Toast.LENGTH_SHORT).show()
            return
        }

        // Preparar los nuevos valores para actualizar
        val updatedQuantity = book.quantity - 1
        val updatedStatus = if (updatedQuantity == 0) "No disponible" else "Disponible"
        val updatedAssignedTo = (book.assignedTo ?: mutableListOf()).toMutableList()
        val updatedAssignedWithNames = (book.assignedWithNames ?: mutableListOf()).toMutableList()
        val updatedAssignedToEmails = (book.assignedToEmails ?: mutableListOf()).toMutableList()

        // Agregar el usuario al arreglo
        updatedAssignedTo.add(user.uid)
        updatedAssignedWithNames.add(user.name)
        updatedAssignedToEmails.add(user.email)

        // Referencia del libro en Firestore
        val bookRef = firestore.collection("books").document(book.id)

        // Realizar la actualización en Firestore
        val updates = mapOf(
            "quantity" to updatedQuantity,
            "status" to updatedStatus,
            "assignedTo" to updatedAssignedTo,
            "assignedWithNames" to updatedAssignedWithNames,
            "assignedToEmails" to updatedAssignedToEmails
        )

        bookRef.update(updates)
            .addOnSuccessListener {
                showProgressBar(holder, false)
                holder.bookUserSearch.setText("")
                Toast.makeText(
                    context,
                    "El libro '${book.title}' ha sido asignado a ${user.name}.",
                    Toast.LENGTH_SHORT
                ).show()

                // Actualizar la lista local de libros y notificar el adaptador
                val updatedBook = book.copy(
                    quantity = updatedQuantity,
                    status = updatedStatus,
                    assignedTo = updatedAssignedTo,
                    assignedWithNames = updatedAssignedWithNames,
                    assignedToEmails = updatedAssignedToEmails
                )
                books = books.map { if (it.id == book.id) updatedBook else it }
                notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                showProgressBar(holder, false)
                Toast.makeText(context, "Error al asignar el libro: $e", Toast.LENGTH_LONG).show()
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

    // Método auxiliar para mostrar u ocultar el ProgressBar
    private fun showProgressBar(holder: BookViewHolder, show: Boolean) {
        holder.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        holder.assignUserButton.isEnabled = !show
        holder.bookUserSearch.isEnabled = !show
    }
}