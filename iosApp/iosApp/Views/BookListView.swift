import SwiftUI
import shared

struct BookListView: View {
    let isAdminView: Bool
    
    @StateObject private var viewModel = BookListViewModel()
    @State private var showingFilters = false
    @State private var selectedBook: BookDisplayItem?
    @State private var showingBookDetail = false
    
    private let categories = [
        "Todas", "Biblia", "Liderazgo", "Jóvenes", "Mujeres",
        "Profecía bíblica", "Familia", "Matrimonio", "Finanzas",
        "Estudio bíblico", "Evangelismo", "Navidad", "Emaus",
        "Misiones", "Devocionales", "Curso vida", "Iglesia",
        "Vida cristiana", "Libros de la Biblia", "Enciclopedia",
        "Religiones", "Inglés", "Infantil"
    ]
    
    var body: some View {
        VStack(spacing: 0) {
            // Barra de búsqueda y filtros
            VStack(spacing: 12) {
                // Búsqueda
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.secondary)
                    
                    TextField("Buscar libros...", text: $viewModel.searchText)
                        .textFieldStyle(PlainTextFieldStyle())
                        .onChange(of: viewModel.searchText) { _ in
                            viewModel.filterBooks()
                        }
                    
                    if !viewModel.searchText.isEmpty {
                        Button(action: {
                            viewModel.searchText = ""
                            viewModel.filterBooks()
                        }) {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundColor(.secondary)
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(Color.secondary.opacity(0.1))
                .cornerRadius(12)
                
                // Filtros de categorías
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(categories, id: \.self) { category in
                            CategoryChip(
                                title: category,
                                isSelected: viewModel.selectedCategory == category
                            ) {
                                viewModel.selectedCategory = category
                                viewModel.filterBooks()
                            }
                        }
                    }
                    .padding(.horizontal, 20)
                }
                
                // Contador de resultados
                HStack {
                    Text("\(viewModel.filteredBooks.count) libros")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    Spacer()
                    
                    Button(action: { showingFilters = true }) {
                        HStack(spacing: 4) {
                            Image(systemName: "slider.horizontal.3")
                            Text("Filtros")
                        }
                        .font(.caption)
                        .foregroundColor(.blue)
                    }
                }
                .padding(.horizontal, 20)
            }
            .padding(.vertical, 16)
            .background(Color(UIColor.systemBackground))
            
            // Lista de libros
            if viewModel.isLoading {
                Spacer()
                ProgressView("Cargando libros...")
                    .foregroundColor(.secondary)
                Spacer()
            } else if viewModel.filteredBooks.isEmpty {
                EmptyStateView(
                    icon: viewModel.searchText.isEmpty ? "books.vertical" : "magnifyingglass",
                    title: viewModel.searchText.isEmpty ? "No hay libros" : "Sin resultados",
                    subtitle: viewModel.searchText.isEmpty ? 
                        "Aún no se han agregado libros a la biblioteca" :
                        "Intenta con otros términos de búsqueda"
                )
            } else {
                ScrollView {
                    LazyVGrid(columns: [
                        GridItem(.flexible()),
                        GridItem(.flexible())
                    ], spacing: 16) {
                        ForEach(viewModel.filteredBooks) { book in
                            BookCard(book: book, isAdminView: isAdminView) {
                                selectedBook = book
                                showingBookDetail = true
                            }
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 20)
                }
                .refreshable {
                    await viewModel.loadBooks()
                }
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if isAdminView {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: {
                        // Navigate to add book
                    }) {
                        Image(systemName: "plus")
                    }
                }
            }
        }
        .sheet(isPresented: $showingFilters) {
            FiltersView(
                selectedCategory: $viewModel.selectedCategory,
                categories: categories
            ) {
                viewModel.filterBooks()
                showingFilters = false
            }
        }
        .sheet(isPresented: $showingBookDetail) {
            if let book = selectedBook {
                BookDetailView(book: book, isAdminView: isAdminView)
            }
        }
        .onAppear {
            viewModel.loadBooks()
        }
    }
}

// MARK: - Supporting Views
struct CategoryChip: View {
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
                .padding(.vertical, 6)
                .background(
                    isSelected ? 
                        Color.blue : 
                        Color.secondary.opacity(0.1)
                )
                .cornerRadius(16)
        }
        .buttonStyle(PlainButtonStyle())
    }
}

struct BookCard: View {
    let book: BookDisplayItem
    let isAdminView: Bool
    let onTap: () -> Void
    
    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 12) {
                // Imagen del libro
                Rectangle()
                    .fill(Color.secondary.opacity(0.2))
                    .frame(height: 160)
                    .cornerRadius(8)
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
                                    .offset(y: 8)
                            }
                        }
                    )
                
                // Información del libro
                VStack(alignment: .leading, spacing: 6) {
                    Text(book.title)
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundColor(.primary)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                    
                    Text(book.author)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                    
                    if let category = book.categories.first {
                        Text(category)
                            .font(.caption2)
                            .foregroundColor(.blue)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color.blue.opacity(0.1))
                            .cornerRadius(4)
                    }
                    
                    if isAdminView {
                        HStack {
                            Text("Total: \(book.quantity)")
                                .font(.caption2)
                                .foregroundColor(.secondary)
                            
                            Spacer()
                            
                            Text("Prestados: \(book.assignedCount)")
                                .font(.caption2)
                                .foregroundColor(book.assignedCount > 0 ? .orange : .secondary)
                        }
                    }
                }
                
                Spacer()
            }
            .padding(12)
            .frame(height: 280)
            .background(Color.secondary.opacity(0.05))
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(Color.secondary.opacity(0.2), lineWidth: 1)
            )
        }
        .buttonStyle(PlainButtonStyle())
    }
}

struct EmptyStateView: View {
    let icon: String
    let title: String
    let subtitle: String
    
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: icon)
                .font(.system(size: 60))
                .foregroundColor(.secondary.opacity(0.6))
            
            VStack(spacing: 8) {
                Text(title)
                    .font(.headline)
                    .foregroundColor(.primary)
                
                Text(subtitle)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct FiltersView: View {
    @Binding var selectedCategory: String
    let categories: [String]
    let onApply: () -> Void
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        NavigationView {
            List {
                Section("Categorías") {
                    ForEach(categories, id: \.self) { category in
                        Button(action: {
                            selectedCategory = category
                        }) {
                            HStack {
                                Text(category)
                                    .foregroundColor(.primary)
                                
                                Spacer()
                                
                                if selectedCategory == category {
                                    Image(systemName: "checkmark")
                                        .foregroundColor(.blue)
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle("Filtros")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cerrar") {
                        dismiss()
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Aplicar") {
                        onApply()
                    }
                    .fontWeight(.semibold)
                }
            }
        }
    }
}

// MARK: - Models
struct BookDisplayItem: Identifiable {
    let id: String
    let title: String
    let author: String
    let description: String
    let categories: [String]
    let imageUrl: String?
    let isAvailable: Bool
    let quantity: Int
    let assignedCount: Int
}

#Preview {
    NavigationView {
        BookListView(isAdminView: true)
    }
}
