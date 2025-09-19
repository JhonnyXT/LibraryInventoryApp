# 🔐 CONFIGURACIÓN DE CLAVES Y VARIABLES DE ENTORNO

## 📋 **IMPORTANTE - CONFIGURACIÓN REQUERIDA**

### **🚨 ANTES DE USAR EL SISTEMA:**

Para usar el sistema de automatización de releases y notificaciones, necesitas configurar las siguientes claves de acceso:

---

## 🔑 **1. BREVO API KEY**

### **📧 Para notificaciones por email:**

1. **Obtener API Key de Brevo:**
   - Ve a [Brevo.com](https://www.brevo.com/es/) (anteriormente Sendinblue)
   - Crea una cuenta gratuita (9,000 emails/mes)
   - Ve a Settings > API Keys: [https://app.brevo.com/settings/keys/api](https://app.brevo.com/settings/keys/api)
   - Crea una nueva API Key con permisos "Envío de emails transaccionales"
   - Copia la clave que empieza con `xkeysib-xxxxx`

2. **Configurar en el código:**

   **📱 Para EmailService.kt (Android):**
   ```kotlin
   // En app/src/main/java/com/example/libraryinventoryapp/utils/EmailService.kt
   private const val BREVO_API_KEY = "xkeysib-TU_CLAVE_AQUI"
   private const val FROM_EMAIL = "tu-email@ejemplo.com"
   ```

   **🖥️ Para notify_new_version.js (Node.js):**
   ```bash
   # Opción 1: Variable de entorno (RECOMENDADO)
   set BREVO_API_KEY=xkeysib-TU_CLAVE_AQUI
   set FROM_EMAIL=tu-email@ejemplo.com

   # Opción 2: Editar directamente el archivo
   # En notify_new_version.js, cambiar las líneas 15-16
   ```

---

## 🐙 **2. GITHUB TOKEN**

### **📦 Para crear releases automáticos:**

1. **Crear Personal Access Token:**
   - Ve a GitHub.com > Settings > Developer settings > Personal access tokens
   - Genera un nuevo token (classic)
   - Selecciona scope: `repo` (Full control of private repositories)
   - Copia el token generado

2. **Configurar token:**
   ```bash
   # Variable de entorno (RECOMENDADO)
   set GITHUB_TOKEN=ghp_TU_TOKEN_AQUI

   # O editar scripts/release.js directamente
   const GITHUB_TOKEN = 'ghp_TU_TOKEN_AQUI';
   ```

---

## 🔒 **3. FIREBASE SERVICE ACCOUNT**

### **🔥 Para acceso a Firestore desde Node.js:**

1. **Descargar clave de servicio:**
   - Firebase Console > Configuración del proyecto
   - Pestaña "Cuentas de servicio"
   - "Generar nueva clave privada"
   - Guardar como `serviceAccountKey.json` en la raíz del proyecto

---

## ⚡ **USO DE VARIABLES DE ENTORNO (RECOMENDADO)**

### **🖥️ En Windows PowerShell:**
```powershell
# Configurar variables de entorno temporales
$env:BREVO_API_KEY = "xkeysib-TU_CLAVE_AQUI"
$env:FROM_EMAIL = "tu-email@ejemplo.com"
$env:GITHUB_TOKEN = "ghp_TU_TOKEN_AQUI"

# Ejecutar release
npm run release:patch "Descripción del cambio"
```

### **💻 En Windows CMD:**
```cmd
set BREVO_API_KEY=xkeysib-TU_CLAVE_AQUI
set FROM_EMAIL=tu-email@ejemplo.com
set GITHUB_TOKEN=ghp_TU_TOKEN_AQUI

npm run release:patch "Descripción del cambio"
```

### **🐧 En Linux/macOS:**
```bash
export BREVO_API_KEY="xkeysib-TU_CLAVE_AQUI"
export FROM_EMAIL="tu-email@ejemplo.com"
export GITHUB_TOKEN="ghp_TU_TOKEN_AQUI"

npm run release:patch "Descripción del cambio"
```

---

## 📁 **ESTRUCTURA DE ARCHIVOS REQUERIDA:**

```
LibraryInventoryApp/
├── serviceAccountKey.json  ← Clave Firebase (NO SUBIR A GIT)
├── notify_new_version.js   ← Configurar BREVO_API_KEY
├── scripts/
│   └── release.js          ← Configurar GITHUB_TOKEN
└── app/src/main/java/com/example/libraryinventoryapp/utils/
    └── EmailService.kt     ← Configurar BREVO_API_KEY
```

---

## 🚨 **SEGURIDAD - MUY IMPORTANTE:**

### **❌ NUNCA hagas esto:**
- ❌ No subas claves a GitHub
- ❌ No compartas API keys públicamente
- ❌ No hagas commits con claves hardcodeadas

### **✅ SÍ haz esto:**
- ✅ Usa variables de entorno
- ✅ Mantén `serviceAccountKey.json` en `.gitignore`
- ✅ Regenera claves si se comprometen
- ✅ Usa diferentes claves para desarrollo/producción

---

## 🔧 **VERIFICAR CONFIGURACIÓN:**

### **🧪 Probar notificaciones:**
```bash
node notify_new_version.js "1.0.0-test" "https://ejemplo.com"
```

### **🧪 Probar release completo:**
```bash
npm run release:patch "Prueba de configuración"
```

---

## 🆘 **SOLUCIÓN DE PROBLEMAS:**

### **📧 Si no llegan emails:**
1. Verifica BREVO_API_KEY sea correcta
2. Confirma FROM_EMAIL esté verificado en Brevo
3. Revisa logs en consola para errores

### **🐙 Si falla GitHub Release:**
1. Verifica GITHUB_TOKEN tenga permisos `repo`
2. Confirma repositorio exista y tengas acceso
3. Asegúrate de haber hecho `git push` antes

### **🔥 Si falla Firebase:**
1. Verifica `serviceAccountKey.json` esté en la raíz
2. Confirma el archivo sea de tu proyecto Firebase
3. Revisa permisos de la cuenta de servicio

---

## 📞 **CONTACTO:**

Si tienes problemas con la configuración, verifica:
1. Todas las claves estén configuradas correctamente
2. Los archivos estén en las ubicaciones correctas
3. Las variables de entorno estén definidas antes de ejecutar

**¡Una vez configurado, el sistema funcionará automáticamente! 🚀**
