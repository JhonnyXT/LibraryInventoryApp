import SwiftUI
import shared

struct AdminHomeView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @State private var selectedTab = 0
    @State private var showingProfile = false
    
    var body: some View {
        TabView(selection: $selectedTab) {
            // Home/Dashboard
            NavigationView {
                AdminDashboardView()
                    .navigationTitle("Panel Admin")
                    .navigationBarTitleDisplayMode(.large)
                    .toolbar {
                        ToolbarItem(placement: .navigationBarTrailing) {
                            Button(action: { showingProfile = true }) {
                                Image(systemName: "person.circle")
                                    .font(.title2)
                            }
                        }
                    }
            }
            .tabItem {
                Image(systemName: "house.fill")
                Text("Inicio")
            }
            .tag(0)
            
            // Gestión de Libros
            NavigationView {
                BookListView(isAdminView: true)
                    .navigationTitle("Libros")
                    .navigationBarTitleDisplayMode(.large)
            }
            .tabItem {
                Image(systemName: "books.vertical.fill")
                Text("Libros")
            }
            .tag(1)
            
            // Registrar Libro
            NavigationView {
                RegisterBookView()
                    .navigationTitle("Nuevo Libro")
                    .navigationBarTitleDisplayMode(.large)
            }
            .tabItem {
                Image(systemName: "plus.circle.fill")
                Text("Agregar")
            }
            .tag(2)
            
            // Libros Vencidos
            NavigationView {
                OverdueBooksView()
                    .navigationTitle("Vencidos")
                    .navigationBarTitleDisplayMode(.large)
            }
            .tabItem {
                Image(systemName: "exclamationmark.triangle.fill")
                Text("Vencidos")
            }
            .tag(3)
            
            // Notificaciones
            NavigationView {
                NotificationsView()
                    .navigationTitle("Notificaciones")
                    .navigationBarTitleDisplayMode(.large)
            }
            .tabItem {
                Image(systemName: "bell.fill")
                Text("Alertas")
            }
            .tag(4)
        }
        .accentColor(.blue)
        .sheet(isPresented: $showingProfile) {
            ProfileView()
                .environmentObject(authViewModel)
        }
    }
}

// MARK: - Admin Dashboard
struct AdminDashboardView: View {
    @State private var totalBooks = 0
    @State private var availableBooks = 0
    @State private var assignedBooks = 0
    @State private var overdueBooks = 0
    @State private var isLoading = true
    
    var body: some View {
        ScrollView {
            LazyVStack(spacing: 20) {
                // Estadísticas principales
                VStack(alignment: .leading, spacing: 16) {
                    Text("Estadísticas de la Biblioteca")
                        .font(.title2)
                        .fontWeight(.bold)
                        .padding(.horizontal, 20)
                    
                    LazyVGrid(columns: [
                        GridItem(.flexible()),
                        GridItem(.flexible())
                    ], spacing: 16) {
                        StatCard(
                            title: "Total Libros",
                            value: "\(totalBooks)",
                            icon: "books.vertical.fill",
                            color: .blue
                        )
                        
                        StatCard(
                            title: "Disponibles",
                            value: "\(availableBooks)",
                            icon: "checkmark.circle.fill",
                            color: .green
                        )
                        
                        StatCard(
                            title: "Prestados",
                            value: "\(assignedBooks)",
                            icon: "person.fill.checkmark",
                            color: .orange
                        )
                        
                        StatCard(
                            title: "Vencidos",
                            value: "\(overdueBooks)",
                            icon: "exclamationmark.triangle.fill",
                            color: .red
                        )
                    }
                    .padding(.horizontal, 20)
                }
                
                // Acciones rápidas
                VStack(alignment: .leading, spacing: 16) {
                    Text("Acciones Rápidas")
                        .font(.title2)
                        .fontWeight(.bold)
                        .padding(.horizontal, 20)
                    
                    VStack(spacing: 12) {
                        QuickActionCard(
                            title: "Registrar Nuevo Libro",
                            subtitle: "Agregar un libro al inventario",
                            icon: "plus.circle.fill",
                            color: .blue,
                            action: {
                                // Navigate to RegisterBookView
                            }
                        )
                        
                        QuickActionCard(
                            title: "Gestionar Préstamos",
                            subtitle: "Ver y administrar libros prestados",
                            icon: "person.2.fill",
                            color: .orange,
                            action: {
                                // Navigate to assigned books
                            }
                        )
                        
                        QuickActionCard(
                            title: "Enviar Recordatorios",
                            subtitle: "Notificar sobre libros vencidos",
                            icon: "bell.badge.fill",
                            color: .red,
                            action: {
                                // Send reminders
                            }
                        )
                    }
                    .padding(.horizontal, 20)
                }
                
                // Actividad reciente
                VStack(alignment: .leading, spacing: 16) {
                    Text("Actividad Reciente")
                        .font(.title2)
                        .fontWeight(.bold)
                        .padding(.horizontal, 20)
                    
                    // Placeholder para actividad reciente
                    VStack(spacing: 12) {
                        ForEach(0..<3, id: \.self) { index in
                            RecentActivityCard(
                                title: "Libro asignado a usuario",
                                subtitle: "Hace 2 horas",
                                icon: "book.circle.fill"
                            )
                        }
                    }
                    .padding(.horizontal, 20)
                }
            }
            .padding(.bottom, 20)
        }
        .refreshable {
            await loadDashboardData()
        }
        .onAppear {
            loadDashboardData()
        }
    }
    
    private func loadDashboardData() {
        // TODO: Implementar carga de datos reales en Fase 6
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            totalBooks = 156
            availableBooks = 98
            assignedBooks = 45
            overdueBooks = 13
            isLoading = false
        }
    }
    
    @MainActor
    private func loadDashboardData() async {
        // Simulación de carga asíncrona
        try? await Task.sleep(nanoseconds: 1_000_000_000) // 1 segundo
        totalBooks = 156
        availableBooks = 98
        assignedBooks = 45
        overdueBooks = 13
        isLoading = false
    }
}

// MARK: - Supporting Views
struct StatCard: View {
    let title: String
    let value: String
    let icon: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: icon)
                .font(.title)
                .foregroundColor(color)
            
            VStack(spacing: 4) {
                Text(value)
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(.primary)
                
                Text(title)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
        }
        .frame(height: 100)
        .frame(maxWidth: .infinity)
        .background(color.opacity(0.1))
        .cornerRadius(16)
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(color.opacity(0.3), lineWidth: 1)
        )
    }
}

struct QuickActionCard: View {
    let title: String
    let subtitle: String
    let icon: String
    let color: Color
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 16) {
                Image(systemName: icon)
                    .font(.title2)
                    .foregroundColor(color)
                    .frame(width: 44, height: 44)
                    .background(color.opacity(0.1))
                    .cornerRadius(12)
                
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
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color.secondary.opacity(0.05))
            .cornerRadius(12)
        }
        .buttonStyle(PlainButtonStyle())
    }
}

struct RecentActivityCard: View {
    let title: String
    let subtitle: String
    let icon: String
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundColor(.blue)
                .frame(width: 36, height: 36)
                .background(Color.blue.opacity(0.1))
                .cornerRadius(8)
            
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .foregroundColor(.primary)
                
                Text(subtitle)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(Color.secondary.opacity(0.05))
        .cornerRadius(8)
    }
}

#Preview {
    AdminHomeView()
        .environmentObject(AuthViewModel())
}
