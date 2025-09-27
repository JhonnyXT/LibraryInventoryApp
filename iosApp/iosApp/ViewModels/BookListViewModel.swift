import SwiftUI
import shared

// MARK: - BookListViewModel
@MainActor
class BookListViewModel: ObservableObject {
    @Published var books: [BookDisplayItem] = []
    @Published var filteredBooks: [BookDisplayItem] = []
    @Published var isLoading = true
    @Published var errorMessage: String?
    @Published var searchText = ""
    @Published var selectedCategory = "Todas"
    
    private let emailService = EmailService(apiKey: "demo_key", fromEmail: "demo@email.com")
    private let wishlistService = WishlistService()
    
    private let categories = [
        "Todas", "Biblia", "Liderazgo", "Jóvenes", "Mujeres",
        "Profecía bíblica", "Familia", "Matrimonio", "Finanzas",
        "Estudio bíblico", "Evangelismo", "Navidad", "Emaus",
        "Misiones", "Devocionales", "Curso vida", "Iglesia",
        "Vida cristiana", "Libros de la Biblia", "Enciclopedia",
        "Religiones", "Inglés", "Infantil"
    ]
    
    func loadBooks() {
        isLoading = true
        errorMessage = nil
        
        Task {
            do {
                // TODO: En Fase 6 usaremos BookRepository KMP real
                // Por ahora mantenemos datos simulados pero con estructura KMP
                await loadSimulatedBooks()
                await MainActor.run {
                    self.filterBooks()
                    self.isLoading = false
                }
            } catch {
                await MainActor.run {
                    self.errorMessage = "Error cargando libros: \(error.localizedDescription)"
                    self.isLoading = false
                }
            }
        }
    }
    
    private func loadSimulatedBooks() async {
        // Simulamos delay de red
        try? await Task.sleep(nanoseconds: 1_000_000_000)
        
        await MainActor.run {
            self.books = [
                BookDisplayItem(
                    id: "1",
                    title: "Biblia de Estudio",
                    author: "Varios Autores",
                    description: "Biblia completa con comentarios detallados para el estudio profundo de las Escrituras.",
                    categories: ["Biblia", "Estudio bíblico"],
                    imageUrl: nil,
                    isAvailable: true,
                    quantity: 5,
                    assignedCount: 2
                ),
                BookDisplayItem(
                    id: "2",
                    title: "Liderazgo Cristiano",
                    author: "John Maxwell",
                    description: "Principios de liderazgo bíblico para la vida moderna.",
                    categories: ["Liderazgo", "Vida cristiana"],
                    imageUrl: nil,
                    isAvailable: true,
                    quantity: 3,
                    assignedCount: 1
                ),
                BookDisplayItem(
                    id: "3",
                    title: "Matrimonio y Familia",
                    author: "Gary Chapman",
                    description: "Fundamentos bíblicos para una familia sólida.",
                    categories: ["Matrimonio", "Familia"],
                    imageUrl: nil,
                    isAvailable: false,
                    quantity: 2,
                    assignedCount: 2
                ),
                BookDisplayItem(
                    id: "4",
                    title: "Devocionales Diarios",
                    author: "Charles Spurgeon",
                    description: "365 días de reflexión espiritual profunda.",
                    categories: ["Devocionales", "Vida cristiana"],
                    imageUrl: nil,
                    isAvailable: true,
                    quantity: 4,
                    assignedCount: 0
                ),
                BookDisplayItem(
                    id: "5",
                    title: "Finanzas Bíblicas",
                    author: "Larry Burkett",
                    description: "Principios bíblicos para el manejo sabio del dinero.",
                    categories: ["Finanzas", "Vida cristiana"],
                    imageUrl: nil,
                    isAvailable: true,
                    quantity: 2,
                    assignedCount: 1
                ),
                BookDisplayItem(
                    id: "6",
                    title: "Evangelismo Efectivo",
                    author: "Billy Graham",
                    description: "Cómo compartir el evangelio con amor y efectividad.",
                    categories: ["Evangelismo", "Misiones"],
                    imageUrl: nil,
                    isAvailable: true,
                    quantity: 3,
                    assignedCount: 1
                )
            ]
        }
    }
    
    func filterBooks() {
        var filtered = books
        
        // Filtrar por categoría
        if selectedCategory != "Todas" {
            filtered = filtered.filter { $0.categories.contains(selectedCategory) }
        }
        
        // Filtrar por búsqueda
        if !searchText.isEmpty {
            filtered = filtered.filter { book in
                book.title.localizedCaseInsensitiveContains(searchText) ||
                book.author.localizedCaseInsensitiveContains(searchText) ||
                book.categories.joined().localizedCaseInsensitiveContains(searchText)
            }
        }
        
        filteredBooks = filtered
        print("🔧 Filtrado: \(filteredBooks.count) libros de \(books.count) total")
    }
    
    func addToWishlist(_ book: BookDisplayItem) async -> Bool {
        do {
            // TODO: En Fase 6 usaremos WishlistService KMP real con usuario actual
            let result = try await wishlistService.addToWishlist(
                userId: "demo-user",
                bookId: book.id,
                bookTitle: book.title
            )
            
            switch result {
            case .success:
                print("🔧 Libro agregado a wishlist: \(book.title)")
                return true
            case .failure(let error):
                print("🔧 Error agregando a wishlist: \(error)")
                return false
            }
        } catch {
            print("🔧 Excepción agregando a wishlist: \(error)")
            return false
        }
    }
    
    func requestBook(_ book: BookDisplayItem) async -> Bool {
        // TODO: En Fase 6 implementar solicitud real
        try? await Task.sleep(nanoseconds: 500_000_000)
        print("🔧 Libro solicitado: \(book.title)")
        return true
    }
}
