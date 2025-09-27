import SwiftUI
import shared

// MARK: - AssignedBooksViewModel
@MainActor
class AssignedBooksViewModel: ObservableObject {
    @Published var assignedBooks: [AssignedBookItem] = []
    @Published var isLoading = true
    @Published var errorMessage: String?
    
    private let currentUserId = "demo-user-id" // TODO: En Fase 6 obtener del AuthService
    
    func loadAssignedBooks() {
        isLoading = true
        errorMessage = nil
        
        Task {
            do {
                // TODO: En Fase 6 usar BookRepository KMP real con filtro por usuario
                await loadSimulatedAssignedBooks()
                await MainActor.run {
                    self.isLoading = false
                }
            } catch {
                await MainActor.run {
                    self.errorMessage = "Error cargando libros asignados: \(error.localizedDescription)"
                    self.isLoading = false
                }
            }
        }
    }
    
    private func loadSimulatedAssignedBooks() async {
        // Simulamos delay de red
        try? await Task.sleep(nanoseconds: 1_000_000_000)
        
        let currentDate = Date()
        
        await MainActor.run {
            self.assignedBooks = [
                AssignedBookItem(
                    id: "a1",
                    title: "Biblia de Estudio",
                    author: "Varios Autores",
                    imageUrl: nil,
                    assignedDate: Calendar.current.date(byAdding: .day, value: -7, to: currentDate) ?? currentDate,
                    dueDate: Calendar.current.date(byAdding: .day, value: 7, to: currentDate) ?? currentDate,
                    isOverdue: false,
                    daysUntilDue: 7
                ),
                AssignedBookItem(
                    id: "a2",
                    title: "Liderazgo Cristiano",
                    author: "John Maxwell",
                    imageUrl: nil,
                    assignedDate: Calendar.current.date(byAdding: .day, value: -14, to: currentDate) ?? currentDate,
                    dueDate: Calendar.current.date(byAdding: .day, value: -2, to: currentDate) ?? currentDate,
                    isOverdue: true,
                    daysUntilDue: -2
                ),
                AssignedBookItem(
                    id: "a3",
                    title: "Devocionales Diarios",
                    author: "Charles Spurgeon",
                    imageUrl: nil,
                    assignedDate: Calendar.current.date(byAdding: .day, value: -3, to: currentDate) ?? currentDate,
                    dueDate: Calendar.current.date(byAdding: .day, value: 1, to: currentDate) ?? currentDate,
                    isOverdue: false,
                    daysUntilDue: 1
                ),
                AssignedBookItem(
                    id: "a4",
                    title: "Matrimonio y Familia",
                    author: "Gary Chapman",
                    imageUrl: nil,
                    assignedDate: Calendar.current.date(byAdding: .day, value: -21, to: currentDate) ?? currentDate,
                    dueDate: Calendar.current.date(byAdding: .day, value: 9, to: currentDate) ?? currentDate,
                    isOverdue: false,
                    daysUntilDue: 9
                )
            ]
        }
        
        print("🔧 Libros asignados cargados: \(assignedBooks.count) libros")
    }
    
    func renewBook(_ book: AssignedBookItem) async -> Bool {
        // TODO: En Fase 6 implementar renovación real usando BookService
        try? await Task.sleep(nanoseconds: 500_000_000)
        print("🔧 Libro renovado: \(book.title)")
        
        // Simulamos actualización de fecha
        await MainActor.run {
            if let index = self.assignedBooks.firstIndex(where: { $0.id == book.id }) {
                let newDueDate = Calendar.current.date(byAdding: .day, value: 14, to: Date()) ?? Date()
                let newDaysUntilDue = Calendar.current.dateComponents([.day], from: Date(), to: newDueDate).day ?? 14
                
                self.assignedBooks[index] = AssignedBookItem(
                    id: book.id,
                    title: book.title,
                    author: book.author,
                    imageUrl: book.imageUrl,
                    assignedDate: book.assignedDate,
                    dueDate: newDueDate,
                    isOverdue: false,
                    daysUntilDue: newDaysUntilDue
                )
            }
        }
        
        return true
    }
    
    func returnBook(_ book: AssignedBookItem) async -> Bool {
        // TODO: En Fase 6 implementar devolución real usando BookService
        try? await Task.sleep(nanoseconds: 500_000_000)
        print("🔧 Libro devuelto: \(book.title)")
        
        await MainActor.run {
            self.assignedBooks.removeAll { $0.id == book.id }
        }
        
        return true
    }
    
    func getNotificationCount() -> Int {
        return assignedBooks.filter { $0.isOverdue || $0.daysUntilDue <= 3 }.count
    }
}
