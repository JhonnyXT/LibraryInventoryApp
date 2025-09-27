import SwiftUI

struct NotificationsView: View {
    @State private var notifications: [NotificationItemDisplay] = []
    @State private var isLoading = true
    @State private var selectedFilter: NotificationFilter = .all
    
    enum NotificationFilter: String, CaseIterable {
        case all = "Todas"
        case unread = "No leídas"
        case reminders = "Recordatorios"
        case assignments = "Asignaciones"
        case availability = "Disponibilidad"
        
        var icon: String {
            switch self {
            case .all: return "bell.fill"
            case .unread: return "bell.badge.fill"
            case .reminders: return "clock.fill"
            case .assignments: return "book.fill"
            case .availability: return "heart.fill"
            }
        }
    }
    
    var filteredNotifications: [NotificationItemDisplay] {
        switch selectedFilter {
        case .all:
            return notifications
        case .unread:
            return notifications.filter { !$0.isRead }
        case .reminders:
            return notifications.filter { $0.type == .reminder }
        case .assignments:
            return notifications.filter { $0.type == .assignment }
        case .availability:
            return notifications.filter { $0.type == .availability }
        }
    }
    
    var body: some View {
        VStack(spacing: 0) {
            // Filtros
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(NotificationFilter.allCases, id: \.self) { filter in
                        NotificationFilterChip(
                            filter: filter,
                            count: getCountForFilter(filter),
                            isSelected: selectedFilter == filter
                        ) {
                            selectedFilter = filter
                        }
                    }
                }
                .padding(.horizontal, 20)
            }
            .padding(.vertical, 16)
            .background(Color(UIColor.systemBackground))
            
            // Lista de notificaciones
            Group {
                if isLoading {
                    ProgressView("Cargando notificaciones...")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if filteredNotifications.isEmpty {
                    EmptyNotificationsView(filter: selectedFilter)
                } else {
                    ScrollView {
                        LazyVStack(spacing: 0) {
                            // Header
                            if selectedFilter == .all || selectedFilter == .unread {
                                NotificationsHeader(
                                    totalCount: notifications.count,
                                    unreadCount: notifications.filter { !$0.isRead }.count
                                )
                            }
                            
                            // Lista
                            ForEach(filteredNotifications) { notification in
                                NotificationCard(notification: notification) {
                                    markAsRead(notification)
                                } onAction: {
                                    handleNotificationAction(notification)
                                }
                                
                                if notification.id != filteredNotifications.last?.id {
                                    Divider()
                                        .padding(.leading, 60)
                                }
                            }
                        }
                        .padding(.bottom, 20)
                    }
                    .refreshable {
                        await loadNotificationsAsync()
                    }
                }
            }
        }
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Menu {
                    Button(action: markAllAsRead) {
                        Label("Marcar todas como leídas", systemImage: "checkmark.circle")
                    }
                    
                    Button(action: clearAllNotifications) {
                        Label("Eliminar todas", systemImage: "trash")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                }
            }
        }
        .onAppear {
            loadNotifications()
        }
    }
    
    private func getCountForFilter(_ filter: NotificationFilter) -> Int {
        switch filter {
        case .all:
            return notifications.count
        case .unread:
            return notifications.filter { !$0.isRead }.count
        case .reminders:
            return notifications.filter { $0.type == .reminder }.count
        case .assignments:
            return notifications.filter { $0.type == .assignment }.count
        case .availability:
            return notifications.filter { $0.type == .availability }.count
        }
    }
    
    private func loadNotifications() {
        isLoading = true
        
        // TODO: Implementar carga real usando servicios KMP en Fase 6
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            notifications = sampleNotifications
            isLoading = false
        }
    }
    
    @MainActor
    private func loadNotificationsAsync() async {
        try? await Task.sleep(nanoseconds: 1_000_000_000)
        notifications = sampleNotifications
        isLoading = false
    }
    
    private func markAsRead(_ notification: NotificationItemDisplay) {
        if let index = notifications.firstIndex(where: { $0.id == notification.id }) {
            notifications[index].isRead = true
        }
    }
    
    private func markAllAsRead() {
        for index in notifications.indices {
            notifications[index].isRead = true
        }
    }
    
    private func clearAllNotifications() {
        notifications.removeAll()
    }
    
    private func handleNotificationAction(_ notification: NotificationItemDisplay) {
        // TODO: Implementar navegación específica según el tipo de notificación
        markAsRead(notification)
    }
    
    private var sampleNotifications: [NotificationItemDisplay] {
        [
            NotificationItemDisplay(
                id: "1",
                title: "Libro próximo a vencer",
                message: "Tu préstamo 'Liderazgo Cristiano' vence en 2 días",
                type: .reminder,
                timestamp: Date().addingTimeInterval(-3600), // 1 hora atrás
                isRead: false,
                priority: .high
            ),
            NotificationItemDisplay(
                id: "2",
                title: "Nuevo libro asignado",
                message: "Se te ha asignado el libro 'Biblia de Estudio'",
                type: .assignment,
                timestamp: Date().addingTimeInterval(-7200), // 2 horas atrás
                isRead: false,
                priority: .normal
            ),
            NotificationItemDisplay(
                id: "3",
                title: "Libro disponible",
                message: "'Matrimonio y Familia' ya está disponible en tu lista de deseos",
                type: .availability,
                timestamp: Date().addingTimeInterval(-86400), // 1 día atrás
                isRead: true,
                priority: .normal
            ),
            NotificationItemDisplay(
                id: "4",
                title: "Recordatorio de devolución",
                message: "No olvides devolver 'Finanzas Bíblicas'",
                type: .reminder,
                timestamp: Date().addingTimeInterval(-172800), // 2 días atrás
                isRead: true,
                priority: .low
            ),
            NotificationItemDisplay(
                id: "5",
                title: "Libro vencido",
                message: "Tu préstamo 'Devocionales Diarios' está vencido hace 3 días",
                type: .reminder,
                timestamp: Date().addingTimeInterval(-259200), // 3 días atrás
                isRead: false,
                priority: .urgent
            )
        ]
    }
}

