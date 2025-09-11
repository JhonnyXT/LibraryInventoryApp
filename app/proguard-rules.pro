# LibraryInventoryApp - ProGuard Rules for Release
# Configuración profesional para optimización y ofuscación

# ========================================
# CONFIGURACIÓN GENERAL
# ========================================
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Mantener clases principales de la aplicación
-keep class com.example.libraryinventoryapp.** { *; }

# ========================================
# FIREBASE CONFIGURATION
# ========================================
# Firebase Authentication
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.firebase.auth.internal.** { *; }

# Firebase Firestore
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firestore.v1.** { *; }
-keepclasseswithmembernames class com.google.firebase.firestore.** { *; }

# Firebase Storage
-keep class com.google.firebase.storage.** { *; }

# Firebase Core
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# ========================================
# GLIDE IMAGE LOADING
# ========================================
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}

# ========================================
# OKHTTP3 NETWORKING
# ========================================
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ========================================
# ZXING BARCODE SCANNING
# ========================================
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }
-dontwarn com.google.zxing.**

# ========================================
# CAMERAX
# ========================================
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ML Kit Barcode Scanning
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ========================================
# MATERIAL DESIGN COMPONENTS
# ========================================
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# ========================================
# KOTLIN & ANDROID
# ========================================
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**

# Android Support Library
-keep class androidx.** { *; }
-dontwarn androidx.**

# ========================================
# SERIALIZATION & PARCELING
# ========================================
-keepclassmembers class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator CREATOR;
}

# ========================================
# REFLECTION & ANNOTATIONS
# ========================================
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations

# ========================================
# DEBUGGING (OPCIONAL - Solo para testing)
# ========================================
# Mantener nombres de clases para mejor debugging en caso de crash
-keepnames class com.example.libraryinventoryapp.**
-keepnames class com.example.libraryinventoryapp.models.**
-keepnames class com.example.libraryinventoryapp.adapters.**
-keepnames class com.example.libraryinventoryapp.fragments.**
-keepnames class com.example.libraryinventoryapp.activities.**