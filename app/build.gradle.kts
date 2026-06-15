import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")

    // 🔥 плагин Google Services
    id("com.google.gms.google-services")
}

fun readLocalProperty(key: String, defaultValue: String = ""): String {
    val properties = Properties()
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { properties.load(it) }
    }
    return properties.getProperty(key, defaultValue)
}

android {
    namespace = "com.example.ecozaschitnik"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.ecozaschitnik"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "OPENROUTER_API_KEY",
            "\"${readLocalProperty("OPENROUTER_API_KEY")}\"",
        )
        buildConfigField(
            "String",
            "LLM_MODEL",
            "\"${readLocalProperty("LLM_MODEL")}\"",
        )
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

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.cardview:cardview:1.0.0")

    // Камера
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")

    // Геолокация
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Карта (osmdroid + кластеризация как на сайте)
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("com.github.MKergall:osmbonuspack:6.9.0")

    // Retrofit + JSON
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Коррутины
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("androidx.activity:activity-ktx:1.11.0")
    implementation("io.coil-kt:coil:2.6.0")

    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // ⬇️ ВАЖНО: Guava — даёт класс ListenableFuture для CameraX
    implementation("com.google.guava:guava:33.5.0-android")

    // Тесты
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

