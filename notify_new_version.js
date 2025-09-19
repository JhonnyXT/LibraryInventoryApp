#!/usr/bin/env node

/**
 * Script para notificar nueva versión de LibraryInventoryApp
 * Envía emails automáticos a todos los usuarios registrados
 * 
 * Uso: node notify_new_version.js <version> <github_release_url>
 * Ejemplo: node notify_new_version.js "1.0.4" "https://github.com/tu-usuario/LibraryInventoryApp/releases/tag/v1.0.4"
 */

// Leer configuración desde local.properties (consistente con Android)
const fs = require('fs');
const path = require('path');

const admin = require('firebase-admin');
const https = require('https');

// Leer local.properties
function readLocalProperties() {
  const localPropertiesPath = path.join(__dirname, 'local.properties');
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

const localProps = readLocalProperties();

// Configuración - Leer desde local.properties
const BREVO_API_KEY = localProps.BREVO_API_KEY || 'TU_BREVO_API_KEY_AQUI';
const FROM_EMAIL = localProps.BREVO_FROM_EMAIL || 'tu-email@ejemplo.com';
const BREVO_URL = 'https://api.brevo.com/v3/smtp/email';

// Inicializar Firebase Admin
// IMPORTANTE: Descargar serviceAccountKey.json desde Firebase Console
// Ve a: Configuración del proyecto > Cuentas de servicio > Generar nueva clave privada
try {
  const serviceAccount = require('./serviceAccountKey.json');
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
} catch (error) {
  console.error('❌ Error: No se encontró serviceAccountKey.json');
  console.error('📋 Para configurarlo:');
  console.error('1. Ve a Firebase Console > Configuración del proyecto');
  console.error('2. Pestaña "Cuentas de servicio"'); 
  console.error('3. Click "Generar nueva clave privada"');
  console.error('4. Guarda el archivo como "serviceAccountKey.json" en la raíz del proyecto');
  process.exit(1);
}

// Función COPIADA EXACTAMENTE del test que funcionó
async function sendBrevoEmail(email, name, subject, htmlContent) {
  return new Promise((resolve, reject) => {
    const data = JSON.stringify({
      sender: { 
        email: FROM_EMAIL, 
        name: "Sistema de Biblioteca" 
      },
      to: [{ 
        email: email, 
        name: name 
      }],
      subject: subject,
      htmlContent: htmlContent
    });

    const options = {
      hostname: 'api.brevo.com',
      path: '/v3/smtp/email',
      method: 'POST',
      headers: {
        'api-key': BREVO_API_KEY,
        'Content-Type': 'application/json',
        'Content-Length': data.length
      }
    };

    const req = https.request(options, (res) => {
      let responseBody = '';
      res.on('data', (chunk) => {
        responseBody += chunk;
      });
      res.on('end', () => {
        if (res.statusCode === 201 || res.statusCode === 200) {
          resolve(`✅ Email enviado exitosamente`);
        } else {
          reject(new Error(`❌ Brevo Error ${res.statusCode}: ${responseBody}`));
        }
      });
    });

    req.on('error', (error) => {
      reject(error);
    });

    req.write(data);
    req.end();
  });
}

async function notifyNewVersion(version, releaseUrl) {
  try {
    console.log(`🚀 Iniciando notificación de versión ${version}...`);
    
    // 1. Obtener todos los usuarios de Firestore
    const db = admin.firestore();
    const usersSnapshot = await db.collection('users').get();
    
    const users = [];
    usersSnapshot.forEach(doc => {
      const userData = doc.data();
      if (userData.email && userData.email.includes('@')) {
        users.push({
          name: userData.name || 'Usuario',
          email: userData.email,
          role: userData.role || 'usuario'
        });
      }
    });
    
    console.log(`👥 Encontrados ${users.length} usuarios para notificar`);
    
    // 2. Enviar emails a TODOS los usuarios con datos sanitizados
    let emailsSent = 0;
    
    for (const user of users) {
      try {
        // SANITIZAR COMPLETAMENTE los datos de Firebase
        const cleanEmail = user.email.toString().trim().toLowerCase();
        const cleanName = user.name.toString()
          .normalize('NFD')
          .replace(/[\u0300-\u036f]/g, '') // Remover acentos
          .replace(/[^\w\s-]/g, '') // Solo letras, números, espacios y guiones
          .trim();
        
        console.log(`📤 Enviando ${emailsSent + 1}/${users.length}: ${cleanName} (${cleanEmail})`);
        
        // Template profesional SIN emojis problemáticos
        const subject = `Sistema de Biblioteca ${version} - Nueva version disponible`;
        const htmlContent = `
          <html>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
              <div style="background: #1976D2; color: white; padding: 20px; text-align: center; border-radius: 8px;">
                <h1>Sistema de Biblioteca</h1>
                <h2>Nueva version ${version} disponible</h2>
              </div>
              
              <div style="background: #f9f9f9; padding: 30px; border-radius: 8px; margin-top: 10px;">
                <p>Hola ${cleanName},</p>
                
                <p>Nos complace informarte que hemos lanzado una nueva version del Sistema de Biblioteca con mejoras y nuevas funcionalidades.</p>
                
                <h3>Novedades en esta version:</h3>
                <ul>
                  <li>Sistema de autenticacion mejorado con Google Sign-In</li>
                  <li>Sistema de notificaciones por email mejorado (9,000 emails/mes gratis)</li>
                  <li>Interfaz de usuario actualizada y moderna</li>
                  <li>Correccion de errores y mejoras de rendimiento</li>
                </ul>
                
                <div style="text-align: center; margin: 30px 0;">
                  <a href="${releaseUrl}" style="background: #4CAF50; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block; font-weight: bold;">Descargar Nueva Version</a>
                </div>
                
                <h3>Instrucciones de instalacion:</h3>
                <ol>
                  <li><strong>Descarga:</strong> Haz clic en el boton verde "Descargar Nueva Version" arriba</li>
                  <li><strong>GitHub:</strong> Se abrira GitHub, busca el archivo "app-release-unsigned.apk" y descargalo</li>
                  <li><strong>Permisos:</strong> En tu dispositivo Android, ve a Configuracion > Seguridad > "Permitir instalacion de fuentes desconocidas" (activar)</li>
                  <li><strong>Instalar:</strong> Abre el archivo APK descargado desde tus archivos y toca "Instalar"</li>
                  <li><strong>Finalizar:</strong> Una vez instalada, puedes desactivar "fuentes desconocidas" si lo prefieres</li>
                </ol>
                
                <div style="background: #fff3cd; border: 1px solid #ffeaa7; padding: 15px; border-radius: 5px; margin: 15px 0;">
                  <p><strong>Recomendaciones importantes:</strong></p>
                  <ul>
                    <li>Desinstala la version anterior antes de instalar la nueva</li>
                    <li>Asegurate de tener conexion a Internet estable</li>
                    <li>Si aparece advertencia de "App no verificada", toca "Instalar de todas formas"</li>
                    <li>En algunos dispositivos Samsung, busca "Instalar apps desconocidas" en Configuracion</li>
                  </ul>
                </div>
                
                <p>Si tienes alguna pregunta o problema, no dudes en contactarnos.</p>
                
                <p><strong>Gracias por usar el Sistema de Biblioteca!</strong></p>
              </div>
              
              <div style="text-align: center; margin-top: 30px; color: #666; font-size: 12px;">
                <p>Este es un email automatico del Sistema de Biblioteca.</p>
              </div>
            </body>
          </html>
        `.replace(/\s+/g, ' ').trim();
        
        await sendBrevoEmail(cleanEmail, cleanName, subject, htmlContent);
        emailsSent++;
        console.log(`📧 ✅ Enviado exitosamente - ${emailsSent}/${users.length}`);
        
        // Pausa para respetar rate limits de Brevo
        await new Promise(resolve => setTimeout(resolve, 500));
        
      } catch (emailError) {
        console.error(`❌ Error enviando a ${user.email}:`, emailError.message);
      }
    }
    
    console.log(`✅ ¡Notificación completada! ${emailsSent} emails enviados`);
    
  } catch (error) {
    console.error('❌ Error enviando notificaciones:', error);
  }
}

// Ejecutar script
const [,, version, releaseUrl] = process.argv;

if (!version || !releaseUrl) {
  console.error('❌ Uso: node notify_new_version.js <version> <github_release_url>');
  process.exit(1);
}

notifyNewVersion(version, releaseUrl);
