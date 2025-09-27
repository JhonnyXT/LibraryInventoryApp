import SwiftUI
import shared

struct RegisterBookView: View {
    @State private var title = ""
    @State private var author = ""
    @State private var isbn = ""
    @State private var description = ""
    @State private var quantity = 1
    @State private var selectedCategories: Set<String> = []
    @State private var showingImagePicker = false
    @State private var selectedImage: UIImage?
    @State private var showingScanner = false
    @State private var isLoading = false
    @State private var showingSuccess = false
    @State private var errorMessage: String?
    
    @Environment(\.dismiss) private var dismiss
    
    private let categories = [
        "Biblia", "Liderazgo", "Jóvenes", "Mujeres", "Profecía bíblica",
        "Familia", "Matrimonio", "Finanzas", "Estudio bíblico", "Evangelismo",
        "Navidad", "Emaus", "Misiones", "Devocionales", "Curso vida",
        "Iglesia", "Vida cristiana", "Libros de la Biblia", "Enciclopedia",
        "Religiones", "Inglés", "Infantil"
    ]
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                // Imagen del libro
                VStack(alignment: .leading, spacing: 12) {
                    Text("Portada del Libro")
                        .font(.headline)
                        .foregroundColor(.primary)
                    
                    HStack(spacing: 16) {
                        // Imagen preview
                        Button(action: { showingImagePicker = true }) {
                            if let image = selectedImage {
                                Image(uiImage: image)
                                    .resizable()
                                    .aspectRatio(contentMode: .fill)
                                    .frame(width: 120, height: 160)
                                    .clipped()
                                    .cornerRadius(8)
                            } else {
                                Rectangle()
                                    .fill(Color.secondary.opacity(0.2))
                                    .frame(width: 120, height: 160)
                                    .cornerRadius(8)
                                    .overlay(
                                        VStack(spacing: 8) {
                                            Image(systemName: "camera.fill")
                                                .font(.title2)
                                                .foregroundColor(.blue)
                                            Text("Agregar Foto")
                                                .font(.caption)
                                                .foregroundColor(.blue)
                                        }
                                    )
                            }
                        }
                        .buttonStyle(PlainButtonStyle())
                        
                        VStack(alignment: .leading, spacing: 12) {
                            Button(action: { showingImagePicker = true }) {
                                Label("Seleccionar Imagen", systemImage: "photo.on.rectangle")
                                    .font(.subheadline)
                                    .foregroundColor(.blue)
                            }
                            
                            Button(action: { showingScanner = true }) {
                                Label("Escanear Código", systemImage: "qrcode.viewfinder")
                                    .font(.subheadline)
                                    .foregroundColor(.green)
                            }
                            
                            if selectedImage != nil {
                                Button(action: { selectedImage = nil }) {
                                    Label("Remover", systemImage: "trash")
                                        .font(.subheadline)
                                        .foregroundColor(.red)
                                }
                            }
                        }
                    }
                }
                
