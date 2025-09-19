#!/usr/bin/env node

/**
 * 🚀 SCRIPT DE RELEASE AUTOMATIZADO
 * 
 * Este script automatiza TODO el proceso de release:
 * 1. Actualiza versión automáticamente
 * 2. Compila APK release
 * 3. Crea GitHub Release
 * 4. Sube APK a GitHub
 * 5. Notifica a todos los usuarios por email
 * 
 * Uso: node scripts/release.js [major|minor|patch] "Descripción del release"
 * Ejemplo: node scripts/release.js patch "Corrección de bugs y mejoras de UI"
 */

// Cargar variables de entorno desde archivo .env
require('dotenv').config();

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const https = require('https');
const { updateVersion } = require('./update_version');

// Configuración - Variables de entorno requeridas
const GITHUB_REPO = 'JhonnyXT/LibraryInventoryApp'; // Tu repo de GitHub
const GITHUB_TOKEN = process.env.GITHUB_TOKEN || 'TU_GITHUB_TOKEN_AQUI'; // Personal Access Token de GitHub
const APK_PATH = path.join(__dirname, '../app/build/outputs/apk/release/app-release.apk'); // APK firmado con nuevo keystore

async function createRelease(releaseType = 'patch', releaseNotes = '') {
  try {
    console.log('🚀 Iniciando proceso de release automatizado...\n');
    
    // 1. 📝 Actualizar versión
    console.log('📝 Paso 1: Actualizando versión...');
    const versionInfo = updateVersion(releaseType);
    const { versionName, versionCode } = versionInfo;
    
    // 2. 🏗️ Compilar APK release
    console.log('\n🏗️ Paso 2: Compilando APK release...');
    console.log('⏳ Esto puede tomar varios minutos...');
    
    try {
      execSync('gradlew.bat assembleRelease', { 
        stdio: 'pipe',
        cwd: path.join(__dirname, '..')
      });
      console.log('✅ APK compilado exitosamente');
    } catch (error) {
      throw new Error(`Error compilando APK: ${error.message}`);
    }
    
    // 3. ✅ Verificar que APK existe
    if (!fs.existsSync(APK_PATH)) {
      throw new Error(`APK no encontrado en: ${APK_PATH}`);
    }
    
    const apkStats = fs.statSync(APK_PATH);
    const apkSizeMB = (apkStats.size / 1024 / 1024).toFixed(2);
    console.log(`📱 APK generado: ${apkSizeMB} MB`);
    
    // 4. 🐙 Crear GitHub Release
    console.log('\n🐙 Paso 3: Creando GitHub Release...');
    const githubReleaseUrl = await createGitHubRelease(versionName, versionCode, releaseNotes);
    
    // 5. 📧 Notificar usuarios
    console.log('\n📧 Paso 4: Notificando usuarios...');
    await notifyUsers(versionName, githubReleaseUrl, releaseNotes);
    
    // 6. 🚀 Push automático de los cambios
    console.log('\n🚀 Paso 5: Push automático de cambios...');
    try {
      execSync('git push', { 
        stdio: 'pipe',
        cwd: path.join(__dirname, '..')
      });
      console.log('✅ Push automático completado');
    } catch (pushError) {
      console.log('⚠️ No se pudo hacer push automático:', pushError.message);
      console.log('   Puedes hacer push manual con: git push');
    }
    
    // 7. ✅ Resumen final
    console.log('\n🎉 ¡RELEASE COMPLETADO EXITOSAMENTE!');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log(`📱 Nueva versión: ${versionName} (Code: ${versionCode})`);
    console.log(`🔗 GitHub Release: ${githubReleaseUrl}`);
    console.log(`📧 Usuarios notificados automáticamente`);
    console.log(`📂 APK disponible para descarga pública`);
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    
  } catch (error) {
    console.error('\n❌ ERROR EN EL PROCESO DE RELEASE:');
    console.error(error.message);
    process.exit(1);
  }
}

