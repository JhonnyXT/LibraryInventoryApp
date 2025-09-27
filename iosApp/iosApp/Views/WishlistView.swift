import SwiftUI
import shared

struct WishlistView: View {
    @StateObject private var viewModel = WishlistViewModel()
    
    var body: some View {
        Group {
            if viewModel.isLoading {
                ProgressView("Cargando lista de deseos...")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if viewModel.wishlistBooks.isEmpty {
                EmptyWishlistView()
            } else {
                ScrollView {
                    LazyVStack(spacing: 16) {
                        // Header
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Mi Lista de Deseos")
                                    .font(.title2)
                                    .fontWeight(.bold)
                                
                                Text("\(viewModel.wishlistBooks.count) libros guardados")
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)
                            }
                            
                            Spacer()
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, 16)
                        
                        // Lista de libros
                        ForEach(viewModel.wishlistBooks) { book in
                            WishlistBookCard(book: book) {
                                Task {
                                    await viewModel.removeFromWishlist(book)
                                }
                            }
                        }
                        .padding(.horizontal, 20)
                    }
                    .padding(.bottom, 20)
                }
                .refreshable {
                    await viewModel.loadWishlistBooks()
                }
            }
        }
        .onAppear {
            viewModel.loadWishlistBooks()
        }
    }
}

// MARK: - Supporting Views
struct EmptyWishlistView: View {
    var body: some View {
        VStack(spacing: 24) {
            VStack(spacing: 16) {
                Image(systemName: "heart")
                    .font(.system(size: 60))
                    .foregroundColor(.pink.opacity(0.6))
                
                VStack(spacing: 8) {
                    Text("Tu lista de deseos está vacía")
                        .font(.title2)
                        .fontWeight(.semibold)
                        .foregroundColor(.primary)
                    
                    Text("Agrega libros que te interesen para recibir notificaciones cuando estén disponibles.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)
                }
            }
            
            // Botón para explorar catálogo
            NavigationLink(destination: BookListView(isAdminView: false)) {
                HStack {
                    Image(systemName: "books.vertical.fill")
                        .font(.title2)
                    Text("Explorar Catálogo")
                        .font(.headline)
                        .fontWeight(.semibold)
                }
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 50)
                .background(
                    LinearGradient(
                        gradient: Gradient(colors: [.pink, .pink.opacity(0.8)]),
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
                .cornerRadius(12)
                .shadow(color: .pink.opacity(0.3), radius: 8, x: 0, y: 4)
            }
            .padding(.horizontal, 40)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct WishlistBookCard: View {
    let book: WishlistBookItem
    let onRemove: () -> Void
    
    var body: some View {
        HStack(spacing: 16) {
            // Imagen del libro
            Rectangle()
                .fill(Color.secondary.opacity(0.2))
                .frame(width: 60, height: 80)
                .cornerRadius(6)
                .overlay(
                    VStack {
                        Image(systemName: "book.closed.fill")
                            .font(.title3)
                            .foregroundColor(.secondary.opacity(0.5))
                        
                        if !book.isAvailable {
                            Text("No disponible")
                                .font(.caption2)
                                .fontWeight(.semibold)
                                .foregroundColor(.white)
                                .padding(.horizontal, 4)
                                .padding(.vertical, 2)
                                .background(Color.red)
                                .cornerRadius(3)
                                .offset(y: 4)
                        }
                    }
                )
            
            // Información del libro
            VStack(alignment: .leading, spacing: 6) {
                Text(book.title)
                    .font(.headline)
                    .fontWeight(.semibold)
                    .foregroundColor(.primary)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                
                Text(book.author)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .lineLimit(1)
                
                // Categorías
                HStack {
                    ForEach(book.categories.prefix(2), id: \.self) { category in
                        Text(category)
                            .font(.caption2)
                            .foregroundColor(.blue)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color.blue.opacity(0.1))
                            .cornerRadius(4)
                    }
                    
                    if book.categories.count > 2 {
                        Text("+\(book.categories.count - 2)")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                }
                
                // Estado y fecha
                HStack {
                    Image(systemName: "calendar")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    Text("Agregado: \(book.addedDate, style: .date)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    Spacer()
                    
                    // Prioridad
                    HStack(spacing: 2) {
                        ForEach(0..<3, id: \.self) { index in
                            Image(systemName: "star.fill")
                                .font(.caption2)
                                .foregroundColor(index < book.priority.rawValue ? .orange : .secondary.opacity(0.3))
                        }
                    }
                }
            }
            
            Spacer()
            
            // Botones de acción
            VStack(spacing: 8) {
                if book.isAvailable {
                    Button(action: {
                        // TODO: Solicitar libro
                    }) {
                        Image(systemName: "plus.circle.fill")
                            .font(.title2)
                            .foregroundColor(.green)
                    }
                } else {
                    Button(action: {
                        // TODO: Notificar cuando esté disponible
                    }) {
                        Image(systemName: "bell.fill")
                            .font(.title2)
                            .foregroundColor(.orange)
                    }
                }
                
                Button(action: onRemove) {
                    Image(systemName: "heart.slash.fill")
                        .font(.title2)
                        .foregroundColor(.red)
                }
            }
        }
        .padding(16)
        .background(Color.secondary.opacity(0.05))
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color.secondary.opacity(0.2), lineWidth: 1)
        )
        .contextMenu {
            Button(action: {
                // TODO: Ver detalles del libro
            }) {
                Label("Ver Detalles", systemImage: "info.circle")
            }
            
            if book.isAvailable {
                Button(action: {
                    // TODO: Solicitar libro
                }) {
                    Label("Solicitar Libro", systemImage: "plus.circle")
                }
            } else {
                Button(action: {
                    // TODO: Notificar disponibilidad
                }) {
                    Label("Notificar Disponibilidad", systemImage: "bell")
                }
            }
            
            Divider()
            
            Button(role: .destructive, action: onRemove) {
                Label("Remover de Lista", systemImage: "heart.slash")
            }
        }
    }
}

// MARK: - Models
struct WishlistBookItem: Identifiable {
    let id: String
    let title: String
    let author: String
    let imageUrl: String?
    let categories: [String]
    let addedDate: Date
    let isAvailable: Bool
    let priority: Priority
    
    enum Priority: Int, CaseIterable {
        case low = 1
        case normal = 2
        case high = 3
        
        var title: String {
            switch self {
            case .low: return "Baja"
            case .normal: return "Normal"
            case .high: return "Alta"
            }
        }
    }
}

#Preview {
    NavigationView {
        WishlistView()
    }
}
