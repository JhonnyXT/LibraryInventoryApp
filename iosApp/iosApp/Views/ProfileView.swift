import SwiftUI
import shared

struct ProfileView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var showingLogoutAlert = false
    @State private var showingEditProfile = false
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    // Header con avatar
                    VStack(spacing: 16) {
                        // Avatar
                        Circle()
                            .fill(
                                LinearGradient(
                                    gradient: Gradient(colors: [.blue, .purple]),
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                            .frame(width: 100, height: 100)
                            .overlay(
                                Text(getInitials())
                                    .font(.largeTitle)
                                    .fontWeight(.semibold)
                                    .foregroundColor(.white)
                            )
                        
                        // Información del usuario
                        VStack(spacing: 8) {
                            Text(authViewModel.currentUser?.displayName ?? "Usuario")
                                .font(.title2)
                                .fontWeight(.bold)
                                .foregroundColor(.primary)
                            
                            Text(authViewModel.currentUser?.email ?? "")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                            
                            // Badge de rol
                            HStack {
                                Image(systemName: authViewModel.isAdmin ? "person.badge.key.fill" : "person.fill")
                                    .font(.caption)
                                    .foregroundColor(.white)
                                
                                Text(authViewModel.isAdmin ? "Administrador" : "Usuario")
                                    .font(.caption)
                                    .fontWeight(.semibold)
                                    .foregroundColor(.white)
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(authViewModel.isAdmin ? Color.orange : Color.blue)
                            .cornerRadius(16)
                        }
                    }
                    .padding(.top, 20)
                    
                    // Estadísticas del usuario
                    if !authViewModel.isAdmin {
                        UserStatsSection()
                    }
                    
                    // Opciones del perfil
                    VStack(spacing: 0) {
                        ProfileOptionRow(
                            icon: "person.circle",
                            title: "Editar Perfil",
                            subtitle: "Actualizar información personal",
                            action: { showingEditProfile = true }
                        )
                        
                        Divider()
                            .padding(.leading, 60)
                        
                        ProfileOptionRow(
                            icon: "bell.circle",
                            title: "Notificaciones",
                            subtitle: "Configurar alertas y recordatorios",
                            action: {
                                // TODO: Navegar a configuración de notificaciones
                            }
                        )
                        
                        if authViewModel.isAdmin {
                            Divider()
                                .padding(.leading, 60)
                            
                            ProfileOptionRow(
                                icon: "chart.bar.circle",
                                title: "Estadísticas",
                                subtitle: "Ver métricas de la biblioteca",
                                action: {
                                    // TODO: Navegar a estadísticas de admin
                                }
                            )
                            
                            Divider()
                                .padding(.leading, 60)
                            
                            ProfileOptionRow(
                                icon: "gear.circle",
                                title: "Configuración",
                                subtitle: "Ajustes de administrador",
                                action: {
                                    // TODO: Navegar a configuración de admin
                                }
                            )
                        }
                        
                        Divider()
                            .padding(.leading, 60)
                        
                        ProfileOptionRow(
                            icon: "questionmark.circle",
                            title: "Ayuda y Soporte",
                            subtitle: "Preguntas frecuentes y contacto",
                            action: {
                                // TODO: Navegar a ayuda
                            }
                        )
                        
                        Divider()
                            .padding(.leading, 60)
                        
                        ProfileOptionRow(
                            icon: "info.circle",
                            title: "Acerca de",
                            subtitle: "Versión 1.3.8",
                            action: {
                                // TODO: Mostrar información de la app
                            }
                        )
                    }
                    .background(Color.secondary.opacity(0.05))
                    .cornerRadius(12)
                    .padding(.horizontal, 20)
                    
                    // Botón de cerrar sesión
                    Button(action: { showingLogoutAlert = true }) {
                        HStack {
                            Image(systemName: "rectangle.portrait.and.arrow.right")
                                .font(.title2)
                            Text("Cerrar Sesión")
                                .font(.headline)
                                .fontWeight(.semibold)
                        }
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 54)
                        .background(
                            LinearGradient(
                                gradient: Gradient(colors: [.red, .red.opacity(0.8)]),
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .cornerRadius(16)
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 16)
                }
                .padding(.bottom, 40)
            }
            .navigationTitle("Perfil")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Cerrar") {
                        dismiss()
                    }
                }
            }
        }
        .alert("Cerrar Sesión", isPresented: $showingLogoutAlert) {
            Button("Cancelar", role: .cancel) { }
            Button("Cerrar Sesión", role: .destructive) {
                authViewModel.performLogout()
                dismiss()
            }
        } message: {
            Text("¿Estás seguro de que quieres cerrar sesión?")
        }
        .sheet(isPresented: $showingEditProfile) {
            EditProfileView()
                .environmentObject(authViewModel)
        }
    }
    
    private func getInitials() -> String {
        let name = authViewModel.currentUser?.displayName ?? authViewModel.currentUser?.email ?? "U"
        let components = name.components(separatedBy: " ")
        
        if components.count >= 2 {
            let firstInitial = components[0].prefix(1).uppercased()
            let lastInitial = components[1].prefix(1).uppercased()
            return "\(firstInitial)\(lastInitial)"
        } else {
            return String(name.prefix(1).uppercased())
        }
    }
}