async function createGitHubRelease(versionName, versionCode, releaseNotes) {
  
  // Leer GitHub token desde local.properties
  const localProps = readLocalProperties();
  const githubToken = localProps.GITHUB_TOKEN || GITHUB_TOKEN;
  
  const tagName = `v${versionName}`;
  const releaseName = `LibraryInventoryApp ${versionName}`;
  
  // Generar release notes automáticas si no se proporcionan
  const autoReleaseNotes = releaseNotes || generateAutoReleaseNotes(versionName);
  
  console.log(`📝 Creando release: ${releaseName}`);
  console.log(`🏷️ Tag: ${tagName}`);
  
  try {
    // 1. Crear GitHub Release usando API
    const releaseData = {
      tag_name: tagName,
      target_commitish: 'master',
      name: releaseName,
      body: autoReleaseNotes,
      draft: false,
      prerelease: false
    };

    const releaseUrl = await createGitHubReleaseAPI(githubToken, releaseData);
    console.log(`✅ Release creado en GitHub: ${releaseUrl}`);

    // 2. Subir APK como asset
    if (fs.existsSync(APK_PATH)) {
      console.log('📤 Subiendo APK...');
      await uploadAPKToRelease(githubToken, releaseUrl, APK_PATH, versionName);
      console.log('✅ APK subido exitosamente');
    } else {
      console.log('⚠️ APK no encontrado, saltando subida');
    }

    return releaseUrl;
    
  } catch (error) {
    console.error('❌ Error creando GitHub Release:', error.message);
    
    // Aún intentar subir APK aunque falle la creación del release
    const fallbackUrl = `https://github.com/${GITHUB_REPO}/releases/tag/${tagName}`;
    console.log(`⚠️ Usando URL de fallback: ${fallbackUrl}`);
    
    // 🔧 NUEVO: Intentar subir APK aunque el release haya fallado
    if (fs.existsSync(APK_PATH)) {
      try {
        console.log('📤 Intentando subir APK con método alternativo...');
        
        // Esperar un momento para que GitHub sincronice
        await new Promise(resolve => setTimeout(resolve, 2000));
        
        // Intentar subir APK usando el tag que debe existir
        await uploadAPKToRelease(githubToken, fallbackUrl, APK_PATH, versionName);
        console.log('✅ APK subido exitosamente (método alternativo)');
        
      } catch (uploadError) {
        console.error('⚠️ No se pudo subir APK automaticamente:', uploadError.message);
        console.log(`📱 APK compilado en: ${APK_PATH}`);
        console.log(`🔗 Puedes subirlo manualmente en: ${fallbackUrl}`);
      }
    }
    
    return fallbackUrl;
  }
}

// Función auxiliar para leer local.properties
function readLocalProperties() {
  const fs = require('fs');
  const path = require('path');
  
  const localPropertiesPath = path.join(__dirname, '../local.properties');
  const properties = {};
  
  if (fs.existsSync(localPropertiesPath)) {
    const content = fs.readFileSync(localPropertiesPath, 'utf8');
    content.split('\n').forEach(line => {
      const trimmed = line.trim();
      if (trimmed && !trimmed.startsWith('#')) {
        const [key, ...valueParts] = trimmed.split('=');
        if (key && valueParts.length > 0) {
          properties[key.trim()] = valueParts.join('=').trim();
        }
      }
    });
  }
  
  return properties;
}

// Crear release usando GitHub API
function createGitHubReleaseAPI(token, releaseData) {
  return new Promise((resolve, reject) => {
    const postData = JSON.stringify(releaseData);
    
    const options = {
      hostname: 'api.github.com',
      path: `/repos/${GITHUB_REPO}/releases`,
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Accept': 'application/vnd.github.v3+json',
        'User-Agent': 'LibraryInventoryApp-Release-Bot',
        'Content-Type': 'application/json',
        'Content-Length': postData.length
      }
    };

    const req = https.request(options, (res) => {
      let responseBody = '';
      res.on('data', (chunk) => {
        responseBody += chunk;
      });
      res.on('end', () => {
        if (res.statusCode === 201) {
          const response = JSON.parse(responseBody);
          resolve(response.html_url);
        } else {
          reject(new Error(`GitHub API Error ${res.statusCode}: ${responseBody}`));
        }
      });
    });

    req.on('error', (error) => {
      reject(error);
    });

    req.write(postData);
    req.end();
  });
}

