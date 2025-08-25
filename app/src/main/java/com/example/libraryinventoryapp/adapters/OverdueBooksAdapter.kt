package com.example.libraryinventoryapp.adapters

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
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
    private val overdueBooks: MutableList<OverdueBookItem>,
    private val onSendReminderClick: (OverdueBookItem, () -> Unit) -> Unit,
    private val onBookReturned: (OverdueBookItem) -> Unit
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
        val buttonsContainer: LinearLayout = itemView.findViewById(R.id.buttons_container)
        val progressContainer: LinearLayout = itemView.findViewById(R.id.progress_container)
        val progressText: TextView = itemView.findViewById(R.id.progress_text)

        init {
            firestore = FirebaseFirestore.getInstance()

            sendReminderButton.setOnClickListener {
                val overdueItem = overdueBooks[adapterPosition]
                showProgress("Enviando recordatorio...")
                onSendReminderClick(overdueItem) { hideProgress() }
            }

            markReturnedButton.setOnClickListener {
                val overdueItem = overdueBooks[adapterPosition]
                showMarkAsReturnedDialog(overdueItem)
            }
        }

        fun showProgress(message: String) {
            buttonsContainer.visibility = View.GONE
            progressText.text = message
            progressContainer.visibility = View.VISIBLE
        }

        fun hideProgress() {
            progressContainer.visibility = View.GONE
            buttonsContainer.visibility = View.VISIBLE
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
        
        // Días de retraso/próximo vencimiento con énfasis según gravedad
        val daysText = when {
            overdueItem.daysOverdue > 0 -> {
                // Libro vencido
                if (overdueItem.daysOverdue == 1) {
                    "Vencido hace ${overdueItem.daysOverdue} día"
                } else {
                    "Vencido hace ${overdueItem.daysOverdue} días"
                }
            }
            overdueItem.daysOverdue == 0 -> {
                "Vence HOY"
            }
            else -> {
                // Libro próximo a vencer (daysOverdue negativo)
                val daysUntilDue = kotlin.math.abs(overdueItem.daysOverdue)
                if (daysUntilDue == 1) {
                    "Vence MAÑANA"
                } else {
                    "Vence en $daysUntilDue días"
                }
            }
        }
        holder.daysOverdue.text = daysText

        // Color de urgencia según días de retraso/vencimiento
        when {
            overdueItem.daysOverdue >= 30 -> {
                // Muy vencido
                holder.urgencyBadge.text = "🚨 CRÍTICO"
                holder.urgencyBadge.setBackgroundColor(holder.itemView.context.getColor(android.R.color.holo_red_dark))
                holder.daysOverdue.setTextColor(holder.itemView.context.getColor(android.R.color.holo_red_dark))
            }
            overdueItem.daysOverdue >= 14 -> {
                // Bastante vencido
                holder.urgencyBadge.text = "⚠️ URGENTE"
                holder.urgencyBadge.setBackgroundColor(holder.itemView.context.getColor(android.R.color.holo_orange_dark))
                holder.daysOverdue.setTextColor(holder.itemView.context.getColor(android.R.color.holo_orange_dark))
            }
            overdueItem.daysOverdue >= 7 -> {
                // Una semana vencido
                holder.urgencyBadge.text = "⏰ TARDE"
                holder.urgencyBadge.setBackgroundColor(holder.itemView.context.getColor(android.R.color.holo_orange_light))
                holder.daysOverdue.setTextColor(holder.itemView.context.getColor(android.R.color.holo_orange_light))
            }
            overdueItem.daysOverdue >= 1 -> {
                // Recién vencido
                holder.urgencyBadge.text = "📋 VENCIDO"
                holder.urgencyBadge.setBackgroundColor(holder.itemView.context.getColor(android.R.color.holo_red_light))
                holder.daysOverdue.setTextColor(holder.itemView.context.getColor(android.R.color.holo_red_light))
            }
            overdueItem.daysOverdue == 0 -> {
                // Vence hoy
                holder.urgencyBadge.text = "🔥 VENCE HOY"
                holder.urgencyBadge.setBackgroundColor(holder.itemView.context.getColor(android.R.color.holo_orange_dark))
                holder.daysOverdue.setTextColor(holder.itemView.context.getColor(android.R.color.holo_orange_dark))
            }
            overdueItem.daysOverdue == -1 -> {
                // Vence mañana
                holder.urgencyBadge.text = "⚡ MAÑANA"
                holder.urgencyBadge.setBackgroundColor(holder.itemView.context.getColor(android.R.color.holo_orange_light))
                holder.daysOverdue.setTextColor(holder.itemView.context.getColor(android.R.color.holo_orange_light))
            }
            else -> {
                // Próximo a vencer (2-5 días)
                holder.urgencyBadge.text = "⏳ PRÓXIMO"
                holder.urgencyBadge.setBackgroundColor(holder.itemView.context.getColor(android.R.color.holo_blue_light))
                holder.daysOverdue.setTextColor(holder.itemView.context.getColor(android.R.color.holo_blue_light))
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
        // Crear mensaje personalizado según el estado del préstamo
        val statusMessage = when {
            overdueItem.daysOverdue > 0 -> {
                val daysText = if (overdueItem.daysOverdue == 1) "1 día" else "${overdueItem.daysOverdue} días"
                "Este libro está vencido hace $daysText."
            }
            overdueItem.daysOverdue == 0 -> {
                "Este libro vence HOY."
            }
            else -> {
                val daysUntil = kotlin.math.abs(overdueItem.daysOverdue)
                val daysText = if (daysUntil == 1) "mañana" else "en $daysUntil días"
                "Este libro vence $daysText."
            }
        }
        
        AlertDialog.Builder(itemView.context)
            .setTitle("Marcar como Devuelto")
            .setMessage("¿Confirmas que ${overdueItem.userName} devolvió el libro '${overdueItem.book.title}'?\n\n$statusMessage\n\nEsta acción removerá la asignación del libro.")
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton("✅ SÍ, DEVUELTO") { _, _ ->
                showProgress("Procesando devolución...")
                markBookAsReturned(overdueItem)
            }
            .setNegativeButton("❌ CANCELAR", null)
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

        // Convertir listas vacías a null para limpieza en Firestore
        val finalAssignedTo = if (updatedAssignedTo.isEmpty()) null else updatedAssignedTo
        val finalAssignedWithNames = if (updatedAssignedWithNames.isEmpty()) null else updatedAssignedWithNames
        val finalAssignedToEmails = if (updatedAssignedToEmails.isEmpty()) null else updatedAssignedToEmails
        val finalAssignedDates = if (updatedAssignedDates.isEmpty()) null else updatedAssignedDates
        val finalLoanExpirationDates = if (updatedLoanExpirationDates.isEmpty()) null else updatedLoanExpirationDates

        // Calcular status basado en disponibilidad real (NO cambiar quantity física)
        val totalCopies = book.quantity // Mantener cantidad física original
        val remainingAssignedCopies = finalAssignedTo?.size ?: 0
        val updatedStatus = if (remainingAssignedCopies >= totalCopies) "No disponible" else "Disponible"

        // Actualizar en Firestore (SIN cambiar quantity)
        val updates = mapOf(
            "status" to updatedStatus,
            "assignedTo" to finalAssignedTo,
            "assignedWithNames" to finalAssignedWithNames,
            "assignedToEmails" to finalAssignedToEmails,
            "assignedDates" to finalAssignedDates,
            "loanExpirationDates" to finalLoanExpirationDates,
            "lastEditedDate" to com.google.firebase.Timestamp.now()
        )

        firestore.collection("books").document(book.id)
            .update(updates)
            .addOnSuccessListener {
                hideProgress()
                Toast.makeText(
                    itemView.context,
                    "✅ Libro '${book.title}' marcado como devuelto por ${overdueItem.userName}",
                    Toast.LENGTH_LONG
                ).show()
                
                // Notificar al fragmento para que remueva el item de la lista
                onBookReturned(overdueItem)
            }
            .addOnFailureListener { e ->
                hideProgress()
                Toast.makeText(
                    itemView.context,
                    "❌ Error al marcar como devuelto: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