                // Información básica
                VStack(alignment: .leading, spacing: 16) {
                    Text("Información del Libro")
                        .font(.headline)
                        .foregroundColor(.primary)
                    
                    VStack(alignment: .leading, spacing: 12) {
                        FormField(title: "Título *", text: $title, placeholder: "Nombre del libro")
                        
                        FormField(title: "Autor *", text: $author, placeholder: "Nombre del autor")
                        
                        HStack(spacing: 12) {
                            FormField(title: "ISBN", text: $isbn, placeholder: "Código ISBN")
                            
                            Button(action: { showingScanner = true }) {
                                Image(systemName: "qrcode.viewfinder")
                                    .font(.title2)
                                    .foregroundColor(.blue)
                            }
                            .frame(width: 44, height: 44)
                            .background(Color.blue.opacity(0.1))
                            .cornerRadius(8)
                        }
                        
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Descripción")
                                .font(.subheadline)
                                .fontWeight(.medium)
                                .foregroundColor(.primary)
                            
                            TextEditor(text: $description)
                                .frame(height: 80)
                                .padding(8)
                                .background(Color.secondary.opacity(0.1))
                                .cornerRadius(8)
                        }
                        
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Cantidad de Ejemplares")
                                .font(.subheadline)
                                .fontWeight(.medium)
                                .foregroundColor(.primary)
                            
                            HStack {
                                Button(action: {
                                    if quantity > 1 { quantity -= 1 }
                                }) {
                                    Image(systemName: "minus.circle.fill")
                                        .font(.title2)
                                        .foregroundColor(quantity > 1 ? .blue : .gray)
                                }
                                .disabled(quantity <= 1)
                                
                                Text("\(quantity)")
                                    .font(.title2)
                                    .fontWeight(.semibold)
                                    .frame(minWidth: 50)
                                
                                Button(action: {
                                    if quantity < 99 { quantity += 1 }
                                }) {
                                    Image(systemName: "plus.circle.fill")
                                        .font(.title2)
                                        .foregroundColor(quantity < 99 ? .blue : .gray)
                                }
                                .disabled(quantity >= 99)
                                
                                Spacer()
                            }
                        }
                    }
                }
                
                // Categorías
                VStack(alignment: .leading, spacing: 12) {
                    Text("Categorías")
                        .font(.headline)
                        .foregroundColor(.primary)
                    
                    LazyVGrid(columns: [
                        GridItem(.flexible()),
                        GridItem(.flexible())
                    ], spacing: 8) {
                        ForEach(categories, id: \.self) { category in
                            CategorySelectionChip(
                                title: category,
                                isSelected: selectedCategories.contains(category)
                            ) {
                                if selectedCategories.contains(category) {
                                    selectedCategories.remove(category)
                                } else {
                                    selectedCategories.insert(category)
                                }
                            }
                        }
                    }
                }
                
                // Botón de guardar
                Button(action: saveBook) {
                    HStack {
                        if isLoading {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                .scaleEffect(0.8)
                        } else {
                            Image(systemName: "checkmark.circle.fill")
                                .font(.title2)
                        }
                        
                        Text(isLoading ? "Guardando..." : "Registrar Libro")
                            .font(.headline)
                            .fontWeight(.semibold)
                    }
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 54)
                    .background(
                        LinearGradient(
                            gradient: Gradient(colors: [.green, .green.opacity(0.8)]),
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .cornerRadius(16)
                }
                .disabled(isLoading || title.isEmpty || author.isEmpty)
                .opacity((isLoading || title.isEmpty || author.isEmpty) ? 0.6 : 1.0)
                
                if let errorMessage = errorMessage {
                    Text(errorMessage)
                        .font(.caption)
                        .foregroundColor(.red)
                        .multilineTextAlignment(.center)
                }
            }
            .padding(20)
        }
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showingImagePicker) {
            ImagePicker(selectedImage: $selectedImage)
        }
        .sheet(isPresented: $showingScanner) {
            CodeScannerView { result in
                isbn = result
                showingScanner = false
            }
        }
        .alert("¡Libro Registrado!", isPresented: $showingSuccess) {
            Button("OK") {
                dismiss()
            }
        } message: {
            Text("El libro ha sido agregado exitosamente a la biblioteca.")
        }
    }
    
    private func saveBook() {
        isLoading = true
        
        // TODO: Implementar guardado real usando servicios KMP en Fase 6
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
            isLoading = false
            showingSuccess = true
        }
    }
}

// MARK: - Supporting Views
struct FormField: View {
    let title: String
    @Binding var text: String
    let placeholder: String
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.subheadline)
                .fontWeight(.medium)
                .foregroundColor(.primary)
            
            TextField(placeholder, text: $text)
                .textFieldStyle(CustomFormFieldStyle())
        }
    }
}

struct CustomFormFieldStyle: TextFieldStyle {
    func _body(configuration: TextField<Self._Label>) -> some View {
        configuration
            .padding(.horizontal, 12)
            .padding(.vertical, 12)
            .background(Color.secondary.opacity(0.1))
            .cornerRadius(8)
    }
}

struct CategorySelectionChip: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.caption)
                .fontWeight(isSelected ? .semibold : .regular)
                .foregroundColor(isSelected ? .white : .primary)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .frame(maxWidth: .infinity)
                .background(
                    isSelected ? 
                        Color.blue : 
                        Color.secondary.opacity(0.1)
                )
                .cornerRadius(8)
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(isSelected ? Color.clear : Color.secondary.opacity(0.3), lineWidth: 1)
                )
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// MARK: - Placeholder Views (Para Fase 6)
struct ImagePicker: UIViewControllerRepresentable {
    @Binding var selectedImage: UIImage?
    @Environment(\.dismiss) private var dismiss
    
    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.delegate = context.coordinator
        picker.allowsEditing = true
        return picker
    }
    
    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}
    
    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }
    
    class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let parent: ImagePicker
        
        init(_ parent: ImagePicker) {
            self.parent = parent
        }
        
        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
            if let image = info[.editedImage] as? UIImage {
                parent.selectedImage = image
            }
            parent.dismiss()
        }
        
        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.dismiss()
        }
    }
}

struct CodeScannerView: View {
    let onCodeScanned: (String) -> Void
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        NavigationView {
            VStack {
                Text("Escaneo de códigos de barras")
                    .font(.title2)
                    .padding()
                
                Text("Funcionalidad de escaneo será implementada en Fase 6")
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding()
                
                Button("Simular Escaneo") {
                    onCodeScanned("9781234567890")
                }
                .buttonStyle(.borderedProminent)
                
                Spacer()
            }
            .navigationTitle("Escanear Código")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Cerrar") {
                        dismiss()
                    }
                }
            }
        }
    }
}

#Preview {
    NavigationView {
        RegisterBookView()
    }
}
