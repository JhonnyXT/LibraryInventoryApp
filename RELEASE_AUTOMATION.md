# 🚀 Sistema de Release Automatizado - LibraryInventoryApp

## 🎯 ¿Qué hace este sistema?

Automatiza **COMPLETAMENTE** el proceso de distribución de nuevas versiones:

1. ✅ **Version bump automático** con commit automático del build.gradle.kts
2. ✅ **Compila APK firmado** release optimizado (7.47 MB)  
3. ✅ **Crea GitHub Release** con APK como asset descargable
4. ✅ **Sube APK** con sistema robusto y método alternativo
5. ✅ **Notifica usuarios masivamente** por email (templates responsive)
6. ✅ **Push automático** a GitHub - CERO pasos manuales
7. ✅ **Gestiona versionado** semántico Google estándar

---

## ⚙️ Configuración Inicial (Solo una vez)

### 📋 1. Descargar Service Account de Firebase

```bash
1. Ve a: https://console.firebase.google.com
2. Selecciona tu proyecto: "libraryinventoryapp"
3. ⚙️ Configuración del proyecto > Cuentas de servicio
4. 🔑 "Generar nueva clave privada"
5. 💾 Guarda como: serviceAccountKey.json (en la raíz del proyecto)
```

### 🐙 2. Configurar GitHub Token (Opcional)

```bash
1. Ve a: https://github.com/settings/tokens
2. "Generate new token (classic)"
3. Permisos: repo, write:packages
4. Copia el token
5. Edita scripts/release.js línea 15: GITHUB_TOKEN = 'tu_token'
```

---

## 🚀 Uso del Sistema

### 🎯 Comandos Principales

```bash
# 🔧 Corrección de bugs (1.0.5 → 1.0.6)
npm run release:patch "Corrección de crash en login"

# ✨ Nueva funcionalidad (1.0.5 → 1.1.0) 
npm run release:minor "Agregado sistema de comentarios"

# 🔄 Cambio mayor (1.0.5 → 2.0.0)
npm run release:major "Rediseño completo de la interfaz"
```

### 📋 Comandos Individuales

```bash
# Solo actualizar versión
npm run version:update patch

# Solo compilar APK
npm run build

# Solo notificar usuarios (requiere URL)
npm run notify "1.0.6" "https://github.com/JhonnyXT/LibraryInventoryApp/releases/tag/v1.0.6"

# Ver ayuda
npm run help
```

---

## 🔄 Versionado Automático según Google

### 📱 Sistema Android Estándar

| Tipo | versionCode | versionName | Cuándo usar |
|------|-------------|-------------|-------------|
| **patch** | +1 | 1.0.5 → 1.0.6 | 🔧 Bug fixes |
| **minor** | +1 | 1.0.5 → 1.1.0 | ✨ Nuevas features |
| **major** | +1 | 1.0.5 → 2.0.0 | 🔄 Cambios grandes |

### 📋 Ejemplos de versionado:

```bash
# Situación: Corregiste un bug de Google Sign-In
npm run release:patch "Arreglado error en autenticación Google"
# Resultado: 1.0.5 → 1.0.6

# Situación: Agregaste sistema de notificaciones push
npm run release:minor "Implementado sistema de notificaciones push"  
# Resultado: 1.0.6 → 1.1.0

# Situación: Rediseño completo de la app
npm run release:major "Rediseño completo con Material Design 3"
# Resultado: 1.1.0 → 2.0.0
```

---

## 📧 Sistema de Notificaciones

### 👥 ¿Quién recibe emails?

✅ **TODOS** los usuarios registrados en Firebase:
- Admins
- Usuarios regulares  
- Usuarios registrados con Google
- Usuarios registrados con email/password

### 📨 Contenido del email:

```
📱 LibraryInventoryApp 1.0.6 - Nueva versión disponible

¡Hola [Nombre]!

🆕 Novedades en esta versión:
- 🔐 Inicio de sesión con Google implementado
- 🎨 Interfaz de usuario mejorada
- ⚡ Rendimiento optimizado

📥 [BOTÓN: Descargar nueva versión]

📱 Instrucciones de instalación:
1. Haz clic en el botón de descarga
2. GitHub: Se abrirá GitHub, busca el archivo LibraryInventoryApp-v[versión].apk y descárgalo
3. Permite "Fuentes desconocidas" si es necesario
4. Instala la nueva versión
```

---

## 🔄 Flujo Completo Automatizado

### ⚡ Lo que pasa cuando ejecutas `npm run release:patch`:

