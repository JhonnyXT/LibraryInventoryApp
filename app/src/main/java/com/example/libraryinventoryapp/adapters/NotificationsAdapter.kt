package com.example.libraryinventoryapp.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.libraryinventoryapp.R
import com.example.libraryinventoryapp.models.NotificationItem
import com.example.libraryinventoryapp.models.NotificationType
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

/**
 * 🎨 Adapter SIMPLE para notificaciones - solo libro + fecha
 */
class NotificationsAdapter(
    private val notifications: MutableList<NotificationItem>,
    private val onAction: (action: String, notification: NotificationItem) -> Unit
) : RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

    companion object {
        private const val TAG = "NotificationsAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount(): Int = notifications.size

    /**
     * 🎯 ViewHolder SIMPLIFICADO
     */
    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        
        // 🏗️ UI Components simples
        private val iconUrgency: ImageView = itemView.findViewById(R.id.icon_urgency)
        private val textBookTitle: TextView = itemView.findViewById(R.id.text_book_title)
        private val textDueDate: TextView = itemView.findViewById(R.id.text_due_date)
        private val btnMarkRead: MaterialButton = itemView.findViewById(R.id.btn_mark_read)

        fun bind(notification: NotificationItem) {
            // 📚 Información SIMPLE
            textBookTitle.text = notification.bookTitle
            textDueDate.text = generateSimpleDueDateText(notification)

            // 🎨 Color según urgencia
            applyUrgencyStyle(notification)

            // ✅ Botón marcar como leído
            setupMarkReadButton(notification)

            // 👆 Click en toda la card
            itemView.setOnClickListener {
                onAction("click", notification)
            }
        }

        /**
         * 📅 Generar texto simple de fecha
         */
        private fun generateSimpleDueDateText(notification: NotificationItem): String {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dueDateText = notification.expirationDate?.toDate()?.let { dateFormat.format(it) } ?: "Sin fecha"
            
            return when {
                notification.daysUntilDue > 0 -> "Debes entregarlo el $dueDateText"
                notification.daysUntilDue == 0 -> "⚠️ Debes entregarlo HOY ($dueDateText)"
                notification.daysUntilDue < 0 -> {
                    val overdueDays = kotlin.math.abs(notification.daysUntilDue)
                    "🔴 Vencido hace $overdueDays días ($dueDateText)"
                }
                else -> "Fecha: $dueDateText"
            }
        }

        /**
         * 🎨 Aplicar estilo según urgencia
         */
        private fun applyUrgencyStyle(notification: NotificationItem) {
            val (backgroundColor, iconResource) = when (notification.type) {
                NotificationType.CRITICAL -> Pair("#D32F2F", android.R.drawable.ic_dialog_alert)
                NotificationType.URGENT -> Pair("#F57C00", android.R.drawable.ic_dialog_info)
                NotificationType.DUE_TODAY -> Pair("#388E3C", android.R.drawable.ic_menu_today)
                NotificationType.UPCOMING -> Pair("#1976D2", android.R.drawable.ic_menu_recent_history)
                NotificationType.INFO -> Pair("#616161", android.R.drawable.ic_dialog_info)
            }

            // 🎯 Cambiar color de fondo del ícono
            val drawable = iconUrgency.background as? GradientDrawable
            drawable?.setColor(Color.parseColor(backgroundColor))
            
            // 🔥 Cambiar ícono
            iconUrgency.setImageResource(iconResource)
            iconUrgency.imageTintList = ColorStateList.valueOf(Color.WHITE)
        }

        /**
         * ✅ Configurar botón marcar como leído
         */
        private fun setupMarkReadButton(notification: NotificationItem) {
            if (notification.isRead) {
                btnMarkRead.text = "✓ Leído"
                btnMarkRead.isEnabled = false
                btnMarkRead.alpha = 0.6f
                btnMarkRead.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
            } else {
                btnMarkRead.text = "Leído"
                btnMarkRead.isEnabled = true
                btnMarkRead.alpha = 1.0f
                btnMarkRead.backgroundTintList = ColorStateList.valueOf(
                    itemView.context.getColor(R.color.primary_color)
                )
                btnMarkRead.setOnClickListener {
                    onAction("mark_read", notification)
                }
            }
        }
    }

    /**
     * 🗑️ Remover notificación de la lista (para botón "Leído")
     */
    fun removeNotification(notification: NotificationItem) {
        val position = notifications.indexOf(notification)
        if (position >= 0) {
            notifications.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, notifications.size)
        }
    }

    /**
     * 📊 Obtener cantidad de notificaciones no leídas
     */
    fun getUnreadCount(): Int {
        return notifications.count { !it.isRead }
    }
}