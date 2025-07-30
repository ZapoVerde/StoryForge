import java.util.Properties


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    }

// --- Load local.properties ---
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        load(file.inputStream())
    }
}

val deepseekApiKey: String = localProperties.getProperty("DEEPSEEK_API_KEY")
    ?: System.getenv("DEEPSEEK_API_KEY")
    ?: "MISSING_API_KEY"


android {
    namespace = "com.example.storyforge"
    compileSdk = 36  // Updated to latest stable SDK

    defaultConfig {
        applicationId = "com.example.storyforge"
        minSdk = 29 // Lowered to API 24 for wider compatibility
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"



        // Safe API key loading with fallbacks
        buildConfigField("String", "DEEPSEEK_API_KEY", "\"$deepseekApiKey\"")

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    implementation("com.google.accompanist:accompanist-pager:0.36.0")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.36.0")


    implementation("androidx.compose.ui:ui:1.8.3") // or newer
    implementation("androidx.compose.ui:ui-tooling:1.8.3")
    implementation(libs.androidx.foundation.v161)
    implementation(libs.androidx.material.icons.extended)
    //implementation("androidx.compose.foundation:foundation:1.4.3")

    // Jetpack Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.9.2")

    // Optional but useful:
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    // Networking (Add these one group at a time)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")                // Core Retrofit
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")          // JSON parsing
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.14")            // HTTP client
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.14") // Debugging

    // Coroutines (if not already present)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation(libs.material3)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Testing (optional)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}