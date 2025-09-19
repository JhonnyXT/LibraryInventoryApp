package com.example.libraryinventoryapp.adapters

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import com.google.android.material.button.MaterialButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputLayout
import com.bumptech.glide.Glide
import com.example.libraryinventoryapp.R
import com.example.libraryinventoryapp.models.Book
import com.example.libraryinventoryapp.models.User
import com.example.libraryinventoryapp.utils.EmailService
import com.example.libraryinventoryapp.utils.LibraryNotificationManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BookAdapter(
    private var books: List<Book>,
    private var userNames: List<String>,
    private var userList: List<User>,
    private val onBookClick: ((Book) -> Unit)? = null
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private val emailService = EmailService()

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
        val editBookButton: MaterialButton = view.findViewById(R.id.editBookButton)
        val deleteBookButton: MaterialButton = view.findViewById(R.id.deleteBookButton)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBarAssignBook)
        val expirationAlert: TextView = view.findViewById(R.id.expiration_alert)

        init {
            firestore = FirebaseFirestore.getInstance()
            auth = FirebaseAuth.getInstance()
            storage = FirebaseStorage.getInstance()

            // Click en toda la tarjeta para navegar al detalle
            itemView.setOnClickListener {
                val book = books[adapterPosition]
                onBookClick?.invoke(book)
            }

            assignUserButton.setOnClickListener {
                val book = books[adapterPosition]
                val userName = bookUserSearch.text.toString()
                showAssignConfirmationDialog(book, userName, this, itemView.context)
            }

            editBookButton.setOnClickListener {
                val book = books[adapterPosition]
                editBook(book, itemView.context)
            }

            deleteBookButton.setOnClickListener {
                val book = books[adapterPosition]
                showDeleteConfirmationDialog(book, this, itemView.context)
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
        // Calcular disponibilidad real para las tarjetas
        val totalCopies = book.quantity
        val assignedCopies = book.assignedTo?.size ?: 0
        val availableCopies = totalCopies - assignedCopies
        
        holder.bookStatus.text = when {
            totalCopies == 0 -> "Estado: ❌ Sin libros"
            availableCopies <= 0 -> "Estado: ❌ Todos asignados"
            else -> "Estado: ✅ $availableCopies disponibles"
        }
        
        // Crear vista dinámica para usuarios asignados con botones de desasignar
        setupAssignedUsersView(holder, book)
        
        // Verificar si hay libros vencidos y mostrar alerta
        checkAndShowExpirationAlert(holder, book)
        
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
                .placeholder(R.drawable.ic_book_default)
                .error(R.drawable.ic_book_default)
                .into(holder.bookImage)
        } else {
            holder.bookImage.setImageResource(R.drawable.ic_book_default)
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

    private fun setupAssignedUsersView(holder: BookViewHolder, book: Book) {
        if (book.assignedWithNames.isNullOrEmpty()) {
            holder.bookAssignedTo.text = "Asignado a: Nadie"
            holder.bookAssignedTo.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
            holder.bookAssignedTo.isClickable = false
            return
        }

        // Crear texto con puntos suspensivos + contador para mejor UX
        val assignedNames = book.assignedWithNames
        val assignedText = when {
            assignedNames.size == 1 -> "Asignado a: ${assignedNames[0]}"
            assignedNames.size <= 2 -> "Asignado a: ${assignedNames.joinToString(", ")}"
            else -> {
                val first = assignedNames.take(2).joinToString(", ")
                val remaining = assignedNames.size - 2
                "Asignado a: $first... (+$remaining más)"
            }
        }
        
        holder.bookAssignedTo.text = assignedText
        
        // Hacer clickeable para mostrar opciones de desasignar
        holder.bookAssignedTo.setOnClickListener {
            showUnassignDialog(holder.itemView.context, book, holder)
        }
        
        // Cambiar apariencia para indicar que es clickeable
        holder.bookAssignedTo.setTextColor(holder.itemView.context.getColor(android.R.color.holo_blue_dark))
        holder.bookAssignedTo.isClickable = true
    }

    private fun showUnassignDialog(context: Context, book: Book, holder: BookViewHolder) {
        if (book.assignedWithNames.isNullOrEmpty()) return

        val assignedUsers = book.assignedWithNames.toTypedArray()
        
        AlertDialog.Builder(context)
            .setTitle("Desasignar libro")
            .setMessage("Selecciona el usuario al que quieres quitar la asignación:")
            .setItems(assignedUsers) { dialog, which ->
                val userToUnassign = assignedUsers[which]
                val userId = book.assignedTo?.get(which)
                val userEmail = book.assignedToEmails?.get(which)
                
                if (userId != null) {
                    unassignBookFromUser(context, book, userId, userToUnassign, userEmail, holder)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun unassignBookFromUser(
        context: Context, 
        book: Book, 
        userId: String, 
        userName: String, 
        userEmail: String?,
        holder: BookViewHolder
    ) {
        showProgressBar(holder, true)

        // Preparar listas actualizadas eliminando al usuario
        val updatedAssignedTo = book.assignedTo?.toMutableList() ?: mutableListOf()
        val updatedAssignedWithNames = book.assignedWithNames?.toMutableList() ?: mutableListOf()
        val updatedAssignedToEmails = book.assignedToEmails?.toMutableList() ?: mutableListOf()
        val updatedAssignedDates = book.assignedDates?.toMutableList() ?: mutableListOf()

        val userIndex = updatedAssignedTo.indexOf(userId)
        if (userIndex != -1) {
            updatedAssignedTo.removeAt(userIndex)
            if (userIndex < updatedAssignedWithNames.size) {
                updatedAssignedWithNames.removeAt(userIndex)
            }
            if (userIndex < updatedAssignedToEmails.size) {
                updatedAssignedToEmails.removeAt(userIndex)
            }
            if (userIndex < updatedAssignedDates.size) {
                updatedAssignedDates.removeAt(userIndex)
            }
        }

        val updatedQuantity = book.quantity + 1
        val updatedStatus = "Disponible"

        val updates = mapOf(
            "quantity" to updatedQuantity,
            "status" to updatedStatus,
            "assignedTo" to updatedAssignedTo,
            "assignedWithNames" to updatedAssignedWithNames,
            "assignedToEmails" to updatedAssignedToEmails,
            "assignedDates" to updatedAssignedDates
        )

        firestore.collection("books").document(book.id).update(updates)
            .addOnSuccessListener {
                showProgressBar(holder, false)
                Toast.makeText(
                    context,
                    "Libro '${book.title}' desasignado de $userName correctamente.",
                    Toast.LENGTH_SHORT
                ).show()

                // Actualizar la lista local
                val updatedBook = book.copy(
                    quantity = updatedQuantity,
                    status = updatedStatus,
                    assignedTo = updatedAssignedTo,
                    assignedWithNames = updatedAssignedWithNames,
                    assignedToEmails = updatedAssignedToEmails,
                    assignedDates = updatedAssignedDates
                )
                books = books.map { if (it.id == book.id) updatedBook else it }
                notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                showProgressBar(holder, false)
                Toast.makeText(context, "Error al desasignar el libro: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showAssignConfirmationDialog(
        book: Book, 
        userName: String, 
        holder: BookViewHolder, 
        context: Context
    ) {
        if (userName.isBlank()) {
            Toast.makeText(context, "Por favor, selecciona un usuario para asignar el libro.", Toast.LENGTH_SHORT).show()
            return
        }

        // Primer diálogo para confirmar asignación
        AlertDialog.Builder(context)
            .setTitle("Confirmar Asignación")
            .setMessage("¿Estás seguro de que quieres asignar el libro '${book.title}' a $userName?")
            .setPositiveButton("ACEPTAR") { _, _ ->
                // Segundo paso: seleccionar fecha de devolución
                showLoanExpirationDatePicker(book, userName, holder, context)
            }
            .setNegativeButton("CANCELAR", null)
            .show()
    }

    private fun showLoanExpirationDatePicker(
        book: Book,
        userName: String,
        holder: BookViewHolder,
        context: Context
    ) {
        val calendar = Calendar.getInstance()
        // Fecha mínima: mañana
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val minDate = calendar.timeInMillis
        
        // Fecha sugerida: 15 días a partir de hoy
        calendar.add(Calendar.DAY_OF_MONTH, 14) // +1 ya agregado arriba = 15 días total
        val suggestedYear = calendar.get(Calendar.YEAR)
        val suggestedMonth = calendar.get(Calendar.MONTH)
        val suggestedDay = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(context, { _, year, month, dayOfMonth ->
            val expirationDate = Calendar.getInstance()
            expirationDate.set(year, month, dayOfMonth, 23, 59, 59) // Hasta el final del día
            
            // Convertir a Timestamp de Firebase
            val expirationTimestamp = Timestamp(expirationDate.time)
            
            // Proceder con la asignación incluyendo fecha de vencimiento
            assignUserToBookWithExpiration(book, userName, expirationTimestamp, holder, context)
            
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            Toast.makeText(
                context, 
                "Libro asignado hasta: ${dateFormat.format(expirationDate.time)}", 
                Toast.LENGTH_SHORT
            ).show()
            
        }, suggestedYear, suggestedMonth, suggestedDay)

        // Establecer fecha mínima
        datePickerDialog.datePicker.minDate = minDate
        
        datePickerDialog.setTitle("Fecha de Devolución")
        datePickerDialog.setMessage("Selecciona hasta cuándo estará prestado el libro:")
        datePickerDialog.show()
    }



    private fun showDeleteConfirmationDialog(book: Book, holder: BookViewHolder, context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que quieres eliminar permanentemente el libro '${book.title}'?\n\nEsta acción no se puede deshacer.")
            .setPositiveButton("ACEPTAR") { _, _ ->
                deleteBook(book, holder, context)
            }
            .setNegativeButton("CANCELAR", null)
            .show()
    }

    private fun editBook(book: Book, context: Context) {
        onBookClick?.invoke(book)
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

    private fun assignUserToBookWithExpiration(
        book: Book,
        userName: String,
        expirationDate: Timestamp,
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
        val updatedAssignedDates = (book.assignedDates ?: mutableListOf()).toMutableList()
        val updatedLoanExpirationDates = (book.loanExpirationDates ?: mutableListOf()).toMutableList()

        // Agregar el usuario y la fecha de vencimiento a los arreglos
        updatedAssignedTo.add(user.uid)
        updatedAssignedWithNames.add(user.name)
        updatedAssignedToEmails.add(user.email)
        updatedAssignedDates.add(Timestamp.now())
        updatedLoanExpirationDates.add(expirationDate)

        // Referencia del libro en Firestore
        val bookRef = firestore.collection("books").document(book.id)

        // Realizar la actualización en Firestore
        val updates = mapOf(
            "quantity" to updatedQuantity,
            "status" to updatedStatus,
            "assignedTo" to updatedAssignedTo,
            "assignedWithNames" to updatedAssignedWithNames,
            "assignedToEmails" to updatedAssignedToEmails,
            "assignedDates" to updatedAssignedDates,
            "loanExpirationDates" to updatedLoanExpirationDates
        )

        bookRef.update(updates)
            .addOnSuccessListener {
                showProgressBar(holder, false)
                holder.bookUserSearch.setText("")
                
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                Toast.makeText(
                    context,
                    "El libro '${book.title}' ha sido asignado a ${user.name} hasta ${dateFormat.format(expirationDate.toDate())}.",
                    Toast.LENGTH_LONG
                ).show()

                // Actualizar la lista local de libros y notificar el adaptador
                val updatedBook = book.copy(
                    quantity = updatedQuantity,
                    status = updatedStatus,
                    assignedTo = updatedAssignedTo,
                    assignedWithNames = updatedAssignedWithNames,
                    assignedToEmails = updatedAssignedToEmails,
                    assignedDates = updatedAssignedDates,
                    loanExpirationDates = updatedLoanExpirationDates
                )
                books = books.map { if (it.id == book.id) updatedBook else it }
                notifyDataSetChanged()

                // Enviar correos de notificación
                sendAssignmentNotificationEmails(book, user, context)
                
                // 🔔 NUEVA FUNCIONALIDAD: Programar notificaciones push
                try {
                    val notificationManager = LibraryNotificationManager(context)
                    
                    // 📚 Notificación inmediata de asignación
                    notificationManager.scheduleBookAssignmentNotification(
                        bookId = book.id,
                        bookTitle = book.title,
                        bookAuthor = book.author,
                        userId = user.uid,
                        userName = user.name,
                        expirationDate = expirationDate
                    )
                    
                    // 📅 Notificaciones programadas según vencimiento
                    notificationManager.scheduleBookLoanNotifications(
                        bookId = book.id,
                        bookTitle = book.title,
                        bookAuthor = book.author,
                        userId = user.uid,
                        userName = user.name,
                        expirationDate = expirationDate
                    )
                    Log.d("BookAdapter", "📱 Notificaciones programadas para ${user.name} - ${book.title}")
                } catch (e: Exception) {
                    Log.e("BookAdapter", "❌ Error programando notificaciones: ${e.message}")
                }
            }
            .addOnFailureListener { e ->
                showProgressBar(holder, false)
                Toast.makeText(context, "Error al asignar el libro: $e", Toast.LENGTH_LONG).show()
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
        val updatedAssignedDates = (book.assignedDates ?: mutableListOf()).toMutableList()

        // Agregar el usuario al arreglo
        updatedAssignedTo.add(user.uid)
        updatedAssignedWithNames.add(user.name)
        updatedAssignedToEmails.add(user.email)
        updatedAssignedDates.add(Timestamp.now())

        // Referencia del libro en Firestore
        val bookRef = firestore.collection("books").document(book.id)

        // Realizar la actualización en Firestore
        val updates = mapOf(
            "quantity" to updatedQuantity,
            "status" to updatedStatus,
            "assignedTo" to updatedAssignedTo,
            "assignedWithNames" to updatedAssignedWithNames,
            "assignedToEmails" to updatedAssignedToEmails,
            "assignedDates" to updatedAssignedDates
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
                    assignedToEmails = updatedAssignedToEmails,
                    assignedDates = updatedAssignedDates
                )
                books = books.map { if (it.id == book.id) updatedBook else it }
                notifyDataSetChanged()

                // Enviar correos de notificación
                sendAssignmentNotificationEmails(book, user, context)
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

    private fun sendAssignmentNotificationEmails(book: Book, user: User, context: Context) {
        // Obtener información del admin actual
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Error: No se pudo identificar al administrador", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val adminName = document.getString("name") ?: "Administrador"
                    val adminEmail = currentUser.email ?: "admin@biblioteca.com"

                    // Enviar correos REALES con SendGrid
                    CoroutineScope(Dispatchers.Main).launch {
                        val result = emailService.sendBookAssignmentEmail(
                            adminEmail = adminEmail,
                            userEmail = user.email,
                            userName = user.name,
                            bookTitle = book.title,
                            bookAuthor = book.author,
                            adminName = adminName
                        )
                        
                        if (result.isSuccess) {
                            Log.d("EmailService", "✅ SendGrid: Correos enviados exitosamente")
                        } else {
                            Log.e("EmailService", "❌ SendGrid Error: ${result.exceptionOrNull()?.message}")
                        }
                    }

                    Toast.makeText(
                        context, 
                        "Notificaciones enviadas por correo ✉️", 
                        Toast.LENGTH_SHORT
                    ).show()


                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error obteniendo datos del admin: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Método para verificar libros vencidos y mostrar alerta visual
    // Mostrar alertas para usuarios con préstamos vencidos o por vencer
    private fun checkAndShowExpirationAlert(holder: BookViewHolder, book: Book) {
        val currentTime = System.currentTimeMillis()
        val warningPeriod = 5 * 24 * 60 * 60 * 1000L // 5 días en milisegundos
        
        // Verificar que tenemos usuarios asignados y fechas de vencimiento
        if (book.assignedTo.isNullOrEmpty() || book.loanExpirationDates.isNullOrEmpty() || book.assignedWithNames.isNullOrEmpty()) {
            holder.expirationAlert.visibility = View.GONE
            return
        }
        
        val usersToAlert = mutableListOf<Pair<String, String>>() // Pair de (nombre, estado)
        
        // Revisar cada préstamo
        for (i in book.loanExpirationDates.indices) {
            if (i < book.assignedWithNames.size) {
                val expirationDate = book.loanExpirationDates[i]
                val userName = book.assignedWithNames[i]
                val timeDiff = expirationDate.toDate().time - currentTime
                
                when {
                    timeDiff < 0 -> {
                        // Vencido
                        val daysOverdue = kotlin.math.abs(timeDiff) / (24 * 60 * 60 * 1000L)
                        usersToAlert.add(Pair(userName, "VENCIDO ${daysOverdue}d"))
                    }
                    timeDiff <= warningPeriod -> {
                        // Por vencer (dentro de 5 días)
                        val daysLeft = (timeDiff / (24 * 60 * 60 * 1000L)).toInt()
                        when (daysLeft) {
                            0 -> usersToAlert.add(Pair(userName, "HOY"))
                            1 -> usersToAlert.add(Pair(userName, "MAÑANA"))
                            else -> usersToAlert.add(Pair(userName, "EN ${daysLeft}D"))
                        }
                    }
                }
            }
        }
        
        // Mostrar alerta si hay usuarios que deben ser notificados
        if (usersToAlert.isNotEmpty()) {
            holder.expirationAlert.visibility = View.VISIBLE
            
            // Determinar el nivel de urgencia más alto
            val hasOverdue = usersToAlert.any { it.second.contains("VENCIDO") }
            val hasToday = usersToAlert.any { it.second == "HOY" }
            val hasTomorrow = usersToAlert.any { it.second == "MAÑANA" }
            
            // Configurar color y emoji según urgencia
            when {
                hasOverdue -> {
                    holder.expirationAlert.setTextColor(holder.itemView.context.getColor(android.R.color.holo_red_dark))
                    holder.expirationAlert.text = "🚨 ${formatUsersAlert(usersToAlert)}"
                }
                hasToday -> {
                    holder.expirationAlert.setTextColor(holder.itemView.context.getColor(android.R.color.holo_orange_dark))
                    holder.expirationAlert.text = "⚠️ ${formatUsersAlert(usersToAlert)}"
                }
                hasTomorrow -> {
                    holder.expirationAlert.setTextColor(holder.itemView.context.getColor(android.R.color.holo_orange_light))
                    holder.expirationAlert.text = "⏰ ${formatUsersAlert(usersToAlert)}"
                }
                else -> {
                    holder.expirationAlert.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
                    holder.expirationAlert.text = "📋 ${formatUsersAlert(usersToAlert)}"
                }
            }
        } else {
            holder.expirationAlert.visibility = View.GONE
        }
    }
    
    // Formatear la lista de usuarios con alertas de manera compacta
    private fun formatUsersAlert(usersToAlert: List<Pair<String, String>>): String {
        return when (usersToAlert.size) {
            1 -> {
                val (name, status) = usersToAlert[0]
                val firstName = name.split(" ").firstOrNull() ?: name
                "$status: $firstName debe devolver"
            }
            2 -> {
                val names = usersToAlert.map { 
                    val firstName = it.first.split(" ").firstOrNull() ?: it.first
                    "$firstName (${it.second})"
                }
                "DEBEN DEVOLVER: ${names.joinToString(", ")}"
            }
            else -> {
                val urgentCount = usersToAlert.count { it.second.contains("VENCIDO") || it.second == "HOY" }
                val totalCount = usersToAlert.size
                when {
                    urgentCount > 0 -> "URGENTE: $urgentCount de $totalCount usuarios"
                    else -> "POR VENCER: $totalCount usuarios"
                }
            }
        }
    }

    // Función para mostrar el BottomSheet con detalles completos del libro
    private fun showBookDetailsBottomSheet(book: Book, context: Context, holder: BookViewHolder) {
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
        // bs_alert_container ahora es un View vacío, usamos directamente bs_expiration_alert
        
        // Elementos de gestión de usuarios eliminados - ahora solo información + botones Editar/Eliminar
        
        // Contenedor de botones admin (solo Editar y Eliminar)
        val bsButtonsContainer = bottomSheetView.findViewById<View>(R.id.bs_buttons_container)
        
        // Botones simplificados (solo Editar y Eliminar)
        val bsEditButton = bottomSheetView.findViewById<MaterialButton>(R.id.bs_editBookButton)
        val bsDeleteButton = bottomSheetView.findViewById<MaterialButton>(R.id.bs_deleteBookButton)
        val bsProgressContainer = bottomSheetView.findViewById<LinearLayout>(R.id.bs_progress_container)

        // MOSTRAR botones (ya que esto es BookAdapter para admins)
        bsButtonsContainer.visibility = View.VISIBLE

        // Poblar con datos del libro (simplificado - gestión de usuarios ahora en EditBook)
        populateBottomSheetData(
            book, bsBookImage, bsBookTitle, bsBookAuthor, bsBookStatus, 
            bsBookCategory, bsBookQuantity, bsBookIsbn, bsBookAssignedTo, 
            bsBookDescription, bsExpirationAlert, context
        )

        // Configurar click listeners simplificados (solo Editar y Eliminar)
        setupBottomSheetListeners(
            book, context, holder, bottomSheetDialog,
            bsEditButton, bsDeleteButton, bsProgressContainer
        )

        bottomSheetDialog.show()
    }

    private fun populateBottomSheetData(
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
        
        // Usuarios asignados
        bsBookAssignedTo.text = if (book.assignedWithNames.isNullOrEmpty()) {
            "Ningún usuario asignado"
        } else {
            book.assignedWithNames.joinToString(", ")
        }

        // Verificar y mostrar alerta de vencimiento (solo para 1 usuario)
        val currentTime = System.currentTimeMillis()
        
        if (book.assignedTo?.size == 1 && book.loanExpirationDates?.size == 1) {
            val expirationDate = book.loanExpirationDates[0]
            val userName = book.assignedWithNames?.get(0) ?: "Usuario"
            
            if (expirationDate.toDate().time < currentTime) {
                bsExpirationAlert.visibility = View.VISIBLE
                bsExpirationAlert.text = "⚠️ PRÉSTAMO VENCIDO - $userName debe devolver"
            } else {
                bsExpirationAlert.visibility = View.GONE
            }
        } else {
            bsExpirationAlert.visibility = View.GONE
        }
        
        // Sección de usuarios eliminada - gestión completa ahora en EditBook
    }

    private fun setupBottomSheetListeners(
        book: Book,
        context: Context,
        holder: BookViewHolder,
        bottomSheetDialog: BottomSheetDialog,
        bsEditButton: MaterialButton,
        bsDeleteButton: MaterialButton,
        bsProgressContainer: LinearLayout
    ) {
        bsEditButton.setOnClickListener {
            bottomSheetDialog.dismiss()
            editBook(book, context)
        }

        bsDeleteButton.setOnClickListener {
            bottomSheetDialog.dismiss()
            showDeleteConfirmationDialog(book, holder, context)
            }
    }

    // Método auxiliar para mostrar u ocultar el ProgressBar
    private fun showProgressBar(holder: BookViewHolder, show: Boolean) {
        holder.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        holder.assignUserButton.isEnabled = !show
        holder.bookUserSearch.isEnabled = !show
    }

    // Funciones de gestión de usuarios del BottomSheet eliminadas - gestión completa ahora en EditBookFragment
}