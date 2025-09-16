package com.example.libraryinventoryapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.libraryinventoryapp.R
import com.example.libraryinventoryapp.models.Comment
import java.text.SimpleDateFormat
import java.util.*

/**
 * 💬 Adapter para lista de comentarios con diseño moderno
 */
class CommentsAdapter(
    private val comments: MutableList<Comment>
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    companion object {
        private const val TAG = "CommentsAdapter"
        private const val MAX_COMMENT_LENGTH = 120 // Máximo de caracteres antes de truncar
    }
    
    // 📋 Set para rastrear comentarios expandidos
    private val expandedComments = mutableSetOf<String>()

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textUserInitial: TextView = itemView.findViewById(R.id.text_user_initial)
        val textUserName: TextView = itemView.findViewById(R.id.text_user_name)
        val textCommentTime: TextView = itemView.findViewById(R.id.text_comment_time)
        val textCommentDate: TextView = itemView.findViewById(R.id.text_comment_date)
        val textEditedIndicator: TextView = itemView.findViewById(R.id.text_edited_indicator)
        val textCommentContent: TextView = itemView.findViewById(R.id.text_comment_content)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        
        // 👤 Inicial del usuario (primera letra del nombre)
        val initial = if (comment.userName.isNotEmpty()) {
            comment.userName.first().uppercaseChar().toString()
        } else {
            "?"
        }
        holder.textUserInitial.text = initial
        
        // 👤 Nombre del usuario
        holder.textUserName.text = comment.userName
        
        // 🕒 Tiempo relativo
        holder.textCommentTime.text = comment.getRelativeTime()
        
        // 📅 Fecha específica
        holder.textCommentDate.text = formatCommentDate(comment.timestamp.toDate())
        
        // ✏️ Indicador de editado
        if (comment.isEdited) {
            holder.textEditedIndicator.visibility = View.VISIBLE
        } else {
            holder.textEditedIndicator.visibility = View.GONE
        }
        
        // 💬 Contenido del comentario con truncado/expansión
        setupCommentContent(holder, comment)
    }
    
    /**
     * 📅 Formatear fecha del comentario
     */
    private fun formatCommentDate(date: Date): String {
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return formatter.format(date)
    }
    
    /**
     * 💬 Configurar contenido del comentario con truncado/expansión
     */
    private fun setupCommentContent(holder: CommentViewHolder, comment: Comment) {
        val isExpanded = expandedComments.contains(comment.id)
        val commentText = comment.comment
        
        if (commentText.length > MAX_COMMENT_LENGTH && !isExpanded) {
            // Mostrar versión truncada
            val truncatedText = commentText.substring(0, MAX_COMMENT_LENGTH) + "..."
            holder.textCommentContent.text = truncatedText
        } else {
            // Mostrar texto completo
            holder.textCommentContent.text = commentText
        }
        
        // 👆 Click listener para expandir/contraer
        holder.textCommentContent.setOnClickListener {
            if (commentText.length > MAX_COMMENT_LENGTH) {
                if (isExpanded) {
                    expandedComments.remove(comment.id)
                } else {
                    expandedComments.add(comment.id)
                }
                notifyItemChanged(holder.adapterPosition)
            }
        }
        
        // 👆 Click en toda la tarjeta también expande
        holder.itemView.setOnClickListener {
            if (commentText.length > MAX_COMMENT_LENGTH) {
                if (isExpanded) {
                    expandedComments.remove(comment.id)
                } else {
                    expandedComments.add(comment.id)
                }
                notifyItemChanged(holder.adapterPosition)
            }
        }
    }

    override fun getItemCount(): Int = comments.size

    /**
     * 🔄 Actualizar lista de comentarios
     */
    fun updateComments(newComments: List<Comment>) {
        val sortedComments = newComments.sortedByDescending { it.timestamp }
        comments.clear()
        comments.addAll(sortedComments)
        notifyDataSetChanged()
        
        android.util.Log.d(TAG, "🔄 Adapter actualizado con ${comments.size} comentarios")
    }

    /**
     * ➕ Agregar nuevo comentario
     */
    fun addComment(comment: Comment) {
        comments.add(0, comment) // Agregar al inicio
        notifyItemInserted(0)
    }
}
