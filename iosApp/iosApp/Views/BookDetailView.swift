import SwiftUI
import shared

struct BookDetailView: View {
    let book: BookDisplayItem
    let isAdminView: Bool
    @Environment(\.dismiss) private var dismiss
    @State private var showingAssignDialog = false
    @State private var showingEditView = false
    @State private var isAddingToWishlist = false
    @State private var showingSuccessMessage = false
    @State private var successMessage = ""
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    // Imagen y información principal
                    HStack(alignment: .top, spacing: 20) {
                        // Imagen del libro
                        Rectangle()
                            .fill(Color.secondary.opacity(0.2))
                            .frame(width: 120, height: 160)
                            .cornerRadius(12)
                            .overlay(
                                VStack {
                                    Image(systemName: "book.closed.fill")
                                        .font(.largeTitle)
                                        .foregroundColor(.secondary.opacity(0.5))
                                    
                                    if !book.isAvailable {
                                        Text("No Disponible")
                                            .font(.caption)
                                            .fontWeight(.semibold)
                                            .foregroundColor(.white)
                                            .padding(.horizontal, 8)
                                            .padding(.vertical, 4)
                                            .background(Color.red)
                                            .cornerRadius(4)
                                            .offset(y: 12)
                                    }
                                }
                            )
                        
                        // Información del libro
                        VStack(alignment: .leading, spacing: 12) {
                            Text(book.title)
                                .font(.title2)
                                .fontWeight(.bold)
                                .foregroundColor(.primary)
                                .multilineTextAlignment(.leading)
                            
                            Text("por \(book.author)")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                            
                            // Estado del libro
                            HStack {
                                Image(systemName: book.isAvailable ? "checkmark.circle.fill" : "xmark.circle.fill")
                                    .foregroundColor(book.isAvailable ? .green : .red)
                                
                                Text(book.isAvailable ? "Disponible" : "No disponible")
                                    .font(.subheadline)
                                    .fontWeight(.medium)
                                    .foregroundColor(book.isAvailable ? .green : .red)
                            }
                            
                            if isAdminView {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text("Cantidad total: \(book.quantity)")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                    
                                    Text("Prestados: \(book.assignedCount)")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                    
                                    Text("Disponibles: \(book.quantity - book.assignedCount)")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                            }
                        }
                        
                        Spacer()
                    }
                    .padding(.horizontal, 20)
                    
                    // Categorías
                    if !book.categories.isEmpty {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Categorías")
                                .font(.headline)
                                .fontWeight(.semibold)
                                .padding(.horizontal, 20)
                            
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 8) {
                                    ForEach(book.categories, id: \.self) { category in
                                        Text(category)
                                            .font(.caption)
                                            .fontWeight(.medium)
                                            .foregroundColor(.blue)
                                            .padding(.horizontal, 12)
                                            .padding(.vertical, 6)
                                            .background(Color.blue.opacity(0.1))
                                            .cornerRadius(16)
                                    }
                                }
                                .padding(.horizontal, 20)
                            }
                        }
                    }
                    
                    // Descripción
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Descripción")
                            .font(.headline)
                            .fontWeight(.semibold)
                            .padding(.horizontal, 20)
                        
                        Text(book.description.isEmpty ? "Sin descripción disponible." : book.description)
                            .font(.body)
                            .foregroundColor(.secondary)
                            .padding(.horizontal, 20)
                    }
                    
                    // Botones de acción
                    VStack(spacing: 12) {
                        if isAdminView {
                            // Botones de admin
                            Button(action: { showingEditView = true }) {
                                HStack {
                                    Image(systemName: "pencil.circle.fill")
                                        .font(.title2)
                                    Text("Editar Libro")
                                        .font(.headline)
                                        .fontWeight(.semibold)
                                }
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .frame(height: 50)
                                .background(Color.blue)
                                .cornerRadius(12)
                            }
                            
                            if book.isAvailable {
                                Button(action: { showingAssignDialog = true }) {
                                    HStack {
                                        Image(systemName: "person.badge.plus")
                                            .font(.title2)
                                        Text("Asignar a Usuario")
                                            .font(.headline)
                                            .fontWeight(.semibold)
                                    }
                                    .foregroundColor(.white)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 50)
                                    .background(Color.green)
                                    .cornerRadius(12)
                                }
                            }
                        } else {
                            // Botones de usuario
                            if book.isAvailable {
                                Button(action: requestBook) {
                                    HStack {
                                        Image(systemName: "plus.circle.fill")
                                            .font(.title2)
                                        Text("Solicitar Libro")
                                            .font(.headline)
                                            .fontWeight(.semibold)
                                    }
                                    .foregroundColor(.white)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 50)
                                    .background(Color.green)
                                    .cornerRadius(12)
                                }
                            }
                            
                            Button(action: addToWishlist) {
                                HStack {
                                    if isAddingToWishlist {
                                        ProgressView()
                                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                            .scaleEffect(0.8)
                                    } else {
                                        Image(systemName: "heart.fill")
                                            .font(.title2)
                                    }
                                    Text(isAddingToWishlist ? "Agregando..." : "Agregar a Deseados")
                                        .font(.headline)
                                        .fontWeight(.semibold)
                                }
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .frame(height: 50)
                                .background(Color.pink)
                                .cornerRadius(12)
                            }
                            .disabled(isAddingToWishlist)
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 40)
                }
            }
            .navigationTitle("Detalles")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Cerrar") {
                        dismiss()
                    }
                }
            }
        }
        .sheet(isPresented: $showingEditView) {
            EditBookView(book: book)
        }
        .alert("Éxito", isPresented: $showingSuccessMessage) {
            Button("OK") { }
        } message: {
            Text(successMessage)
        }
        .sheet(isPresented: $showingAssignDialog) {
            AssignBookView(book: book) { success in
                if success {
                    successMessage = "Libro asignado exitosamente"
                    showingSuccessMessage = true
                }
            }
        }
    }
    
    private func requestBook() {
        // TODO: Implementar solicitud de libro real en Fase 6
        successMessage = "Solicitud enviada. Te notificaremos cuando el libro esté listo."
        showingSuccessMessage = true
    }
    
    private func addToWishlist() {
        isAddingToWishlist = true
        
        // TODO: Implementar añadir a wishlist real usando servicios KMP en Fase 6
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            isAddingToWishlist = false
            successMessage = "Libro agregado a tu lista de deseos"
            showingSuccessMessage = true
        }
    }
}

