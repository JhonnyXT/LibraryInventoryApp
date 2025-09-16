package com.example.libraryinventoryapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.libraryinventoryapp.R
import com.example.libraryinventoryapp.adapters.OverdueBooksAdapter
import com.example.libraryinventoryapp.models.Book
import com.example.libraryinventoryapp.models.OverdueBookItem
import com.example.libraryinventoryapp.utils.EmailService
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class OverdueBooksFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var overdueAdapter: OverdueBooksAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    
    // 🎨 Componentes de estados modernos
    private lateinit var loadingState: LinearLayout
    private lateinit var progressBar: com.google.android.material.progressindicator.CircularProgressIndicator
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var emptyStateText: TextView
    
    // 🔍 Componentes del filtro
    private lateinit var cardFilterContainer: MaterialCardView
    private lateinit var chipGroupFilters: ChipGroup
    private lateinit var textFilterCount: TextView
    
    private val emailService = EmailService()
    
    // 📊 Estados para filtrado
    enum class FilterState {
        ALL, UPCOMING, TODAY, RECENT_OVERDUE, LATE, URGENT, CRITICAL
    }
    
    private var overdueBooksList: MutableList<OverdueBookItem> = mutableListOf()
    private var filteredOverdueBooksList: MutableList<OverdueBookItem> = mutableListOf()
    private var currentFilter: FilterState = FilterState.ALL

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_overdue_books, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar componentes básicos
        recyclerView = view.findViewById(R.id.overdue_books_recycler_view)
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)
        
        // 🎨 Inicializar estados modernos
        loadingState = view.findViewById(R.id.loading_state)
        progressBar = view.findViewById(R.id.progress_bar)
        emptyStateLayout = view.findViewById(R.id.empty_state_layout)
        emptyStateText = view.findViewById(R.id.empty_state_text)
        
        // 🔍 Inicializar componentes del filtro
        cardFilterContainer = view.findViewById(R.id.card_filter_container)
        chipGroupFilters = view.findViewById(R.id.chip_group_filters)
        textFilterCount = view.findViewById(R.id.text_filter_count)

        recyclerView.layoutManager = LinearLayoutManager(context)
        firestore = FirebaseFirestore.getInstance()

        // Configurar SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            loadOverdueBooks()
        }

        // ✅ PASO 1: Configurar el adaptador ANTES de los filtros
        overdueAdapter = OverdueBooksAdapter(
            filteredOverdueBooksList,
            onSendReminderClick = { overdueItem, hideProgress ->
                sendReminderEmail(overdueItem, hideProgress)
            },
            onBookReturned = { overdueItem ->
                removeBookFromList(overdueItem)
            }
        )
        recyclerView.adapter = overdueAdapter

        // ✅ PASO 2: Configurar filtros DESPUÉS de inicializar adapter
        setupFilters()

        loadOverdueBooks()
    }

    /**
     * 🔍 Configurar sistema de filtros
     */
    private fun setupFilters() {
        // Configurar listeners para cada chip
        chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            when {
                checkedIds.contains(R.id.chip_all) -> applyFilter(FilterState.ALL)
                checkedIds.contains(R.id.chip_upcoming) -> applyFilter(FilterState.UPCOMING)
                checkedIds.contains(R.id.chip_today) -> applyFilter(FilterState.TODAY)
                checkedIds.contains(R.id.chip_recent_overdue) -> applyFilter(FilterState.RECENT_OVERDUE)
                checkedIds.contains(R.id.chip_late) -> applyFilter(FilterState.LATE)
                checkedIds.contains(R.id.chip_urgent) -> applyFilter(FilterState.URGENT)
                checkedIds.contains(R.id.chip_critical) -> applyFilter(FilterState.CRITICAL)
                else -> applyFilter(FilterState.ALL) // Default si no hay selección
            }
        }
        
        // Aplicar filtro inicial
        applyFilter(FilterState.ALL)
        
        Log.d("OverdueBooksFragment", "🔍 Sistema de filtros configurado")
    }

    /**
     * 🎯 Aplicar filtro específico
     */
    private fun applyFilter(filterState: FilterState) {
        currentFilter = filterState
        filteredOverdueBooksList.clear()
        
        val filtered = when (filterState) {
            FilterState.ALL -> overdueBooksList
            FilterState.UPCOMING -> overdueBooksList.filter { it.daysOverdue < 0 } // Próximos (valores negativos)
            FilterState.TODAY -> overdueBooksList.filter { it.daysOverdue == 0 } // Vence hoy
            FilterState.RECENT_OVERDUE -> overdueBooksList.filter { it.daysOverdue in 1..6 } // Vencidos recientes
            FilterState.LATE -> overdueBooksList.filter { it.daysOverdue in 7..13 } // Tarde
            FilterState.URGENT -> overdueBooksList.filter { it.daysOverdue in 14..29 } // Urgente
            FilterState.CRITICAL -> overdueBooksList.filter { it.daysOverdue >= 30 } // Crítico
        }
        
        filteredOverdueBooksList.addAll(filtered)
        
        // ✅ Validación adicional para prevenir crashes futuros
        if (::overdueAdapter.isInitialized) {
            overdueAdapter.notifyDataSetChanged()
        } else {
            android.util.Log.w("OverdueBooksFragment", "⚠️ overdueAdapter no inicializado - saltando notifyDataSetChanged")
        }
        
        // Actualizar contador
        updateFilterCount(filterState, filtered.size)
        
        // Mostrar/ocultar estado vacío
        updateEmptyState()
        
        Log.d("OverdueBooksFragment", "🎯 Filtro aplicado: $filterState (${filtered.size} resultados)")
    }

    /**
     * 📊 Actualizar contador de filtros
     */
    private fun updateFilterCount(filterState: FilterState, count: Int) {
        val filterName = when (filterState) {
            FilterState.ALL -> "Todos"
            FilterState.UPCOMING -> "Próximos"
            FilterState.TODAY -> "Hoy"
            FilterState.RECENT_OVERDUE -> "Vencidos"
            FilterState.LATE -> "Tarde"
            FilterState.URGENT -> "Urgente"
            FilterState.CRITICAL -> "Crítico"
        }
        
        textFilterCount.text = "$filterName ($count)"
    }

    /**
     * 📄 Actualizar estado vacío según filtro - Centrado perfecto
     */
    private fun updateEmptyState() {
        if (filteredOverdueBooksList.isEmpty()) {
            // 🎯 Mostrar estado vacío centrado en toda el área de la lista
            emptyStateLayout.visibility = View.VISIBLE
            swipeRefreshLayout.visibility = View.GONE
            
            val message = when (currentFilter) {
                FilterState.ALL -> "No hay libros pendientes de devolución"
                FilterState.UPCOMING -> "No hay libros próximos a vencer"
                FilterState.TODAY -> "No hay libros que venzan hoy"
                FilterState.RECENT_OVERDUE -> "No hay libros recién vencidos"
                FilterState.LATE -> "No hay libros con retraso moderado"
                FilterState.URGENT -> "No hay libros urgentes"
                FilterState.CRITICAL -> "No hay libros en estado crítico"
            }
            emptyStateText.text = message
            
            Log.d("OverdueBooksFragment", "📭 Estado vacío mostrado: $message")
        } else {
            // 🎯 Mostrar lista con datos
            emptyStateLayout.visibility = View.GONE
            swipeRefreshLayout.visibility = View.VISIBLE
            
            Log.d("OverdueBooksFragment", "📋 Lista mostrada con ${filteredOverdueBooksList.size} elementos")
        }
    }

    private fun loadOverdueBooks() {
        Log.i("OverdueBooksFragment", "🔄 INICIANDO CARGA DE LIBROS VENCIDOS/PRÓXIMOS A VENCER")
        
        // 🔄 Mostrar estado de carga elegante
        loadingState.visibility = View.VISIBLE
        emptyStateLayout.visibility = View.GONE
        swipeRefreshLayout.visibility = View.GONE
        
        firestore.collection("books")
            .get()
            .addOnSuccessListener { result ->
                Log.d("OverdueBooksFragment", "📚 Obtenidos ${result.size()} libros de Firestore")
                overdueBooksList.clear()
                val currentTime = System.currentTimeMillis()
                val fiveDaysFromNow = currentTime + (5 * 24 * 60 * 60 * 1000L) // 5 días en milisegundos

                for (document in result) {
                    val book = document.toObject(Book::class.java)
                    book.id = document.id

                    // Verificar si el libro tiene asignaciones y fechas de vencimiento
                    book.loanExpirationDates?.forEachIndexed { index, expirationDate ->
                        val expirationTime = expirationDate.toDate().time
                        
                        // Incluir libros vencidos Y próximos a vencer (dentro de 5 días)
                        if (expirationTime <= fiveDaysFromNow) {
                            val userId = book.assignedTo?.getOrNull(index)
                            val userName = book.assignedWithNames?.getOrNull(index)
                            val userEmail = book.assignedToEmails?.getOrNull(index)
                            val assignedDate = book.assignedDates?.getOrNull(index)

                            if (userId != null && userName != null) {
                                // Calcular días: positivo = vencido, negativo = próximo a vencer, 0 = vence hoy
                                val daysOverdue = TimeUnit.MILLISECONDS.toDays(
                                    currentTime - expirationTime
                                ).toInt()

                                val overdueItem = OverdueBookItem(
                                    book = book,
                                    userId = userId,
                                    userName = userName,
                                    userEmail = userEmail ?: "",
                                    expirationDate = expirationDate,
                                    assignedDate = assignedDate,
                                    daysOverdue = daysOverdue
                                )
                                
                                // Log detallado de cada item cargado
                                Log.d("OverdueBooksFragment", """
                                    📖 Libro cargado para alertas:
                                    - Título: ${book.title}
                                    - Usuario: $userName
                                    - Email: ${userEmail ?: "NO DISPONIBLE ⚠️"}
                                    - Días diferencia: $daysOverdue
                                    - Fecha vencimiento: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(expirationDate.toDate())}
                                """.trimIndent())
                                
                                overdueBooksList.add(overdueItem)
                            } else {
                                Log.w("OverdueBooksFragment", "⚠️ Usuario incompleto para libro ${book.title}: userId=$userId, userName=$userName")
                            }
                        }
                    }
                }

                // Ordenar: primero los más vencidos, luego los próximos a vencer
                overdueBooksList.sortByDescending { it.daysOverdue }

                // Log resumen de la carga
                Log.i("OverdueBooksFragment", """
                    📊 RESUMEN DE CARGA DE LIBROS:
                    - Total libros cargados: ${overdueBooksList.size}
                    - Vencidos: ${overdueBooksList.count { it.daysOverdue > 0 }}
                    - Vencen hoy: ${overdueBooksList.count { it.daysOverdue == 0 }}
                    - Próximos a vencer: ${overdueBooksList.count { it.daysOverdue < 0 }}
                    - Con email: ${overdueBooksList.count { it.userEmail.isNotBlank() }}
                    - Sin email: ${overdueBooksList.count { it.userEmail.isBlank() }}
                """.trimIndent())

                // Actualizar UI
                updateUI()
                
                // 🎯 Ocultar estado de carga
                loadingState.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
            }
            .addOnFailureListener { exception ->
                // 🚨 Ocultar loading en caso de error
                loadingState.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
                Toast.makeText(
                    context, 
                    "Error al cargar devoluciones pendientes: ${exception.message}", 
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun updateUI() {
        // Aplicar el filtro actual con los nuevos datos
        applyFilter(currentFilter)
        
        Log.d("OverdueBooksFragment", "🔄 UI actualizada con ${overdueBooksList.size} libros totales")
    }

    private fun sendReminderEmail(overdueItem: OverdueBookItem, hideProgress: () -> Unit) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        
        // Logs detallados del inicio del proceso
        Log.i("OverdueBooksFragment", """
            🎯 INICIANDO PROCESO DE RECORDATORIO 🎯
            Usuario: ${overdueItem.userName}
            Email usuario: ${overdueItem.userEmail}
            Libro: ${overdueItem.book.title}
            Autor: ${overdueItem.book.author}
            Fecha vencimiento: ${dateFormat.format(overdueItem.expirationDate.toDate())}
            Días de diferencia: ${overdueItem.daysOverdue}
        """.trimIndent())
        
        // Verificar que el email del usuario no esté vacío
        if (overdueItem.userEmail.isBlank()) {
            Log.e("OverdueBooksFragment", "❌ ERROR: Email del usuario está vacío para ${overdueItem.userName}")
            hideProgress()
            Toast.makeText(context, "❌ Error: No se encontró email para ${overdueItem.userName}", Toast.LENGTH_LONG).show()
            return
        }
        
        // Obtener información del admin actual
        Log.d("OverdueBooksFragment", "🔍 Obteniendo información del admin...")
        getCurrentAdminInfo { adminName, adminEmail ->
            
            Log.d("OverdueBooksFragment", "✅ Info admin obtenida: $adminName ($adminEmail)")
            
            // Crear mensaje personalizado según el estado del libro
            val daysText = when {
                overdueItem.daysOverdue > 0 -> {
                    if (overdueItem.daysOverdue == 1) "Vencido hace 1 día" else "Vencido hace ${overdueItem.daysOverdue} días"
                }
                overdueItem.daysOverdue == 0 -> "Vence hoy"
                else -> {
                    val daysUntil = kotlin.math.abs(overdueItem.daysOverdue)
                    if (daysUntil == 1) "Vence mañana" else "Vence en $daysUntil días"
                }
            }
            
            Log.i("OverdueBooksFragment", "📧 Enviando recordatorio REAL via SendGrid...")
            
            // CAMBIO: Usar la versión REAL de SendGrid (no demo)
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val result = emailService.sendBookExpirationReminderEmail(
                        adminEmail = adminEmail,
                        userEmail = overdueItem.userEmail,
                        userName = overdueItem.userName,
                        bookTitle = overdueItem.book.title,
                        bookAuthor = overdueItem.book.author,
                        adminName = adminName,
                        expirationDate = dateFormat.format(overdueItem.expirationDate.toDate()),
                        daysOverdue = daysText
                    )
                    
                    if (result.isSuccess) {
                        hideProgress()
                        Log.i("OverdueBooksFragment", "✅ Recordatorio enviado exitosamente!")
                        
                        // Toast personalizado según el estado (sin admin)
                        val toastMessage = when {
                            overdueItem.daysOverdue > 0 -> {
                                "✅ Recordatorio enviado a ${overdueItem.userName}\n" +
                                "📧 Email: ${overdueItem.userEmail}\n" +
                                "📚 Libro: ${overdueItem.book.title}\n" +
                                "⚠️ $daysText"
                            }
                            overdueItem.daysOverdue == 0 -> {
                                "✅ Recordatorio enviado a ${overdueItem.userName}\n" +
                                "📧 Email: ${overdueItem.userEmail}\n" +
                                "📚 Libro: ${overdueItem.book.title}\n" +
                                "🔥 Vence HOY"
                            }
                            else -> {
                                "✅ Recordatorio enviado a ${overdueItem.userName}\n" +
                                "📧 Email: ${overdueItem.userEmail}\n" +
                                "📚 Libro: ${overdueItem.book.title}\n" +
                                "⏳ $daysText"
                            }
                        }
                        
                        Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()
                        
                    } else {
                        hideProgress()
                        val errorMsg = result.exceptionOrNull()?.message ?: "Error desconocido"
                        Log.e("OverdueBooksFragment", "❌ Error enviando recordatorio: $errorMsg")
                        Toast.makeText(context, "❌ Error enviando recordatorio: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                    
                } catch (e: Exception) {
                    hideProgress()
                    Log.e("OverdueBooksFragment", "❌ Excepción enviando recordatorio: ${e.message}", e)
                    Toast.makeText(context, "❌ Error inesperado: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun removeBookFromList(overdueItem: OverdueBookItem) {
        Log.i("OverdueBooksFragment", """
            🗑️ REMOVIENDO LIBRO DE LA LISTA:
            Usuario: ${overdueItem.userName}
            Libro: ${overdueItem.book.title}
            Lista antes: ${overdueBooksList.size} items
            Lista filtrada antes: ${filteredOverdueBooksList.size} items
        """.trimIndent())
        
        // Buscar y remover de la lista principal
        val mainPosition = overdueBooksList.indexOfFirst { 
            it.book.id == overdueItem.book.id && it.userId == overdueItem.userId 
        }
        
        // Buscar y remover de la lista filtrada
        val filteredPosition = filteredOverdueBooksList.indexOfFirst { 
            it.book.id == overdueItem.book.id && it.userId == overdueItem.userId 
        }
        
        if (mainPosition != -1) {
            overdueBooksList.removeAt(mainPosition)
            Log.i("OverdueBooksFragment", "✅ Removido de lista principal en posición: $mainPosition")
        }
        
        if (filteredPosition != -1) {
            filteredOverdueBooksList.removeAt(filteredPosition)
            
            // ✅ Validación adicional para prevenir crashes
            if (::overdueAdapter.isInitialized) {
                overdueAdapter.notifyItemRemoved(filteredPosition)
            } else {
                android.util.Log.w("OverdueBooksFragment", "⚠️ overdueAdapter no inicializado - saltando notifyItemRemoved")
            }
            
            Log.i("OverdueBooksFragment", "✅ Removido de lista filtrada en posición: $filteredPosition")
        }
        
        if (mainPosition != -1 || filteredPosition != -1) {
            Log.i("OverdueBooksFragment", """
                ✅ LIBRO REMOVIDO EXITOSAMENTE:
                Lista principal después: ${overdueBooksList.size} items
                Lista filtrada después: ${filteredOverdueBooksList.size} items
            """.trimIndent())
            
            // Actualizar contador y UI
            updateFilterCount(currentFilter, filteredOverdueBooksList.size)
            updateEmptyState()
            
            // Log resumen actualizado
            Log.i("OverdueBooksFragment", """
                📊 RESUMEN ACTUALIZADO:
                - Total libros: ${overdueBooksList.size}
                - Vencidos: ${overdueBooksList.count { it.daysOverdue > 0 }}
                - Vencen hoy: ${overdueBooksList.count { it.daysOverdue == 0 }}
                - Próximos a vencer: ${overdueBooksList.count { it.daysOverdue < 0 }}
            """.trimIndent())
            
        } else {
            Log.w("OverdueBooksFragment", "⚠️ No se encontró el libro para remover de la lista")
        }
    }

    private fun getCurrentAdminInfo(callback: (String, String) -> Unit) {
        Log.d("OverdueBooksFragment", "🔍 Iniciando obtención de información del admin...")
        
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.w("OverdueBooksFragment", "⚠️ No hay usuario autenticado, usando datos por defecto")
            callback("Administrador", "admin@biblioteca.com")
            return
        }

        Log.d("OverdueBooksFragment", "👤 Usuario autenticado: ${currentUser.uid}, email: ${currentUser.email}")
        
        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val adminName = document.getString("name") ?: "Administrador"
                    val adminEmail = currentUser.email ?: "admin@biblioteca.com"
                    val userRole = document.getString("role") ?: "desconocido"
                    
                    Log.i("OverdueBooksFragment", """
                        ✅ Información del admin obtenida exitosamente:
                        Nombre: $adminName
                        Email: $adminEmail
                        Rol: $userRole
                        UID: ${currentUser.uid}
                    """.trimIndent())
                    
                    callback(adminName, adminEmail)
                } else {
                    Log.w("OverdueBooksFragment", "⚠️ Documento de usuario no existe, usando email de Firebase Auth")
                    val fallbackEmail = currentUser.email ?: "admin@biblioteca.com"
                    Log.d("OverdueBooksFragment", "📧 Email de fallback: $fallbackEmail")
                    callback("Administrador", fallbackEmail)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("OverdueBooksFragment", "❌ Error obteniendo datos del admin: ${exception.message}", exception)
                val fallbackEmail = currentUser.email ?: "admin@biblioteca.com"
                Log.d("OverdueBooksFragment", "📧 Email de fallback por error: $fallbackEmail")
                callback("Administrador", fallbackEmail)
            }
    }
}
