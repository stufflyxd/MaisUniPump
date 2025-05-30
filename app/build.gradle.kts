plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")                                  // necessário para Glide
    id("com.google.gms.google-services")            // Google Services (Firebase)
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.unipump"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.unipump"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // AndroidX & UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Firebase BOM para gerenciar versões de Auth, Firestore e Storage
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))

    // Firebase (sem versões individuais, herdadas do BOM)
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // Glide para carregamento de imagem
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    // Outros
    implementation(libs.firebase.common.ktx)
    implementation(libs.generativeai)

    // Testes
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// Aplica o plugin do Google Services para que o Gradle processe o google-services.json
apply(plugin = "com.google.gms.google-services")
