import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    kotlin("kapt")
}

// Leer propiedades del archivo local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.example.libraryinventoryapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.libraryinventoryapp"
        minSdk = 26  // Android 8.0+ (Cambiado desde Android 7.0)
        targetSdk = 34  // Android 14 (última versión)
        versionCode = 14
        versionName = "1.0.17"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Configuración de Brevo desde local.properties
        buildConfigField("String", "BREVO_API_KEY", "\"${localProperties.getProperty("BREVO_API_KEY", "TU_API_KEY_DE_BREVO_AQUI")}\"")
        buildConfigField("String", "BREVO_FROM_EMAIL", "\"${localProperties.getProperty("BREVO_FROM_EMAIL", "hermanosencristobello@gmail.com")}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            storeFile = file("../libraryinventorykeystore.jks")
            storePassword = "library123"
            keyAlias = "librarykey"
            keyPassword = "library123"
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            versionNameSuffix = "-DEBUG"
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // signingConfig = signingConfigs.getByName("release") // Temporalmente deshabilitado
            
            // Optimizaciones adicionales
            isJniDebuggable = false
            isPseudoLocalesEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Firebase
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    
    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // ZXing
    implementation(libs.zxing.core)
    implementation(libs.zxing.android.embedded)

    // CameraX
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // Scanning
    implementation(libs.play.services.mlkit.barcode.scanning)

    // Glide
    implementation(libs.glide)
    kapt(libs.glide.compiler)

    // HTTP requests para envío de correos
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Kotlin Coroutines para operaciones asíncronas
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}