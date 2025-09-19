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
        const htmlContent = `<!DOCTYPE html><html><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><style>@media screen and (max-width: 600px) { .container { width: 100% !important; padding: 15px !important; } .header h1 { font-size: 24px !important; } }</style></head><body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f8fafc; line-height: 1.6;"><div class="container" style="max-width: 600px; margin: 0 auto; padding: 20px; background-color: #ffffff;"><div class="header" style="text-align: center; padding: 30px 0; background: linear-gradient(135deg, #4CAF50 0%, #66BB6A 100%); border-radius: 12px; margin-bottom: 30px; color: white;"><h1 style="margin: 0; font-size: 28px; font-weight: 600;">Sistema de Biblioteca</h1><p style="margin: 10px 0 0 0; opacity: 0.9; font-size: 16px;">Nueva version ${version} disponible</p></div><div style="text-align: center; margin-bottom: 25px;"><h2 style="color: #2d3748; margin: 0 0 10px 0; font-size: 24px; font-weight: 500;">Hola ${cleanName}</h2><p style="color: #4a5568; margin: 0; font-size: 16px;">Nos complace informarte que hemos lanzado una nueva version del Sistema de Biblioteca con mejoras importantes.</p></div><div style="background-color: #f7fafc; padding: 25px; border-radius: 12px; border: 1px solid #e2e8f0; margin: 25px 0;"><h3 style="text-align: center; color: #2d3748; margin-bottom: 20px; font-size: 20px;">Principales mejoras:</h3><div style="text-align: left; max-width: 400px; margin: 0 auto;"><div style="margin-bottom: 15px;"><span style="display: inline-block; background-color: #e3f2fd; color: #1976d2; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 500; margin-right: 10px;">UX/UI</span><span>Sistema profesional con Material Design 3</span></div><div style="margin-bottom: 15px;"><span style="display: inline-block; background-color: #f3e5f5; color: #7b1fa2; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 500; margin-right: 10px;">AUTH</span><span>Google Sign-In completamente funcional</span></div><div style="margin-bottom: 15px;"><span style="display: inline-block; background-color: #e8f5e8; color: #388e3c; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 500; margin-right: 10px;">AUTO</span><span>Sistema de automatizacion completo</span></div><div><span style="display: inline-block; background-color: #fff3e0; color: #e65100; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 500; margin-right: 10px;">APK</span><span>Version firmada y optimizada</span></div></div></div><div style="text-align: center; margin: 30px 0;"><a href="${releaseUrl}" style="background: #4CAF50; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block; font-weight: bold; font-size: 16px;">Descargar Nueva Version</a></div><div style="background: #fff3cd; border: 1px solid #ffeaa7; padding: 20px; border-radius: 8px; margin: 20px 0;"><h3 style="text-align: center; margin-bottom: 15px; color: #856404;">Instrucciones de instalacion:</h3><ol style="max-width: 400px; margin: 0 auto; text-align: left;"><li style="margin-bottom: 8px;"><strong>Descarga:</strong> Haz clic en el enlace de descarga</li><li style="margin-bottom: 8px;"><strong>GitHub:</strong> Busca el archivo LibraryInventoryApp-v${version}.apk</li><li style="margin-bottom: 8px;"><strong>Permisos:</strong> Permite fuentes desconocidas en Android</li><li><strong>Instalar:</strong> Abre el archivo APK e instala</li></ol></div><div style="margin-top: 40px; padding-top: 30px; border-top: 2px solid #e2e8f0; text-align: center;"><p style="margin: 0 0 5px 0; color: #2d3748; font-weight: 600; font-size: 16px;">Iglesia Hermanos en Cristo Bello</p><p style="margin: 0 0 15px 0; color: #718096; font-size: 14px;">Sistema de Biblioteca Digital</p><div style="background-color: #f7fafc; padding: 15px; border-radius: 8px; margin: 20px 0;"><p style="margin: 0; color: #4a5568; font-size: 12px;">Este es un email automatico del sistema de biblioteca.<br>Para cualquier consulta, contacta con el administrador.</p></div></div></div></body></html>`;
        
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
