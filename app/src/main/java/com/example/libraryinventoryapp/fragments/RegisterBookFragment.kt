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
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.libraryinventoryapp.LoginActivity
import com.example.libraryinventoryapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
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

    private lateinit var bookTitleInput: EditText
    private lateinit var bookAuthorInput: EditText
    private lateinit var bookIsbnInput: EditText
    private lateinit var bookDescriptionInput: EditText
    private lateinit var scanCodeButton: Button
    private lateinit var captureImageButton: Button
    private lateinit var registerBookButton: Button
    private lateinit var logOutButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var capturedImageView: ImageView // ImageView para mostrar la imagen capturada
    private lateinit var selectCategoryButton: Button
    private lateinit var selectedCategoriesTextView: TextView
    private lateinit var bookQuantittyInput: EditText
    private var selectedCategories = mutableListOf<String>()
    private val categoriesArray by lazy { resources.getStringArray(R.array.book_categories) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register_book, container, false)

        // Initialize views
        bookTitleInput = view.findViewById(R.id.book_title_input)
        bookAuthorInput = view.findViewById(R.id.book_author_input)
        bookIsbnInput = view.findViewById(R.id.book_isbn_input)
        bookDescriptionInput = view.findViewById(R.id.book_description_input)
        scanCodeButton = view.findViewById(R.id.scan_code_button)
        captureImageButton = view.findViewById(R.id.capture_image_button) // Nuevo botón para capturar imagen
        registerBookButton = view.findViewById(R.id.register_book_button)
        logOutButton = view.findViewById(R.id.logout_button)
        progressBar = view.findViewById(R.id.progress_bar)
        capturedImageView = view.findViewById(R.id.captured_image_view) // ImageView para mostrar la imagen capturada
        selectCategoryButton = view.findViewById(R.id.selectCategoryButton)
        selectedCategoriesTextView = view.findViewById(R.id.selectedCategoriesTextView)
        bookQuantittyInput = view.findViewById(R.id.book_quantity_input)

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Verificar permisos de almacenamiento
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                CAMERA_PERMISSION_REQUEST_CODE)
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
            val quantity = bookQuantittyInput.text.toString()

            if (title.isNotEmpty() && author.isNotEmpty() && isbn.isNotEmpty() && imageUri != null && selectedCategories.isNotEmpty() && description.isNotEmpty()) {
                showProgressBar()
                uploadBookToFirebase(title, author, isbn, description, selectedCategories, quantity)
            } else {
                Toast.makeText(context, "Todos los campos son obligatorios y debe capturar una imagen", Toast.LENGTH_SHORT).show()
            }
        }

        logOutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            activity?.finish()
        }

        return view
    }

    private fun scanBarcode() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
            return
        }

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivityForResult(intent, SCAN_BARCODE_REQUEST_CODE)
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
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
            return
        }

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Proceso de captura de imagen
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            imageUri = getImageUri(imageBitmap) // Obtener URI de la imagen
            if (imageUri != null) {
                capturedImageView.setImageBitmap(imageBitmap) // Mostrar la imagen capturada
                capturedImageView.visibility = View.VISIBLE // Asegúrate de que el ImageView sea visible
            } else {
                Toast.makeText(context, "Error al obtener la URI de la imagen", Toast.LENGTH_SHORT).show()
            }
        }
        // Proceso de escaneo de código de barras
        else if (requestCode == SCAN_BARCODE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            processBarcodeImage(imageBitmap)
        }
    }

    private fun processBarcodeImage(bitmap: Bitmap) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val scanner: BarcodeScanner = BarcodeScanning.getClient()

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue
                    bookIsbnInput.setText(rawValue)
                    Toast.makeText(context, "Código escaneado: $rawValue", Toast.LENGTH_SHORT).show()
                    break
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error al escanear el código: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getImageUri(bitmap: Bitmap): Uri? {
        val file = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "captured_image.jpg")

        try {
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

        return FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
    }

    private fun uploadBookToFirebase(
        title: String,
        author: String,
        isbn: String,
        description: String,
        categories: List<String>,
        quantity: String
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
                        "status" to "Disponible",
                        "quantity" to finalQuantity
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
                "imageUrl" to null,
                "assignedTo" to null,
                "status" to "Disponible",
                "quantity" to finalQuantity
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
        bookTitleInput.text.clear()
        bookAuthorInput.text.clear()
        bookIsbnInput.text.clear()
        bookDescriptionInput.text.clear()
        capturedImageView.setImageBitmap(null) // Limpiar la imagen capturada
        imageUri = null // Limpiar la URI de la imagen
        capturedImageView.visibility = View.GONE // Ocultar el ImageView
        selectedCategories.clear() // Limpiar las categorías seleccionadas
        selectedCategoriesTextView.text = "Categorías seleccionadas: Ninguna" // Actualizar el TextView
        bookQuantittyInput.text.clear()
    }

    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        progressBar.visibility = View.GONE
    }
}