```bash
🚀 Iniciando proceso de release automatizado...

📝 Paso 1: Actualizando versión...
   ├── Versión actual: 1.3.2 (Code: 22)
   ├── ✅ Nueva versión: 1.3.3 (Code: 23)
   ├── 🔄 Commit automático de build.gradle.kts
   └── ✅ Commit completado automáticamente

🏗️ Paso 2: Compilando APK release...
   ├── ⏳ Ejecutando: gradlew assembleRelease
   ├── ⚡ APK firmado con libraryapp-keystore.jks
   ├── 🔧 Aplicando Proguard + shrinking + minification
   └── ✅ APK generado: 7.47 MB

🐙 Paso 3: Creando GitHub Release...
   ├── 📝 Tag: v1.3.3 con GitHub API 2022-11-28
   ├── 📄 Release notes automáticas
   ├── 📤 Subiendo LibraryInventoryApp-v1.3.3.apk
   └── ✅ Release público: https://github.com/JhonnyXT/LibraryInventoryApp/releases/tag/v1.3.3

📧 Paso 4: Notificando usuarios...
   ├── 👥 Leyendo usuarios de Firebase: 4 usuarios
   ├── 📨 Templates responsive HTML5 + Material Design 3
   ├── 📤 Enviando con Brevo (Sendinblue): 4/4 exitoso
   └── ✅ 4 emails enviados exitosamente

🚀 Paso 5: Push automático...
   └── ✅ Push completado a GitHub

🎉 ¡RELEASE COMPLETADO EXITOSAMENTE!
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📱 Nueva versión: 1.3.3 (Code: 23)
🔗 GitHub Release: https://github.com/JhonnyXT/LibraryInventoryApp/releases/tag/v1.3.3
📧 Usuarios notificados automáticamente  
📂 APK: LibraryInventoryApp-v1.3.3.apk (7.47 MB)
🚀 Push automático completado - CERO pasos manuales
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## 🎯 Ventajas del Sistema

### ⚡ Para ti (Desarrollador):
```
✅ Un solo comando = release completo
✅ Versionado automático según estándares
✅ No más uploads manuales
✅ No más emails individuales
✅ Proceso reproducible y confiable
✅ Documentación automática de releases
```

### 📱 Para tus usuarios:
```
✅ Notificación inmediata de nuevas versiones
✅ Link directo de descarga 
✅ Instrucciones claras de instalación
✅ Información de novedades
✅ Descarga desde GitHub (confiable)
```

---

## 🔧 Troubleshooting

### ❌ "Error: No se encontró serviceAccountKey.json"
```bash
Solución:
1. Ve a Firebase Console > Configuración > Cuentas de servicio
2. Generar nueva clave privada  
3. Guarda como serviceAccountKey.json en la raíz
```

### ❌ "Error compilando APK"
```bash
Solución:
1. Verifica que tengas Android SDK instalado
2. Ejecuta: ./gradlew clean
3. Intenta: npm run build
```

### ❌ "Error enviando emails"
```bash
Solución:
1. Verifica SENDGRID_API_KEY en notify_new_version.js
2. Verifica FROM_EMAIL esté verificado en SendGrid
```

---

## 📋 Archivos del Sistema

```
LibraryInventoryApp/
├── scripts/
│   ├── update_version.js     # Actualiza versionCode/versionName
│   └── release.js            # Script principal de automatización
├── notify_new_version.js     # Sistema de notificaciones  
├── package.json              # Scripts npm + dependencias
├── serviceAccountKey.json    # Credenciales Firebase (no subir a Git)
└── RELEASE_AUTOMATION.md     # Esta documentación
```

---

---

## 🆕 **NOVEDADES Y MEJORAS RECIENTES (v1.3.0 - v1.3.3)**

### 🚀 **SISTEMA 100% AUTOMÁTICO (v1.3.2-1.3.3)**
- **✅ Commit automático**: build.gradle.kts se commitea automáticamente después del version bump
- **✅ Push automático**: Se hace push automático al final del proceso 
- **✅ GitHub API optimizada**: Headers 2022-11-28 para máxima compatibilidad
- **✅ Upload robusto**: Método alternativo si falla la API principal
- **✅ CERO pasos manuales**: Un comando hace absolutamente todo

### 🎨 **UX/UI PROFESIONAL (v1.3.0)**
- **✅ NotificationHelper**: Snackbars animadas Material Design 3
- **✅ Progress indicators**: Feedback visual elegante durante emails
- **✅ Templates responsive**: Emails HTML5 con gradientes profesionales  
- **✅ Fallbacks robustos**: Toast profesional cuando no hay vista disponible

### 🔧 **GOOGLE SIGN-IN FUNCIONANDO (v1.3.1)**
- **✅ SHA-1 configurado**: 2D:27:86:D0:77:63:36:D6:D2:B9:57:46:15:C4:6B:C3:BC:F4:4D:58
- **✅ APK firmada**: libraryapp-keystore.jks completamente funcional
- **✅ Firebase actualizado**: google-services.json con configuración correcta

### 📱 **APK DE PRODUCCIÓN**
- **✅ Tamaño optimizado**: 7.47 MB con Proguard + shrinking
- **✅ Instalable**: 100% funcional sin errores "paquete no válido"  
- **✅ Firmada**: Keystore de producción con contraseñas conocidas
- **✅ Google Sign-In**: Completamente operativo en dispositivos

---

## 🎉 ¡Todo listo!

```bash
# Para crear tu primer release automatizado:
npm run release:patch "Mi primer release automático"
```

**🚀 El sistema se encarga de TODO automáticamente:**
- Version bump → Commit → Compilar → GitHub Release → Subir APK → Emails → Push

**¡Solo siéntate y observa la magia empresarial! 🏆**
