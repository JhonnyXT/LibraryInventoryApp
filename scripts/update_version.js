#!/usr/bin/env node

/**
 * Script para actualizar versión automáticamente siguiendo estándares Google
 * Uso: node scripts/update_version.js [major|minor|patch]
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const GRADLE_FILE = path.join(__dirname, '../app/build.gradle.kts');

function updateVersion(releaseType = 'patch') {
  try {
    // 1. Leer archivo gradle actual
    let gradleContent = fs.readFileSync(GRADLE_FILE, 'utf8');
    
    // 2. Extraer versionCode y versionName actuales
    const versionCodeMatch = gradleContent.match(/versionCode = (\d+)/);
    const versionNameMatch = gradleContent.match(/versionName = "([^"]+)"/);
    
    if (!versionCodeMatch || !versionNameMatch) {
      throw new Error('No se pudo encontrar versionCode o versionName en build.gradle.kts');
    }
    
    let currentVersionCode = parseInt(versionCodeMatch[1]);
    let currentVersionName = versionNameMatch[1];
    
    console.log(`📱 Versión actual: ${currentVersionName} (Code: ${currentVersionCode})`);
    
    // 3. Incrementar versionCode (siempre +1)
    const newVersionCode = currentVersionCode + 1;
    
    // 4. Incrementar versionName según tipo de release
    const versionParts = currentVersionName.split('.').map(v => parseInt(v));
    let [major, minor, patch] = versionParts;
    
    switch (releaseType) {
      case 'major':
        major += 1;
        minor = 0;
        patch = 0;
        break;
      case 'minor':
        minor += 1;
        patch = 0;
        break;
      case 'patch':
      default:
        patch += 1;
        break;
    }
    
    const newVersionName = `${major}.${minor}.${patch}`;
    
    // 5. Actualizar archivo gradle
    gradleContent = gradleContent.replace(
      /versionCode = \d+/,
      `versionCode = ${newVersionCode}`
    );
    
    gradleContent = gradleContent.replace(
      /versionName = "[^"]+"/,
      `versionName = "${newVersionName}"`
    );
    
    // 6. Guardar cambios
    fs.writeFileSync(GRADLE_FILE, gradleContent);
    
    console.log(`✅ Nueva versión: ${newVersionName} (Code: ${newVersionCode})`);
    console.log(`📝 Archivo actualizado: ${GRADLE_FILE}`);
    
    // 7. 🚀 Commit automático de la nueva versión
    try {
      console.log('🔄 Haciendo commit automático de la nueva versión...');
      
      // Git add del archivo gradle
      execSync(`git add "${GRADLE_FILE}"`, { 
        stdio: 'pipe',
        cwd: path.join(__dirname, '..')
      });
      
      // Git commit con mensaje descriptivo
      const commitMessage = `📱 Bump version to ${newVersionName} (Code: ${newVersionCode})

✅ VERSION BUMPED AUTOMÁTICAMENTE:
- versionName: ${currentVersionName} → ${newVersionName}
- versionCode: ${currentVersionCode} → ${newVersionCode}
- Release type: ${releaseType}

🤖 Commit automático generado por el script de release`;

      execSync(`git commit -m "${commitMessage}"`, { 
        stdio: 'pipe',
        cwd: path.join(__dirname, '..')
      });
      
      console.log('✅ Commit automático completado');
      console.log(`📝 Commit: v${newVersionName} bump automático`);
      
    } catch (error) {
      console.log('⚠️ No se pudo hacer commit automático (posiblemente no hay cambios o git no configurado)');
      console.log(`   Puedes hacer commit manual con: git add app/build.gradle.kts && git commit`);
    }
    
    // 8. Retornar nueva versión para otros scripts
    return {
      versionCode: newVersionCode,
      versionName: newVersionName,
      releaseType
    };
    
  } catch (error) {
    console.error('❌ Error actualizando versión:', error.message);
    process.exit(1);
  }
}

// Ejecutar si se llama directamente
if (require.main === module) {
  const releaseType = process.argv[2] || 'patch';
  
  if (!['major', 'minor', 'patch'].includes(releaseType)) {
    console.error('❌ Tipo de release inválido. Usa: major, minor, o patch');
    process.exit(1);
  }
  
  updateVersion(releaseType);
}

module.exports = { updateVersion };
