import SwiftUI
import shared

struct ContentView: View {
    @StateObject private var authViewModel = AuthViewModel()
    
    var body: some View {
        Group {
            if authViewModel.isAuthenticated {
                if authViewModel.isAdmin {
                    AdminHomeView()
                        .environmentObject(authViewModel)
                } else {
                    UserHomeView()
                        .environmentObject(authViewModel)
                }
            } else {
                LoginView()
                    .environmentObject(authViewModel)
            }
        }
        .onAppear {
            authViewModel.checkAuthenticationStatus()
        }
    }
}

// MARK: - AuthViewModel
@MainActor
class AuthViewModel: ObservableObject {
    @Published var isAuthenticated = false
    @Published var isAdmin = false
    @Published var currentUser: AuthUser?
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let authService = AuthService()
    
    func checkAuthenticationStatus() {
        isLoading = true
        
        Task {
            do {
                let result = try await authService.getCurrentUser()
                
                await MainActor.run {
                    switch result {
                    case .success(let user):
                        if let user = user {
                            self.currentUser = user
                            self.isAuthenticated = true
                            self.isAdmin = user.isAdmin
                        } else {
                            self.isAuthenticated = false
                            self.isAdmin = false
                            self.currentUser = nil
                        }
                    case .failure(let error):
                        print("🔧 Error verificando autenticación: \(error)")
                        self.isAuthenticated = false
                        self.isAdmin = false
                        self.currentUser = nil
                        self.errorMessage = "Error verificando autenticación"
                    }
                    self.isLoading = false
                }
            } catch {
                await MainActor.run {
                    print("🔧 Excepción verificando autenticación: \(error)")
                    self.errorMessage = "Error verificando autenticación: \(error.localizedDescription)"
                    self.isAuthenticated = false
                    self.isAdmin = false
                    self.currentUser = nil
                    self.isLoading = false
                }
            }
        }
    }
    
    func performLogout() {
        isLoading = true
        
        Task {
            do {
                let result = try await authService.performLogout()
                
                await MainActor.run {
                    switch result {
                    case .success:
                        self.isAuthenticated = false
                        self.isAdmin = false
                        self.currentUser = nil
                        self.errorMessage = nil
                        print("🔧 Logout exitoso")
                    case .failure(let error):
                        print("🔧 Error en logout: \(error)")
                        self.errorMessage = "Error al cerrar sesión"
                    }
                    self.isLoading = false
                }
            } catch {
                await MainActor.run {
                    print("🔧 Excepción en logout: \(error)")
                    self.errorMessage = "Error al cerrar sesión: \(error.localizedDescription)"
                    self.isLoading = false
                }
            }
        }
    }
    
    func simulateLogin(email: String, password: String, asAdmin: Bool = false) {
        isLoading = true
        
        // 🚀 LOGIN SIMULADO PERO USANDO ESTRUCTURA KMP
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            if !email.isEmpty && !password.isEmpty {
                let user = AuthUser(
                    uid: "test-\(UUID().uuidString)",
                    email: email,
                    displayName: email.components(separatedBy: "@").first ?? "Usuario",
                    isAdmin: asAdmin
                )
                
                self.currentUser = user
                self.isAuthenticated = true
                self.isAdmin = asAdmin
                self.errorMessage = nil
                print("🔧 Login simulado exitoso como \(asAdmin ? "admin" : "usuario")")
            } else {
                self.errorMessage = "Por favor ingresa email y contraseña"
            }
            self.isLoading = false
        }
    }
}

#Preview {
    ContentView()
}