// MARK: - Supporting Views
struct UserStatsSection: View {
    @State private var booksRead = 15
    @State private var currentBooks = 3
    @State private var wishlistCount = 7
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Mi Actividad")
                .font(.headline)
                .fontWeight(.bold)
                .padding(.horizontal, 20)
            
            HStack(spacing: 20) {
                UserStatCard(
                    title: "Libros Leídos",
                    value: "\(booksRead)",
                    icon: "book.fill",
                    color: .green
                )
                
                UserStatCard(
                    title: "Prestados",
                    value: "\(currentBooks)",
                    icon: "book.closed.fill",
                    color: .blue
                )
                
                UserStatCard(
                    title: "En Wishlist",
                    value: "\(wishlistCount)",
                    icon: "heart.fill",
                    color: .pink
                )
            }
            .padding(.horizontal, 20)
        }
    }
}

struct UserStatCard: View {
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
                .multilineTextAlignment(.center)
                .lineLimit(2)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 80)
        .background(color.opacity(0.1))
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(color.opacity(0.3), lineWidth: 1)
        )
    }
}

struct ProfileOptionRow: View {
    let icon: String
    let title: String
    let subtitle: String
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 16) {
                Image(systemName: icon)
                    .font(.title2)
                    .foregroundColor(.blue)
                    .frame(width: 28, height: 28)
                
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.headline)
                        .foregroundColor(.primary)
                        .multilineTextAlignment(.leading)
                    
                    Text(subtitle)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.leading)
                }
                
                Spacer()
                
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// MARK: - Edit Profile View
struct EditProfileView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var displayName = ""
    @State private var email = ""
    @State private var isLoading = false
    
    var body: some View {
        NavigationView {
            VStack(spacing: 24) {
                // Avatar editable
                VStack(spacing: 16) {
                    Circle()
                        .fill(
                            LinearGradient(
                                gradient: Gradient(colors: [.blue, .purple]),
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .frame(width: 100, height: 100)
                        .overlay(
                            Text(getInitials())
                                .font(.largeTitle)
                                .fontWeight(.semibold)
                                .foregroundColor(.white)
                        )
                        .overlay(
                            Button(action: {
                                // TODO: Cambiar foto de perfil
                            }) {
                                Image(systemName: "camera.fill")
                                    .font(.caption)
                                    .foregroundColor(.white)
                                    .frame(width: 28, height: 28)
                                    .background(Color.blue)
                                    .cornerRadius(14)
                            }
                            .offset(x: 35, y: 35)
                        )
                    
                    Text("Toca para cambiar foto")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .padding(.top, 20)
                
                // Campos de edición
                VStack(alignment: .leading, spacing: 16) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Nombre")
                            .font(.headline)
                            .foregroundColor(.primary)
                        
                        TextField("Tu nombre completo", text: $displayName)
                            .textFieldStyle(CustomFormFieldStyle())
                    }
                    
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Correo Electrónico")
                            .font(.headline)
                            .foregroundColor(.primary)
                        
                        TextField("tu@email.com", text: $email)
                            .textFieldStyle(CustomFormFieldStyle())
                            .keyboardType(.emailAddress)
                            .textContentType(.emailAddress)
                            .autocapitalization(.none)
                            .disableAutocorrection(true)
                    }
                }
                .padding(.horizontal, 20)
                
                Spacer()
                
                // Botón de guardar
                Button(action: saveProfile) {
                    HStack {
                        if isLoading {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                .scaleEffect(0.8)
                        } else {
                            Image(systemName: "checkmark.circle.fill")
                                .font(.title2)
                        }
                        
                        Text(isLoading ? "Guardando..." : "Guardar Cambios")
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
                }
                .disabled(isLoading || displayName.isEmpty || email.isEmpty)
                .opacity((isLoading || displayName.isEmpty || email.isEmpty) ? 0.6 : 1.0)
                .padding(.horizontal, 20)
                .padding(.bottom, 40)
            }
            .navigationTitle("Editar Perfil")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancelar") {
                        dismiss()
                    }
                }
            }
        }
        .onAppear {
            displayName = authViewModel.currentUser?.displayName ?? ""
            email = authViewModel.currentUser?.email ?? ""
        }
    }
    
    private func getInitials() -> String {
        let name = displayName.isEmpty ? authViewModel.currentUser?.displayName ?? email : displayName
        let components = name.components(separatedBy: " ")
        
        if components.count >= 2 {
            let firstInitial = components[0].prefix(1).uppercased()
            let lastInitial = components[1].prefix(1).uppercased()
            return "\(firstInitial)\(lastInitial)"
        } else {
            return String(name.prefix(1).uppercased())
        }
    }
    
    private func saveProfile() {
        isLoading = true
        
        // TODO: Implementar guardado real usando servicios KMP en Fase 6
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            isLoading = false
            dismiss()
        }
    }
}

#Preview {
    ProfileView()
        .environmentObject(AuthViewModel())
}
