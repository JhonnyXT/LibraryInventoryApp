import SwiftUI
import shared

// MARK: - EmailViewModel
@MainActor
class EmailViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var lastResult: String?
    @Published var errorMessage: String?
    
    private let emailService = EmailService(apiKey: "demo_api_key", fromEmail: "demo@biblioteca.com")
    
    func sendBookAssignmentEmail(
        adminEmail: String,
        userEmail: String,
        userName: String,
        bookTitle: String,
        bookAuthor: String,
        adminName: String
    ) async -> Bool {
        isLoading = true
        errorMessage = nil
        
        do {
            print("🔧 Enviando email de asignación...")
            let result = try await emailService.sendBookAssignmentEmail(
                adminEmail: adminEmail,
                userEmail: userEmail,
                userName: userName,
                bookTitle: bookTitle,
                bookAuthor: bookAuthor,
                adminName: adminName
            )
            
            await MainActor.run {
                switch result {
                case .success(let message):
                    self.lastResult = message
                    print("🔧 Email enviado exitosamente: \(message)")
                case .failure(let error):
                    self.errorMessage = "Error enviando email: \(error)"
                    print("🔧 Error enviando email: \(error)")
                }
                self.isLoading = false
            }
            
            return result.isSuccess
            
        } catch {
            await MainActor.run {
                self.errorMessage = "Excepción enviando email: \(error.localizedDescription)"
                self.isLoading = false
            }
            print("🔧 Excepción enviando email: \(error)")
            return false
        }
    }
    
    func sendBookExpirationReminder(
        userEmail: String,
        userName: String,
        bookTitle: String,
        bookAuthor: String,
        expirationDate: String,
        daysOverdue: String
    ) async -> Bool {
        isLoading = true
        errorMessage = nil
        
        do {
            print("🔧 Enviando recordatorio de vencimiento...")
            let result = try await emailService.sendBookExpirationReminderEmail(
                userEmail: userEmail,
                userName: userName,
                bookTitle: bookTitle,
                bookAuthor: bookAuthor,
                expirationDate: expirationDate,
                daysOverdue: daysOverdue
            )
            
            await MainActor.run {
                switch result {
                case .success(let message):
                    self.lastResult = message
                    print("🔧 Recordatorio enviado exitosamente: \(message)")
                case .failure(let error):
                    self.errorMessage = "Error enviando recordatorio: \(error)"
                    print("🔧 Error enviando recordatorio: \(error)")
                }
                self.isLoading = false
            }
            
            return result.isSuccess
            
        } catch {
            await MainActor.run {
                self.errorMessage = "Excepción enviando recordatorio: \(error.localizedDescription)"
                self.isLoading = false
            }
            print("🔧 Excepción enviando recordatorio: \(error)")
            return false
        }
    }
    
    func clearResults() {
        lastResult = nil
        errorMessage = nil
    }
}
