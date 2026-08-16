import java.net.URI

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
}

val debugApiBaseUrl = providers.gradleProperty("DEARLY_API_BASE_URL")
    .orElse("http://10.0.2.2:8080/api/v1/")
val releaseApiBaseUrl = providers.gradleProperty("DEARLY_RELEASE_API_BASE_URL")
    .orElse("https://api.dearly.invalid/api/v1/")

fun validatedApiBaseUrl(value: String, requireHttps: Boolean): String {
    val scheme = runCatching { URI(value).scheme?.lowercase() }.getOrNull()
    require(scheme == "http" || scheme == "https") { "API base URL must use HTTP or HTTPS" }
    require(value.endsWith('/')) { "API base URL must end with /" }
    if (requireHttps) {
        require(scheme == "https") { "Release API base URL must use HTTPS" }
    }
    return value
}

android {
    namespace = "com.dearly.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dearly.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        getByName("debug") {
            val apiUrl = validatedApiBaseUrl(debugApiBaseUrl.get(), requireHttps = false)
            buildConfigField("String", "API_BASE_URL", "\"$apiUrl\"")
        }
        release {
            val apiUrl = validatedApiBaseUrl(releaseApiBaseUrl.get(), requireHttps = true)
            buildConfigField("String", "API_BASE_URL", "\"$apiUrl\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Keep Kotlin bytecode aligned with the Java 21 Android configuration.
// Android Studio currently runs Gradle on JDK 25, which otherwise makes
// Kotlin fall back to JVM 24 and causes an incompatible-target build error.
kotlin {
    jvmToolchain(21)
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-metadata-jvm:2.1.0")
        force("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.9.0")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose BOM & Extended Icons
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Hilt 2.59.2 with KSP
    implementation("com.google.dagger:hilt-android:2.59.2")
    ksp("com.google.dagger:hilt-compiler:2.59.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Room with KSP
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Firebase authentication and push messaging used by the backend session flow.
    implementation(platform("com.google.firebase:firebase-bom:34.16.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    // implementation("androidx.credentials:credentials:1.3.0")
    // implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    // implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
