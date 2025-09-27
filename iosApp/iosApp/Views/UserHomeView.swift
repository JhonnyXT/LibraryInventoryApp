import SwiftUI
import shared

struct UserHomeView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @State private var selectedTab = 0
    @State private var showingProfile = false
    @StateObject private var assignedBooksViewModel = AssignedBooksViewModel()
    
    private var notificationBadgeCount: Int {
        assignedBooksViewModel.getNotificationCount()
    }
    
    var body: some View {
        TabView(selection: $selectedTab) {
            // Home/Explorar
            NavigationView {
                UserDashboardView()
                    .navigationTitle("Biblioteca")
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
            
            // Catálogo de Libros
            NavigationView {
                BookListView(isAdminView: false)
                    .navigationTitle("Catálogo")
                    .navigationBarTitleDisplayMode(.large)
            }
            .tabItem {
                Image(systemName: "books.vertical.fill")
                Text("Catálogo")
            }
            .tag(1)
            
            // Mis Libros
            NavigationView {
                AssignedBooksView()
                    .navigationTitle("Mis Libros")
                    .navigationBarTitleDisplayMode(.large)
            }
            .tabItem {
                Image(systemName: "book.closed.fill")
                Text("Mis Libros")
            }
            .tag(2)
            
            // Wishlist
            NavigationView {
                WishlistView()
                    .navigationTitle("Deseados")
                    .navigationBarTitleDisplayMode(.large)
            }
            .tabItem {
                Image(systemName: "heart.fill")
                Text("Deseados")
            }
            .tag(3)
            
            // Notificaciones
            NavigationView {
                NotificationsView()
                    .navigationTitle("Notificaciones")
                    .navigationBarTitleDisplayMode(.large)
            }
            .tabItem {
                ZStack {
                    Image(systemName: "bell.fill")
                    
                    if notificationBadgeCount > 0 {
                        Text("\(notificationBadgeCount)")
                            .font(.caption2)
                            .foregroundColor(.white)
                            .frame(width: 16, height: 16)
                            .background(Color.red)
                            .cornerRadius(8)
                            .offset(x: 8, y: -8)
                    }
                }
                Text("Alertas")
            }
            .tag(4)
        }
        .accentColor(.blue)
        .sheet(isPresented: $showingProfile) {
            ProfileView()
                .environmentObject(authViewModel)
        }
        .onAppear {
            assignedBooksViewModel.loadAssignedBooks()
        }
    }
}

// MARK: - User Dashboard
struct UserDashboardView: View {
    @State private var assignedBooksCount = 0
    @State private var wishlistCount = 0
    @State private var dueSoonCount = 0
    @State private var featuredBooks: [BookItem] = []
    @State private var isLoading = true
    
