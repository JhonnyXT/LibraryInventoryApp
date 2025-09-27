import SwiftUI

struct OverdueBooksView: View {
    @State private var overdueBooks: [OverdueBookItemDisplay] = []
    @State private var isLoading = true
    @State private var selectedUrgency: UrgencyFilter = .all
    
    enum UrgencyFilter: String, CaseIterable {
        case all = "Todos"
        case critical = "Críticos"
        case high = "Alto"
        case medium = "Medio"
        case low = "Bajo"
        
        var color: Color {
            switch self {
            case .all: return .primary
            case .critical: return .red
            case .high: return .orange
            case .medium: return .yellow
            case .low: return .green
            }
        }
    }
    
    var filteredBooks: [OverdueBookItemDisplay] {
        if selectedUrgency == .all {
            return overdueBooks
        } else {
            return overdueBooks.filter { $0.urgencyLevel.rawValue == selectedUrgency.rawValue }
        }
    }
    
    var body: some View {
        VStack(spacing: 0) {
            // Filtros de urgencia
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(UrgencyFilter.allCases, id: \.self) { filter in
                        FilterChip(
                            title: filter.rawValue,
                            count: getCountForFilter(filter),
                            isSelected: selectedUrgency == filter,
                            color: filter.color
                        ) {
                            selectedUrgency = filter
                        }
                    }
                }
                .padding(.horizontal, 20)
            }
            .padding(.vertical, 16)
            .background(Color(UIColor.systemBackground))
            
            // Lista de libros vencidos
            Group {
                if isLoading {
                    ProgressView("Cargando libros vencidos...")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if filteredBooks.isEmpty {
                    EmptyOverdueBooksView(filter: selectedUrgency)
                } else {
                    ScrollView {
                        LazyVStack(spacing: 16) {
                            // Header con estadísticas
                            OverdueStatsHeader(books: filteredBooks)
                            
                            // Lista de libros
                            ForEach(filteredBooks) { book in
                                OverdueBookCard(book: book) {
                                    sendReminder(for: book)
                                }
                            }
                            .padding(.horizontal, 20)
                        }
                        .padding(.bottom, 20)
                    }
                    .refreshable {
                        await loadOverdueBooksAsync()
                    }
                }
            }
        }
        .onAppear {
            loadOverdueBooks()
        }
    }
    
    private func getCountForFilter(_ filter: UrgencyFilter) -> Int {
        if filter == .all {
            return overdueBooks.count
        } else {
            return overdueBooks.filter { $0.urgencyLevel.rawValue == filter.rawValue }.count
        }
    }
    
    private func loadOverdueBooks() {
        isLoading = true
        
        // TODO: Implementar carga real usando servicios KMP en Fase 6
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            overdueBooks = sampleOverdueBooks
            isLoading = false
        }
    }
    
    @MainActor
    private func loadOverdueBooksAsync() async {
        try? await Task.sleep(nanoseconds: 1_000_000_000)
        overdueBooks = sampleOverdueBooks
        isLoading = false
    }
    
    private func sendReminder(for book: OverdueBookItemDisplay) {
        // TODO: Implementar envío de recordatorio real en Fase 6
    }
    
    private var sampleOverdueBooks: [OverdueBookItemDisplay] {
        [
            OverdueBookItemDisplay(
                id: "1",
                bookTitle: "Liderazgo Cristiano",
                bookAuthor: "John Maxwell",
                userName: "Juan Pérez",
                userEmail: "juan@email.com",
                daysOverdue: 15,
                dueDate: Date().addingTimeInterval(-15 * 24 * 60 * 60),
                assignedDate: Date().addingTimeInterval(-45 * 24 * 60 * 60),
                urgencyLevel: .critical
            ),
            OverdueBookItemDisplay(
                id: "2",
                bookTitle: "Matrimonio y Familia",
                bookAuthor: "Gary Chapman",
                userName: "María García",
                userEmail: "maria@email.com",
                daysOverdue: 8,
                dueDate: Date().addingTimeInterval(-8 * 24 * 60 * 60),
                assignedDate: Date().addingTimeInterval(-38 * 24 * 60 * 60),
                urgencyLevel: .high
            ),
            OverdueBookItemDisplay(
                id: "3",
                bookTitle: "Finanzas Bíblicas",
                bookAuthor: "Larry Burkett",
                userName: "Carlos López",
                userEmail: "carlos@email.com",
                daysOverdue: 3,
                dueDate: Date().addingTimeInterval(-3 * 24 * 60 * 60),
                assignedDate: Date().addingTimeInterval(-33 * 24 * 60 * 60),
                urgencyLevel: .medium
            )
        ]
    }
}

// MARK: - Supporting Views
struct FilterChip: View {
    let title: String
    let count: Int
    let isSelected: Bool
    let color: Color
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 4) {
                Text(title)
                    .font(.caption)
                    .fontWeight(isSelected ? .semibold : .regular)
                
                if count > 0 {
                    Text("(\(count))")
                        .font(.caption2)
                        .opacity(0.8)
                }
            }
            .foregroundColor(isSelected ? .white : color)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(
                isSelected ? color : color.opacity(0.1)
            )
            .cornerRadius(16)
        }
        .buttonStyle(PlainButtonStyle())
    }
}

struct OverdueStatsHeader: View {
    let books: [OverdueBookItemDisplay]
    
    var averageDaysOverdue: Int {
        guard !books.isEmpty else { return 0 }
        return Int(books.map { $0.daysOverdue }.reduce(0, +) / books.count)
    }
    