// MARK: - Supporting Views
struct AssignBookView: View {
    let book: BookDisplayItem
    let onComplete: (Bool) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var selectedUserId = ""
    @State private var searchText = ""
    @State private var users: [UserItem] = []
    @State private var isLoading = false
    
    var filteredUsers: [UserItem] {
        if searchText.isEmpty {
            return users
        } else {
            return users.filter { user in
                user.name.localizedCaseInsensitiveContains(searchText) ||
                user.email.localizedCaseInsensitiveContains(searchText)
            }
        }
    }
    
    var body: some View {
        NavigationView {
            VStack(spacing: 16) {
                // Búsqueda de usuarios
                TextField("Buscar usuario...", text: $searchText)
                    .textFieldStyle(CustomFormFieldStyle())
                    .padding(.horizontal, 20)
                
                // Lista de usuarios
                List(filteredUsers) { user in
                    Button(action: {
                        selectedUserId = user.id
                    }) {
                        HStack {
                            Circle()
                                .fill(Color.blue)
                                .frame(width: 40, height: 40)
                                .overlay(
                                    Text(user.initials)
                                        .font(.subheadline)
                                        .fontWeight(.semibold)
                                        .foregroundColor(.white)
                                )
                            
                            VStack(alignment: .leading, spacing: 4) {
                                Text(user.name)
                                    .font(.headline)
                                    .foregroundColor(.primary)
                                
                                Text(user.email)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            
                            Spacer()
                            
                            if selectedUserId == user.id {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundColor(.green)
                            }
                        }
                    }
                    .buttonStyle(PlainButtonStyle())
                }
                
                // Botón de asignar
                Button(action: assignBook) {
                    HStack {
                        if isLoading {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                .scaleEffect(0.8)
                        } else {
                            Image(systemName: "checkmark.circle.fill")
                                .font(.title2)
                        }
                        
                        Text(isLoading ? "Asignando..." : "Asignar Libro")
                            .font(.headline)
                            .fontWeight(.semibold)
                    }
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(Color.green)
                    .cornerRadius(12)
                }
                .disabled(selectedUserId.isEmpty || isLoading)
                .opacity((selectedUserId.isEmpty || isLoading) ? 0.6 : 1.0)
                .padding(.horizontal, 20)
                .padding(.bottom, 20)
            }
            .navigationTitle("Asignar Libro")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancelar") {
                        dismiss()
                    }
                }
            }
        }
        .onAppear {
            loadUsers()
        }
    }
    
    private func loadUsers() {
        // TODO: Cargar usuarios reales en Fase 6
        users = [
            UserItem(id: "1", name: "Juan Pérez", email: "juan@email.com"),
            UserItem(id: "2", name: "María García", email: "maria@email.com"),
            UserItem(id: "3", name: "Carlos López", email: "carlos@email.com"),
            UserItem(id: "4", name: "Ana Martínez", email: "ana@email.com")
        ]
    }
    
    private func assignBook() {
        isLoading = true
        
        // TODO: Implementar asignación real usando servicios KMP en Fase 6
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            isLoading = false
            onComplete(true)
            dismiss()
        }
    }
}

// MARK: - Models
struct UserItem: Identifiable {
    let id: String
    let name: String
    let email: String
    
    var initials: String {
        let components = name.components(separatedBy: " ")
        if components.count >= 2 {
            let firstInitial = components[0].prefix(1).uppercased()
            let lastInitial = components[1].prefix(1).uppercased()
            return "\(firstInitial)\(lastInitial)"
        } else {
            return String(name.prefix(1).uppercased())
        }
    }
}

#Preview {
    BookDetailView(
        book: BookDisplayItem(
            id: "1",
            title: "Biblia de Estudio",
            author: "Varios Autores",
            description: "Una biblia completa con comentarios detallados para el estudio profundo de las Escrituras.",
            categories: ["Biblia", "Estudio bíblico"],
            imageUrl: nil,
            isAvailable: true,
            quantity: 5,
            assignedCount: 2
        ),
        isAdminView: true
    )
}
