import SwiftUI
import shared

struct LoginView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @State private var email = ""
    @State private var password = ""
    @State private var showingPassword = false
    @State private var isAdminLogin = false
    @FocusState private var focusedField: Field?
    
    enum Field: Hashable {
        case email, password
    }
    
    var body: some View {
        GeometryReader { geometry in
            ScrollView {
                VStack(spacing: 30) {
                    Spacer(minLength: 50)
                    
                    // Logo y título
                    VStack(spacing: 20) {
                        Image(systemName: "books.vertical.fill")
                            .font(.system(size: 80))
                            .foregroundColor(.blue)
                        
                        VStack(spacing: 8) {
                            Text("Biblioteca Iglesia")
                                .font(.largeTitle)
                                .fontWeight(.bold)
                                .foregroundColor(.primary)
                            
                            Text("Hermanos en Cristo Bello")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding(.bottom, 30)
                    
                    // Formulario de login
                    VStack(spacing: 20) {
                        // Campo Email
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Correo Electrónico")
                                .font(.headline)
                                .foregroundColor(.primary)
                            
                            TextField("ejemplo@iglesia.com", text: $email)
                                .textFieldStyle(CustomTextFieldStyle())
                                .keyboardType(.emailAddress)
                                .textContentType(.emailAddress)
                                .autocapitalization(.none)
                                .disableAutocorrection(true)
                                .focused($focusedField, equals: .email)
                                .onSubmit {
                                    focusedField = .password
                                }
                        }
                        
                        // Campo Contraseña
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Contraseña")
                                .font(.headline)
                                .foregroundColor(.primary)
                            
                            HStack {
                                Group {
                                    if showingPassword {
                                        TextField("Tu contraseña", text: $password)
                                    } else {
                                        SecureField("Tu contraseña", text: $password)
                                    }
                                }
                                .textFieldStyle(CustomTextFieldStyle())
                                .textContentType(.password)
                                .focused($focusedField, equals: .password)
                                .onSubmit {
                                    performLogin()
                                }
                                
                                Button(action: {
                                    showingPassword.toggle()
                                }) {
                                    Image(systemName: showingPassword ? "eye.slash" : "eye")
                                        .foregroundColor(.secondary)
                                        .padding(.trailing, 12)
                                }
                            }
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(Color.secondary.opacity(0.3), lineWidth: 1)
                            )
                        }
                        
                        // Switch Administrador
                        Toggle(isOn: $isAdminLogin) {
                            HStack {
                                Image(systemName: "person.badge.key.fill")
                                    .foregroundColor(.orange)
                                Text("Iniciar como Administrador")
                                    .font(.subheadline)
                            }
                        }
                        .toggleStyle(SwitchToggleStyle(tint: .orange))
                        .padding(.horizontal, 4)
                    }
                    .padding(.horizontal, 24)
                    
                    // Botón de Login
                    VStack(spacing: 16) {
                        Button(action: performLogin) {
                            HStack {
                                if authViewModel.isLoading {
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                        .scaleEffect(0.8)
                                } else {
                                    Image(systemName: "arrow.right.circle.fill")
                                        .font(.title2)
                                }
                                
                                Text(authViewModel.isLoading ? "Iniciando sesión..." : "Iniciar Sesión")
                                    .font(.headline)
                                    .fontWeight(.semibold)
                            }
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 54)
                            .background(
                                LinearGradient(
                                    gradient: Gradient(colors: [.blue, .blue.opacity(0.8)]),
                                    startPoint: .leading,
                                    endPoint: .trailing
                                )
                            )
                            .cornerRadius(16)
                            .shadow(color: .blue.opacity(0.3), radius: 8, x: 0, y: 4)
                        }
                        .disabled(authViewModel.isLoading || email.isEmpty || password.isEmpty)
                        .opacity((authViewModel.isLoading || email.isEmpty || password.isEmpty) ? 0.6 : 1.0)
                        
                        // Google Sign-In Button (Placeholder para Fase 6)
                        Button(action: {
                            // TODO: Implementar Google Sign-In en Fase 6
                        }) {
                            HStack {
                                Image(systemName: "globe.americas.fill")
                                    .font(.title2)
                                    .foregroundColor(.red)
                                
                                Text("Continuar con Google")
                                    .font(.headline)
                                    .fontWeight(.medium)
                                    .foregroundColor(.primary)
                            }
                            .frame(maxWidth: .infinity)
                            .frame(height: 54)
                            .background(Color.secondary.opacity(0.1))
                            .overlay(
                                RoundedRectangle(cornerRadius: 16)
                                    .stroke(Color.secondary.opacity(0.3), lineWidth: 1)
                            )
                            .cornerRadius(16)
                        }
                        .disabled(true) // Habilitado en Fase 6
                        .opacity(0.5)
                    }
                    .padding(.horizontal, 24)
                    
                    // Mensaje de error
                    if let errorMessage = authViewModel.errorMessage {
                        Text(errorMessage)
                            .font(.caption)
                            .foregroundColor(.red)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 24)
                    }
                    
                    Spacer(minLength: 50)
                }
                .frame(minHeight: geometry.size.height)
            }
        }
        .background(
            LinearGradient(
                gradient: Gradient(colors: [
                    Color.blue.opacity(0.05),
                    Color.purple.opacity(0.05)
                ]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .onTapGesture {
            hideKeyboard()
        }
    }
    
    private func performLogin() {
        hideKeyboard()
        authViewModel.simulateLogin(email: email, password: password, asAdmin: isAdminLogin)
    }
    
    private func hideKeyboard() {
        focusedField = nil
    }
}

// MARK: - Custom Text Field Style
struct CustomTextFieldStyle: TextFieldStyle {
    func _body(configuration: TextField<Self._Label>) -> some View {
        configuration
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(Color.secondary.opacity(0.1))
            .cornerRadius(12)
    }
}

#Preview {
    LoginView()
        .environmentObject(AuthViewModel())
}
