package com.example.libraryinventoryapp.fragments

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.bumptech.glide.Glide
import com.example.libraryinventoryapp.R
import com.example.libraryinventoryapp.models.Book
import com.example.libraryinventoryapp.models.User
import com.example.libraryinventoryapp.utils.LibraryNotificationManager
import com.example.libraryinventoryapp.utils.EmailService
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.zxing.integration.android.IntentIntegrator
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.Locale

class EditBookFragment : Fragment() {

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
        private const val REQUEST_IMAGE_CAPTURE = 1002
        const val ARG_BOOK = "book"

        fun newInstance(book: Book): EditBookFragment {
            val fragment = EditBookFragment()
            val bundle = Bundle()
            bundle.putString(ARG_BOOK, book.id)
            fragment.arguments = bundle
            return fragment
        }
    }

    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private val emailService = EmailService()
    private var imageUri: Uri? = null
    private var originalImageUrl: String? = null
    private var editingBook: Book? = null

    // Views principales
    private lateinit var bookTitleInput: TextInputEditText
    private lateinit var bookAuthorInput: TextInputEditText
    private lateinit var bookIsbnInput: TextInputEditText
    private lateinit var bookDescriptionInput: TextInputEditText
    private lateinit var scanCodeButton: MaterialButton
    private lateinit var captureImageButton: MaterialButton
    private lateinit var updateBookButton: MaterialButton
    private lateinit var cancelButton: MaterialButton
    private lateinit var progressContainer: FrameLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var capturedImageView: ImageView
    private lateinit var selectCategoryButton: MaterialButton
    private lateinit var selectedCategoriesTextView: TextView
    private lateinit var bookQuantityInput: TextInputEditText
    private lateinit var bookCodeInput: TextInputEditText

    // Views para gestión completa de préstamos
    private lateinit var loanManagementSection: FrameLayout
    private lateinit var assignedUsersContainer: LinearLayout
    private lateinit var noUsersMessage: TextView
    private lateinit var userSearchAutoComplete: AutoCompleteTextView
    private lateinit var assignNewUserButton: MaterialButton

    // Variables para gestión de préstamos
    private var allUsers: MutableList<User> = mutableListOf()
    private var availableUserNames: MutableList<String> = mutableListOf()

    private var selectedCategories = mutableListOf<String>()
    private val categoriesArray by lazy { resources.getStringArray(R.array.book_categories) }

    // Modern Activity Result Launchers
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var barcodeLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        
        // Inicializar launchers modernos
        initializeLaunchers()
    }

    private fun initializeLaunchers() {
        // Camera launcher
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            try {
                if (result.resultCode == Activity.RESULT_OK) {
                    Log.d("EditBookFragment", "Camera result OK, processing image...")
                    
                    result.data?.let { data ->
                        Log.d("EditBookFragment", "Intent data received")
                        
                        val extras = data.extras
                        if (extras != null) {
                            Log.d("EditBookFragment", "Extras found: ${extras.keySet()}")
                            
                            val imageBitmap = extras.get("data") as? Bitmap
                            if (imageBitmap != null) {
                                Log.d("EditBookFragment", "Bitmap extracted successfully: ${imageBitmap.width}x${imageBitmap.height}")
                                
                                // UI operations on main thread
                                capturedImageView.post {
                                    capturedImageView.setImageBitmap(imageBitmap)
                                    capturedImageView.visibility = View.VISIBLE
                                }
                                
                                // Save image in background
                                Thread {
                                    val success = saveImageToStorage(imageBitmap)
                                    
                                    // Show result on main thread
                                    capturedImageView.post {
                                        if (success) {
                                            Toast.makeText(context, "✅ Imagen capturada exitosamente", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "⚠️ Error al guardar imagen", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }.start()
                                
                            } else {
                                Log.e("EditBookFragment", "No bitmap found in camera result")
                                Toast.makeText(context, "❌ Error: No se pudo obtener la imagen", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Log.e("EditBookFragment", "No extras in camera intent")
                            Toast.makeText(context, "❌ Error: Datos de imagen no encontrados", Toast.LENGTH_SHORT).show()
                        }
                    } ?: run {
                        Log.e("EditBookFragment", "No data in camera result")
                        Toast.makeText(context, "❌ Error: Sin datos de la cámara", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.w("EditBookFragment", "Camera cancelled or failed, result code: ${result.resultCode}")
                    Toast.makeText(context, "📷 Captura cancelada", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("EditBookFragment", "Exception in camera result handler", e)
                Toast.makeText(context, "❌ Error procesando imagen: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Permission launcher
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                launchCamera()
            } else {
                Toast.makeText(context, "❌ Permiso de cámara requerido", Toast.LENGTH_SHORT).show()
            }
        }

        // Barcode launcher (usando ZXing)
        barcodeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scannedCode = result.data?.getStringExtra("SCAN_RESULT")
                scannedCode?.let {
                    bookIsbnInput.setText(it)
                    Toast.makeText(context, "✅ ISBN escaneado: $it", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_book, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupEventListeners()
        loadBookData()
    }

    private fun initializeViews(view: View) {
        // Views principales
        bookTitleInput = view.findViewById(R.id.book_title_input)
        bookAuthorInput = view.findViewById(R.id.book_author_input)
        bookIsbnInput = view.findViewById(R.id.book_isbn_input)
        bookDescriptionInput = view.findViewById(R.id.book_description_input)
        scanCodeButton = view.findViewById(R.id.scan_code_button)
        captureImageButton = view.findViewById(R.id.capture_image_button)
        updateBookButton = view.findViewById(R.id.update_book_button)
        cancelButton = view.findViewById(R.id.cancel_button)
        progressContainer = view.findViewById(R.id.progress_container)
        progressBar = view.findViewById(R.id.progress_bar)
        capturedImageView = view.findViewById(R.id.captured_image_view)
        selectCategoryButton = view.findViewById(R.id.selectCategoryButton)
        selectedCategoriesTextView = view.findViewById(R.id.selectedCategoriesTextView)
        bookQuantityInput = view.findViewById(R.id.book_quantity_input)
        bookCodeInput = view.findViewById(R.id.book_code_input)

        // Views para gestión completa de préstamos
        loanManagementSection = view.findViewById(R.id.loan_management_section)
        assignedUsersContainer = view.findViewById(R.id.assigned_users_container)
        noUsersMessage = view.findViewById(R.id.no_users_message)
        userSearchAutoComplete = view.findViewById(R.id.user_search_autocomplete)
        assignNewUserButton = view.findViewById(R.id.assign_new_user_button)
    }

    private fun setupEventListeners() {
        scanCodeButton.setOnClickListener { scanBarcode() }
        captureImageButton.setOnClickListener { captureImage() }
        selectCategoryButton.setOnClickListener { showCategorySelectionDialog() }
        updateBookButton.setOnClickListener { showUpdateConfirmationDialog() }
        cancelButton.setOnClickListener { 
            parentFragmentManager.popBackStack()
        }

        // Listener para asignar nuevo usuario
        assignNewUserButton.setOnClickListener { assignNewUser() }
    }

    private fun loadBookData() {
        val bookId = arguments?.getString(ARG_BOOK) ?: return
        
        showProgressBar()
        firestore.collection("books").document(bookId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val book = document.toObject(Book::class.java)
                    book?.let {
                        it.id = document.id
                        editingBook = it
                        populateFields(it)
                        loadUsersWithLoans(it)
                    }
                }
                hideProgressBar()
            }
            .addOnFailureListener { e ->
                hideProgressBar()
                Toast.makeText(context, "Error al cargar datos: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("EditBookFragment", "Error loading book", e)
            }
    }

    private fun populateFields(book: Book) {
        bookTitleInput.setText(book.title)
        bookAuthorInput.setText(book.author)
        bookIsbnInput.setText(book.isbn)
        bookDescriptionInput.setText(book.description)
        bookQuantityInput.setText(book.quantity.toString())
        
        // Código del libro (campo opcional no en modelo actual)
        bookCodeInput.setText("") // Inicializar vacío
        
        // Categorías
        selectedCategories.clear()
        selectedCategories.addAll(book.categories)
        updateSelectedCategoriesDisplay()

        // Imagen
        originalImageUrl = book.imageUrl
        if (!book.imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(book.imageUrl)
                .placeholder(R.drawable.ic_book_default)
                .error(R.drawable.ic_book_default)
                .into(capturedImageView)
            capturedImageView.visibility = View.VISIBLE
        }
        
        Toast.makeText(context, "📚 Datos del libro cargados correctamente", Toast.LENGTH_SHORT).show()
    }

    private fun loadUsersWithLoans(book: Book) {
        // Limpiar contenedor
        assignedUsersContainer.removeAllViews()
        
        if (book.assignedTo.isNullOrEmpty()) {
            // No hay usuarios asignados - mostrar mensaje
            noUsersMessage.visibility = View.VISIBLE
            assignedUsersContainer.visibility = View.GONE
        } else {
            // Hay usuarios asignados - poblar lista
            noUsersMessage.visibility = View.GONE
            assignedUsersContainer.visibility = View.VISIBLE
            
            // Crear item para cada usuario asignado
            for (i in book.assignedTo.indices) {
                val userId = book.assignedTo[i]
                val userName = book.assignedWithNames?.getOrNull(i) ?: "Usuario desconocido"
                val userEmail = book.assignedToEmails?.getOrNull(i) ?: ""
                val assignedDate = book.assignedDates?.getOrNull(i)
                val expirationDate = book.loanExpirationDates?.getOrNull(i)
                
                addUserItemToContainer(userId, userName, userEmail, assignedDate, expirationDate, book)
            }
        }
        
        // Cargar usuarios disponibles para asignación
        loadAvailableUsers()
    }

    private fun addUserItemToContainer(
        userId: String, 
        userName: String, 
        userEmail: String,
        assignedDate: com.google.firebase.Timestamp?,
        expirationDate: com.google.firebase.Timestamp?,
        book: Book
    ) {
        // Inflar el layout del item de usuario
        val userItemView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_edit_assigned_user, assignedUsersContainer, false)
        
        // Referencias a los elementos del item
        val userInitialTextView = userItemView.findViewById<TextView>(R.id.user_initial)
        val userNameTextView = userItemView.findViewById<TextView>(R.id.user_name)
        val userEmailTextView = userItemView.findViewById<TextView>(R.id.user_email)
        val assignedDateTextView = userItemView.findViewById<TextView>(R.id.assigned_date)
        val expirationDateTextView = userItemView.findViewById<TextView>(R.id.expiration_date)
        val expirationStatusTextView = userItemView.findViewById<TextView>(R.id.expiration_status)
        val statusIndicator = userItemView.findViewById<View>(R.id.status_indicator)
        val changeDateButton = userItemView.findViewById<MaterialButton>(R.id.change_date_button)
        val unassignButton = userItemView.findViewById<MaterialButton>(R.id.unassign_button)
        
        // Poblar datos del usuario
        userNameTextView.text = userName
        userInitialTextView.text = getUserInitials(userName)
        if (userEmail.isNotEmpty()) {
            userEmailTextView.text = userEmail
        } else {
            userEmailTextView.visibility = View.GONE
        }
        
        // Fechas
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        
        if (assignedDate != null) {
            assignedDateTextView.text = "📅 Asignado: ${dateFormat.format(assignedDate.toDate())}"
        } else {
            assignedDateTextView.text = "📅 Asignado: No disponible"
        }
        
        if (expirationDate != null) {
            expirationDateTextView.text = "⏰ Vence: ${dateFormat.format(expirationDate.toDate())}"
            
            // Calcular estado de vencimiento
            val currentTime = System.currentTimeMillis()
            val timeDiff = expirationDate.toDate().time - currentTime
            
            when {
                timeDiff < 0 -> {
                    // Vencido
                    val daysOverdue = kotlin.math.abs(timeDiff) / (24 * 60 * 60 * 1000L)
                    expirationStatusTextView.visibility = View.VISIBLE
                    expirationStatusTextView.text = "🚨 VENCIDO HACE ${daysOverdue} DÍAS"
                    expirationStatusTextView.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                    statusIndicator.setBackgroundColor(resources.getColor(android.R.color.holo_red_dark, null))
                }
                timeDiff <= (24 * 60 * 60 * 1000L) -> {
                    // Vence hoy o mañana
                    val daysLeft = (timeDiff / (24 * 60 * 60 * 1000L)).toInt()
                    expirationStatusTextView.visibility = View.VISIBLE
                    expirationStatusTextView.text = if (daysLeft == 0) "⚠️ VENCE HOY" else "⏰ VENCE MAÑANA"
                    expirationStatusTextView.setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))
                    statusIndicator.setBackgroundColor(resources.getColor(android.R.color.holo_orange_dark, null))
                }
                timeDiff <= (5 * 24 * 60 * 60 * 1000L) -> {
                    // Vence en 2-5 días
                    val daysLeft = (timeDiff / (24 * 60 * 60 * 1000L)).toInt()
                    expirationStatusTextView.visibility = View.VISIBLE
                    expirationStatusTextView.text = "📋 VENCE EN $daysLeft DÍAS"
                    expirationStatusTextView.setTextColor(resources.getColor(android.R.color.darker_gray, null))
                    statusIndicator.setBackgroundColor(resources.getColor(android.R.color.holo_orange_light, null))
                }
                else -> {
                    // Todo bien
                    expirationStatusTextView.visibility = View.GONE
                    statusIndicator.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark, null))
                }
            }
        } else {
            expirationDateTextView.text = "⏰ Sin fecha límite"
            expirationStatusTextView.visibility = View.GONE
            statusIndicator.setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
        }
        
        // Configurar listeners de botones
        changeDateButton.setOnClickListener {
            showChangeDateDialog(book, userId, userName)
        }
        
        unassignButton.setOnClickListener {
            showUnassignConfirmationDialog(book, userId, userName)
        }
        
        // Agregar el item al contenedor
        assignedUsersContainer.addView(userItemView)
    }

    /**
     * Extrae las iniciales del nombre del usuario
     * @param fullName Nombre completo del usuario
     * @return Iniciales (máximo 2 caracteres)
     */
    private fun getUserInitials(fullName: String): String {
        if (fullName.isBlank()) return "U"
        
        val nameParts = fullName.trim().split(" ").filter { it.isNotBlank() }
        return when {
            nameParts.isEmpty() -> "U"
            nameParts.size == 1 -> {
                // Solo un nombre: tomar la primera letra
                nameParts[0].first().uppercaseChar().toString()
            }
            else -> {
                // Múltiples nombres: tomar primera letra del primer y segundo nombre
                val firstInitial = nameParts[0].first().uppercaseChar()
                val secondInitial = nameParts[1].first().uppercaseChar()
                "$firstInitial$secondInitial"
            }
        }
    }

    private fun loadAvailableUsers() {
        firestore.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                allUsers.clear()
                availableUserNames.clear()
                
                for (document in documents) {
                    val user = document.toObject(User::class.java)
                    if (user.role != "admin") { // Solo usuarios normales
                        allUsers.add(user)
                        availableUserNames.add(user.name)
                    }
                }
                
                // Configurar AutoCompleteTextView
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, availableUserNames)
                userSearchAutoComplete.setAdapter(adapter)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al cargar usuarios: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Función para asignar nuevo usuario (reemplaza la funcionalidad anterior)
    private fun assignNewUser() {
        val selectedUserName = userSearchAutoComplete.text.toString().trim()
        
        if (selectedUserName.isEmpty()) {
            Toast.makeText(requireContext(), "❌ Selecciona un usuario", Toast.LENGTH_SHORT).show()
            return
        }
        
        val selectedUser = allUsers.find { it.name == selectedUserName }
        if (selectedUser == null) {
            Toast.makeText(requireContext(), "❌ Usuario no encontrado", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Verificar si el usuario ya está asignado
        if (editingBook?.assignedTo?.contains(selectedUser.uid) == true) {
            Toast.makeText(requireContext(), "⚠️ El usuario ya tiene este libro asignado", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Mostrar DatePicker para seleccionar fecha de devolución
        showLoanExpirationDatePicker(selectedUser)
    }

    private fun showLoanExpirationDatePicker(user: User) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 15) // Sugerir 15 días por defecto

        // Calcular fecha máxima: 1 mes (30 días) desde hoy
        val maxCalendar = Calendar.getInstance()
        maxCalendar.add(Calendar.DAY_OF_YEAR, 30)

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val expirationDate = Calendar.getInstance()
                expirationDate.set(year, month, dayOfMonth)
                assignUserWithExpiration(user, expirationDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis()
            datePicker.maxDate = maxCalendar.timeInMillis // MÁXIMO 30 DÍAS
            setTitle("📅 Fecha de devolución para ${user.name} (máx. 30 días)")
            show()
        }
    }

    private fun assignUserWithExpiration(user: User, expirationDate: Calendar) {
        val book = editingBook ?: return

        // Crear listas actualizadas
        val updatedAssignedTo = (book.assignedTo ?: emptyList()).toMutableList().apply { add(user.uid) }
        val updatedAssignedWithNames = (book.assignedWithNames ?: emptyList()).toMutableList().apply { add(user.name) }
        val updatedAssignedToEmails = (book.assignedToEmails ?: emptyList()).toMutableList().apply { add(user.email) }
        val updatedAssignedDates = (book.assignedDates ?: emptyList()).toMutableList().apply { 
            add(com.google.firebase.Timestamp.now()) 
        }
        val updatedLoanExpirationDates = (book.loanExpirationDates ?: emptyList()).toMutableList().apply { 
            add(com.google.firebase.Timestamp(expirationDate.time)) 
        }

        // Calcular status basado en disponibilidad real
        val totalCopies = book.quantity
        val newAssignedCopies = updatedAssignedTo.size
        val newStatus = if (newAssignedCopies >= totalCopies) "No disponible" else "Disponible"

        val updates = hashMapOf<String, Any?>(
            "assignedTo" to updatedAssignedTo,
            "assignedWithNames" to updatedAssignedWithNames,
            "assignedToEmails" to updatedAssignedToEmails,
            "assignedDates" to updatedAssignedDates,
            "loanExpirationDates" to updatedLoanExpirationDates,
            "status" to newStatus,
            "lastEditedDate" to com.google.firebase.Timestamp.now()
        )

        firestore.collection("books").document(book.id)
            .update(updates)
            .addOnSuccessListener {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                Toast.makeText(requireContext(), 
                    "✅ Usuario \"${user.name}\" asignado hasta ${dateFormat.format(expirationDate.time)}", 
                    Toast.LENGTH_LONG).show()
                    
                // 🔔 NUEVA FUNCIONALIDAD: Programar notificaciones push
                try {
                    val notificationManager = LibraryNotificationManager(requireContext())
                    
                    // 📚 Notificación inmediata de asignación
                    notificationManager.scheduleBookAssignmentNotification(
                        bookId = book.id,
                        bookTitle = book.title,
                        bookAuthor = book.author,
                        userId = user.uid,
                        userName = user.name,
                        expirationDate = com.google.firebase.Timestamp(expirationDate.time)
                    )
                    
                    // 📅 Notificaciones programadas según vencimiento
                    notificationManager.scheduleBookLoanNotifications(
                        bookId = book.id,
                        bookTitle = book.title,
                        bookAuthor = book.author,
                        userId = user.uid,
                        userName = user.name,
                        expirationDate = com.google.firebase.Timestamp(expirationDate.time)
                    )
                    Log.d("EditBookFragment", "📱 Notificaciones programadas para ${user.name} - ${book.title}")
                } catch (e: Exception) {
                    Log.e("EditBookFragment", "❌ Error programando notificaciones: ${e.message}")
                }
                
                // 📧 ENVÍO DE CORREO AL USUARIO
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        // Obtener información del admin actual
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        if (currentUser != null) {
                            firestore.collection("users").document(currentUser.uid).get()
                                .addOnSuccessListener { adminDoc ->
                                    val adminName = adminDoc.getString("name") ?: "Admin"
                                    val adminEmail = adminDoc.getString("email") ?: currentUser.email ?: "admin@biblioteca.com"
                                    
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        val result = emailService.sendBookAssignmentEmail(
                                            adminEmail = adminEmail,
                                            userEmail = user.email,
                                            userName = user.name,
                                            bookTitle = book.title,
                                            bookAuthor = book.author,
                                            adminName = adminName
                                        )
                                        
                                        if (result.isSuccess) {
                                            Log.d("EmailService", "✅ SendGrid: Correo enviado exitosamente a ${user.name}")
                                        } else {
                                            Log.e("EmailService", "❌ SendGrid Error: ${result.exceptionOrNull()?.message}")
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("EmailService", "❌ Error obteniendo datos del admin: ${e.message}")
                                }
                        }
                    } catch (e: Exception) {
                        Log.e("EmailService", "❌ Error enviando correo: ${e.message}")
                    }
                }
                    
                userSearchAutoComplete.setText("")
                loadBookData() // Recargar datos para actualizar la vista
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "❌ Error al asignar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Nueva función para mostrar DatePicker para cambiar fecha de un usuario específico
    private fun showChangeDateDialog(book: Book, userId: String, userName: String) {
        val userIndex = book.assignedTo?.indexOf(userId) ?: -1
        if (userIndex == -1) {
            Toast.makeText(requireContext(), "❌ Error: Usuario no encontrado", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtener fecha actual o sugerir 15 días
        val calendar = Calendar.getInstance()
        val currentExpirationDate = book.loanExpirationDates?.getOrNull(userIndex)
        if (currentExpirationDate != null) {
            calendar.time = currentExpirationDate.toDate()
            // Si la fecha actual está más allá de 30 días, ajustar a 15 días desde hoy
            val maxAllowedDate = Calendar.getInstance()
            maxAllowedDate.add(Calendar.DAY_OF_YEAR, 30)
            if (calendar.timeInMillis > maxAllowedDate.timeInMillis) {
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_YEAR, 15)
            }
        } else {
            calendar.add(Calendar.DAY_OF_YEAR, 15)
        }

        // Calcular fecha máxima: 1 mes (30 días) desde hoy
        val maxCalendar = Calendar.getInstance()
        maxCalendar.add(Calendar.DAY_OF_YEAR, 30)

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val newDate = Calendar.getInstance()
                newDate.set(year, month, dayOfMonth)
                updateUserLoanDate(book, userId, userName, userIndex, newDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis()
            datePicker.maxDate = maxCalendar.timeInMillis // MÁXIMO 30 DÍAS
            setTitle("📅 Nueva Fecha para $userName (máx. 30 días)")
            show()
        }
    }

    // Nueva función para actualizar fecha de préstamo de un usuario específico
    private fun updateUserLoanDate(book: Book, userId: String, userName: String, userIndex: Int, newDate: Calendar) {
        val updatedLoanExpirationDates = book.loanExpirationDates?.toMutableList()
        updatedLoanExpirationDates?.set(userIndex, com.google.firebase.Timestamp(newDate.time))

        val updates = hashMapOf<String, Any?>(
            "loanExpirationDates" to updatedLoanExpirationDates,
            "lastEditedDate" to com.google.firebase.Timestamp.now()
        )

        firestore.collection("books").document(book.id)
            .update(updates)
            .addOnSuccessListener {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                Toast.makeText(requireContext(), 
                    "✅ Fecha actualizada para $userName: ${dateFormat.format(newDate.time)}", 
                    Toast.LENGTH_LONG).show()
                
                // 🔔 REPROGRAMAR NOTIFICACIONES: Cancelar viejas y crear nuevas
                try {
                    val notificationManager = LibraryNotificationManager(requireContext())
                    
                    // 🗑️ Cancelar notificaciones anteriores
                    notificationManager.cancelBookNotifications(book.id, userId)
                    Log.d("EditBookFragment", "🗑️ Notificaciones anteriores canceladas para $userName")
                    
                    // 📅 Programar nuevas notificaciones con la fecha actualizada
                    notificationManager.scheduleBookLoanNotifications(
                        bookId = book.id,
                        bookTitle = book.title,
                        bookAuthor = book.author,
                        userId = userId,
                        userName = userName,
                        expirationDate = com.google.firebase.Timestamp(newDate.time)
                    )
                    Log.d("EditBookFragment", "📱 Nuevas notificaciones programadas para $userName - ${dateFormat.format(newDate.time)}")
                    
                    // 🔔 Mostrar confirmación de reprogramación
                    Toast.makeText(requireContext(), 
                        "🔔 Notificaciones reprogramadas para la nueva fecha", 
                        Toast.LENGTH_SHORT).show()
                        
                } catch (e: Exception) {
                    Log.e("EditBookFragment", "❌ Error reprogramando notificaciones: ${e.message}")
                }
                
                loadBookData() // Recargar datos para actualizar la vista
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "❌ Error al actualizar fecha: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Nueva función para mostrar confirmación de desasignación desde botón individual
    private fun showUnassignConfirmationDialog(book: Book, userId: String, userName: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Confirmar Desasignación")
            .setMessage("¿Estás seguro de que quieres desasignar el libro \"${book.title}\" del usuario \"$userName\"?\n\nEsta acción es irreversible.")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("✅ SÍ, DESASIGNAR") { _, _ ->
                unassignUserFromBook(book, userId, userName)
            }
            .setNegativeButton("❌ CANCELAR", null)
            .show()
    }

    // Nueva función para desasignar usuario específico
    private fun unassignUserFromBook(book: Book, userId: String, userName: String) {
        val userIndex = book.assignedTo?.indexOf(userId) ?: -1
        if (userIndex == -1) {
            Toast.makeText(requireContext(), "❌ Error: Usuario no encontrado", Toast.LENGTH_SHORT).show()
            return
        }

        // Crear copias mutables y remover el usuario
        val updatedAssignedTo = book.assignedTo?.toMutableList()
        val updatedAssignedWithNames = book.assignedWithNames?.toMutableList()
        val updatedAssignedToEmails = book.assignedToEmails?.toMutableList()
        val updatedAssignedDates = book.assignedDates?.toMutableList()
        val updatedLoanExpirationDates = book.loanExpirationDates?.toMutableList()

        // Remover el usuario de todas las listas
        updatedAssignedTo?.removeAt(userIndex)
        updatedAssignedWithNames?.removeAt(userIndex)
        updatedAssignedToEmails?.removeAt(userIndex)
        updatedAssignedDates?.removeAt(userIndex)
        updatedLoanExpirationDates?.removeAt(userIndex)

        // Determinar valores finales
        val finalAssignedTo = if (updatedAssignedTo?.isEmpty() == true) null else updatedAssignedTo
        val finalAssignedWithNames = if (updatedAssignedWithNames?.isEmpty() == true) null else updatedAssignedWithNames
        val finalAssignedToEmails = if (updatedAssignedToEmails?.isEmpty() == true) null else updatedAssignedToEmails
        val finalAssignedDates = if (updatedAssignedDates?.isEmpty() == true) null else updatedAssignedDates
        val finalLoanExpirationDates = if (updatedLoanExpirationDates?.isEmpty() == true) null else updatedLoanExpirationDates
        
        // Calcular status basado en disponibilidad real
        val totalCopies = book.quantity
        val remainingAssignedCopies = finalAssignedTo?.size ?: 0
        val newStatus = if (remainingAssignedCopies >= totalCopies) "No disponible" else "Disponible"

        val updates = mutableMapOf<String, Any?>()
        updates["assignedTo"] = finalAssignedTo
        updates["assignedWithNames"] = finalAssignedWithNames
        updates["assignedToEmails"] = finalAssignedToEmails
        updates["assignedDates"] = finalAssignedDates
        updates["loanExpirationDates"] = finalLoanExpirationDates
        updates["status"] = newStatus
        updates["lastEditedDate"] = com.google.firebase.Timestamp.now()

        firestore.collection("books").document(book.id)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "✅ Usuario \"$userName\" desasignado exitosamente", Toast.LENGTH_LONG).show()
                
                // 🔔 CANCELAR NOTIFICACIONES: Al desasignar, cancelar todas las notificaciones programadas
                try {
                    val notificationManager = LibraryNotificationManager(requireContext())
                    notificationManager.cancelBookNotifications(book.id, userId)
                    Log.d("EditBookFragment", "🗑️ Notificaciones canceladas para $userName - ${book.title}")
                } catch (e: Exception) {
                    Log.e("EditBookFragment", "❌ Error cancelando notificaciones: ${e.message}")
                }
                
                loadBookData() // Recargar datos para actualizar la vista
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "❌ Error al desasignar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Funciones obsoletas eliminadas - reemplazadas por la nueva gestión de préstamos

    private fun showCategorySelectionDialog() {
        val checkedItems = BooleanArray(categoriesArray.size) { index ->
            selectedCategories.contains(categoriesArray[index])
        }

        AlertDialog.Builder(requireContext())
            .setTitle("🏷️ Seleccionar Categorías")
            .setMultiChoiceItems(categoriesArray, checkedItems) { _, which, isChecked ->
                if (isChecked) {
                    selectedCategories.add(categoriesArray[which])
                } else {
                    selectedCategories.remove(categoriesArray[which])
                }
            }
            .setPositiveButton("Aceptar") { _, _ ->
                updateSelectedCategoriesDisplay()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateSelectedCategoriesDisplay() {
        selectedCategoriesTextView.text = if (selectedCategories.isEmpty()) {
            "Categorías: Ninguna seleccionada"
        } else {
            "Categorías seleccionadas: ${selectedCategories.joinToString(", ")}"
        }
    }

    private fun scanBarcode() {
        try {
            val integrator = IntentIntegrator.forSupportFragment(this)
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES)
            integrator.setPrompt("📚 Escanea el código de barras del ISBN")
            integrator.setCameraId(0) // Usar cámara trasera
            integrator.setBeepEnabled(true)
            integrator.setBarcodeImageEnabled(true)
            integrator.setOrientationLocked(false)
            
            val intent = integrator.createScanIntent()
            barcodeLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e("EditBookFragment", "Error launching barcode scanner", e)
            Toast.makeText(context, "❌ Error al iniciar escáner: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun captureImage() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        
        launchCamera()
    }

    private fun launchCamera() {
        try {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent.resolveActivity(requireActivity().packageManager) != null) {
                cameraLauncher.launch(takePictureIntent)
            } else {
                Toast.makeText(context, "❌ No hay app de cámara disponible", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("EditBookFragment", "Error launching camera", e)
            Toast.makeText(context, "❌ Error al abrir cámara: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showUpdateConfirmationDialog() {
        val book = editingBook ?: return
        
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Actualización")
            .setMessage("¿Estás seguro de que quieres actualizar la información del libro '${book.title}'?")
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton("✅ SÍ, ACTUALIZAR") { _, _ ->
                updateBook()
            }
            .setNegativeButton("❌ CANCELAR", null)
            .show()
    }

    private fun updateBook() {
        val book = editingBook ?: return

        if (!validateFields()) return

        showProgressBar()

        val updates = hashMapOf<String, Any>(
            "title" to bookTitleInput.text.toString().trim(),
            "author" to bookAuthorInput.text.toString().trim(),
            "isbn" to bookIsbnInput.text.toString().trim(),
            "description" to bookDescriptionInput.text.toString().trim(),
            "categories" to selectedCategories,
            "quantity" to try { bookQuantityInput.text.toString().toInt() } catch (e: Exception) { 0 },
            "lastEditedDate" to Timestamp.now()
        )
        
        // Solo agregar código si no está vacío (campo extra opcional)
        val codeText = bookCodeInput.text.toString().trim()
        if (codeText.isNotEmpty()) {
            updates["code"] = codeText
        }

        // Si hay nueva imagen, subirla primero
        if (imageUri != null) {
            uploadImageAndUpdateBook(book, updates)
        } else {
            updateBookInFirestore(book, updates)
        }
    }

    private fun validateFields(): Boolean {
        if (bookTitleInput.text.toString().trim().isEmpty()) {
            Toast.makeText(context, "❌ El título es obligatorio", Toast.LENGTH_SHORT).show()
            return false
        }
        if (bookAuthorInput.text.toString().trim().isEmpty()) {
            Toast.makeText(context, "❌ El autor es obligatorio", Toast.LENGTH_SHORT).show()
            return false
        }
        val quantityText = bookQuantityInput.text.toString()
        val quantity = try { quantityText.toInt() } catch (e: Exception) { -1 }
        if (quantity < 0) {
            Toast.makeText(context, "❌ La cantidad debe ser un número válido", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun uploadImageAndUpdateBook(book: Book, updates: HashMap<String, Any>) {
        val imageRef = storage.reference.child("book_images/${book.id}_${System.currentTimeMillis()}.jpg")
        
        imageRef.putFile(imageUri!!)
            .addOnSuccessListener { taskSnapshot ->
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    updates["imageUrl"] = uri.toString()
                    updateBookInFirestore(book, updates)
                }
            }
            .addOnFailureListener { e ->
                hideProgressBar()
                Toast.makeText(context, "❌ Error al subir imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("EditBookFragment", "Error uploading image", e)
            }
    }

    private fun updateBookInFirestore(book: Book, updates: HashMap<String, Any>) {
        firestore.collection("books").document(book.id).update(updates)
            .addOnSuccessListener {
                hideProgressBar()
                Toast.makeText(context, "✅ Libro actualizado exitosamente", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                hideProgressBar()
                Toast.makeText(context, "❌ Error al actualizar: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("EditBookFragment", "Error updating book", e)
            }
    }

    // onActivityResult eliminated - now using modern ActivityResultLaunchers

    private fun saveImageToStorage(bitmap: Bitmap): Boolean {
        return try {
            Log.d("EditBookFragment", "Starting image save process...")
            
            // Usar almacenamiento interno de la app - no requiere permisos
            val internalDir = File(requireContext().filesDir, "book_images")
            if (!internalDir.exists()) {
                val created = internalDir.mkdirs()
                Log.d("EditBookFragment", "Directory created: $created")
            }
            
            val fileName = "book_${System.currentTimeMillis()}.jpg"
            val file = File(internalDir, fileName)
            Log.d("EditBookFragment", "Saving image to: ${file.absolutePath}")
            
            FileOutputStream(file).use { out ->
                val compressionSuccess = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                if (!compressionSuccess) {
                    Log.e("EditBookFragment", "Failed to compress bitmap")
                    return false
                }
                out.flush()
                Log.d("EditBookFragment", "Bitmap compressed and flushed successfully")
            }
            
            // Verify file was created and has content
            if (!file.exists()) {
                Log.e("EditBookFragment", "Image file does not exist after creation")
                return false
            }
            
            val fileSize = file.length()
            if (fileSize == 0L) {
                Log.e("EditBookFragment", "Image file is empty")
                return false
            }
            
            Log.d("EditBookFragment", "File created successfully with size: $fileSize bytes")
            
            // Crear URI usando FileProvider con files-path
            imageUri = FileProvider.getUriForFile(
                requireContext(), 
                "${requireContext().packageName}.fileprovider", 
                file
            )
            
            Log.d("EditBookFragment", "FileProvider URI created: $imageUri")
            
            // Verificar que el URI es válido
            val inputStream = requireContext().contentResolver.openInputStream(imageUri!!)
            inputStream?.use {
                Log.d("EditBookFragment", "URI verification successful - can read ${it.available()} bytes")
            }
            
            Log.d("EditBookFragment", "Image saved and verified successfully")
            true
            
        } catch (e: Exception) {
            Log.e("EditBookFragment", "Error saving image to storage", e)
            Log.e("EditBookFragment", "Exception details: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private fun showProgressBar() {
        progressContainer.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        progressContainer.visibility = View.GONE
    }

    // Funciones obsoletas eliminadas - gestión de préstamos ahora es individual por usuario
}