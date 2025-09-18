# 🚀 Sistema de Release Automatizado - LibraryInventoryApp

## 🎯 ¿Qué hace este sistema?

Automatiza **COMPLETAMENTE** el proceso de distribución de nuevas versiones:

1. ✅ **Actualiza versión** siguiendo estándares Google
2. ✅ **Compila APK** release optimizado  
3. ✅ **Sube a GitHub** Releases públicamente
4. ✅ **Notifica usuarios** automáticamente por email
5. ✅ **Gestiona versionado** semántico

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
2. Descarga el archivo APK  
3. Permite "Fuentes desconocidas"
4. Instala la nueva versión
```

---

## 🔄 Flujo Completo Automatizado

### ⚡ Lo que pasa cuando ejecutas `npm run release:patch`:

```bash
🚀 Iniciando proceso de release automatizado...

📝 Paso 1: Actualizando versión...
   ├── Versión actual: 1.0.5 (Code: 2)
   └── ✅ Nueva versión: 1.0.6 (Code: 3)

🏗️ Paso 2: Compilando APK release...
   ├── ⏳ Ejecutando: gradlew assembleRelease
   ├── ⚡ Aplicando Proguard y optimizaciones
   └── ✅ APK generado: 6.8 MB

🐙 Paso 3: Creando GitHub Release...
   ├── 📝 Tag: v1.0.6
   ├── 📄 Release notes automáticas
   ├── 📤 Subiendo APK
   └── ✅ Release público: https://github.com/.../releases/tag/v1.0.6

📧 Paso 4: Notificando usuarios...
   ├── 👥 Leyendo usuarios de Firebase: 15 usuarios
   ├── 📨 Generando emails personalizados
   ├── 📤 Enviando con SendGrid: Lote 1/1
   └── ✅ 15 emails enviados exitosamente

🎉 ¡RELEASE COMPLETADO EXITOSAMENTE!
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📱 Nueva versión: 1.0.6 (Code: 3)
🔗 GitHub Release: https://github.com/.../releases/tag/v1.0.6  
📧 Usuarios notificados automáticamente
📂 APK disponible para descarga pública
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

## 🎉 ¡Todo listo!

```bash
# Para crear tu primer release automatizado:
npm run release:patch "Mi primer release automático"
```

**🚀 El sistema se encarga de todo automáticamente. ¡Solo siéntate y observa la magia!**
