package com.example.libraryinventoryapp.constants

/**
 * 📚 Constantes compartidas de la aplicación LibraryInventoryApp
 */
object LibraryConstants {
    
    // 🎯 Roles de usuario
    object UserRoles {
        const val ADMIN = "admin"
        const val USER = "usuario"
    }
    
    // 📖 Estados de libros
    object BookStatus {
        const val AVAILABLE = "Disponible"
        const val UNAVAILABLE = "No disponible"
    }
    
    // 🏷️ Categorías de libros disponibles
    val BOOK_CATEGORIES = listOf(
        "Biblia",
        "Liderazgo", 
        "Jóvenes",
        "Mujeres",
        "Profecía bíblica",
        "Familia",
        "Matrimonio",
        "Finanzas",
        "Estudio bíblico",
        "Evangelismo",
        "Navidad",
        "Emaus",
        "Misiones",
        "Devocionales",
        "Curso vida",
        "Iglesia",
        "Vida cristiana",
        "Libros de la Biblia",
        "Enciclopedia",
        "Religiones",
        "Inglés",
        "Infantil"
    )
    
    // ⏰ Configuración de préstamos
    object LoanConfig {
        const val DEFAULT_LOAN_DAYS = 15
        const val MAX_LOAN_DAYS = 30
        const val RENEWAL_DAYS = 7
        const val WARNING_DAYS_BEFORE_DUE = 3
        const val OVERDUE_REMINDER_DAYS = 1
    }
    
    // 🔔 Configuración de notificaciones
    object NotificationConfig {
        const val UPCOMING_NOTIFICATION_DAYS = 3
        const val CRITICAL_OVERDUE_DAYS = 7
        const val MAX_NOTIFICATIONS_PER_USER = 50
    }
    
    // 🎨 Colores Material Design 3
    object Colors {
        const val PRIMARY = "#1976D2"
        const val SECONDARY = "#424242"
        const val SUCCESS = "#4CAF50"
        const val WARNING = "#FF9800"
        const val ERROR = "#F44336"
        const val INFO = "#2196F3"
        
        // Estados de urgencia
        const val CRITICAL_RED = "#D32F2F"
        const val URGENT_ORANGE = "#F57C00"  
        const val DUE_TODAY_GREEN = "#388E3C"
        const val UPCOMING_BLUE = "#1976D2"
        const val INFO_GRAY = "#757575"
    }
    
    // 📊 Límites de la aplicación
    object Limits {
        const val MAX_BOOKS_PER_USER = 5
        const val MAX_COMMENT_LENGTH = 500
        const val MAX_BOOK_TITLE_LENGTH = 100
        const val MAX_BOOK_DESCRIPTION_LENGTH = 1000
        const val MAX_AUTHOR_NAME_LENGTH = 100
        const val MAX_ISBN_LENGTH = 20
        const val MIN_SEARCH_QUERY_LENGTH = 2
        const val MAX_WISHLIST_ITEMS = 20
    }
    
    // 🔍 Configuración de búsqueda
    object Search {
        const val MIN_QUERY_LENGTH = 2
        const val SEARCH_DEBOUNCE_MS = 300
        val SEARCHABLE_FIELDS = listOf("title", "author", "isbn", "categories")
    }
    
    // 📧 Configuración de emails
    object Email {
        const val FROM_NAME = "Sistema de Biblioteca"
        const val SUBJECT_ASSIGNMENT = "📚 Nuevo libro asignado"
        const val SUBJECT_REMINDER = "⏰ Recordatorio de devolución"
        const val SUBJECT_OVERDUE = "🚨 Libro vencido"
        const val SUBJECT_NEW_VERSION = "🎉 Nueva versión disponible"
    }
    
    // 🔗 URLs y endpoints
    object Urls {
        const val GITHUB_RELEASES = "https://github.com/JhonnyXT/LibraryInventoryApp/releases"
        const val SUPPORT_EMAIL = "hermanosencristobello@gmail.com"
    }
    
    // 📱 Información de la aplicación
    object AppInfo {
        const val NAME = "LibraryInventoryApp"
        const val DESCRIPTION = "Sistema de gestión de inventario de bibliotecas"
        const val ORGANIZATION = "Hermanos en Cristo Bello"
    }
}
