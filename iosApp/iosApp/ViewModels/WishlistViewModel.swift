import SwiftUI
import shared

// MARK: - WishlistViewModel
@MainActor
class WishlistViewModel: ObservableObject {
    @Published var wishlistBooks: [WishlistBookItem] = []
    @Published var isLoading = true
    @Published var errorMessage: String?
    
    private let wishlistService = WishlistService()
    private let currentUserId = "demo-user-id" // TODO: En Fase 6 obtener del AuthService
    
    func loadWishlistBooks() {
        isLoading = true
        errorMessage = nil
        
        Task {
            do {
                // TODO: En Fase 6 usar WishlistRepository KMP real
                await loadSimulatedWishlist()
                await MainActor.run {
                    self.isLoading = false
                }
            } catch {
                await MainActor.run {
                    self.errorMessage = "Error cargando lista de deseos: \(error.localizedDescription)"
                    self.isLoading = false
                }
            }
        }
    }
    
    private func loadSimulatedWishlist() async {
        // Simulamos delay de red
        try? await Task.sleep(nanoseconds: 1_000_000_000)
        
        await MainActor.run {
            self.wishlistBooks = [
                WishlistBookItem(
                    id: "w1",
                    title: "Matrimonio y Familia",
                    author: "Gary Chapman",
                    imageUrl: nil,
                    categories: ["Matrimonio", "Familia"],
                    addedDate: Date().addingTimeInterval(-5 * 24 * 60 * 60),
                    isAvailable: false,
                    priority: .high
                ),
                WishlistBookItem(
                    id: "w2",
                    title: "Finanzas Bíblicas",
                    author: "Larry Burkett",
                    imageUrl: nil,
                    categories: ["Finanzas", "Vida cristiana"],
                    addedDate: Date().addingTimeInterval(-10 * 24 * 60 * 60),
                    isAvailable: true,
                    priority: .normal
                ),
                WishlistBookItem(
                    id: "w3",
                    title: "Evangelismo Efectivo",
                    author: "Billy Graham",
                    imageUrl: nil,
                    categories: ["Evangelismo", "Misiones"],
                    addedDate: Date().addingTimeInterval(-15 * 24 * 60 * 60),
                    isAvailable: true,
                    priority: .low
                ),
                WishlistBookItem(
                    id: "w4",
                    title: "Profecías Bíblicas",
                    author: "Hal Lindsey",
                    imageUrl: nil,
                    categories: ["Profecía bíblica", "Estudio bíblico"],
                    addedDate: Date().addingTimeInterval(-20 * 24 * 60 * 60),
                    isAvailable: false,
                    priority: .normal
                ),
                WishlistBookItem(
                    id: "w5",
                    title: "Historia de la Iglesia",
                    author: "Justo González",
                    imageUrl: nil,
                    categories: ["Iglesia", "Historia"],
                    addedDate: Date().addingTimeInterval(-25 * 24 * 60 * 60),
                    isAvailable: true,
                    priority: .high
                )
            ]
        }
        print("🔧 Wishlist cargada: \(wishlistBooks.count) libros")
    }
    
    func removeFromWishlist(_ book: WishlistBookItem) async -> Bool {
        do {
            let result = try await wishlistService.removeFromWishlist(
                userId: currentUserId,
                bookId: book.id
            )
            
            switch result {
            case .success:
                await MainActor.run {
                    self.wishlistBooks.removeAll { $0.id == book.id }
                }
                print("🔧 Libro removido de wishlist: \(book.title)")
                return true
            case .failure(let error):
                print("🔧 Error removiendo de wishlist: \(error)")
                return false
            }
        } catch {
            print("🔧 Excepción removiendo de wishlist: \(error)")
            return false
        }
    }
    
    func requestBook(_ book: WishlistBookItem) async -> Bool {
        // TODO: En Fase 6 implementar solicitud real usando BookService
        try? await Task.sleep(nanoseconds: 500_000_000)
        print("🔧 Libro solicitado desde wishlist: \(book.title)")
        return true
    }
    
    func toggleAvailabilityNotification(_ book: WishlistBookItem) async -> Bool {
        do {
            // TODO: En Fase 6 implementar notificación real
            let result = try await wishlistService.startMonitoring(userId: currentUserId)
            
            switch result {
            case .success:
                print("🔧 Notificación de disponibilidad activada: \(book.title)")
                return true
            case .failure(let error):
                print("🔧 Error activando notificación: \(error)")
                return false
            }
        } catch {
            print("🔧 Excepción activando notificación: \(error)")
            return false
        }
    }
}