// MARK: - Supporting Views
struct NotificationFilterChip: View {
    let filter: NotificationFilter
    let count: Int
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Image(systemName: filter.icon)
                    .font(.caption2)
                
                Text(filter.rawValue)
                    .font(.caption)
                    .fontWeight(isSelected ? .semibold : .regular)
                
                if count > 0 {
                    Text("(\(count))")
                        .font(.caption2)
                        .opacity(0.8)
                }
            }
            .foregroundColor(isSelected ? .white : .blue)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(
                isSelected ? Color.blue : Color.blue.opacity(0.1)
            )
            .cornerRadius(16)
        }
        .buttonStyle(PlainButtonStyle())
    }
}

struct NotificationsHeader: View {
    let totalCount: Int
    let unreadCount: Int
    
    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("Notificaciones")
                    .font(.title2)
                    .fontWeight(.bold)
                
                Text("\(totalCount) notificaciones, \(unreadCount) sin leer")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 16)
    }
}

struct EmptyNotificationsView: View {
    let filter: NotificationFilter
    
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: filter.icon)
                .font(.system(size: 60))
                .foregroundColor(.secondary.opacity(0.6))
            
            VStack(spacing: 8) {
                Text(getEmptyTitle())
                    .font(.title2)
                    .fontWeight(.semibold)
                    .foregroundColor(.primary)
                
                Text(getEmptyMessage())
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
    
    private func getEmptyTitle() -> String {
        switch filter {
        case .all: return "Sin notificaciones"
        case .unread: return "¡Todo al día!"
        case .reminders: return "Sin recordatorios"
        case .assignments: return "Sin asignaciones"
        case .availability: return "Sin disponibilidad"
        }
    }
    
    private func getEmptyMessage() -> String {
        switch filter {
        case .all: return "No tienes notificaciones en este momento."
        case .unread: return "Has leído todas tus notificaciones."
        case .reminders: return "No hay recordatorios pendientes."
        case .assignments: return "No tienes nuevas asignaciones de libros."
        case .availability: return "No hay libros disponibles de tu lista de deseos."
        }
    }
}

struct NotificationCard: View {
    @State var notification: NotificationItemDisplay
    let onMarkAsRead: () -> Void
    let onAction: () -> Void
    
    var body: some View {
        Button(action: onAction) {
            HStack(spacing: 16) {
                // Icono de notificación
                Circle()
                    .fill(notification.priority.color.opacity(0.2))
                    .frame(width: 44, height: 44)
                    .overlay(
                        Image(systemName: notification.type.icon)
                            .font(.title3)
                            .foregroundColor(notification.priority.color)
                    )
                
                // Contenido
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Text(notification.title)
                            .font(.headline)
                            .fontWeight(notification.isRead ? .medium : .semibold)
                            .foregroundColor(.primary)
                            .lineLimit(1)
                        
                        Spacer()
                        
                        if !notification.isRead {
                            Circle()
                                .fill(Color.blue)
                                .frame(width: 8, height: 8)
                        }
                    }
                    
                    Text(notification.message)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                    
                    HStack {
                        Text(notification.timestamp, style: .relative)
                            .font(.caption)
                            .foregroundColor(.secondary)
                        
                        Spacer()
                        
                        Text(notification.priority.title)
                            .font(.caption2)
                            .fontWeight(.semibold)
                            .foregroundColor(notification.priority.color)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(notification.priority.color.opacity(0.1))
                            .cornerRadius(4)
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
            .background(notification.isRead ? Color.clear : Color.blue.opacity(0.05))
        }
        .buttonStyle(PlainButtonStyle())
        .contextMenu {
            if !notification.isRead {
                Button(action: {
                    notification.isRead = true
                    onMarkAsRead()
                }) {
                    Label("Marcar como leída", systemImage: "checkmark.circle")
                }
            }
            
            Button(role: .destructive, action: {
                // TODO: Eliminar notificación
            }) {
                Label("Eliminar", systemImage: "trash")
            }
        }
    }
}

// MARK: - Models
struct NotificationItemDisplay: Identifiable {
    let id: String
    let title: String
    let message: String
    let type: NotificationType
    let timestamp: Date
    var isRead: Bool
    let priority: NotificationPriority
    
    enum NotificationType {
        case reminder
        case assignment
        case availability
        case general
        
        var icon: String {
            switch self {
            case .reminder: return "clock.fill"
            case .assignment: return "book.fill"
            case .availability: return "heart.fill"
            case .general: return "bell.fill"
            }
        }
    }
    
    enum NotificationPriority {
        case low
        case normal
        case high
        case urgent
        
        var title: String {
            switch self {
            case .low: return "Baja"
            case .normal: return "Normal"
            case .high: return "Alta"
            case .urgent: return "Urgente"
            }
        }
        
        var color: Color {
            switch self {
            case .low: return .green
            case .normal: return .blue
            case .high: return .orange
            case .urgent: return .red
            }
        }
    }
}

#Preview {
    NavigationView {
        NotificationsView()
    }
}
