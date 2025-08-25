package com.example.libraryinventoryapp.adapters

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.libraryinventoryapp.R
import com.example.libraryinventoryapp.models.OverdueBookItem
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class OverdueBooksAdapter(
    private val overdueBooks: List<OverdueBookItem>,
    private val onSendReminderClick: (OverdueBookItem) -> Unit
) : RecyclerView.Adapter<OverdueBooksAdapter.OverdueViewHolder>() {

    private lateinit var firestore: FirebaseFirestore

    inner class OverdueViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bookImage: ImageView = itemView.findViewById(R.id.book_image)
        val bookTitle: TextView = itemView.findViewById(R.id.book_title)
        val bookAuthor: TextView = itemView.findViewById(R.id.book_author)
        val userName: TextView = itemView.findViewById(R.id.user_name)
        val expirationDate: TextView = itemView.findViewById(R.id.expiration_date)
        val loanDate: TextView = itemView.findViewById(R.id.loan_date)
        val daysOverdue: TextView = itemView.findViewById(R.id.days_overdue)
        val sendReminderButton: Button = itemView.findViewById(R.id.send_reminder_button)
        val markReturnedButton: Button = itemView.findViewById(R.id.mark_returned_button)
        val urgencyBadge: TextView = itemView.findViewById(R.id.urgency_badge)

        init {
            firestore = FirebaseFirestore.getInstance()

            sendReminderButton.setOnClickListener {
                val overdueItem = overdueBooks[adapterPosition]
                onSendReminderClick(overdueItem)
            }

            markReturnedButton.setOnClickListener {
                val overdueItem = overdueBooks[adapterPosition]
                showMarkAsReturnedDialog(overdueItem)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OverdueViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_overdue_book, parent, false)
        return OverdueViewHolder(view)
    }

    override fun onBindViewHolder(holder: OverdueViewHolder, position: Int) {
        val overdueItem = overdueBooks[position]
        val book = overdueItem.book
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // Información del libro
        holder.bookTitle.text = book.title
        holder.bookAuthor.text = "por ${book.author}"

        // Información del préstamo
        holder.userName.text = overdueItem.userName
        holder.expirationDate.text = dateFormat.format(overdueItem.expirationDate.toDate())
        
        // Fecha de préstamo (assigned date)
        if (overdueItem.assignedDate != null) {
            holder.loanDate.text = dateFormat.format(overdueItem.assignedDate.toDate())
        } else {
            holder.loanDate.text = "No disponible"
        }
        
        // Días de retraso con énfasis según gravedad
        val daysText = if (overdueItem.daysOverdue == 1) {
            "${overdueItem.daysOverdue} día"
        } else {
            "${overdueItem.daysOverdue} días"
        }
        holder.daysOverdue.text = daysText

        // Color de urgencia según días de retraso
        when {
            overdueItem.daysOverdue >= 30 -> {
                holder.urgencyBadge.text = "🚨 CRÍTICO"
                holder.urgencyBadge.setBackgroundColor(holder.itemView.context.getColor(android.R.color.holo_red_dark))
                holder.daysOverdue.setTextColor(holder.itemView.context.getColor(android.R.color.holo_red_dark))
            }
            overdueItem.daysOverdue >= 14 -> {
                holder.urgencyBadge.text = "⚠️ URGENTE"
                holder.urgencyBadge.setBackgroundColor(holder.itemView.context.getColor(android.R.color.holo_orange_dark))
                holder.daysOverdue.setTextColor(holder.itemView.context.getColor(android.R.color.holo_orange_dark))
            }
            overdueItem.daysOverdue >= 7 -> {
                holder.urgencyBadge.text = "⏰ TARDE"
                holder.urgencyBadge.setBackgroundColor(holder.itemView.context.getColor(android.R.color.holo_orange_light))
                holder.daysOverdue.setTextColor(holder.itemView.context.getColor(android.R.color.holo_orange_light))
            }
            else -> {
                holder.urgencyBadge.text = "📋 VENCIDO"
                holder.urgencyBadge.setBackgroundColor(holder.itemView.context.getColor(android.R.color.holo_red_light))
                holder.daysOverdue.setTextColor(holder.itemView.context.getColor(android.R.color.holo_red_light))
            }
        }

        // Cargar imagen del libro
        if (!book.imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(book.imageUrl)
                .placeholder(R.drawable.ic_book_default)
                .error(R.drawable.ic_book_default)
                .into(holder.bookImage)
        } else {
            holder.bookImage.setImageResource(R.drawable.ic_book_default)
        }
    }

    override fun getItemCount(): Int = overdueBooks.size

    private fun OverdueViewHolder.showMarkAsReturnedDialog(overdueItem: OverdueBookItem) {
        AlertDialog.Builder(itemView.context)
            .setTitle("Marcar como Devuelto")
            .setMessage("¿Confirmas que ${overdueItem.userName} devolvió el libro '${overdueItem.book.title}'?\n\nEsta acción removerá la asignación del libro.")
            .setPositiveButton("CONFIRMAR") { _, _ ->
                markBookAsReturned(overdueItem)
            }
            .setNegativeButton("CANCELAR", null)
            .show()
    }

    private fun OverdueViewHolder.markBookAsReturned(overdueItem: OverdueBookItem) {
        val book = overdueItem.book
        val userIndex = book.assignedTo?.indexOf(overdueItem.userId) ?: -1
        
        if (userIndex == -1) return

        // Crear nuevas listas sin el usuario que devolvió el libro
        val updatedAssignedTo = book.assignedTo?.toMutableList()?.apply { removeAt(userIndex) } ?: mutableListOf()
        val updatedAssignedWithNames = book.assignedWithNames?.toMutableList()?.apply { removeAt(userIndex) } ?: mutableListOf()
        val updatedAssignedToEmails = book.assignedToEmails?.toMutableList()?.apply { removeAt(userIndex) } ?: mutableListOf()
        val updatedAssignedDates = book.assignedDates?.toMutableList()?.apply { removeAt(userIndex) } ?: mutableListOf()
        val updatedLoanExpirationDates = book.loanExpirationDates?.toMutableList()?.apply { removeAt(userIndex) } ?: mutableListOf()

        // Actualizar cantidad y estado
        val updatedQuantity = book.quantity + 1
        val updatedStatus = "Disponible"

        // Actualizar en Firestore
        val updates = mapOf(
            "quantity" to updatedQuantity,
            "status" to updatedStatus,
            "assignedTo" to updatedAssignedTo,
            "assignedWithNames" to updatedAssignedWithNames,
            "assignedToEmails" to updatedAssignedToEmails,
            "assignedDates" to updatedAssignedDates,
            "loanExpirationDates" to updatedLoanExpirationDates
        )

        firestore.collection("books").document(book.id)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(
                    itemView.context,
                    "✅ Libro '${book.title}' marcado como devuelto por ${overdueItem.userName}",
                    Toast.LENGTH_LONG
                ).show()
                
                // Notificar al fragmento que debe recargar la lista
                // (El fragmento detectará automáticamente al hacer swipe to refresh)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    itemView.context,
                    "❌ Error al marcar como devuelto: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
