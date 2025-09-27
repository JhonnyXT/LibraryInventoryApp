import SwiftUI
import shared

struct AssignedBooksView: View {
    @StateObject private var viewModel = AssignedBooksViewModel()
    
    var body: some View {
        Group {
            if viewModel.isLoading {
                ProgressView("Cargando mis libros...")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if viewModel.assignedBooks.isEmpty {
                EmptyAssignedBooksView()
            } else {
                ScrollView {
                    LazyVStack(spacing: 16) {
                        // Header con contador
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Mis Libros Prestados")
                                    .font(.title2)
                                    .fontWeight(.bold)
                                
                                Text("\(viewModel.assignedBooks.count) libros en tu poder")
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)
                            }
                            
                            Spacer()
                            
                            Button(action: {
                                viewModel.loadAssignedBooks()
                            }) {
                                Image(systemName: "arrow.clockwise")
                                    .font(.title2)
                                    .foregroundColor(.blue)
                            }
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, 16)
                        
                        // Lista de libros
                        ForEach(viewModel.assignedBooks) { book in
                            AssignedBookCard(book: book) {
                                // Action for book details
                            }
                        }
                        .padding(.horizontal, 20)
                    }
                    .padding(.bottom, 20)
                }
                .refreshable {
                    await viewModel.loadAssignedBooks()
                }
            }
        }
        .onAppear {
            viewModel.loadAssignedBooks()
        }
    }
}

// MARK: - Supporting Views
struct EmptyAssignedBooksView: View {
    var body: some View {
        VStack(spacing: 24) {
            VStack(spacing: 16) {
                Image(systemName: "book.closed")
                    .font(.system(size: 60))
                    .foregroundColor(.secondary.opacity(0.6))
                
                VStack(spacing: 8) {
                    Text("No tienes libros prestados")
                        .font(.title2)
                        .fontWeight(.semibold)
                        .foregroundColor(.primary)
                    
                    Text("Explora nuestro catálogo y solicita tus primeros libros para comenzar tu biblioteca personal.")
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
                        gradient: Gradient(colors: [.blue, .blue.opacity(0.8)]),
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
                .cornerRadius(12)
                .shadow(color: .blue.opacity(0.3), radius: 8, x: 0, y: 4)
            }
            .padding(.horizontal, 40)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct AssignedBookCard: View {
    let book: AssignedBookItem
    let onTap: () -> Void
    
    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 16) {
                // Imagen del libro
                Rectangle()
                    .fill(Color.secondary.opacity(0.2))
                    .frame(width: 60, height: 80)
                    .cornerRadius(6)
                    .overlay(
                        Image(systemName: "book.closed.fill")
                            .font(.title3)
                            .foregroundColor(.secondary.opacity(0.5))
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
                    
                    // Estado del préstamo
                    VStack(alignment: .leading, spacing: 4) {
                        HStack(spacing: 4) {
                            Image(systemName: "calendar")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            
                            Text("Prestado: \(book.assignedDate, style: .date)")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        
                        HStack(spacing: 4) {
                            Image(systemName: book.isOverdue ? "exclamationmark.triangle.fill" : "clock.fill")
                                .font(.caption)
                                .foregroundColor(book.isOverdue ? .red : (book.daysUntilDue <= 3 ? .orange : .green))
                            
                            Text(book.dueDateText)
                                .font(.caption)
                                .fontWeight(.medium)
                                .foregroundColor(book.isOverdue ? .red : (book.daysUntilDue <= 3 ? .orange : .green))
                        }
                    }
                }
                
                Spacer()
                
                // Indicador de estado
                VStack(spacing: 8) {
                    Circle()
                        .fill(book.isOverdue ? Color.red : (book.daysUntilDue <= 3 ? Color.orange : Color.green))
                        .frame(width: 12, height: 12)
                    
                    if book.isOverdue || book.daysUntilDue <= 3 {
                        Text("\(abs(book.daysUntilDue))")
                            .font(.caption2)
                            .fontWeight(.bold)
                            .foregroundColor(book.isOverdue ? .red : .orange)
                    }
                }
            }
            .padding(16)
            .background(
                book.isOverdue ? 
                    Color.red.opacity(0.05) : 
                    (book.daysUntilDue <= 3 ? Color.orange.opacity(0.05) : Color.secondary.opacity(0.05))
            )
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(
                        book.isOverdue ? 
                            Color.red.opacity(0.3) : 
                            (book.daysUntilDue <= 3 ? Color.orange.opacity(0.3) : Color.secondary.opacity(0.2)),
                        lineWidth: 1
                    )
            )
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// MARK: - Models
struct AssignedBookItem: Identifiable {
    let id: String
    let title: String
    let author: String
    let imageUrl: String?
    let assignedDate: Date
    let dueDate: Date
    let isOverdue: Bool
    let daysUntilDue: Int
    
    var dueDateText: String {
        if isOverdue {
            return "Vencido hace \(abs(daysUntilDue)) días"
        } else if daysUntilDue == 0 {
            return "Vence hoy"
        } else if daysUntilDue == 1 {
            return "Vence mañana"
        } else {
            return "Vence en \(daysUntilDue) días"
        }
    }
}

#Preview {
    NavigationView {
        AssignedBooksView()
    }
}
