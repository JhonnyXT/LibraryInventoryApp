# 📚 LibraryInventoryApp

<div align="center">

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-039BE5?style=for-the-badge&logo=Firebase&logoColor=white)
![Material Design](https://img.shields.io/badge/Material%20Design-757575?style=for-the-badge&logo=material-design&logoColor=white)

**Sistema de gestión de inventario de bibliotecas de clase empresarial**

[![Version](https://img.shields.io/badge/version-1.3.14-blue.svg)](https://github.com/JhonnyXT/LibraryInventoryApp/releases)
[![License](https://img.shields.io/badge/license-ISC-green.svg)](LICENSE)
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/JhonnyXT/LibraryInventoryApp/actions)

</div>

---

## 🎯 **Descripción del Proyecto**

**LibraryInventoryApp** es una aplicación móvil Android desarrollada en Kotlin para la gestión completa de inventarios de bibliotecas. La aplicación combina tecnologías modernas con un sistema de automatización empresarial, ofreciendo una experiencia de usuario profesional comparable a aplicaciones comerciales de grandes empresas tecnológicas.

### 🏆 **Características Principales**

- **🔐 Autenticación dual**: Firebase Auth + Google Sign-In
- **📱 Material Design 3**: Interfaz moderna y profesional
- **📚 Gestión completa de libros**: Registro, edición, asignación y seguimiento
- **🔔 Sistema híbrido de notificaciones**: Push + Email con templates responsive
- **🚀 Automatización empresarial**: Releases automáticos con un solo comando
- **📧 Notificaciones masivas**: Sistema profesional de comunicación
- **📊 Dashboard avanzado**: Libros vencidos, filtros y reportes
- **📷 Escaneo de códigos**: Códigos de barras e ISBN con ML Kit
- **☁️ Backend en la nube**: Firebase Firestore + Storage

---

## 🏗️ **Arquitectura y Tecnologías**

### **Stack Tecnológico Principal**

| Categoría | Tecnología | Versión | Propósito |
|-----------|------------|---------|-----------|
| **Lenguaje** | Kotlin | 1.8+ | Desarrollo nativo Android |
| **UI Framework** | Material Design 3 | Latest | Componentes modernos |
| **Backend** | Firebase | Latest | Autenticación, base de datos, storage |
| **Base de Datos** | Cloud Firestore | Latest | NoSQL en tiempo real |
| **Autenticación** | Firebase Auth + Google Sign-In | Latest | Login seguro |
| **Notificaciones** | Brevo (Sendinblue) | 3.0.1 | Emails transaccionales |
| **Automatización** | Node.js | 18+ | Scripts de release |
| **Escaneo** | ZXing + ML Kit | Latest | Códigos de barras/QR |
| **Cámara** | CameraX | Latest | Captura de imágenes |
| **Imágenes** | Glide | Latest | Carga y cache de imágenes |

### **Dependencias Principales**

```kotlin
// Firebase
implementation(libs.firebase.auth.ktx)
implementation(libs.firebase.firestore)
implementation(libs.firebase.storage)

// Google Sign-In
implementation("com.google.android.gms:play-services-auth:21.0.0")

// Escaneo de códigos
implementation(libs.zxing.core)
implementation(libs.zxing.android.embedded)
implementation(libs.play.services.mlkit.barcode.scanning)

// Cámara
implementation(libs.camerax.core)
implementation(libs.camerax.camera2)
implementation(libs.camerax.lifecycle)
implementation(libs.camerax.view)

// Imágenes
implementation(libs.glide)
kapt(libs.glide.compiler)

// HTTP requests
implementation("com.squareup.okhttp3:okhttp:4.12.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
```

---

## 📱 **Funcionalidades por Rol**

### 👨‍💼 **Administrador**

#### **📚 Gestión de Libros**
- ✅ **Registro completo**: Título, autor, ISBN, categorías, descripción, imagen
- ✅ **Escaneo de códigos**: Códigos de barras e ISBN con cámara
- ✅ **Edición avanzada**: Modificar todos los campos de libros existentes
- ✅ **Asignación inteligente**: Asignar libros a usuarios específicos
- ✅ **Desasignación**: Remover libros de usuarios
- ✅ **Control de inventario**: Gestión de cantidad de ejemplares
- ✅ **Eliminación segura**: Borrar libros del sistema

#### **📊 Dashboard y Reportes**
- ✅ **Vista completa del inventario**: Todos los libros con filtros avanzados
- ✅ **Libros vencidos**: Dashboard especializado con recordatorios
- ✅ **Filtros inteligentes**: Por usuario, fecha, categoría, estado
- ✅ **Búsqueda en tiempo real**: Título e ISBN con normalización
- ✅ **Estadísticas**: Cantidad total, libros asignados, vencidos

#### **🔔 Sistema de Notificaciones**
- ✅ **Notificaciones automáticas**: Emails profesionales al asignar libros
- ✅ **Recordatorios de vencimiento**: Sistema escalonado por urgencia
- ✅ **Templates responsive**: Emails HTML5 con Material Design 3
- ✅ **Notificaciones push**: Sistema híbrido con horarios optimizados

### 👤 **Usuario Regular**

#### **📖 Exploración de Libros**
- ✅ **Catálogo completo**: Navegación por todos los libros disponibles
- ✅ **Búsqueda avanzada**: Por título, autor, ISBN con filtros
- ✅ **Categorías**: 22 categorías predefinidas (Biblia, Liderazgo, etc.)
- ✅ **Lista de deseos**: Guardar libros favoritos
- ✅ **Notificaciones de disponibilidad**: Cuando libros deseados estén disponibles

#### **📚 Libros Asignados**
- ✅ **Vista personal**: Solo libros asignados al usuario
- ✅ **Información detallada**: Fechas de asignación y vencimiento
- ✅ **Estado de préstamo**: Disponible, asignado, vencido
- ✅ **Historial**: Seguimiento de todos los préstamos

#### **👤 Gestión de Perfil**
- ✅ **Perfil personal**: Información del usuario
- ✅ **Centro de notificaciones**: Bandeja de entrada profesional
- ✅ **Configuración**: Preferencias de la aplicación

---

## 🗄️ **Modelos de Datos**

### **📚 Modelo Book**
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
```

### **👤 Modelo User**
```kotlin
data class User(
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val uid: String = ""
)
```

### **🔔 Modelo NotificationItem**
```kotlin
data class NotificationItem(
    val id: String = "",
    val bookId: String = "",
    val bookTitle: String = "",
    val bookAuthor: String = "",
    val userId: String = "",
    val userName: String = "",
    val daysUntilDue: Int = 0,
    val expirationDate: Timestamp? = null,
    val type: NotificationType = NotificationType.UPCOMING,
    val isRead: Boolean = false,
    val timestamp: Timestamp = Timestamp.now()
)
```

---

## 🔔 **Sistema de Notificaciones Híbrido**

### **📱 Notificaciones Push**

#### **⚡ Tipos de Notificaciones**
1. **📅 Próximos (3-5 días)**: 1 vez al día a las 10:00 AM
2. **⚠️ Muy próximos (1-2 días)**: 1 vez al día a las 6:00 PM
3. **🚨 Vence HOY**: 2 veces al día (9:00 AM + 6:00 PM)
4. **🔴 Vencido reciente (1-3 días)**: 2 veces al día (10:00 AM + 4:00 PM)
5. **🔥 Vencido medio (4-7 días)**: Cada 8 horas (3 veces al día)
6. **🚨 CRÍTICO (+7 días)**: Cada 4 horas (6 veces al día)
7. **⚡ INMEDIATAS**: Al cambiar fechas próximas a vencer

#### **🎯 Características Avanzadas**
- **Escalamiento inteligente**: Más frecuente = más crítico
- **Horarios optimizados**: Mañana y tarde para máxima visibilidad
- **Canales diferenciados**: Próximos, Vencidos, Críticos
- **Compatibilidad universal**: Android 8.0+ con manejo automático de permisos
- **Navegación inteligente**: Abre la app según el rol del usuario

### **📧 Notificaciones Email**

#### **🎨 Templates Profesionales**
- **HTML5 Responsive**: Compatible móvil y desktop
- **Material Design 3**: Gradientes y efectos visuales modernos
- **Tipografía Premium**: Segoe UI para máxima legibilidad
- **Colores dinámicos**: Headers que cambian según urgencia
- **Branding personalizado**: Mensaje motivacional para la iglesia

#### **📨 Tipos de Emails**
1. **Asignación de Libros**: Dual (usuario + admin)
2. **Recordatorios de Vencimiento**: Con escalamiento visual
3. **Releases de App**: Notificaciones masivas de nuevas versiones

---

## 🚀 **Sistema de Automatización Empresarial**

### **⚡ Release Automatizado**

Un solo comando hace **TODO** el proceso:

```bash
npm run release:patch "Descripción del cambio"
```

#### **🔄 Proceso Completo Automatizado**
1. **📝 Version bump**: Actualiza versionCode y versionName
2. **💾 Commit automático**: Guarda cambios en build.gradle.kts
3. **🏗️ Compilación APK**: Genera APK firmado optimizado (7.47 MB)
4. **🐙 GitHub Release**: Crea release con APK como asset
5. **📧 Notificaciones masivas**: Emails a todos los usuarios
6. **🚀 Push automático**: Sube cambios a GitHub

#### **📱 Tipos de Release**
```bash
npm run release:patch   # 1.3.2 → 1.3.3 (bug fixes)
npm run release:minor   # 1.3.2 → 1.4.0 (nuevas features)
npm run release:major   # 1.3.2 → 2.0.0 (cambios grandes)
```

### **🔧 Scripts de Automatización**

| Archivo | Propósito |
|---------|-----------|
| `scripts/release.js` | Script principal de release automatizado |
| `scripts/update_version.js` | Actualización automática de versiones |
| `notify_new_version.js` | Sistema de notificaciones masivas |

---

## 📁 **Estructura del Proyecto**

```
LibraryInventoryApp/
├── 📱 app/
│   ├── build.gradle.kts
│   ├── google-services.json
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/libraryinventoryapp/
│       │   ├── activities/           # Actividades principales
│       │   │   ├── LoginActivity.kt
│       │   │   ├── RegisterActivity.kt
│       │   │   ├── AdminActivity.kt
│       │   │   └── UserActivity.kt
│       │   ├── fragments/            # Fragmentos por funcionalidad
│       │   │   ├── RegisterBookFragment.kt
│       │   │   ├── ViewBooksFragment.kt
│       │   │   ├── EditBookFragment.kt
│       │   │   ├── OverdueBooksFragment.kt
│       │   │   ├── BookListFragment.kt
│       │   │   ├── AssignedBooksFragment.kt
│       │   │   ├── ProfileFragment.kt
│       │   │   └── NotificationsFragment.kt
│       │   ├── adapters/             # Adaptadores RecyclerView
│       │   │   ├── BookAdapter.kt
│       │   │   ├── BookListAdapter.kt
│       │   │   ├── AssignedBooksAdapter.kt
│       │   │   ├── OverdueBooksAdapter.kt
│       │   │   └── NotificationsAdapter.kt
│       │   ├── models/               # Modelos de datos
│       │   │   ├── Book.kt
│       │   │   ├── User.kt
│       │   │   ├── NotificationItem.kt
│       │   │   ├── OverdueBookItem.kt
│       │   │   └── WishlistItem.kt
│       │   └── utils/                # Utilidades
│       │       ├── EmailService.kt
│       │       ├── NotificationHelper.kt
│       │       ├── LibraryNotificationManager.kt
│       │       ├── NotificationReceiver.kt
│       │       ├── BootReceiver.kt
│       │       └── PermissionHelper.kt
│       └── res/                      # Recursos Android
│           ├── layout/               # 20+ archivos XML
│           ├── values/               # Material Design 3
│           ├── drawable/             # Iconos profesionales
│           └── menu/                 # Navegación
├── 🚀 scripts/                       # Sistema de automatización
│   ├── release.js
│   └── update_version.js
├── 📧 notify_new_version.js          # Notificaciones masivas
├── 📦 package.json                   # Configuración NPM
├── 🔐 serviceAccountKey.json         # Firebase Admin (no versionar)
├── 🔑 local.properties               # API Keys (no versionar)
├── 🔐 libraryapp-keystore.jks        # Keystore de producción
├── 📚 RELEASE_AUTOMATION.md          # Documentación automatización
├── 🔧 CONFIGURACION_CLAVES.md        # Guía de configuración
└── 📋 README.md                      # Este archivo
```

---

## ⚙️ **Configuración e Instalación**

### **📋 Requisitos del Sistema**

| Componente | Versión | Descripción |
|------------|---------|-------------|
| **Android SDK** | 26+ | Mínimo Android 8.0 |
| **Kotlin** | 1.8+ | Lenguaje de programación |
| **Gradle** | 8.0+ | Sistema de build |
| **Node.js** | 18+ | Para scripts de automatización |
| **Firebase** | Latest | Backend y servicios |

### **🔧 Configuración Inicial**

#### **1. Clonar el Repositorio**
```bash
git clone https://github.com/JhonnyXT/LibraryInventoryApp.git
cd LibraryInventoryApp
```

#### **2. Configurar Firebase**
1. Crear proyecto en [Firebase Console](https://console.firebase.google.com)
2. Descargar `google-services.json` y colocarlo en `app/`
3. Habilitar Authentication, Firestore y Storage
4. Configurar Google Sign-In con SHA-1 de producción

#### **3. Configurar API Keys**
```bash
# Crear archivo local.properties
echo "BREVO_API_KEY=tu_clave_brevo_aqui" >> local.properties
echo "GITHUB_TOKEN=tu_token_github_aqui" >> local.properties
```

#### **4. Instalar Dependencias**
```bash
# Dependencias Android
./gradlew build

# Dependencias Node.js
npm install
```

### **🔐 Configuración de Seguridad**

#### **Permisos Requeridos**
```xml
<!-- Permisos básicos -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Permisos para notificaciones -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

#### **Keystore de Producción**
- **Archivo**: `libraryapp-keystore.jks`
- **SHA-1**: `2D:27:86:D0:77:63:36:D6:D2:B9:57:46:15:C4:6B:C3:BC:F4:4D:58`
- **Configurado**: Para Google Sign-In en Firebase

---

## 🚀 **Uso del Sistema**

### **📱 Para Desarrolladores**

#### **Compilar APK**
```bash
# Debug
./gradlew assembleDebug

# Release (firmado)
./gradlew assembleRelease
```

#### **Crear Release Automatizado**
```bash
# Corrección de bugs
npm run release:patch "Corrección de crash en login"

# Nueva funcionalidad
npm run release:minor "Sistema de comentarios agregado"

# Cambio mayor
npm run release:major "Rediseño completo de la interfaz"
```

### **👥 Para Usuarios Finales**

#### **Descargar e Instalar**
1. **Recibir notificación**: Email automático con nueva versión
2. **Acceder al link**: Click en enlace de descarga
3. **GitHub Release**: Se abre página con APK descargable
4. **Descargar APK**: Click en `LibraryInventoryApp-v[versión].apk`
5. **Permitir instalación**: Habilitar "Fuentes desconocidas" si es necesario
6. **Instalar**: Proceso automático Android estándar

#### **Ubicación de la APK**
- **GitHub Release**: [Releases](https://github.com/JhonnyXT/LibraryInventoryApp/releases)
- **Tamaño**: ~7.47 MB (optimizado con Proguard)
- **Nombre**: `LibraryInventoryApp-v[versión].apk`

---

## 📊 **Métricas y Rendimiento**

### **🎯 Optimizaciones Implementadas**

| Optimización | Descripción | Resultado |
|--------------|-------------|-----------|
| **Proguard** | Minificación agresiva | APK de 7.47 MB |
| **Glide** | Cache de imágenes eficiente | Carga rápida |
| **RecyclerView** | ViewHolders optimizados | Listas fluidas |
| **Firebase Offline** | Soporte modo offline | Funciona sin internet |
| **Material Design 3** | Componentes modernos | UI profesional |

### **📱 Compatibilidad**

| Android | API Level | Soporte | Características |
|---------|-----------|---------|-----------------|
| **8.0+** | 26+ | ✅ Completo | Todas las funciones |
| **12+** | 31+ | ✅ Optimizado | Notificaciones exactas |
| **14** | 34+ | ✅ Última | Target SDK |

---

## 🆕 **Novedades y Cambios Recientes**

### **🎨 v1.3.0 - UX/UI Profesional**
- **NotificationHelper**: Snackbars animadas Material Design 3
- **Progress indicators**: Feedback visual elegante
- **Templates responsive**: Emails HTML5 con gradientes
- **Sistema híbrido**: Fallbacks robustos para todos los contextos

### **🔧 v1.3.1 - Google Sign-In Funcionando**
- **SHA-1 configurado**: Keystore de producción correcto
- **APK firmada**: Completamente funcional
- **Firebase actualizado**: google-services.json optimizado

### **🚀 v1.3.2 - Sistema 100% Automático**
- **Commit automático**: build.gradle.kts se commitea automáticamente
- **Push automático**: Se hace push al final del proceso
- **Cero pasos manuales**: Un comando hace absolutamente todo

### **🔧 v1.3.3 - GitHub API Optimizada**
- **Headers modernos**: GitHub API 2022-11-28
- **Upload robusto**: Método alternativo si falla la API principal
- **Eliminación error 422**: Payload JSON optimizado

### **📧 v1.3.8 - Templates Email Perfeccionados**
- **Remitente optimizado**: "Sistema de Biblioteca"
- **Diseño centrado**: Todos los textos perfectamente alineados
- **Iconos CSS**: Reemplazaron imágenes problemáticas
- **Consistencia visual**: Templates unificados

---

## 🐛 **Debugging y Testing**

### **🧪 Testing Setup**
- **Unit Tests**: JUnit configuration
- **Instrumented Tests**: Android Test framework
- **Firebase Test Lab**: Cloud testing capabilities
- **Manual Testing**: APK instalable en dispositivos reales

### **📝 Logging y Debugging**
- **Sistema robusto**: Logs detallados en todas las operaciones
- **Error handling**: Manejo comprehensivo con UX elegante
- **NotificationHelper**: Feedback visual profesional
- **Toast Messages**: Fallback user-friendly

---

## 📝 **Patrones y Mejores Prácticas**

### **🏗️ Patrones Utilizados**
- **MVP Pattern**: Separación clara de responsabilidades
- **Observer Pattern**: LiveData y listeners eficientes
- **Adapter Pattern**: RecyclerViews optimizados
- **Singleton Pattern**: Firebase instances reutilizables
- **Factory Pattern**: NotificationHelper para diferentes tipos

### **✅ Mejores Prácticas Implementadas**
- **Kotlin null safety**: Prevención completa de NullPointerException
- **Resource management**: Cleanup automático de resources
- **Error handling elegante**: UX profesional en todos los escenarios
- **User feedback**: Mensajes claros y opciones de acción
- **Material Design 3**: Componentes y colores modernos consistentes
- **Responsive Design**: Templates email compatibles con todos los dispositivos

---

## 🤝 **Contribución**

### **🔄 Flujo de Contribución**
1. Fork del repositorio
2. Crear rama feature (`git checkout -b feature/nueva-funcionalidad`)
3. Commit cambios (`git commit -m 'Agregar nueva funcionalidad'`)
4. Push a la rama (`git push origin feature/nueva-funcionalidad`)
5. Crear Pull Request

### **📋 Estándares de Código**
- **Kotlin**: Seguir convenciones oficiales
- **Commits**: Mensajes descriptivos en español
- **Documentación**: Comentarios JSDoc para funciones complejas
- **Testing**: Agregar tests para nuevas funcionalidades

---

## 📄 **Licencia**

Este proyecto está bajo la Licencia ISC. Ver el archivo [LICENSE](LICENSE) para más detalles.

---

## 📞 **Contacto y Soporte**

### **🐛 Reportar Bugs**
- **GitHub Issues**: [Crear issue](https://github.com/JhonnyXT/LibraryInventoryApp/issues)
- **Descripción detallada**: Incluir pasos para reproducir
- **Logs**: Adjuntar logs de error si es posible

### **💡 Solicitar Funcionalidades**
- **GitHub Discussions**: [Discusiones](https://github.com/JhonnyXT/LibraryInventoryApp/discussions)
- **Descripción clara**: Explicar el caso de uso
- **Prioridad**: Indicar importancia de la funcionalidad

---

## 🎉 **Conclusión**

**LibraryInventoryApp** es una aplicación de **clase empresarial** que combina:

### 🏆 **Nivel Técnico**
- **Tecnologías modernas**: Kotlin + Firebase + Material Design 3
- **Automatización completa**: Sistema de releases nivel Fortune 500
- **UX/UI profesional**: Experiencia comparable a apps premium
- **Arquitectura robusta**: Escalable y mantenible a largo plazo

### 🚀 **Nivel de Automatización**
- **Un comando = release completo**: `npm run release:patch "Descripción"`
- **Cero pasos manuales**: Todo automatizado de extremo a extremo
- **Notificaciones masivas**: Usuarios siempre actualizados
- **GitHub Integration**: Distribución profesional y confiable

### 🎨 **Nivel de Experiencia**
- **Material Design 3**: Componentes modernos y consistentes
- **Templates responsive**: Emails profesionales en todos los dispositivos
- **Feedback visual elegante**: Snackbars, progress indicators, animaciones
- **Google Sign-In**: Autenticación fluida y profesional

### 📱 **Nivel de Distribución**
- **APK firmada**: Lista para Play Store o distribución privada
- **SHA-1 configurado**: Google Sign-In funcionando perfectamente
- **Optimización avanzada**: 7.47 MB con Proguard y minification
- **Sistema robusto**: Funciona aunque fallen servicios externos

**Esta aplicación está lista para competir con aplicaciones comerciales de grandes empresas tecnológicas.** 🎯

El código está perfectamente organizado, documentado y automatizado, facilitando futuras mejoras y un mantenimiento eficiente a largo plazo.

---

<div align="center">

**⭐ Si te gusta este proyecto, ¡dale una estrella en GitHub! ⭐**

[![GitHub stars](https://img.shields.io/github/stars/JhonnyXT/LibraryInventoryApp?style=social)](https://github.com/JhonnyXT/LibraryInventoryApp/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/JhonnyXT/LibraryInventoryApp?style=social)](https://github.com/JhonnyXT/LibraryInventoryApp/network)

---

*Desarrollado con ❤️ para la gestión moderna de bibliotecas*

</div>
