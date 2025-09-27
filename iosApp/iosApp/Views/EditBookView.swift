import SwiftUI

struct EditBookView: View {
    let book: BookDisplayItem
    @Environment(\.dismiss) private var dismiss
    @State private var title: String
    @State private var author: String
    @State private var description: String
    @State private var quantity: Int
    @State private var selectedCategories: Set<String>
    @State private var isLoading = false
    @State private var showingDeleteAlert = false
    
    private let categories = [
        "Biblia", "Liderazgo", "Jóvenes", "Mujeres", "Profecía bíblica",
        "Familia", "Matrimonio", "Finanzas", "Estudio bíblico", "Evangelismo",
        "Navidad", "Emaus", "Misiones", "Devocionales", "Curso vida",
        "Iglesia", "Vida cristiana", "Libros de la Biblia", "Enciclopedia",
        "Religiones", "Inglés", "Infantil"
    ]
    
    init(book: BookDisplayItem) {
        self.book = book
        self._title = State(initialValue: book.title)
        self._author = State(initialValue: book.author)
        self._description = State(initialValue: book.description)
        self._quantity = State(initialValue: book.quantity)
        self._selectedCategories = State(initialValue: Set(book.categories))
    }
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    // Información básica
                    VStack(alignment: .leading, spacing: 16) {
                        Text("Información del Libro")
                            .font(.headline)
                            .fontWeight(.semibold)
                        
                        FormField(title: "Título *", text: $title, placeholder: "Nombre del libro")
                        FormField(title: "Autor *", text: $author, placeholder: "Nombre del autor")
                        
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Descripción")
                                .font(.subheadline)
                                .fontWeight(.medium)
                            
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
                            
                            HStack {
                                Button(action: { if quantity > 1 { quantity -= 1 } }) {
                                    Image(systemName: "minus.circle.fill")
                                        .font(.title2)
                                        .foregroundColor(quantity > 1 ? .blue : .gray)
                                }
                                .disabled(quantity <= 1)
                                
                                Text("\(quantity)")
                                    .font(.title2)
                                    .fontWeight(.semibold)
                                    .frame(minWidth: 50)
                                
                                Button(action: { if quantity < 99 { quantity += 1 } }) {
                                    Image(systemName: "plus.circle.fill")
                                        .font(.title2)
                                        .foregroundColor(quantity < 99 ? .blue : .gray)
                                }
                                .disabled(quantity >= 99)
                                
                                Spacer()
                                
                                Text("Prestados: \(book.assignedCount)")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                    
                    // Categorías
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Categorías")
                            .font(.headline)
                            .fontWeight(.semibold)
                        
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
                    
                    // Botones de acción
                    VStack(spacing: 12) {
                        // Guardar cambios
                        Button(action: saveChanges) {
                            HStack {
                                if isLoading {
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                        .scaleEffect(0.8)
                                } else {
                                    Image(systemName: "checkmark.circle.fill")
                                        .font(.title2)
                                }
                                
                                Text(isLoading ? "Guardando..." : "Guardar Cambios")
                                    .font(.headline)
                                    .fontWeight(.semibold)
                            }
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 54)
                            .background(Color.blue)
                            .cornerRadius(12)
                        }
                        .disabled(isLoading || title.isEmpty || author.isEmpty)
                        
                        // Eliminar libro
                        Button(action: { showingDeleteAlert = true }) {
                            HStack {
                                Image(systemName: "trash.circle.fill")
                                    .font(.title2)
                                Text("Eliminar Libro")
                                    .font(.headline)
                                    .fontWeight(.semibold)
                            }
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 54)
                            .background(Color.red)
                            .cornerRadius(12)
                        }
                        .disabled(isLoading)
                    }
                }
                .padding(20)
            }
            .navigationTitle("Editar Libro")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancelar") {
                        dismiss()
                    }
                }
            }
        }
        .alert("Eliminar Libro", isPresented: $showingDeleteAlert) {
            Button("Cancelar", role: .cancel) { }
            Button("Eliminar", role: .destructive) {
                deleteBook()
            }
        } message: {
            Text("¿Estás seguro de que quieres eliminar este libro? Esta acción no se puede deshacer.")
        }
    }
    
    private func saveChanges() {
        isLoading = true
        
        // TODO: Implementar guardado real usando servicios KMP en Fase 6
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            isLoading = false
            dismiss()
        }
    }
    
    private func deleteBook() {
        isLoading = true
        
        // TODO: Implementar eliminación real usando servicios KMP en Fase 6
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            isLoading = false
            dismiss()
        }
    }
}

#Preview {
    EditBookView(
        book: BookDisplayItem(
            id: "1",
            title: "Biblia de Estudio",
            author: "Varios Autores",
            description: "Biblia completa con comentarios",
            categories: ["Biblia", "Estudio bíblico"],
            imageUrl: nil,
            isAvailable: true,
            quantity: 5,
            assignedCount: 2
        )
    )
}
