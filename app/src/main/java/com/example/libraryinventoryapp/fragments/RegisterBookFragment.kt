package com.example.libraryinventoryapp.fragments

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.libraryinventoryapp.LoginActivity
import com.example.libraryinventoryapp.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.zxing.integration.android.IntentIntegrator
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class RegisterBookFragment : Fragment() {

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
        private const val REQUEST_IMAGE_CAPTURE = 1002
        private const val SCAN_BARCODE_REQUEST_CODE = 1003
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var imageUri: Uri? = null

    // Views principales
    private lateinit var btnBack: ImageButton
    private lateinit var bookTitleInput: TextInputEditText
    private lateinit var bookAuthorInput: TextInputEditText
    private lateinit var bookIsbnInput: TextInputEditText
    private lateinit var bookDescriptionInput: TextInputEditText
    private lateinit var scanCodeButton: MaterialButton
    private lateinit var captureImageButton: MaterialButton
    private lateinit var registerBookButton: MaterialButton
    private lateinit var progressContainer: FrameLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var capturedImageView: ImageView
    private lateinit var selectCategoryButton: MaterialButton
    private lateinit var selectedCategoriesTextView: TextView
    private lateinit var bookQuantityInput: TextInputEditText
    private lateinit var bookCodeInput: TextInputEditText

    private var selectedCategories = mutableListOf<String>()
    private val categoriesArray by lazy { resources.getStringArray(R.array.book_categories) }

    // Modern Activity Result Launchers
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var barcodeLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
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
                    Log.d("RegisterBookFragment", "Camera result OK, processing image...")
                    
                    result.data?.let { data ->
                        val extras = data.extras
                        if (extras != null) {
                            val imageBitmap = extras.get("data") as? Bitmap
                            if (imageBitmap != null) {
                                Log.d("RegisterBookFragment", "Bitmap extracted successfully: ${imageBitmap.width}x${imageBitmap.height}")
                                
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
                                Log.e("RegisterBookFragment", "No bitmap found in camera result")
                                Toast.makeText(context, "❌ Error: No se pudo obtener la imagen", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Log.e("RegisterBookFragment", "No extras in camera intent")
                            Toast.makeText(context, "❌ Error: Datos de imagen no encontrados", Toast.LENGTH_SHORT).show()
                        }
                    } ?: run {
                        Log.e("RegisterBookFragment", "No data in camera result")
                        Toast.makeText(context, "❌ Error: Sin datos de la cámara", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.w("RegisterBookFragment", "Camera cancelled or failed, result code: ${result.resultCode}")
                    Toast.makeText(context, "📷 Captura cancelada", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("RegisterBookFragment", "Exception in camera result handler", e)
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
        val view = inflater.inflate(R.layout.fragment_register_book, container, false)

        // Initialize views
        btnBack = view.findViewById(R.id.btn_back)
        bookTitleInput = view.findViewById(R.id.book_title_input)
        bookAuthorInput = view.findViewById(R.id.book_author_input)
        bookIsbnInput = view.findViewById(R.id.book_isbn_input)
        bookDescriptionInput = view.findViewById(R.id.book_description_input)
        scanCodeButton = view.findViewById(R.id.scan_code_button)
        captureImageButton = view.findViewById(R.id.capture_image_button)
        registerBookButton = view.findViewById(R.id.register_book_button)
        progressContainer = view.findViewById(R.id.progress_container)
        progressBar = view.findViewById(R.id.progress_bar)
        capturedImageView = view.findViewById(R.id.captured_image_view)
        selectCategoryButton = view.findViewById(R.id.selectCategoryButton)
        selectedCategoriesTextView = view.findViewById(R.id.selectedCategoriesTextView)
        bookQuantityInput = view.findViewById(R.id.book_quantity_input)
        bookCodeInput = view.findViewById(R.id.book_code_input)

        // Verificar permisos de almacenamiento
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                CAMERA_PERMISSION_REQUEST_CODE)
        }

        // 🔙 Handle back button navigation
        btnBack.setOnClickListener {
            Log.d("RegisterBookFragment", "🔙 Back button clicked - Returning to previous screen")
            parentFragmentManager.popBackStack()
        }

        // Handle QR/barcode scanning
        scanCodeButton.setOnClickListener {
            scanBarcode()
        }

        // Configurar el botón para seleccionar múltiples categorías
        selectCategoryButton.setOnClickListener {
            showCategorySelectionDialog()
        }

        // Handle image capture
        captureImageButton.setOnClickListener {
            if (imageUri != null) {
                // Si ya hay una imagen capturada, limpiar la vista y permitir nueva captura
                capturedImageView.setImageBitmap(null) // Limpiar la imagen anterior
                imageUri = null // Limpiar URI
                Toast.makeText(context, "Captura una nueva imagen", Toast.LENGTH_SHORT).show()
            }
            captureImage() // Llama a la función para capturar la imagen
        }

        // Handle book registration
        registerBookButton.setOnClickListener {
            val title = bookTitleInput.text.toString().trim()
            val author = bookAuthorInput.text.toString().trim()
            val isbn = bookIsbnInput.text.toString().trim()
            val description = bookDescriptionInput.text.toString().trim()
            val quantity = bookQuantityInput.text.toString()
            val code = bookCodeInput.text.toString()

            if (title.isNotEmpty() && author.isNotEmpty() && isbn.isNotEmpty() && imageUri != null && selectedCategories.isNotEmpty() && description.isNotEmpty()) {
                showProgressBar()
                uploadBookToFirebase(title, author, isbn, description, selectedCategories, quantity, code)
            } else {
                Toast.makeText(context, "Todos los campos son obligatorios y debe capturar una imagen", Toast.LENGTH_SHORT).show()
            }
        }

        return view
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
            Log.e("RegisterBookFragment", "Error launching barcode scanner", e)
            Toast.makeText(context, "❌ Error al iniciar escáner: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCategorySelectionDialog() {
        val selectedItems = BooleanArray(categoriesArray.size) { i ->
            selectedCategories.contains(categoriesArray[i])
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Selecciona Categorías")
            .setMultiChoiceItems(categoriesArray, selectedItems) { dialog, which, isChecked ->
                if (isChecked) {
                    // Si está marcada, la agregamos a las seleccionadas
                    selectedCategories.add(categoriesArray[which])
                } else {
                    // Si está desmarcada, la quitamos de las seleccionadas
                    selectedCategories.remove(categoriesArray[which])
                }
            }
            .setPositiveButton("Aceptar") { dialog, _ ->
                // Mostrar las categorías seleccionadas en el TextView
                selectedCategoriesTextView.text = "Categorías seleccionadas: ${selectedCategories.joinToString(", ")}"
            }
            .setNegativeButton("Cancelar", null)
            .setNeutralButton("Borrar Selección") { dialog, _ ->
                // Limpiar la selección de categorías
                selectedCategories.clear()
                selectedCategoriesTextView.text = "Categorías seleccionadas: Ninguna"
            }
            .show()
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
            Log.e("RegisterBookFragment", "Error launching camera", e)
            Toast.makeText(context, "❌ Error al abrir cámara: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // onActivityResult eliminated - now using modern ActivityResultLaunchers

    private fun saveImageToStorage(bitmap: Bitmap): Boolean {
        return try {
            Log.d("RegisterBookFragment", "Starting image save process...")
            
            // Usar almacenamiento interno de la app - no requiere permisos
            val internalDir = File(requireContext().filesDir, "book_images")
            if (!internalDir.exists()) {
                val created = internalDir.mkdirs()
                Log.d("RegisterBookFragment", "Directory created: $created")
            }
            
            val fileName = "book_${System.currentTimeMillis()}.jpg"
            val file = File(internalDir, fileName)
            Log.d("RegisterBookFragment", "Saving image to: ${file.absolutePath}")
            
            FileOutputStream(file).use { out ->
                val compressionSuccess = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                if (!compressionSuccess) {
                    Log.e("RegisterBookFragment", "Failed to compress bitmap")
                    return false
                }
                out.flush()
                Log.d("RegisterBookFragment", "Bitmap compressed and flushed successfully")
            }
            
            // Verify file was created and has content
            if (!file.exists()) {
                Log.e("RegisterBookFragment", "Image file does not exist after creation")
                return false
            }
            
            val fileSize = file.length()
            if (fileSize == 0L) {
                Log.e("RegisterBookFragment", "Image file is empty")
                return false
            }
            
            Log.d("RegisterBookFragment", "File created successfully with size: $fileSize bytes")
            
            // Crear URI usando FileProvider con files-path
            imageUri = FileProvider.getUriForFile(
                requireContext(), 
                "${requireContext().packageName}.fileprovider", 
                file
            )
            
            Log.d("RegisterBookFragment", "FileProvider URI created: $imageUri")
            
            // Verificar que el URI es válido
            val inputStream = requireContext().contentResolver.openInputStream(imageUri!!)
            inputStream?.use {
                Log.d("RegisterBookFragment", "URI verification successful - can read ${it.available()} bytes")
            }
            
            Log.d("RegisterBookFragment", "Image saved and verified successfully")
            true
            
        } catch (e: Exception) {
            Log.e("RegisterBookFragment", "Error saving image to storage", e)
            Log.e("RegisterBookFragment", "Exception details: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private fun uploadBookToFirebase(
        title: String,
        author: String,
        isbn: String,
        description: String,
        categories: List<String>,
        quantity: String,
        code: String
    ) {
        val finalQuantity = if (quantity.isBlank()) 1 else quantity.toIntOrNull() ?: 1

        if (imageUri != null) {
            // Subir la imagen a Firebase Storage
            val ref = storage.reference.child("books/${System.currentTimeMillis()}.jpg")
            ref.putFile(imageUri!!).addOnSuccessListener { taskSnapshot ->
                // Obtener la URL de descarga de la imagen
                ref.downloadUrl.addOnSuccessListener { uri ->
                    // Guardar detalles del libro en Firestore
                    val book = hashMapOf(
                        "title" to title,
                        "author" to author,
                        "isbn" to isbn,
                        "description" to description,
                        "categories" to categories,
                        "imageUrl" to uri.toString(),
                        "assignedTo" to null,
                        "assignedWithNames" to null,
                        "assignedToEmails" to null,
                        "assignedDates" to null,
                        "loanExpirationDates" to null,
                        "status" to "Disponible",
                        "quantity" to finalQuantity,
                        "code" to code,
                        "createdDate" to com.google.firebase.Timestamp.now(),
                        "lastEditedDate" to null
                    )
                    firestore.collection("books").add(book)
                        .addOnSuccessListener {
                            // Notificar al usuario y limpiar campos
                            hideProgressBar()
                            Toast.makeText(context, "Libro registrado con éxito", Toast.LENGTH_SHORT).show()
                            clearFields()
                        }
                        .addOnFailureListener { e ->
                            hideProgressBar()
                            Log.e("Firebase", "Error al registrar el libro", e)
                            Toast.makeText(context, "Error al registrar el libro", Toast.LENGTH_SHORT).show()
                        }
                }
            }.addOnFailureListener { e ->
                hideProgressBar()
                Log.e("Firebase", "Error al subir la imagen", e)
                Toast.makeText(context, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Si no hay imagen seleccionada, guardar el libro sin imagen
            val book = hashMapOf(
                "title" to title,
                "author" to author,
                "isbn" to isbn,
                "description" to description,
                "categories" to categories,
                "imageUrl" to null,
                "assignedTo" to null,
                "assignedWithNames" to null,
                "assignedToEmails" to null,
                "assignedDates" to null,
                "loanExpirationDates" to null,
                "status" to "Disponible",
                "quantity" to finalQuantity,
                "code" to code,
                "createdDate" to com.google.firebase.Timestamp.now(),
                "lastEditedDate" to null
            )
            firestore.collection("books").add(book)
                .addOnSuccessListener {
                    // Notificar al usuario y limpiar campos
                    hideProgressBar()
                    Toast.makeText(context, "Libro registrado con éxito", Toast.LENGTH_SHORT).show()
                    clearFields()
                }
                .addOnFailureListener { e ->
                    hideProgressBar()
                    Log.e("Firebase", "Error al registrar el libro sin imagen", e)
                    Toast.makeText(context, "Error al registrar el libro", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun clearFields() {
        bookTitleInput.text?.clear()
        bookAuthorInput.text?.clear()
        bookIsbnInput.text?.clear()
        bookDescriptionInput.text?.clear()
        capturedImageView.setImageBitmap(null)
        imageUri = null
        capturedImageView.visibility = View.GONE
        selectedCategories.clear()
        selectedCategoriesTextView.text = "Categorías: Ninguna seleccionada"
        bookQuantityInput.text?.clear()
        bookCodeInput.text?.clear()
        
        Toast.makeText(context, "✅ Campos limpiados", Toast.LENGTH_SHORT).show()
    }

    private fun showProgressBar() {
        progressContainer.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        progressContainer.visibility = View.GONE
    }
}