    var criticalCount: Int {
        books.filter { $0.urgencyLevel == .critical }.count
    }
    
    var body: some View {
        VStack(spacing: 16) {
            Text("Resumen de Libros Vencidos")
                .font(.headline)
                .fontWeight(.bold)
                .padding(.horizontal, 20)
            
            HStack(spacing: 20) {
                StatCard(
                    title: "Total",
                    value: "\(books.count)",
                    icon: "exclamationmark.triangle.fill",
                    color: .orange
                )
                
                StatCard(
                    title: "Críticos",
                    value: "\(criticalCount)",
                    icon: "exclamationmark.circle.fill",
                    color: .red
                )
                
                StatCard(
                    title: "Promedio",
                    value: "\(averageDaysOverdue)d",
                    icon: "clock.fill",
                    color: .blue
                )
            }
            .padding(.horizontal, 20)
        }
        .padding(.top, 16)
    }
}

struct StatCard: View {
    let title: String
    let value: String
    let icon: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(color)
            
            Text(value)
                .font(.title3)
                .fontWeight(.bold)
                .foregroundColor(.primary)
            
            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 70)
        .background(color.opacity(0.1))
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(color.opacity(0.3), lineWidth: 1)
        )
    }
}

struct EmptyOverdueBooksView: View {
    let filter: UrgencyFilter
    
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 60))
                .foregroundColor(.green.opacity(0.6))
            
            VStack(spacing: 8) {
                Text(filter == .all ? "¡Excelente!" : "Sin libros \(filter.rawValue.lowercased())")
                    .font(.title2)
                    .fontWeight(.semibold)
                    .foregroundColor(.primary)
                
                Text(filter == .all ? 
                    "No hay libros vencidos en este momento." :
                    "No hay libros vencidos en esta categoría de urgencia."
                )
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct OverdueBookCard: View {
    let book: OverdueBookItemDisplay
    let onSendReminder: () -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header con urgencia
            HStack {
                HStack(spacing: 6) {
                    Circle()
                        .fill(book.urgencyLevel.color)
                        .frame(width: 8, height: 8)
                    
                    Text(book.urgencyLevel.title)
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundColor(book.urgencyLevel.color)
                }
                
                Spacer()
                
                Text("\(book.daysOverdue) días vencido")
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundColor(.red)
            }
            
            // Información del libro
            HStack(spacing: 12) {
                Rectangle()
                    .fill(Color.secondary.opacity(0.2))
                    .frame(width: 50, height: 70)
                    .cornerRadius(6)
                    .overlay(
                        Image(systemName: "book.closed.fill")
                            .font(.title3)
                            .foregroundColor(.secondary.opacity(0.5))
                    )
                
                VStack(alignment: .leading, spacing: 4) {
                    Text(book.bookTitle)
                        .font(.headline)
                        .fontWeight(.semibold)
                        .foregroundColor(.primary)
                        .lineLimit(2)
                    
                    Text("por \(book.bookAuthor)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    Text("Prestado a: \(book.userName)")
                        .font(.caption)
                        .foregroundColor(.blue)
                }
                
                Spacer()
            }
            
            // Fechas
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Vencimiento")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    
                    Text(book.dueDate, style: .date)
                        .font(.caption)
                        .fontWeight(.medium)
                        .foregroundColor(.red)
                }
                
                Spacer()
                
                VStack(alignment: .trailing, spacing: 2) {
                    Text("Prestado")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    
                    Text(book.assignedDate, style: .date)
                        .font(.caption)
                        .fontWeight(.medium)
                        .foregroundColor(.secondary)
                }
            }
            
            // Botones de acción
            HStack(spacing: 12) {
                Button(action: onSendReminder) {
                    HStack(spacing: 6) {
                        Image(systemName: "envelope.fill")
                            .font(.caption)
                        Text("Recordatorio")
                            .font(.caption)
                            .fontWeight(.medium)
                    }
                    .foregroundColor(.white)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(Color.orange)
                    .cornerRadius(8)
                }
                
                Button(action: {
                    // TODO: Contactar usuario
                }) {
                    HStack(spacing: 6) {
                        Image(systemName: "phone.fill")
                            .font(.caption)
                        Text("Contactar")
                            .font(.caption)
                            .fontWeight(.medium)
                    }
                    .foregroundColor(.white)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(Color.blue)
                    .cornerRadius(8)
                }
                
                Spacer()
            }
        }
        .padding(16)
        .background(book.urgencyLevel.color.opacity(0.05))
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(book.urgencyLevel.color.opacity(0.3), lineWidth: 1)
        )
    }
}

// MARK: - Models
struct OverdueBookItemDisplay: Identifiable {
    let id: String
    let bookTitle: String
    let bookAuthor: String
    let userName: String
    let userEmail: String
    let daysOverdue: Int
    let dueDate: Date
    let assignedDate: Date
    let urgencyLevel: UrgencyLevel
    
    enum UrgencyLevel: String, CaseIterable {
        case low = "Bajo"
        case medium = "Medio"
        case high = "Alto"
        case critical = "Críticos"
        
        var title: String { rawValue }
        
        var color: Color {
            switch self {
            case .low: return .green
            case .medium: return .yellow
            case .high: return .orange
            case .critical: return .red
            }
        }
    }
}

#Preview {
    NavigationView {
        OverdueBooksView()
    }
}
