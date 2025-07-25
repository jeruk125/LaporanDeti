plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.laporandeti"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.laporandeti"
        minSdk = 30
        targetSdk = 36
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
    buildFeatures {
        compose = true
    }
}

// Cara mendefinisikan variabel versi di Kotlin DSL
val cameraxVersion = "1.4.2" // <--- PERBAIKAN DI SINI: ganti def dengan val
// Gunakan versi terbaru yang stabil dari CameraX
// Anda bisa cek di: https://developer.android.com/jetpack/androidx/releases/camera
val pagingVersion = "3.3.0" // Atau versi stabil terbaru

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose) // Jika Compose
    implementation(platform(libs.androidx.compose.bom)) // Jika Compose
    implementation(libs.androidx.ui) // Jika Compose
    implementation(libs.androidx.ui.graphics) // Jika Compose
    implementation(libs.androidx.ui.tooling.preview) // Jika Compose
    implementation(libs.androidx.material3) // Jika Compose

    // Dependensi Material Icons (jika digunakan)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)

    // Navigasi Compose
    implementation("androidx.navigation:navigation-compose:2.7.7") // Cek versi terbaru

    // LiveData dengan Compose
    implementation("androidx.compose.runtime:runtime-livedata:1.6.8") // Cek versi terbaru

    // CameraX dependencies
    implementation("androidx.camera:camera-core:${cameraxVersion}")
    implementation("androidx.camera:camera-camera2:${cameraxVersion}")
    implementation("androidx.camera:camera-lifecycle:${cameraxVersion}")
    implementation("androidx.camera:camera-view:${cameraxVersion}")
    implementation("com.google.guava:guava:31.1-android")
    // implementation("androidx.camera:camera-video:${cameraxVersion}") // Jika butuh video
    // implementation("androidx.camera:camera-extensions:${cameraxVersion}") // Jika butuh efek tambahan

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom)) // Jika Compose
    androidTestImplementation(libs.androidx.ui.test.junit4) // Jika Compose
    debugImplementation(libs.androidx.ui.tooling) // Jika Compose
    debugImplementation(libs.androidx.ui.test.manifest) // Jika Compose
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("androidx.paging:paging-runtime-ktx:${pagingVersion}")
    implementation("androidx.paging:paging-compose:${pagingVersion}")
    implementation("androidx.privacysandbox.tools:tools:1.0.0-alpha13") // Atau versi terbaru
    implementation("androidx.privacysandbox.tools:tools-apicompiler:1.0.0-alpha13") // Atau versi terbaru
    implementation("androidx.privacysandbox.tools:tools-apigenerator:1.0.0-alpha13") // Atau versi terbaru
    implementation("androidx.wear.compose:compose-material:1.4.1") // Atau versi stabil/beta terbaru
    // Catatan: androidx.wear.compose:compose-material telah digantikan oleh androidx.wear.compose:compose-material3
    // Jika Anda memulai proyek baru atau bisa upgrade, pertimbangkan material3:
    // implementation("androidx.wear.compose:compose-material3:1.0.0-alpha20") // Atau versi terbaru

    // Wear Compose juga memerlukan compose-foundation untuk Wear:
    implementation("androidx.wear.compose:compose-foundation:1.4.1") // Atau versi terbaru yang cocok
    implementation("androidx.exifinterface:exifinterface:1.3.7")
// Versi terbaru mungkin berbeda
    implementation("androidx.compose.material:material-icons-extended:1.6.8")
    implementation("com.google.android.gms:play-services-location:21.0.1")
// Sesuaikan dengan versi Compose Anda
}