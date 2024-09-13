package com.example.libraryinventoryapp.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.libraryinventoryapp.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class RegisterBookFragment : Fragment() {

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
        private const val REQUEST_IMAGE_CAPTURE = 1002
        private const val REQUEST_PERMISSION_CODE = 1003
        private const val PICK_IMAGE_REQUEST = 1
    }

    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var imageUri: Uri? = null

    private lateinit var bookTitleInput: EditText
    private lateinit var bookAuthorInput: EditText
    private lateinit var bookIsbnInput: EditText
    private lateinit var scanCodeButton: Button
    private lateinit var uploadImageButton: Button
    private lateinit var registerBookButton: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register_book, container, false)

        // Initialize views
        bookTitleInput = view.findViewById(R.id.book_title_input)
        bookAuthorInput = view.findViewById(R.id.book_author_input)
        bookIsbnInput = view.findViewById(R.id.book_isbn_input)
        scanCodeButton = view.findViewById(R.id.scan_code_button)
        uploadImageButton = view.findViewById(R.id.upload_image_button)
        registerBookButton = view.findViewById(R.id.register_book_button)
        progressBar = view.findViewById(R.id.progress_bar)

        // Initialize Firebase instances
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Verificar permisos de almacenamiento
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_PERMISSION_CODE)
        }

        // Handle QR/barcode scanning
        scanCodeButton.setOnClickListener {
            scanBarcode()
        }

        // Handle image upload
        uploadImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // Handle book registration
        registerBookButton.setOnClickListener {
            val title = bookTitleInput.text.toString().trim()
            val author = bookAuthorInput.text.toString().trim()
            val isbn = bookIsbnInput.text.toString().trim()

            if (title.isNotEmpty() && author.isNotEmpty() && isbn.isNotEmpty()) {
                showProgressBar()
                uploadBookToFirebase(title, author, isbn)
            } else {
                Toast.makeText(context, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
            }
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
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            processImage(imageBitmap)
        } else if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            Toast.makeText(context, "Imagen seleccionada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processImage(bitmap: Bitmap) {
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

    private fun uploadBookToFirebase(title: String, author: String, isbn: String) {
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
                        "imageUrl" to uri.toString(),
                        "assignedTo" to null,
                        "status" to "Disponible"
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
                "imageUrl" to null,
                "assignedTo" to null,
                "status" to "Disponible"
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
        imageUri = null
    }

    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        progressBar.visibility = View.GONE
    }
}