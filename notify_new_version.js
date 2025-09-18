#!/usr/bin/env node

/**
 * Script para notificar nueva versión de LibraryInventoryApp
 * Envía emails automáticos a todos los usuarios registrados
 * 
 * Uso: node notify_new_version.js <version> <github_release_url>
 * Ejemplo: node notify_new_version.js "1.0.4" "https://github.com/tu-usuario/LibraryInventoryApp/releases/tag/v1.0.4"
 */

const admin = require('firebase-admin');
const sgMail = require('@sendgrid/mail');

// Configuración
const SENDGRID_API_KEY = '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';
const FROM_EMAIL = 'hermanosencristobello@gmail.com';

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

sgMail.setApiKey(SENDGRID_API_KEY);

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
    
    // 2. Crear emails personalizados
    const emails = users.map(user => ({
      to: user.email,
      from: FROM_EMAIL,
      subject: `📱 LibraryInventoryApp ${version} - Nueva versión disponible`,
      html: generateEmailHTML(user, version, releaseUrl)
    }));
    
    // 3. Enviar emails en lotes de 1000 (límite SendGrid)
    const batchSize = 100;
    for (let i = 0; i < emails.length; i += batchSize) {
      const batch = emails.slice(i, i + batchSize);
      await sgMail.send(batch);
      console.log(`📧 Enviado lote ${Math.floor(i/batchSize) + 1}/${Math.ceil(emails.length/batchSize)}`);
    }
    
    console.log(`✅ ¡Notificación completada! ${emails.length} emails enviados`);
    
  } catch (error) {
    console.error('❌ Error enviando notificaciones:', error);
  }
}

function generateEmailHTML(user, version, releaseUrl) {
  return `
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>Nueva versión disponible</title>
  <style>
    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
    .header { background: #1976D2; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px; }
    .button { display: inline-block; background: #4CAF50; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }
    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header">
      <h1>📱 LibraryInventoryApp</h1>
      <h2>Nueva versión ${version} disponible</h2>
    </div>
    
    <div class="content">
      <p>¡Hola ${user.name}!</p>
      
      <p>Nos complace informarte que hemos lanzado una nueva versión de LibraryInventoryApp con mejoras y nuevas funcionalidades.</p>
      
      <h3>🆕 Novedades en esta versión:</h3>
      <ul>
        <li>🔐 Inicio de sesión con Google implementado</li>
        <li>🎨 Interfaz de usuario mejorada y más moderna</li>
        <li>⚡ Rendimiento optimizado</li>
        <li>🔧 Corrección de errores y estabilidad mejorada</li>
      </ul>
      
      <p style="text-align: center;">
        <a href="${releaseUrl}" class="button">📥 Descargar nueva versión</a>
      </p>
      
      <h3>📱 Instrucciones de instalación:</h3>
      <ol>
        <li>Haz clic en el botón de descarga arriba</li>
        <li>Descarga el archivo APK</li>
        <li>Permite "Fuentes desconocidas" en tu dispositivo si es necesario</li>
        <li>Instala la nueva versión</li>
      </ol>
      
      <p><strong>Nota:</strong> Es recomendable desinstalar la versión anterior antes de instalar la nueva.</p>
      
      <p>Si tienes alguna pregunta o problema, no dudes en contactarnos.</p>
      
      <p>¡Gracias por usar el sistema de inventario de libros!</p>
    </div>
    
    <div class="footer">
      <p>Este es un email automático. Para dejar de recibir notificaciones, contacta al administrador.</p>
    </div>
  </div>
</body>
</html>
  `;
}

// Ejecutar script
const [,, version, releaseUrl] = process.argv;

if (!version || !releaseUrl) {
  console.error('❌ Uso: node notify_new_version.js <version> <github_release_url>');
  process.exit(1);
}

notifyNewVersion(version, releaseUrl);
