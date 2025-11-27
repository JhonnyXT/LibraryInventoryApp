# 📚 GUÍA FUNCIONAL COMPLETA - LibraryInventoryApp

## 📋 ÍNDICE DE CONTENIDOS

1. [Introducción y Visión General](#-introducción-y-visión-general)
2. [Arquitectura del Sistema](#-arquitectura-del-sistema)
3. [Flujo de Autenticación](#-flujo-de-autenticación)
4. [Funcionalidades del Administrador](#-funcionalidades-del-administrador)
5. [Funcionalidades del Usuario](#-funcionalidades-del-usuario)
6. [Sistema de Notificaciones](#-sistema-de-notificaciones)
7. [Sistema de Automatización](#-sistema-de-automatización)
8. [Modelos de Datos y Firebase](#-modelos-de-datos-y-firebase)
9. [Configuración y Requisitos](#-configuración-y-requisitos)
10. [Flujos de Trabajo Completos](#-flujos-de-trabajo-completos)
11. [Consideraciones Importantes](#-consideraciones-importantes)

---

## 🎯 INTRODUCCIÓN Y VISIÓN GENERAL

### **¿Qué es LibraryInventoryApp?**

LibraryInventoryApp es un **sistema completo de gestión de bibliotecas** diseñado para iglesias y organizaciones que necesitan controlar el préstamo de libros de manera profesional y eficiente.

### **Problema que Resuelve**

- ❌ **Antes**: Control manual en cuadernos, sin recordatorios, pérdida de libros
- ✅ **Ahora**: Sistema digital con notificaciones automáticas, seguimiento en tiempo real, reportes instantáneos

### **Usuarios del Sistema**

| Rol | Descripción | Acceso |
|-----|-------------|--------|
| **👨‍💼 Administrador** | Gestiona todo el inventario y usuarios | Pantalla completa de administración |
| **👤 Usuario Regular** | Explora y solicita libros | Pantalla de usuario con catálogo |

### **Tecnologías Principales**

- **Frontend**: Kotlin + Material Design 3
- **Backend**: Firebase (Auth, Firestore, Storage)
- **Notificaciones**: Sistema híbrido (Push + Email con Brevo)
- **Automatización**: Node.js + Scripts personalizados

---

## 🏗️ ARQUITECTURA DEL SISTEMA

### **Estructura de Navegación**

```
📱 LibraryInventoryApp
│
├── 🔐 LoginActivity (Pantalla de Inicio)
│   ├── Login con Email/Password
│   └── Login con Google Sign-In
│
├── 👨‍💼 AdminActivity (Panel Administrador)
│   ├── 📚 Ver Libros (ViewBooksFragment)
│   └── ⏰ Devoluciones (OverdueBooksFragment)
│
└── 👤 UserActivity (Panel Usuario)
    ├── 🏠 Home (HomeModernFragment)
    ├── ⭐ Lista de Deseos (WishlistModernFragment)
    └── 📖 Mis Libros (AssignedBooksFragment)
```

### **Componentes del Sistema**

#### **Activities (Pantallas Principales)**
```kotlin
LoginActivity.kt          // Autenticación inicial
RegisterActivity.kt       // Registro de nuevos usuarios
AdminActivity.kt          // Panel de administrador
UserActivity.kt           // Panel de usuario regular
```

#### **Fragments (Secciones Funcionales)**

**Para Administrador:**
```kotlin
ViewBooksFragment.kt         // Ver y gestionar todos los libros
OverdueBooksFragment.kt      // Dashboard de libros vencidos
RegisterBookFragment.kt      // Registrar nuevos libros
EditBookFragment.kt          // Editar libros existentes
BookDetailAdminFragment.kt   // Detalles completos del libro
```

**Para Usuario:**
```kotlin
HomeModernFragment.kt        // Pantalla principal con catálogo
WishlistModernFragment.kt    // Lista de libros deseados
AssignedBooksFragment.kt     // Libros asignados al usuario
BookDetailModernFragment.kt  // Detalles del libro
NotificationsFragment.kt     // Centro de notificaciones
ProfileFragment.kt           // Perfil del usuario
```

#### **Utilidades (Utils)**
```kotlin
EmailService.kt                    // Envío de correos con Brevo
NotificationHelper.kt              // Notificaciones UI elegantes
LibraryNotificationManager.kt      // Gestor de notificaciones push
NotificationReceiver.kt            // Receptor de notificaciones
BootReceiver.kt                    // Reprogramación después de reinicio
PermissionHelper.kt                // Gestión de permisos
AuthManager.kt                     // Gestión de autenticación
WishlistAvailabilityService.kt     // Monitor de disponibilidad
```

---

## 🔐 FLUJO DE AUTENTICACIÓN

### **1. Inicio de Sesión (LoginActivity)**

#### **Opciones de Login**

**A. Login con Email y Contraseña**
```
Usuario ingresa:
├── Email (validación de formato)
└── Contraseña (mínimo 6 caracteres)
    ↓
Firebase Authentication
    ↓
Verificación de rol en Firestore
    ↓
Redirección según rol:
├── Admin → AdminActivity
└── Usuario → UserActivity
```

**B. Login con Google Sign-In**
```
Usuario hace clic en "Continuar con Google"
    ↓
Selector de cuenta de Google
    ↓
Autenticación con Firebase
    ↓
Verificación en Firestore:
├── Usuario existe → Navegar según rol
└── Usuario nuevo → Crear con rol "usuario"
    ↓
Redirección a pantalla correspondiente
```

#### **Validaciones Implementadas**

```kotlin
// Email
- No vacío
- Formato válido (ejemplo@dominio.com)

// Contraseña
- No vacía
- Mínimo 6 caracteres

// Mensajes de Error Personalizados
- "Correo o contraseña incorrectos"
- "No existe una cuenta con este correo"
- "Error de configuración de Google Sign-In"
```

### **2. Registro de Nuevos Usuarios (RegisterActivity)**

```
Usuario completa formulario:
├── Nombre completo
├── Email
├── Contraseña
└── Confirmación de contraseña
    ↓
Validaciones:
├── Todos los campos llenos
├── Email válido
├── Contraseñas coinciden
└── Contraseña mínimo 6 caracteres
    ↓
Crear usuario en Firebase Auth
    ↓
Guardar datos en Firestore:
{
  "name": "Nombre Usuario",
  "email": "usuario@email.com",
  "role": "usuario",
  "uid": "firebase_uid"
}
    ↓
Login automático
    ↓
Redirección a UserActivity
```

### **3. Verificación de Permisos**

```
Después de login exitoso:
    ↓
PermissionHelper verifica:
├── POST_NOTIFICATIONS (Android 13+)
├── SCHEDULE_EXACT_ALARM (Android 12+)
└── Otros permisos necesarios
    ↓
Si faltan permisos:
├── Mostrar diálogo explicativo
└── Solicitar permisos
    ↓
Navegar a pantalla correspondiente
```

---

## 👨‍💼 FUNCIONALIDADES DEL ADMINISTRADOR

### **📚 1. Ver Libros (ViewBooksFragment)**

#### **Funcionalidades Principales**

**A. Vista General del Inventario**
```
Pantalla muestra:
├── 🔍 Barra de búsqueda (título, autor, ISBN)
├── 📊 Contador total de libros
├── 🏷️ Filtros inteligentes con chips
└── 📚 Lista completa de libros con RecyclerView
```

**B. Sistema de Filtros Avanzados**

```kotlin
Tipos de Filtros:

1. 📚 Todos los Libros
   - Muestra inventario completo
   - Sin restricciones

2. 🏷️ Por Categorías
   - Selección múltiple
   - 22 categorías disponibles:
     • Biblia, Liderazgo, Jóvenes
     • Mujeres, Profecía bíblica, Familia
     • Matrimonio, Finanzas, Estudio bíblico
     • Evangelismo, Navidad, Emaus
     • Misiones, Devocionales, Curso vida
     • Iglesia, Vida cristiana
     • Libros de la Biblia, Enciclopedia
     • Religiones, Inglés, Infantil

3. 👥 Por Usuario
   - Selección de usuario específico
   - Ver todos los libros asignados a ese usuario
   - Útil para seguimiento personalizado

4. 📅 Por Fechas
   - Filtrar por rango de fechas de asignación
   - Ver préstamos en período específico

5. ✅ Disponibles
   - Solo libros con status "Disponible"
   - Cantidad > 0

6. 📖 Asignados
   - Solo libros actualmente prestados
   - Con información de usuarios
```

**C. Búsqueda en Tiempo Real**

```kotlin
Características:
- Búsqueda instantánea mientras escribes
- Busca en: Título, Autor, ISBN
- Normalización de acentos (á = a)
- Case-insensitive (mayúsculas = minúsculas)
- Resultados inmediatos sin lag
```

**D. Acciones por Libro**

```kotlin
Para cada libro en la lista:

1. 👁️ Ver Detalles
   - Información completa del libro
   - Imagen, descripción, categorías
   - Historial de asignaciones

2. ✏️ Editar Libro
   - Modificar cualquier campo
   - Cambiar imagen
   - Actualizar categorías
   - Ajustar cantidad

3. 🗑️ Eliminar Libro
   - Confirmación obligatoria
   - Elimina de Firebase Storage (imagen)
   - Elimina de Firestore (datos)

4. 📤 Asignar a Usuario
   - Seleccionar usuario de lista
   - Establecer fecha de vencimiento
   - Envío automático de email
   - Notificación al usuario y admin

5. 📥 Desasignar de Usuario
   - Seleccionar usuario específico
   - Liberar ejemplar
   - Actualizar disponibilidad
```

**E. FAB (Floating Action Button) - Agregar Libro**

```
Click en botón flotante (+)
    ↓
Abre RegisterBookFragment
    ↓
Formulario de registro completo
```

### **⏰ 2. Devoluciones (OverdueBooksFragment)**

#### **Dashboard de Libros Vencidos**

**A. Vista General**

```
Pantalla muestra:
├── 🔍 Filtros por urgencia
├── 📊 Contador de libros filtrados
├── 📚 Lista de libros con estado
└── 🔄 Pull-to-refresh
```

**B. Filtros por Urgencia**

```kotlin
Estados de Urgencia:

1. 📚 Todos
   - Muestra todos los préstamos activos

2. 📅 Próximos (3-5 días)
   - Libros que vencen pronto
   - Color: Azul
   - Prioridad: Baja

3. ⚠️ Muy Próximos (1-2 días)
   - Vencimiento inminente
   - Color: Naranja claro
   - Prioridad: Media

4. 🚨 Vence HOY
   - Último día de préstamo
   - Color: Verde
   - Prioridad: Alta

5. 🔴 Vencido Reciente (1-3 días)
   - Recién vencidos
   - Color: Naranja
   - Prioridad: Alta

6. 🔥 Vencido Medio (4-7 días)
   - Vencidos hace varios días
   - Color: Rojo claro
   - Prioridad: Muy Alta

7. 🚨 CRÍTICO (+7 días)
   - Vencidos hace más de una semana
   - Color: Rojo intenso
   - Prioridad: Crítica
```

**C. Información por Libro**

```kotlin
Para cada libro vencido se muestra:

📚 Información del Libro:
├── Título
├── Autor
└── Imagen

👤 Información del Usuario:
├── Nombre completo
└── Email

📅 Información de Fechas:
├── Fecha de asignación
├── Fecha de vencimiento
└── Días vencidos (calculado)

🎨 Indicadores Visuales:
├── Badge de urgencia (color dinámico)
├── Icono según estado
└── Mensaje descriptivo
```

**D. Acciones Disponibles**

```kotlin
1. 📧 Enviar Recordatorio
   - Email automático al usuario
   - Template profesional HTML5
   - Información completa del libro
   - Fecha de vencimiento
   - Días vencidos
   - Progress indicator durante envío
   - Confirmación de envío exitoso

2. ✅ Marcar como Devuelto
   - Desasigna el libro del usuario
   - Actualiza disponibilidad
   - Remueve de la lista
   - Animación de eliminación

3. 🔄 Actualizar Lista
   - Pull-to-refresh
   - Recarga datos de Firebase
   - Actualiza contadores
```

**E. Sistema de Recordatorios Automáticos**

```kotlin
Recordatorios por Email:

Template incluye:
├── Header con gradiente dinámico (según urgencia)
├── Información del libro
├── Fecha de vencimiento
├── Días vencidos
├── Mensaje motivacional
└── Footer con información de contacto

Colores de Header:
├── Verde: Próximo a vencer
├── Azul: Recordatorio normal
├── Naranja: Urgente
└── Rojo: Crítico/Vencido
```

### **➕ 3. Registrar Libro (RegisterBookFragment)**

#### **Formulario Completo**

```kotlin
Campos del Formulario:

📝 Información Básica:
├── Título del Libro (requerido)
├── Autor (requerido)
├── ISBN (opcional)
└── Descripción (opcional)

🏷️ Categorización:
├── Categorías (selección múltiple)
└── Diálogo con 22 opciones

📊 Inventario:
└── Cantidad de ejemplares (requerido)

📷 Imagen:
├── Capturar con cámara
├── Seleccionar de galería
└── Vista previa antes de guardar

📱 Escaneo de Código:
└── Escanear código de barras/ISBN
```

#### **Funcionalidades de Cámara**

**A. Captura de Imagen**

```
Usuario hace clic en "Capturar Imagen"
    ↓
Verificar permiso de cámara
    ↓
Abrir CameraX
    ↓
Usuario toma foto
    ↓
Vista previa de la imagen
    ↓
Guardar temporalmente
```

**B. Escaneo de Código de Barras**

```
Usuario hace clic en "Escanear Código"
    ↓
Verificar permiso de cámara
    ↓
Abrir ZXing Scanner
    ↓
Detectar código de barras/QR
    ↓
ML Kit procesa código
    ↓
Autocompletar campo ISBN
```

#### **Proceso de Guardado**

```
Usuario hace clic en "Guardar Libro"
    ↓
Validaciones:
├── Título no vacío
├── Autor no vacío
├── Cantidad > 0
└── Al menos una categoría
    ↓
Si hay imagen:
├── Subir a Firebase Storage
├── Obtener URL pública
└── Progress indicator
    ↓
Guardar en Firestore:
{
  "title": "Título",
  "author": "Autor",
  "isbn": "ISBN",
  "description": "Descripción",
  "categories": ["Cat1", "Cat2"],
  "imageUrl": "https://...",
  "quantity": 5,
  "status": "Disponible",
  "createdDate": Timestamp,
  "assignedTo": [],
  "assignedWithNames": [],
  "assignedToEmails": [],
  "assignedDates": [],
  "loanExpirationDates": []
}
    ↓
Notificación de éxito
    ↓
Limpiar formulario
```

### **✏️ 4. Editar Libro (EditBookFragment)**

#### **Funcionalidades de Edición**

```kotlin
Campos Editables:

✏️ Información:
├── Título
├── Autor
├── ISBN
├── Descripción
├── Categorías
└── Cantidad

📷 Imagen:
├── Mantener imagen actual
├── Cambiar por nueva (cámara)
├── Cambiar por nueva (galería)
└── Vista previa de cambios

📅 Metadatos:
├── Fecha de creación (solo lectura)
└── Última edición (se actualiza automáticamente)
```

#### **Proceso de Actualización**

```
Usuario modifica campos
    ↓
Click en "Actualizar Libro"
    ↓
Validaciones (igual que registro)
    ↓
Si cambió imagen:
├── Eliminar imagen anterior de Storage
├── Subir nueva imagen
└── Actualizar URL
    ↓
Actualizar en Firestore:
{
  ...campos modificados,
  "lastEditedDate": Timestamp.now()
}
    ↓
Notificación de éxito
    ↓
Volver a lista de libros
```

---

## 👤 FUNCIONALIDADES DEL USUARIO

### **🏠 1. Home Moderno (HomeModernFragment)**

#### **Pantalla Principal**

```kotlin
Componentes de la Pantalla:

📱 Header:
├── Saludo personalizado ("¡Hola, [Nombre]!")
├── Título principal
├── Botón de notificaciones (con badge)
└── Botón de logout

🔍 Búsqueda:
├── Barra de búsqueda en tiempo real
└── Busca en título, autor, ISBN

🏷️ Categorías:
├── Chips horizontales con scroll
├── Selección de categoría
└── Filtrado instantáneo

📚 Libros por Categoría:
├── RecyclerView horizontal
├── Cards con imagen y datos
└── Click para ver detalles

⭐ Libros Populares:
├── Sección separada
├── Libros más solicitados
└── Diseño atractivo
```

#### **Interacciones del Usuario**

```kotlin
1. 🔍 Buscar Libro
   - Escribir en barra de búsqueda
   - Resultados instantáneos
   - Filtrado por categoría activa

2. 🏷️ Filtrar por Categoría
   - Click en chip de categoría
   - Muestra solo libros de esa categoría
   - Actualiza contador

3. 👁️ Ver Detalles
   - Click en card de libro
   - Abre BookDetailModernFragment
   - Información completa

4. ⭐ Agregar a Deseados
   - Click en botón de favorito
   - Guarda en lista de deseos
   - Notificación cuando esté disponible

5. 🔔 Ver Notificaciones
   - Click en icono de campana
   - Badge muestra cantidad pendiente
   - Abre NotificationsFragment

6. 🚪 Cerrar Sesión
   - Click en botón de logout
   - Confirmación
   - Limpieza de sesión (Firebase + Google)
   - Volver a LoginActivity
```

### **⭐ 2. Lista de Deseos (WishlistModernFragment)**

#### **Gestión de Favoritos**

```kotlin
Funcionalidades:

📊 Vista General:
├── Contador de libros guardados
├── Lista de libros deseados
└── Estado de disponibilidad

📚 Información por Libro:
├── Imagen del libro
├── Título y autor
├── Categorías
├── Estado: Disponible / No disponible
├── Fecha cuando se agregó
└── Indicador visual de disponibilidad

🔔 Notificaciones Automáticas:
├── WishlistAvailabilityService monitora
├── Detecta cuando libro está disponible
├── Envía notificación push automática
└── Remueve de lista al ser asignado
```

#### **Acciones Disponibles**

```kotlin
1. 👁️ Ver Detalles
   - Click en libro
   - Abre detalles completos
   - Opción de solicitar si disponible

2. 🗑️ Remover de Lista
   - Swipe o botón de eliminar
   - Confirmación
   - Actualiza Firestore

3. 🔄 Actualizar Estado
   - Pull-to-refresh
   - Verifica disponibilidad
   - Actualiza indicadores visuales

4. 📚 Explorar Catálogo
   - Si lista vacía
   - Botón para ir a Home
   - Descubrir más libros
```

#### **Sistema de Monitoreo**

```kotlin
WishlistAvailabilityService:

Inicialización:
├── Se inicia al abrir UserActivity
├── Carga lista de deseos del usuario
└── Configura listeners de Firebase

Monitoreo en Tiempo Real:
├── Escucha cambios en colección "books"
├── Detecta cuando libro pasa a "Disponible"
├── Verifica si está en lista de deseos
└── Envía notificación push

Notificación:
├── Título: "¡Libro Disponible!"
├── Mensaje: "[Título] ya está disponible"
├── Click abre WishlistModernFragment
└── Badge en tab de lista de deseos

Limpieza:
├── Se detiene al cerrar UserActivity
├── Libera recursos
└── Cancela listeners activos
```

### **📖 3. Mis Libros (AssignedBooksFragment)**

#### **Libros Asignados al Usuario**

```kotlin
Vista de Préstamos:

📊 Información General:
├── Contador de libros asignados
├── Lista de préstamos activos
└── Estado de cada préstamo

📚 Información por Libro:
├── Imagen del libro
├── Título y autor
├── Fecha de asignación
├── Fecha de vencimiento
├── Días restantes / vencidos
└── Indicador visual de urgencia
```

#### **Estados de Préstamo**

```kotlin
Indicadores Visuales:

1. 🟢 A Tiempo (5+ días)
   - Color: Verde
   - Mensaje: "Vence en X días"
   - Sin urgencia

2. 🟡 Próximo a Vencer (3-4 días)
   - Color: Amarillo
   - Mensaje: "Vence pronto en X días"
   - Recordatorio suave

3. 🟠 Muy Próximo (1-2 días)
   - Color: Naranja
   - Mensaje: "Vence en X días"
   - Atención requerida

4. 🔴 Vence Hoy
   - Color: Rojo claro
   - Mensaje: "Vence HOY"
   - Urgente

5. 🚨 Vencido
   - Color: Rojo intenso
   - Mensaje: "Vencido hace X días"
   - Crítico
```

#### **Acciones del Usuario**

```kotlin
1. 👁️ Ver Detalles
   - Click en libro
   - Información completa
   - Fechas de préstamo

2. 🔔 Recibir Recordatorios
   - Notificaciones push automáticas
   - Emails de recordatorio
   - Escalamiento según urgencia

3. 🔄 Actualizar Lista
   - Pull-to-refresh
   - Recarga desde Firebase
   - Actualiza estados
```

### **🔔 4. Notificaciones (NotificationsFragment)**

#### **Centro de Notificaciones**

```kotlin
Bandeja de Entrada:

📊 Vista General:
├── Lista de notificaciones
├── Ordenadas por fecha (más reciente primero)
├── Indicador de leídas/no leídas
└── Opciones de gestión

📨 Tipos de Notificaciones:

1. 📚 Asignación de Libro
   - "Te han asignado: [Título]"
   - Fecha de asignación
   - Fecha de vencimiento

2. ⏰ Recordatorio de Vencimiento
   - "Recordatorio: [Título] vence en X días"
   - Urgencia según días restantes
   - Color dinámico

3. 🚨 Libro Vencido
   - "¡Atención! [Título] está vencido"
   - Días de retraso
   - Acción requerida

4. ⭐ Libro Disponible
   - "¡Buenas noticias! [Título] está disponible"
   - De lista de deseos
   - Opción de solicitar

5. ℹ️ Información General
   - Anuncios del sistema
   - Actualizaciones de la app
   - Mensajes administrativos
```

#### **Gestión de Notificaciones**

```kotlin
Acciones Disponibles:

1. 👁️ Ver Notificación
   - Click en notificación
   - Marca como leída
   - Muestra detalles completos

2. ✅ Marcar como Leída
   - Cambia estado visual
   - Actualiza en Firestore
   - Reduce contador de badge

3. 🗑️ Eliminar Notificación
   - Swipe para eliminar
   - Confirmación opcional
   - Elimina de Firestore

4. 🔄 Actualizar
   - Pull-to-refresh
   - Carga nuevas notificaciones
   - Actualiza contador

5. 📋 Marcar Todas como Leídas
   - Opción en menú
   - Actualiza todas a la vez
   - Limpia badge
```

### **👤 5. Perfil (ProfileFragment)**

#### **Información del Usuario**

```kotlin
Datos Mostrados:

👤 Información Personal:
├── Nombre completo
├── Email
├── Rol (Usuario/Admin)
└── UID de Firebase

📊 Estadísticas:
├── Libros actualmente asignados
├── Total de préstamos históricos
├── Libros en lista de deseos
└── Notificaciones pendientes

⚙️ Configuración:
├── Cambiar contraseña
├── Actualizar perfil
├── Preferencias de notificaciones
└── Cerrar sesión
```

---

## 🔔 SISTEMA DE NOTIFICACIONES

### **📱 Notificaciones Push**

#### **Arquitectura del Sistema**

```kotlin
Componentes:

1. LibraryNotificationManager
   - Gestor principal de notificaciones
   - Programa alarmas según urgencia
   - Crea canales de notificación

2. NotificationReceiver
   - BroadcastReceiver
   - Recibe alarmas programadas
   - Muestra notificaciones

3. BootReceiver
   - Escucha reinicio del dispositivo
   - Reprograma todas las alarmas
   - Mantiene notificaciones activas

4. PermissionHelper
   - Solicita permisos necesarios
   - Maneja respuestas del usuario
   - Guía a configuración si necesario
```

#### **Tipos de Notificaciones Push**

```kotlin
Escalamiento Inteligente:

1. 📅 Próximos (3-5 días)
   Frecuencia: 1 vez al día
   Horario: 10:00 AM
   Canal: "Próximos"
   Prioridad: Baja

2. ⚠️ Muy Próximos (1-2 días)
   Frecuencia: 1 vez al día
   Horario: 6:00 PM
   Canal: "Próximos"
   Prioridad: Media

3. 🚨 Vence HOY
   Frecuencia: 2 veces al día
   Horarios: 9:00 AM, 6:00 PM
   Canal: "Vencidos"
   Prioridad: Alta

4. 🔴 Vencido Reciente (1-3 días)
   Frecuencia: 2 veces al día
   Horarios: 10:00 AM, 4:00 PM
   Canal: "Vencidos"
   Prioridad: Alta

5. 🔥 Vencido Medio (4-7 días)
   Frecuencia: 3 veces al día
   Horarios: Cada 8 horas
   Canal: "Críticos"
   Prioridad: Muy Alta

6. 🚨 CRÍTICO (+7 días)
   Frecuencia: 6 veces al día
   Horarios: Cada 4 horas
   Canal: "Críticos"
   Prioridad: Máxima

7. ⚡ INMEDIATAS
   Frecuencia: Instantánea
   Trigger: Al asignar libro / cambiar fecha
   Canal: Según urgencia
   Prioridad: Alta
```

#### **Canales de Notificación**

```kotlin
Canales Configurados:

1. "library_upcoming"
   Nombre: "Próximos Vencimientos"
   Descripción: "Libros que vencen en 3-5 días"
   Importancia: IMPORTANCE_DEFAULT
   Sonido: Predeterminado
   Vibración: Patrón suave

2. "library_overdue"
   Nombre: "Libros Vencidos"
   Descripción: "Libros con vencimiento reciente"
   Importancia: IMPORTANCE_HIGH
   Sonido: Predeterminado
   Vibración: Patrón medio

3. "library_critical"
   Nombre: "Vencimientos Críticos"
   Descripción: "Libros vencidos hace más de 7 días"
   Importancia: IMPORTANCE_HIGH
   Sonido: Personalizado
   Vibración: Patrón intenso

4. "wishlist_availability"
   Nombre: "Libros Deseados Disponibles"
   Descripción: "Cuando libros de tu lista estén disponibles"
   Importancia: IMPORTANCE_DEFAULT
   Sonido: Alegre
   Vibración: Patrón suave
```

#### **Sistema de Alarmas**

```kotlin
Tipos de Alarmas:

1. AlarmManager.setExactAndAllowWhileIdle()
   - Alarmas exactas (Android 12+)
   - Funciona en Doze mode
   - Requiere permiso SCHEDULE_EXACT_ALARM

2. AlarmManager.setAndAllowWhileIdle()
   - Alarmas aproximadas (fallback)
   - No requiere permiso especial
   - Funciona en todos los dispositivos

3. AlarmManager.set()
   - Alarmas básicas (último fallback)
   - Compatibilidad universal
   - Menos preciso pero funcional

Estrategia de Fallback:
├── Intentar alarma exacta
├── Si falla → alarma aproximada
└── Si falla → alarma básica
```

#### **Navegación desde Notificaciones**

```kotlin
PendingIntent Inteligente:

Click en notificación:
    ↓
Verificar usuario autenticado
    ↓
Obtener rol del usuario
    ↓
Navegar según rol:
├── Admin → AdminActivity (tab Devoluciones)
└── Usuario → UserActivity (tab Mis Libros)
    ↓
Flags optimizados:
├── FLAG_ACTIVITY_NEW_TASK
└── FLAG_ACTIVITY_CLEAR_TOP
```

### **📧 Notificaciones Email**

#### **Proveedor: Brevo (Sendinblue)**

```kotlin
Configuración:

API: https://api.brevo.com/v3/smtp/email
Método: POST
Headers:
├── accept: application/json
├── api-key: [BREVO_API_KEY]
└── content-type: application/json

Límites:
├── Plan Gratuito: 9,000 emails/mes
├── Límite diario: 300 emails/día
└── Sin límite de destinatarios
```

#### **Tipos de Emails**

**1. Asignación de Libro (Dual)**

```kotlin
Destinatarios: Usuario + Admin

Template Usuario:
├── Subject: "📚 Nuevo libro asignado: [Título]"
├── Header: Gradiente azul Material Design
├── Contenido:
│   ├── Saludo personalizado
│   ├── Información del libro (card)
│   ├── Fecha de asignación
│   ├── Fecha de vencimiento
│   ├── Días de préstamo
│   └── Mensaje motivacional
└── Footer: Información de contacto

Template Admin:
├── Subject: "✅ Libro asignado exitosamente: [Título]"
├── Header: Gradiente verde
├── Contenido:
│   ├── Confirmación de asignación
│   ├── Información del libro
│   ├── Datos del usuario
│   ├── Fechas del préstamo
│   └── Resumen de inventario
└── Footer: Panel de administración
```

**2. Recordatorio de Vencimiento**

```kotlin
Destinatario: Usuario

Template Dinámico:
├── Subject: Varía según urgencia
│   ├── Próximo: "📅 Recordatorio: [Título] vence en X días"
│   ├── Hoy: "🚨 [Título] vence HOY"
│   └── Vencido: "🔴 [Título] está vencido (X días)"
│
├── Header: Color dinámico según urgencia
│   ├── Verde: Próximo (5+ días)
│   ├── Azul: Normal (3-4 días)
│   ├── Naranja: Urgente (1-2 días)
│   └── Rojo: Vencido
│
├── Contenido:
│   ├── Mensaje según estado
│   ├── Card con información del libro
│   ├── Fecha de vencimiento destacada
│   ├── Días restantes/vencidos
│   ├── Acción requerida
│   └── Mensaje motivacional personalizado
│
└── Footer: Información de contacto
```

**3. Nueva Versión de App**

```kotlin
Destinatarios: Todos los usuarios

Template:
├── Subject: "📱 LibraryInventoryApp [Versión] - Nueva versión disponible"
├── Header: Gradiente moderno
├── Contenido:
│   ├── Saludo personalizado
│   ├── Anuncio de nueva versión
│   ├── Lista de novedades (bullets)
│   ├── Botón de descarga (GitHub Release)
│   ├── Instrucciones de instalación
│   └── Tamaño del APK
└── Footer: Enlaces y soporte
```

#### **Características de los Templates**

```html
Tecnologías:
├── HTML5 con DOCTYPE
├── CSS inline para compatibilidad
├── Meta viewport para responsive
├── Tipografía: Segoe UI, sans-serif
└── Gradientes CSS modernos

Diseño:
├── Ancho máximo: 600px
├── Padding responsive
├── Cards con sombras suaves
├── Botones con hover effects
├── Iconos CSS (sin imágenes externas)
└── Compatible móvil y desktop

Colores Material Design 3:
├── Primario: #1976D2 (Azul)
├── Éxito: #4CAF50 (Verde)
├── Advertencia: #FF9800 (Naranja)
├── Error: #F44336 (Rojo)
└── Gradientes personalizados
```

#### **Proceso de Envío**

```kotlin
EmailService.kt:

Función Principal:
suspend fun sendBookAssignmentEmail(
    adminEmail: String,
    adminName: String,
    userEmail: String,
    userName: String,
    bookTitle: String,
    bookAuthor: String,
    bookImageUrl: String?,
    assignmentDate: String,
    expirationDate: String
): Result<String>

Flujo:
1. Construir template HTML
2. Crear payload JSON:
   {
     "sender": {
       "name": "Sistema de Biblioteca",
       "email": "[FROM_EMAIL]"
     },
     "to": [
       {"email": "[userEmail]", "name": "[userName]"}
     ],
     "subject": "...",
     "htmlContent": "[template]"
   }
3. Enviar con OkHttp
4. Manejar respuesta
5. Retornar Result<String>

Manejo de Errores:
├── Timeout: 30 segundos
├── Reintentos: Automáticos (OkHttp)
├── Logging: Detallado en Logcat
└── UI Feedback: NotificationHelper
```

---

## 🚀 SISTEMA DE AUTOMATIZACIÓN

### **📋 Release Automatizado**

#### **Comando Principal**

```bash
npm run release:patch "Descripción del cambio"
```

#### **Proceso Completo (7 Pasos)**

```
PASO 1: Version Bump
├── Lee build.gradle.kts
├── Incrementa versionCode (34 → 35)
├── Incrementa versionName (1.3.14 → 1.3.15)
├── Guarda cambios en archivo
├── Commit automático: "chore: bump version to 1.3.15"
└── ✅ Versión actualizada

PASO 2: Compilación APK
├── Ejecuta: gradlew.bat assembleRelease
├── Aplica Proguard + shrinking
├── Firma con libraryapp-keystore.jks
├── Genera: app-release.apk
├── Tamaño: ~7.47 MB
└── ✅ APK compilado

PASO 3: GitHub Release
├── Crea tag: v1.3.15
├── Genera release notes automáticas
├── Crea release en GitHub
├── Obtiene upload URL
└── ✅ Release creado

PASO 4: Upload APK
├── Renombra: LibraryInventoryApp-v1.3.15.apk
├── Sube a GitHub Release
├── Método principal: GitHub API
├── Fallback: gh CLI
└── ✅ APK disponible públicamente

PASO 5: Notificaciones Masivas
├── Lee usuarios de Firebase
├── Genera template email para cada uno
├── Envía con Brevo API
├── Log de envíos exitosos
└── ✅ Usuarios notificados

PASO 6: Push Automático
├── Ejecuta: git push
├── Sube commits y tags
├── Actualiza repositorio remoto
└── ✅ GitHub actualizado

PASO 7: Resumen Final
├── Muestra información completa
├── URL del release
├── Cantidad de emails enviados
└── ✅ Proceso completado
```

#### **Scripts de Automatización**

**1. scripts/release.js**

```javascript
Funciones Principales:

createRelease(releaseType, releaseNotes)
├── Orquesta todo el proceso
├── Maneja errores robustamente
└── Genera logs detallados

updateVersion(releaseType)
├── Modifica build.gradle.kts
├── Incrementa versiones
└── Commit automático

createGitHubRelease(versionName, versionCode, notes)
├── Usa GitHub REST API
├── Headers modernos (2022-11-28)
├── Crea release y tag
└── Retorna URL

uploadAPKToRelease(token, releaseUrl, apkPath, version)
├── Método principal: API upload
├── Fallback: gh CLI
├── Renombra APK dinámicamente
└── Verifica éxito

notifyUsers(versionName, releaseUrl, notes)
├── Ejecuta notify_new_version.js
├── Pasa parámetros
└── Captura output
```

**2. scripts/update_version.js**

```javascript
Funciones:

updateVersion(releaseType)
├── Lee build.gradle.kts
├── Parsea versionCode y versionName
├── Incrementa según tipo:
│   ├── patch: 1.3.14 → 1.3.15
│   ├── minor: 1.3.14 → 1.4.0
│   └── major: 1.3.14 → 2.0.0
├── Escribe archivo actualizado
├── Git add + commit
└── Retorna nueva versión

Versionado Semántico:
├── MAJOR: Cambios incompatibles
├── MINOR: Nuevas funcionalidades
└── PATCH: Correcciones de bugs
```

**3. notify_new_version.js**

```javascript
Funciones:

main(versionName, releaseUrl)
├── Inicializa Firebase Admin SDK
├── Lee colección "users"
├── Filtra usuarios válidos
├── Para cada usuario:
│   ├── Genera template HTML
│   ├── Personaliza contenido
│   ├── Envía con Brevo API
│   └── Log resultado
└── Muestra resumen final

sendEmail(userEmail, userName, version, url)
├── Construye template responsive
├── Configura Brevo client
├── Envía email transaccional
└── Maneja errores

Configuración:
├── Firebase: serviceAccountKey.json
├── Brevo: API key desde env
└── GitHub: URL del release
```

#### **Configuración Requerida**

```bash
Archivos Necesarios:

1. serviceAccountKey.json
   Ubicación: Raíz del proyecto
   Obtener: Firebase Console > Cuentas de servicio
   Permisos: Lectura de Firestore

2. local.properties
   Ubicación: Raíz del proyecto
   Contenido:
   BREVO_API_KEY=xkeysib-xxxxx
   GITHUB_TOKEN=ghp_xxxxx
   BREVO_FROM_EMAIL=tu-email@dominio.com

3. libraryapp-keystore.jks
   Ubicación: Raíz del proyecto
   Uso: Firmar APK de producción
   Configurado en: app/build.gradle.kts

4. package.json
   Ubicación: Raíz del proyecto
   Scripts NPM configurados
   Dependencias Node.js instaladas
```

#### **Comandos Disponibles**

```bash
# Release completo
npm run release:patch "Corrección de bugs"
npm run release:minor "Nueva funcionalidad"
npm run release:major "Cambio importante"

# Comandos individuales
npm run version:update patch  # Solo version bump
npm run build                 # Solo compilar APK
npm run notify "1.3.15" "url" # Solo notificaciones

# Ayuda
npm run help
```

---

## 🗄️ MODELOS DE DATOS Y FIREBASE

### **Estructura de Firestore**

```
libraryinventoryapp (Proyecto Firebase)
│
├── 📚 Colección: books
│   ├── Documento: [book_id_1]
│   │   ├── id: String
│   │   ├── title: String
│   │   ├── author: String
│   │   ├── isbn: String
│   │   ├── description: String
│   │   ├── categories: Array<String>
│   │   ├── imageUrl: String
│   │   ├── quantity: Number
│   │   ├── status: String ("Disponible" | "No disponible")
│   │   ├── assignedTo: Array<String> (UIDs)
│   │   ├── assignedWithNames: Array<String>
│   │   ├── assignedToEmails: Array<String>
│   │   ├── assignedDates: Array<Timestamp>
│   │   ├── loanExpirationDates: Array<Timestamp>
│   │   ├── createdDate: Timestamp
│   │   └── lastEditedDate: Timestamp
│   │
│   ├── Documento: [book_id_2]
│   └── ...
│
├── 👥 Colección: users
│   ├── Documento: [user_uid_1]
│   │   ├── uid: String
│   │   ├── name: String
│   │   ├── email: String
│   │   └── role: String ("admin" | "usuario")
│   │
│   ├── Documento: [user_uid_2]
│   └── ...
│
└── ⭐ Colección: wishlist
    ├── Documento: [wishlist_id_1]
    │   ├── id: String
    │   ├── userId: String (UID)
    │   ├── bookId: String
    │   ├── bookTitle: String
    │   ├── bookAuthor: String
    │   ├── bookImageUrl: String
    │   ├── bookCategories: Array<String>
    │   ├── addedDate: Timestamp
    │   ├── isAvailable: Boolean
    │   └── priority: Number
    │
    ├── Documento: [wishlist_id_2]
    └── ...
```

### **Modelo Book (Detallado)**

```kotlin
data class Book(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val author: String = "",
    val isbn: String = "",
    val categories: List<String> = emptyList(),
    val imageUrl: String? = null,
    val quantity: Int = 0,
    val status: String = "Disponible",
    val assignedTo: List<String>? = null,
    val assignedWithNames: List<String>? = null,
    val assignedToEmails: List<String>? = null,
    val assignedDates: List<Timestamp>? = null,
    val loanExpirationDates: List<Timestamp>? = null,
    val createdDate: Timestamp? = null,
    val lastEditedDate: Timestamp? = null
)

Explicación de Campos:

id: Identificador único del libro en Firestore
title: Título del libro (requerido)
description: Descripción detallada (opcional)
author: Nombre del autor (requerido)
isbn: Código ISBN del libro (opcional)
categories: Lista de categorías asignadas
imageUrl: URL de Firebase Storage (opcional)
quantity: Cantidad total de ejemplares
status: "Disponible" o "No disponible"

assignedTo: Array de UIDs de usuarios
assignedWithNames: Array de nombres (mismo índice que assignedTo)
assignedToEmails: Array de emails (mismo índice)
assignedDates: Array de fechas de asignación
loanExpirationDates: Array de fechas de vencimiento

Ejemplo de Asignación:
assignedTo: ["uid1", "uid2", "uid3"]
assignedWithNames: ["Juan Pérez", "María López", "Pedro García"]
assignedToEmails: ["juan@email.com", "maria@email.com", "pedro@email.com"]
assignedDates: [Timestamp(2024-01-15), Timestamp(2024-01-20), Timestamp(2024-01-25)]
loanExpirationDates: [Timestamp(2024-02-15), Timestamp(2024-02-20), Timestamp(2024-02-25)]

createdDate: Fecha de creación del libro
lastEditedDate: Última vez que se editó
```

### **Operaciones CRUD**

**1. Crear Libro**

```kotlin
val newBook = Book(
    title = "Título del Libro",
    author = "Autor",
    isbn = "1234567890",
    description = "Descripción",
    categories = listOf("Biblia", "Liderazgo"),
    imageUrl = "https://storage.googleapis.com/...",
    quantity = 5,
    status = "Disponible",
    createdDate = Timestamp.now(),
    assignedTo = emptyList(),
    assignedWithNames = emptyList(),
    assignedToEmails = emptyList(),
    assignedDates = emptyList(),
    loanExpirationDates = emptyList()
)

firestore.collection("books")
    .add(newBook)
    .addOnSuccessListener { documentReference ->
        val bookId = documentReference.id
        // Actualizar el ID en el documento
        documentReference.update("id", bookId)
    }
```

**2. Leer Libros**

```kotlin
// Todos los libros
firestore.collection("books")
    .get()
    .addOnSuccessListener { documents ->
        val booksList = documents.map { doc ->
            doc.toObject(Book::class.java).apply { id = doc.id }
        }
    }

// Libros disponibles
firestore.collection("books")
    .whereEqualTo("status", "Disponible")
    .get()

// Libros por categoría
firestore.collection("books")
    .whereArrayContains("categories", "Biblia")
    .get()

// Libros asignados a usuario
firestore.collection("books")
    .whereArrayContains("assignedTo", userId)
    .get()
```

**3. Actualizar Libro**

```kotlin
// Actualizar campos básicos
firestore.collection("books").document(bookId)
    .update(
        "title", newTitle,
        "author", newAuthor,
        "lastEditedDate", Timestamp.now()
    )

// Asignar libro a usuario
val currentAssignedTo = book.assignedTo?.toMutableList() ?: mutableListOf()
val currentNames = book.assignedWithNames?.toMutableList() ?: mutableListOf()
val currentEmails = book.assignedToEmails?.toMutableList() ?: mutableListOf()
val currentDates = book.assignedDates?.toMutableList() ?: mutableListOf()
val currentExpirations = book.loanExpirationDates?.toMutableList() ?: mutableListOf()

currentAssignedTo.add(userId)
currentNames.add(userName)
currentEmails.add(userEmail)
currentDates.add(Timestamp.now())
currentExpirations.add(expirationTimestamp)

val newQuantity = book.quantity - 1
val newStatus = if (newQuantity > 0) "Disponible" else "No disponible"

firestore.collection("books").document(bookId)
    .update(
        "assignedTo", currentAssignedTo,
        "assignedWithNames", currentNames,
        "assignedToEmails", currentEmails,
        "assignedDates", currentDates,
        "loanExpirationDates", currentExpirations,
        "quantity", newQuantity,
        "status", newStatus
    )
```

**4. Eliminar Libro**

```kotlin
// Eliminar imagen de Storage
if (book.imageUrl != null) {
    val storageRef = FirebaseStorage.getInstance()
        .getReferenceFromUrl(book.imageUrl!!)
    storageRef.delete()
}

// Eliminar documento de Firestore
firestore.collection("books").document(bookId)
    .delete()
    .addOnSuccessListener {
        // Libro eliminado exitosamente
    }
```

### **Firebase Storage**

```
Estructura:

gs://libraryinventoryapp.appspot.com/
│
└── book_images/
    ├── [book_id_1]_[timestamp].jpg
    ├── [book_id_2]_[timestamp].jpg
    └── ...

Operaciones:

1. Subir Imagen:
val storageRef = FirebaseStorage.getInstance().reference
val imageRef = storageRef.child("book_images/${bookId}_${System.currentTimeMillis()}.jpg")

imageRef.putFile(imageUri)
    .addOnSuccessListener { taskSnapshot ->
        imageRef.downloadUrl.addOnSuccessListener { uri ->
            val imageUrl = uri.toString()
            // Guardar URL en Firestore
        }
    }

2. Eliminar Imagen:
val imageRef = FirebaseStorage.getInstance()
    .getReferenceFromUrl(imageUrl)
imageRef.delete()

3. Actualizar Imagen:
// Eliminar imagen anterior
deleteOldImage(oldImageUrl)
// Subir nueva imagen
uploadNewImage(newImageUri)
```

---

## ⚙️ CONFIGURACIÓN Y REQUISITOS

### **Requisitos del Sistema**

```
Desarrollo:
├── Android Studio: Arctic Fox o superior
├── JDK: 11 o superior
├── Gradle: 8.0+
├── Node.js: 18+ (para automatización)
├── Git: 2.30+ (para control de versiones)
└── Firebase CLI: Opcional pero recomendado

Dispositivo/Emulador:
├── Android: 8.0 (API 26) o superior
├── RAM: 2GB mínimo
├── Espacio: 50MB para la app
└── Internet: Requerido para Firebase
```

### **Configuración Inicial (Paso a Paso)**

#### **1. Clonar Repositorio**

```bash
git clone https://github.com/JhonnyXT/LibraryInventoryApp.git
cd LibraryInventoryApp
```

#### **2. Configurar Firebase**

```bash
# A. Crear proyecto en Firebase Console
1. Ve a: https://console.firebase.google.com
2. Click en "Agregar proyecto"
3. Nombre: "libraryinventoryapp" (o el que prefieras)
4. Habilitar Google Analytics (opcional)
5. Crear proyecto

# B. Configurar Authentication
1. En Firebase Console > Authentication
2. Habilitar "Email/Password"
3. Habilitar "Google Sign-In"
4. Agregar SHA-1 del keystore:
   - Debug: Obtener con `keytool -list -v -keystore ~/.android/debug.keystore`
   - Release: SHA-1 del libraryapp-keystore.jks

# C. Configurar Firestore
1. En Firebase Console > Firestore Database
2. Crear base de datos
3. Modo: Producción
4. Ubicación: us-central (o la más cercana)
5. Configurar reglas de seguridad

# D. Configurar Storage
1. En Firebase Console > Storage
2. Comenzar
3. Modo: Producción
4. Ubicación: Misma que Firestore
5. Configurar reglas de seguridad

# E. Descargar google-services.json
1. En Firebase Console > Configuración del proyecto
2. Agregar app Android
3. Package name: com.example.libraryinventoryapp
4. Descargar google-services.json
5. Colocar en: app/google-services.json
```

#### **3. Configurar API Keys**

```bash
# Crear archivo local.properties en la raíz
touch local.properties

# Agregar contenido:
BREVO_API_KEY=xkeysib-tu_clave_aqui
GITHUB_TOKEN=ghp_tu_token_aqui
BREVO_FROM_EMAIL=tu-email@dominio.com

# Obtener Brevo API Key:
1. Registrarse en: https://www.brevo.com/es/
2. Ir a: Settings > API Keys
3. Crear nueva API Key
4. Copiar y pegar en local.properties

# Obtener GitHub Token:
1. GitHub > Settings > Developer settings
2. Personal access tokens > Tokens (classic)
3. Generate new token
4. Scopes: repo, write:packages
5. Copiar y pegar en local.properties
```

#### **4. Configurar Service Account**

```bash
# Descargar clave de servicio de Firebase
1. Firebase Console > Configuración del proyecto
2. Pestaña "Cuentas de servicio"
3. Click en "Generar nueva clave privada"
4. Guardar como: serviceAccountKey.json
5. Colocar en la raíz del proyecto

# Verificar que esté en .gitignore
echo "serviceAccountKey.json" >> .gitignore
```

#### **5. Instalar Dependencias**

```bash
# Dependencias Android (Gradle)
./gradlew build

# Dependencias Node.js
npm install

# Verificar instalación
npm list
```

#### **6. Configurar Keystore de Producción**

```bash
# El keystore ya existe: libraryapp-keystore.jks
# Configurado en: app/build.gradle.kts

# Credenciales:
Store Password: LibraryApp2024
Key Alias: libraryapp
Key Password: LibraryApp2024

# SHA-1 del keystore:
2D:27:86:D0:77:63:36:D6:D2:B9:57:46:15:C4:6B:C3:BC:F4:4D:58

# Agregar SHA-1 a Firebase:
1. Firebase Console > Configuración del proyecto
2. Tu app Android
3. Agregar huella digital
4. Pegar SHA-1
5. Guardar
```

### **Reglas de Seguridad Firebase**

#### **Firestore Rules**

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Función para verificar si el usuario está autenticado
    function isAuthenticated() {
      return request.auth != null;
    }
    
    // Función para verificar si el usuario es admin
    function isAdmin() {
      return isAuthenticated() && 
             get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
    }
    
    // Función para verificar si es el propio usuario
    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }
    
    // Reglas para colección "users"
    match /users/{userId} {
      // Leer: Solo el propio usuario o admin
      allow read: if isOwner(userId) || isAdmin();
      
      // Crear: Solo durante registro (sin rol admin)
      allow create: if isAuthenticated() && 
                      request.resource.data.uid == request.auth.uid &&
                      request.resource.data.role == 'usuario';
      
      // Actualizar: Solo el propio usuario (sin cambiar rol)
      allow update: if isOwner(userId) && 
                      request.resource.data.role == resource.data.role;
      
      // Eliminar: Solo admin
      allow delete: if isAdmin();
    }
    
    // Reglas para colección "books"
    match /books/{bookId} {
      // Leer: Cualquier usuario autenticado
      allow read: if isAuthenticated();
      
      // Crear: Solo admin
      allow create: if isAdmin();
      
      // Actualizar: Solo admin
      allow update: if isAdmin();
      
      // Eliminar: Solo admin
      allow delete: if isAdmin();
    }
    
    // Reglas para colección "wishlist"
    match /wishlist/{wishlistId} {
      // Leer: Solo el propio usuario o admin
      allow read: if isAuthenticated() && 
                    (resource.data.userId == request.auth.uid || isAdmin());
      
      // Crear: Solo el propio usuario
      allow create: if isAuthenticated() && 
                      request.resource.data.userId == request.auth.uid;
      
      // Actualizar: Solo el propio usuario
      allow update: if isAuthenticated() && 
                      resource.data.userId == request.auth.uid;
      
      // Eliminar: Solo el propio usuario
      allow delete: if isAuthenticated() && 
                      resource.data.userId == request.auth.uid;
    }
  }
}
```

#### **Storage Rules**

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    
    // Función para verificar si el usuario está autenticado
    function isAuthenticated() {
      return request.auth != null;
    }
    
    // Función para verificar si el usuario es admin
    function isAdmin() {
      return isAuthenticated() && 
             firestore.get(/databases/(default)/documents/users/$(request.auth.uid)).data.role == 'admin';
    }
    
    // Función para validar tipo de archivo
    function isImage() {
      return request.resource.contentType.matches('image/.*');
    }
    
    // Función para validar tamaño de archivo (máximo 5MB)
    function isValidSize() {
      return request.resource.size < 5 * 1024 * 1024;
    }
    
    // Reglas para imágenes de libros
    match /book_images/{imageId} {
      // Leer: Cualquier usuario autenticado
      allow read: if isAuthenticated();
      
      // Escribir: Solo admin, solo imágenes, máximo 5MB
      allow write: if isAdmin() && isImage() && isValidSize();
      
      // Eliminar: Solo admin
      allow delete: if isAdmin();
    }
  }
}
```

---

## 🔄 FLUJOS DE TRABAJO COMPLETOS

### **Flujo 1: Registro y Primer Login**

```
1. Usuario abre la app
   ↓
2. Ve LoginActivity
   ↓
3. Click en "Registrarse"
   ↓
4. Completa formulario en RegisterActivity:
   - Nombre: Juan Pérez
   - Email: juan@email.com
   - Contraseña: ******
   - Confirmar: ******
   ↓
5. Click en "Registrarse"
   ↓
6. Sistema valida datos
   ↓
7. Crea usuario en Firebase Auth
   ↓
8. Guarda datos en Firestore:
   {
     "name": "Juan Pérez",
     "email": "juan@email.com",
     "role": "usuario",
     "uid": "abc123"
   }
   ↓
9. Login automático
   ↓
10. PermissionHelper solicita permisos:
    - POST_NOTIFICATIONS
    - SCHEDULE_EXACT_ALARM
   ↓
11. Usuario acepta permisos
   ↓
12. Navega a UserActivity
   ↓
13. Muestra HomeModernFragment
   ↓
14. WishlistAvailabilityService inicia monitoreo
   ↓
15. Usuario puede explorar catálogo
```

### **Flujo 2: Admin Registra y Asigna Libro**

```
1. Admin hace login
   ↓
2. Navega a AdminActivity
   ↓
3. Ve ViewBooksFragment (por defecto)
   ↓
4. Click en FAB (+) para agregar libro
   ↓
5. Abre RegisterBookFragment
   ↓
6. Completa formulario:
   - Título: "Liderazgo Cristiano"
   - Autor: "John Maxwell"
   - ISBN: "9781234567890"
   - Descripción: "Libro sobre liderazgo..."
   - Categorías: ["Liderazgo", "Iglesia"]
   - Cantidad: 3
   ↓
7. Click en "Capturar Imagen"
   ↓
8. Toma foto con cámara
   ↓
9. Vista previa de imagen
   ↓
10. Click en "Guardar Libro"
   ↓
11. Sistema valida datos
   ↓
12. Sube imagen a Firebase Storage
   ↓
13. Obtiene URL de imagen
   ↓
14. Guarda libro en Firestore:
    {
      "title": "Liderazgo Cristiano",
      "author": "John Maxwell",
      "isbn": "9781234567890",
      "description": "Libro sobre...",
      "categories": ["Liderazgo", "Iglesia"],
      "imageUrl": "https://storage...",
      "quantity": 3,
      "status": "Disponible",
      "createdDate": Timestamp.now(),
      "assignedTo": [],
      "assignedWithNames": [],
      "assignedToEmails": [],
      "assignedDates": [],
      "loanExpirationDates": []
    }
   ↓
15. NotificationHelper muestra éxito
   ↓
16. Vuelve a ViewBooksFragment
   ↓
17. Libro aparece en la lista
   ↓
18. Admin busca usuario para asignar
   ↓
19. Click en libro > "Asignar a Usuario"
   ↓
20. Selecciona usuario: "Juan Pérez"
   ↓
21. Selecciona fecha de vencimiento: 30 días
   ↓
22. Click en "Asignar"
   ↓
23. Sistema actualiza libro en Firestore:
    - Agrega UID a assignedTo
    - Agrega nombre a assignedWithNames
    - Agrega email a assignedToEmails
    - Agrega fecha actual a assignedDates
    - Agrega fecha vencimiento a loanExpirationDates
    - Decrementa quantity
    - Actualiza status si quantity = 0
   ↓
24. EmailService envía 2 emails:
    A. Al usuario: "Nuevo libro asignado"
    B. Al admin: "Libro asignado exitosamente"
   ↓
25. LibraryNotificationManager programa notificaciones push
   ↓
26. NotificationHelper muestra éxito con progress
   ↓
27. Libro actualizado en lista
```

### **Flujo 3: Usuario Explora y Guarda en Lista de Deseos**

```
1. Usuario abre app
   ↓
2. Login automático (sesión persistente)
   ↓
3. Navega a UserActivity
   ↓
4. Ve HomeModernFragment
   ↓
5. Explora categorías con chips horizontales
   ↓
6. Click en chip "Liderazgo"
   ↓
7. Lista filtra y muestra solo libros de Liderazgo
   ↓
8. Ve libro "Liderazgo Cristiano" (No disponible)
   ↓
9. Click en libro
   ↓
10. Abre BookDetailModernFragment
   ↓
11. Ve información completa:
    - Imagen
    - Título, autor
    - Descripción
    - Categorías
    - Estado: "No disponible"
   ↓
12. Click en botón "⭐ Agregar a Deseados"
   ↓
13. Sistema guarda en Firestore:
    {
      "userId": "abc123",
      "bookId": "libro_id",
      "bookTitle": "Liderazgo Cristiano",
      "bookAuthor": "John Maxwell",
      "bookImageUrl": "https://...",
      "bookCategories": ["Liderazgo", "Iglesia"],
      "addedDate": Timestamp.now(),
      "isAvailable": false,
      "priority": 0
    }
   ↓
14. WishlistAvailabilityService detecta nuevo item
   ↓
15. Agrega listener para ese libro
   ↓
16. NotificationHelper muestra: "Agregado a lista de deseos"
   ↓
17. Usuario navega a tab "⭐ Deseados"
   ↓
18. Ve WishlistModernFragment
   ↓
19. Lista muestra libro con estado "No disponible"
   ↓
20. [Más tarde] Otro usuario devuelve el libro
   ↓
21. WishlistAvailabilityService detecta cambio
   ↓
22. Libro pasa a "Disponible"
   ↓
23. Sistema envía notificación push:
    "¡Libro Disponible! Liderazgo Cristiano ya está disponible"
   ↓
24. Usuario hace click en notificación
   ↓
25. App abre en WishlistModernFragment
   ↓
26. Libro muestra estado "Disponible" en verde
   ↓
27. Usuario puede solicitar el libro
```

### **Flujo 4: Sistema de Recordatorios Automáticos**

```
Escenario: Libro vence en 3 días

1. LibraryNotificationManager verifica préstamos
   ↓
2. Detecta libro que vence en 3 días
   ↓
3. Clasifica como "Próximo"
   ↓
4. Programa notificación push:
   - Frecuencia: 1 vez al día
   - Horario: 10:00 AM
   - Canal: "library_upcoming"
   ↓
5. A las 10:00 AM del día siguiente:
   ↓
6. NotificationReceiver recibe alarma
   ↓
7. Crea notificación:
   Título: "📅 Recordatorio de Préstamo"
   Mensaje: "Liderazgo Cristiano vence en 2 días"
   ↓
8. Usuario ve notificación
   ↓
9. [Pasa otro día, ahora vence en 1 día]
   ↓
10. Sistema reclasifica como "Muy Próximo"
   ↓
11. Actualiza frecuencia: 1 vez al día a las 6:00 PM
   ↓
12. A las 6:00 PM:
   ↓
13. Nueva notificación con urgencia media
   ↓
14. [Pasa otro día, vence HOY]
   ↓
15. Sistema reclasifica como "Vence HOY"
   ↓
16. Actualiza frecuencia: 2 veces al día (9 AM y 6 PM)
   ↓
17. A las 9:00 AM:
   ↓
18. Notificación urgente:
    "🚨 Vence HOY: Liderazgo Cristiano"
   ↓
19. A las 6:00 PM:
   ↓
20. Segunda notificación del día
   ↓
21. [Usuario no devuelve, libro vence]
   ↓
22. Sistema reclasifica como "Vencido Reciente"
   ↓
23. Actualiza frecuencia: 2 veces al día (10 AM y 4 PM)
   ↓
24. Admin ve en OverdueBooksFragment
   ↓
25. Libro aparece con badge rojo "VENCIDO"
   ↓
26. Admin click en "Enviar Recordatorio"
   ↓
27. EmailService envía email al usuario:
    - Header rojo (vencido)
    - Información del libro
    - Días vencidos: 1
    - Mensaje de acción requerida
   ↓
28. NotificationHelper muestra progress durante envío
   ↓
29. Confirmación: "Recordatorio enviado exitosamente"
   ↓
30. [Si usuario no devuelve en 7 días]
   ↓
31. Sistema reclasifica como "CRÍTICO"
   ↓
32. Actualiza frecuencia: 6 veces al día (cada 4 horas)
   ↓
33. Notificaciones intensivas hasta devolución
```

### **Flujo 5: Release Automatizado**

```
1. Desarrollador completa nuevas funcionalidades
   ↓
2. Hace commits de los cambios
   ↓
3. Decide crear nueva versión
   ↓
4. Abre terminal en raíz del proyecto
   ↓
5. Ejecuta: npm run release:patch "Mejoras de UI y corrección de bugs"
   ↓
6. Script inicia proceso automatizado
   ↓
7. PASO 1: Version Bump
   - Lee build.gradle.kts
   - Versión actual: 1.3.14 (Code: 34)
   - Incrementa a: 1.3.15 (Code: 35)
   - Guarda cambios
   - Git add build.gradle.kts
   - Git commit -m "chore: bump version to 1.3.15"
   - ✅ Versión actualizada
   ↓
8. PASO 2: Compilación APK
   - Ejecuta: gradlew.bat assembleRelease
   - Compila código Kotlin
   - Aplica Proguard (minificación)
   - Shrink resources (optimización)
   - Firma con libraryapp-keystore.jks
   - Genera: app/build/outputs/apk/release/app-release.apk
   - Tamaño: 7.47 MB
   - ✅ APK compilado
   ↓
9. PASO 3: GitHub Release
   - Crea tag: v1.3.15
   - Genera release notes automáticas:
     "## 🆕 Novedades v1.3.15
      - Mejoras de UI y corrección de bugs
      
      ## 📱 Descarga
      - APK: LibraryInventoryApp-v1.3.15.apk (7.47 MB)
      
      ## 🔧 Instalación
      1. Descargar APK
      2. Permitir fuentes desconocidas
      3. Instalar
      
      ## 📊 Información Técnica
      - Version Code: 35
      - Min SDK: 26 (Android 8.0)
      - Target SDK: 34 (Android 14)"
   - Llama a GitHub API
   - Crea release en repositorio
   - Obtiene upload URL
   - ✅ Release creado: https://github.com/JhonnyXT/LibraryInventoryApp/releases/tag/v1.3.15
   ↓
10. PASO 4: Upload APK
    - Renombra APK: LibraryInventoryApp-v1.3.15.apk
    - Intenta método principal: GitHub API
    - Headers:
      * Authorization: token ghp_xxxxx
      * Content-Type: application/zip
      * Accept: application/vnd.github+json
      * X-GitHub-Api-Version: 2022-11-28
    - Sube archivo binario
    - Si falla, usa método alternativo: gh CLI
    - Verifica que APK esté disponible
    - ✅ APK subido: 7.47 MB
   ↓
11. PASO 5: Notificaciones Masivas
    - Ejecuta: node notify_new_version.js "1.3.15" "https://github.com/..."
    - Inicializa Firebase Admin SDK
    - Lee colección "users" de Firestore
    - Obtiene 4 usuarios:
      * admin@email.com (Admin)
      * juan@email.com (Usuario)
      * maria@email.com (Usuario)
      * pedro@email.com (Usuario)
    - Para cada usuario:
      * Genera template HTML personalizado
      * Configura Brevo API client
      * Envía email transaccional
      * Log: "✅ Email enviado a juan@email.com"
    - Resumen:
      * Total usuarios: 4
      * Emails enviados: 4
      * Emails fallidos: 0
    - ✅ Notificaciones enviadas
   ↓
12. PASO 6: Push Automático
    - Ejecuta: git push
    - Sube commits a GitHub
    - Sube tags a GitHub
    - Actualiza rama master
    - ✅ GitHub actualizado
   ↓
13. PASO 7: Resumen Final
    - Muestra en consola:
      
      🎉 ¡RELEASE COMPLETADO EXITOSAMENTE!
      ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
      📱 Nueva versión: 1.3.15 (Code: 35)
      🔗 GitHub Release: https://github.com/JhonnyXT/LibraryInventoryApp/releases/tag/v1.3.15
      📧 Usuarios notificados: 4 emails enviados
      📂 APK disponible: LibraryInventoryApp-v1.3.15.apk (7.47 MB)
      🚀 Push automático completado
      ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
      
    - ✅ Proceso completado
   ↓
14. Usuarios reciben email:
    - Subject: "📱 LibraryInventoryApp 1.3.15 - Nueva versión disponible"
    - Contenido:
      * Saludo personalizado
      * Anuncio de nueva versión
      * Lista de novedades
      * Botón de descarga
      * Instrucciones de instalación
   ↓
15. Usuario hace click en "Descargar APK"
   ↓
16. Se abre GitHub Release en navegador
   ↓
17. Usuario descarga LibraryInventoryApp-v1.3.15.apk
   ↓
18. Android muestra: "¿Permitir instalar desde esta fuente?"
   ↓
19. Usuario acepta
   ↓
20. Instalador de Android abre
   ↓
21. Usuario hace click en "Instalar"
   ↓
22. App se actualiza
   ↓
23. Usuario abre app actualizada
   ↓
24. Ve nuevas funcionalidades y mejoras
```

---

## ⚠️ CONSIDERACIONES IMPORTANTES

### **🔒 Seguridad**

```
1. API Keys y Tokens
   ❌ NUNCA hardcodear en el código
   ✅ Usar local.properties
   ✅ Agregar a .gitignore
   ✅ Rotar periódicamente

2. Firebase Rules
   ✅ Configurar reglas estrictas
   ✅ Validar roles en servidor
   ✅ Limitar acceso por usuario
   ❌ No confiar solo en cliente

3. Keystore de Producción
   ✅ Guardar en lugar seguro
   ✅ Backup encriptado
   ✅ Contraseñas fuertes
   ❌ No compartir públicamente

4. Service Account Key
   ✅ Solo para backend/scripts
   ✅ Permisos mínimos necesarios
   ✅ Rotar anualmente
   ❌ No incluir en repositorio
```

### **📱 Compatibilidad**

```
Versiones de Android:

Android 8.0-11 (API 26-30):
├── ✅ Todas las funciones disponibles
├── ✅ Notificaciones sin permisos especiales
└── ✅ Compatibilidad completa

Android 12-13 (API 31-33):
├── ✅ Todas las funciones disponibles
├── ⚠️ Requiere permiso POST_NOTIFICATIONS
├── ⚠️ Requiere permiso SCHEDULE_EXACT_ALARM
└── ✅ Manejo automático de permisos

Android 14+ (API 34+):
├── ✅ Target SDK 34
├── ✅ Optimizado para última versión
└── ✅ Todas las funciones disponibles

Dispositivos Específicos:

Samsung:
├── ✅ Soporte completo
├── ⚠️ Verificar "Optimización de batería"
└── ⚠️ Permitir "Ejecutar en segundo plano"

Xiaomi/MIUI:
├── ✅ Soporte completo
├── ⚠️ Desactivar "Ahorro de batería"
└── ⚠️ Permitir "Inicio automático"

Huawei:
├── ✅ Soporte completo (sin GMS)
├── ⚠️ Configurar "Administrador de inicio"
└── ⚠️ Permitir notificaciones
```

### **🔔 Notificaciones**

```
Mejores Prácticas:

1. Push Notifications
   ✅ Horarios razonables (9 AM - 9 PM)
   ✅ Frecuencia apropiada
   ✅ Mensajes claros y útiles
   ❌ No spam al usuario

2. Emails
   ✅ Templates responsive
   ✅ Contenido relevante
   ✅ Opción de contacto
   ❌ No enviar excesivamente

3. Permisos
   ✅ Explicar por qué se necesitan
   ✅ Solicitar en momento apropiado
   ✅ Funcionar sin permisos (degradado)
   ❌ No bloquear app sin permisos
```

### **🗄️ Base de Datos**

```
Firestore Limits:

Escrituras:
├── Máximo: 1 escritura/segundo por documento
├── Batch: Hasta 500 operaciones
└── Transacciones: Hasta 500 documentos

Lecturas:
├── Sin límite específico
├── Costo por lectura
└── Usar cache cuando sea posible

Tamaño:
├── Documento: Máximo 1 MB
├── Campo: Máximo 1 MB
└── Colección: Sin límite

Optimizaciones:
✅ Usar índices compuestos
✅ Limitar queries con .limit()
✅ Usar paginación
✅ Cache offline habilitado
❌ No hacer queries en loops
```

### **📦 Storage**

```
Firebase Storage Limits:

Tamaño de Archivo:
├── Máximo: 5 MB por imagen (configurado)
├── Recomendado: 1-2 MB
└── Compresión: Automática en cliente

Formatos Soportados:
├── JPEG/JPG ✅
├── PNG ✅
├── WebP ✅
└── Otros formatos ❌

Optimizaciones:
✅ Comprimir imágenes antes de subir
✅ Usar Glide para cache
✅ Eliminar imágenes antiguas
❌ No subir imágenes muy grandes
```

### **⚡ Rendimiento**

```
Optimizaciones Implementadas:

1. RecyclerView
   ✅ ViewHolder pattern
   ✅ DiffUtil para actualizaciones
   ✅ Paginación en listas grandes
   ✅ Cache de imágenes con Glide

2. Firebase
   ✅ Offline persistence habilitado
   ✅ Listeners eficientes
   ✅ Queries optimizadas
   ✅ Índices compuestos

3. Imágenes
   ✅ Glide con cache disk + memory
   ✅ Placeholder mientras carga
   ✅ Error handling con imagen default
   ✅ Transformaciones eficientes

4. Coroutines
   ✅ Operaciones async en IO dispatcher
   ✅ UI updates en Main dispatcher
   ✅ Cancelación apropiada
   ✅ Exception handling
```

### **🐛 Debugging**

```
Logs Importantes:

Autenticación:
Log.d("LoginActivity", "Usuario autenticado: $userId")
Log.e("GoogleSignIn", "Error: ${e.message}")

Notificaciones:
Log.i("LibraryNotificationManager", "Notificación programada")
Log.d("NotificationReceiver", "Alarma recibida")

Firebase:
Log.d("ViewBooksFragment", "Libros cargados: ${books.size}")
Log.e("EmailService", "Error enviando email: ${e.message}")

Automatización:
console.log("✅ APK compilado exitosamente")
console.error("❌ Error en GitHub API:", error)

Niveles de Log:
├── VERBOSE (V): Información muy detallada
├── DEBUG (D): Información de debugging
├── INFO (I): Información general
├── WARN (W): Advertencias
└── ERROR (E): Errores
```

### **📝 Mantenimiento**

```
Tareas Regulares:

Diarias:
├── Verificar logs de errores
├── Revisar notificaciones enviadas
└── Monitorear uso de Firebase

Semanales:
├── Revisar feedback de usuarios
├── Actualizar dependencias menores
├── Verificar espacio en Storage
└── Revisar métricas de uso

Mensuales:
├── Actualizar dependencias mayores
├── Revisar reglas de seguridad
├── Optimizar queries lentas
├── Limpiar datos antiguos
└── Backup de Firestore

Anuales:
├── Rotar API keys
├── Renovar certificados
├── Revisar arquitectura
└── Planear mejoras mayores
```

### **🚀 Despliegue**

```
Checklist Pre-Release:

1. Código
   ✅ Todos los tests pasan
   ✅ No hay warnings críticos
   ✅ Código revisado
   ✅ Documentación actualizada

2. Configuración
   ✅ API keys configuradas
   ✅ Firebase rules actualizadas
   ✅ Keystore disponible
   ✅ Version bump correcto

3. Testing
   ✅ Probado en múltiples dispositivos
   ✅ Probado en diferentes versiones Android
   ✅ Notificaciones funcionando
   ✅ Emails llegando correctamente

4. Automatización
   ✅ Scripts funcionando
   ✅ GitHub token válido
   ✅ Brevo API key válida
   ✅ Service account key válido

5. Post-Release
   ✅ Verificar GitHub Release
   ✅ Descargar y probar APK
   ✅ Verificar emails enviados
   ✅ Monitorear errores
```

---

## 📞 SOPORTE Y RECURSOS

### **Documentación Adicional**

```
Archivos del Proyecto:
├── README.md - Documentación general
├── GUIA_FUNCIONAL_COMPLETA.md - Este archivo
├── RELEASE_AUTOMATION.md - Guía de automatización
├── CONFIGURACION_CLAVES.md - Configuración de API keys
└── .cursorrules - Reglas del proyecto

Documentación Externa:
├── Firebase: https://firebase.google.com/docs
├── Kotlin: https://kotlinlang.org/docs
├── Material Design: https://m3.material.io
├── Brevo: https://developers.brevo.com
└── GitHub API: https://docs.github.com/rest
```

### **Contacto**

```
Repositorio: https://github.com/JhonnyXT/LibraryInventoryApp
Issues: https://github.com/JhonnyXT/LibraryInventoryApp/issues
Releases: https://github.com/JhonnyXT/LibraryInventoryApp/releases
```

---

## 🎉 CONCLUSIÓN

LibraryInventoryApp es un **sistema completo y profesional** que demuestra:

### **✨ Características Destacadas**

1. **Arquitectura Robusta**: Separación clara de responsabilidades
2. **UX/UI Profesional**: Material Design 3 con animaciones fluidas
3. **Automatización Empresarial**: Release completo con un comando
4. **Sistema de Notificaciones**: Híbrido (Push + Email) con escalamiento inteligente
5. **Seguridad**: Firebase rules, permisos, validaciones
6. **Rendimiento**: Optimizaciones en todos los niveles
7. **Mantenibilidad**: Código limpio, documentado y organizado

### **🎯 Casos de Uso Ideales**

- ✅ Bibliotecas de iglesias
- ✅ Bibliotecas escolares pequeñas
- ✅ Bibliotecas comunitarias
- ✅ Colecciones privadas compartidas
- ✅ Cualquier organización con préstamo de libros

### **🚀 Próximos Pasos**

1. **Explorar la app**: Probar todas las funcionalidades
2. **Configurar Firebase**: Seguir guía de configuración
3. **Personalizar**: Adaptar a necesidades específicas
4. **Desplegar**: Usar sistema de automatización
5. **Mantener**: Seguir mejores prácticas

---

**¡Gracias por usar LibraryInventoryApp!** 📚✨

*Esta guía fue creada para facilitar el entendimiento completo del sistema y servir como referencia durante el desarrollo y mantenimiento.*

**Última actualización**: Noviembre 2024  
**Versión del documento**: 1.0  
**Versión de la app**: 1.3.14
