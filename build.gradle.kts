// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Android plugins
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    
    // Kotlin plugins
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    
    // Firebase
    id("com.google.gms.google-services") version "4.3.15" apply false
}