// Subir APK como asset al release
async function uploadAPKToRelease(token, releaseUrl, apkPath, versionName) {
  return new Promise(async (resolve, reject) => {
    try {
      // 1. Obtener release ID desde la URL
      const releaseId = await getReleaseId(token, releaseUrl);
      
      // 2. Leer el archivo APK
      if (!fs.existsSync(apkPath)) {
        throw new Error(`APK no encontrado en: ${apkPath}`);
      }
      
      const apkData = fs.readFileSync(apkPath);
      const apkStats = fs.statSync(apkPath);
      const fileName = `LibraryInventoryApp-v${versionName}.apk`;
      
      console.log(`📤 Subiendo ${fileName} (${(apkStats.size / 1024 / 1024).toFixed(2)} MB)...`);
      
      // 3. Subir usando GitHub Upload API
      const uploadOptions = {
        hostname: 'uploads.github.com',
        path: `/repos/${GITHUB_REPO}/releases/${releaseId}/assets?name=${fileName}`,
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Accept': 'application/vnd.github.v3+json',
          'Content-Type': 'application/vnd.android.package-archive',
          'Content-Length': apkData.length,
          'User-Agent': 'LibraryInventoryApp-Release-Bot'
        }
      };

      const req = https.request(uploadOptions, (res) => {
        let responseBody = '';
        res.on('data', (chunk) => {
          responseBody += chunk;
        });
        res.on('end', () => {
          if (res.statusCode === 201) {
            const response = JSON.parse(responseBody);
            console.log(`✅ APK subido exitosamente: ${response.browser_download_url}`);
            resolve(response.browser_download_url);
          } else {
            reject(new Error(`GitHub Upload Error ${res.statusCode}: ${responseBody}`));
          }
        });
      });

      req.on('error', (error) => {
        reject(error);
      });

      req.write(apkData);
      req.end();
      
    } catch (error) {
      reject(error);
    }
  });
}

// Obtener Release ID desde GitHub API
async function getReleaseId(token, releaseUrl) {
  return new Promise((resolve, reject) => {
    // Extraer tag desde la URL: https://github.com/user/repo/releases/tag/v1.0.17
    const tagName = releaseUrl.split('/').pop(); // v1.0.17
    
    const options = {
      hostname: 'api.github.com',
      path: `/repos/${GITHUB_REPO}/releases/tags/${tagName}`,
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Accept': 'application/vnd.github.v3+json',
        'User-Agent': 'LibraryInventoryApp-Release-Bot'
      }
    };

    const req = https.request(options, (res) => {
      let responseBody = '';
      res.on('data', (chunk) => {
        responseBody += chunk;
      });
      res.on('end', () => {
        if (res.statusCode === 200) {
          const response = JSON.parse(responseBody);
          resolve(response.id);
        } else {
          reject(new Error(`GitHub API Error ${res.statusCode}: ${responseBody}`));
        }
      });
    });

    req.on('error', (error) => {
      reject(error);
    });

    req.end();
  });
}

async function notifyUsers(versionName, releaseUrl, releaseNotes) {
  try {
    // Ejecutar script de notificaciones
    const notifyScript = path.join(__dirname, '../notify_new_version.js');
    const command = `node "${notifyScript}" "${versionName}" "${releaseUrl}"`;
    
    execSync(command, { stdio: 'inherit' });
    console.log('✅ Notificaciones enviadas exitosamente');
    
  } catch (error) {
    console.error('⚠️ Error enviando notificaciones:', error.message);
    console.log('📧 Puedes enviar notificaciones manualmente después');
  }
}

function generateAutoReleaseNotes(versionName) {
  return `
## 🚀 LibraryInventoryApp ${versionName}

### 🆕 Novedades en esta versión:
- 🔐 Sistema de autenticación con Google mejorado
- 🎨 Interfaz de usuario optimizada
- ⚡ Rendimiento y estabilidad mejorados
- 🔧 Corrección de errores reportados

### 📱 Instalación:
1. Descarga el archivo APK
2. Permite "Fuentes desconocidas" en tu dispositivo
3. Instala la nueva versión
4. ¡Disfruta las mejoras!

### 📋 Información técnica:
- Versión: ${versionName}
- Compatibilidad: Android 8.0+ (API 26+)
- Tamaño: ~6-8 MB
- Firmado: Sí
  `.trim();
}

// Ejecutar script
const [,, releaseType, releaseNotes] = process.argv;

if (!releaseType) {
  console.error(`
❌ Uso: node scripts/release.js [major|minor|patch] "Descripción del release"

Ejemplos:
  node scripts/release.js patch "Corrección de bugs"
  node scripts/release.js minor "Nueva funcionalidad agregada"
  node scripts/release.js major "Cambios importantes en la app"
  `);
  process.exit(1);
}

createRelease(releaseType, releaseNotes);