    var body: some View {
        ScrollView {
            LazyVStack(spacing: 24) {
                // Tarjeta de bienvenida
                WelcomeCard()
                
                // Estadísticas del usuario
                VStack(alignment: .leading, spacing: 16) {
                    Text("Mi Actividad")
                        .font(.title2)
                        .fontWeight(.bold)
                        .padding(.horizontal, 20)
                    
                    LazyVGrid(columns: [
                        GridItem(.flexible()),
                        GridItem(.flexible()),
                        GridItem(.flexible())
                    ], spacing: 12) {
                        UserStatCard(
                            title: "Prestados",
                            value: "\(assignedBooksCount)",
                            icon: "book.fill",
                            color: .blue
                        )
                        
                        UserStatCard(
                            title: "Deseados",
                            value: "\(wishlistCount)",
                            icon: "heart.fill",
                            color: .pink
                        )
                        
                        UserStatCard(
                            title: "Por Vencer",
                            value: "\(dueSoonCount)",
                            icon: "clock.fill",
                            color: dueSoonCount > 0 ? .orange : .green
                        )
                    }
                    .padding(.horizontal, 20)
                }
                
                // Acciones rápidas para usuario
                VStack(alignment: .leading, spacing: 16) {
                    Text("Explorar")
                        .font(.title2)
                        .fontWeight(.bold)
                        .padding(.horizontal, 20)
                    
                    ScrollView(.horizontal, showsIndicators: false) {
                        LazyHStack(spacing: 16) {
                            QuickExploreCard(
                                title: "Nuevos Libros",
                                subtitle: "Últimos agregados",
                                icon: "sparkles",
                                gradient: [.blue, .purple],
                                action: {
                                    // Navigate to new books
                                }
                            )
                            
                            QuickExploreCard(
                                title: "Categorías",
                                subtitle: "Explorar por tema",
                                icon: "list.bullet.rectangle",
                                gradient: [.green, .blue],
                                action: {
                                    // Navigate to categories
                                }
                            )
                            
                            QuickExploreCard(
                                title: "Recomendados",
                                subtitle: "Para ti",
                                icon: "star.fill",
                                gradient: [.orange, .red],
                                action: {
                                    // Navigate to recommendations
                                }
                            )
                        }
                        .padding(.horizontal, 20)
                    }
                }
                
                // Libros destacados
                if !featuredBooks.isEmpty {
                    VStack(alignment: .leading, spacing: 16) {
                        HStack {
                            Text("Libros Destacados")
                                .font(.title2)
                                .fontWeight(.bold)
                            
                            Spacer()
                            
                            Button("Ver todos") {
                                // Navigate to all books
                            }
                            .font(.subheadline)
                            .foregroundColor(.blue)
                        }
                        .padding(.horizontal, 20)
                        
                        ScrollView(.horizontal, showsIndicators: false) {
                            LazyHStack(spacing: 16) {
                                ForEach(featuredBooks) { book in
                                    FeaturedBookCard(book: book)
                                }
                            }
                            .padding(.horizontal, 20)
                        }
                    }
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
            assignedBooksCount = 3
            wishlistCount = 7
            dueSoonCount = 1
            
            // Libros destacados simulados
            featuredBooks = [
                BookItem(id: "1", title: "Vida Cristiana", author: "Autor 1", imageUrl: nil),
                BookItem(id: "2", title: "Estudio Bíblico", author: "Autor 2", imageUrl: nil),
                BookItem(id: "3", title: "Liderazgo", author: "Autor 3", imageUrl: nil)
            ]
            
            isLoading = false
        }
    }
    
    @MainActor
    private func loadDashboardData() async {
        // Simulación de carga asíncrona
        try? await Task.sleep(nanoseconds: 1_000_000_000) // 1 segundo
        assignedBooksCount = 3
        wishlistCount = 7
        dueSoonCount = 1
        
        featuredBooks = [
            BookItem(id: "1", title: "Vida Cristiana", author: "Autor 1", imageUrl: nil),
            BookItem(id: "2", title: "Estudio Bíblico", author: "Autor 2", imageUrl: nil),
            BookItem(id: "3", title: "Liderazgo", author: "Autor 3", imageUrl: nil)
        ]
        
        isLoading = false
    }
}

// MARK: - Supporting Views
struct WelcomeCard: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 8) {
                    Text("¡Bienvenido!")
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                    
                    Text("Explora nuestra biblioteca digital y encuentra tu próxima lectura.")
                        .font(.subheadline)
                        .foregroundColor(.white.opacity(0.9))
                        .lineLimit(2)
                }
                
                Spacer()
                
                Image(systemName: "books.vertical.fill")
                    .font(.largeTitle)
                    .foregroundColor(.white.opacity(0.8))
            }
        }
        .padding(20)
        .background(
            LinearGradient(
                gradient: Gradient(colors: [.blue, .purple]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .cornerRadius(16)
        .padding(.horizontal, 20)
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
                .lineLimit(1)
        }
        .frame(height: 80)
        .frame(maxWidth: .infinity)
        .background(color.opacity(0.1))
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(color.opacity(0.3), lineWidth: 1)
        )
    }
}

struct QuickExploreCard: View {
    let title: String
    let subtitle: String
    let icon: String
    let gradient: [Color]
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 8) {
                Image(systemName: icon)
                    .font(.title2)
                    .foregroundColor(.white)
                
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.headline)
                        .fontWeight(.semibold)
                        .foregroundColor(.white)
                    
                    Text(subtitle)
                        .font(.caption)
                        .foregroundColor(.white.opacity(0.8))
                }
            }
            .frame(width: 140, height: 100)
            .padding(16)
            .background(
                LinearGradient(
                    gradient: Gradient(colors: gradient),
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
            .cornerRadius(12)
        }
        .buttonStyle(PlainButtonStyle())
    }
}

struct FeaturedBookCard: View {
    let book: BookItem
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Rectangle()
                .fill(Color.secondary.opacity(0.2))
                .frame(width: 120, height: 160)
                .cornerRadius(8)
                .overlay(
                    Image(systemName: "book.closed.fill")
                        .font(.largeTitle)
                        .foregroundColor(.secondary.opacity(0.5))
                )
            
            VStack(alignment: .leading, spacing: 4) {
                Text(book.title)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                
                Text(book.author)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(1)
            }
        }
        .frame(width: 120)
    }
}

// MARK: - Models
struct BookItem: Identifiable {
    let id: String
    let title: String
    let author: String
    let imageUrl: String?
}

#Preview {
    UserHomeView()
        .environmentObject(AuthViewModel())
}
