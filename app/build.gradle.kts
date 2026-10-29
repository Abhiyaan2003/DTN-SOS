// :app — Android application.
// BouncyCastle (bcprov-jdk18on) comes transitively from :core.
// We exclude it from the app runtime and instead rely on the lightweight API path
// that does NOT call Security.addProvider(), so there is no JCE conflict with AOSP.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "dtnsos.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "dtnsos.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-m0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }

    // Kotlin 2.x: Compose compiler is managed by the kotlin.compose plugin; no manual extension version needed.

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Protocol core (no Android imports inside)
    implementation(project(":core")) {
        // Exclude the BouncyCastle JAR from being bundled again in the APK runtime classpath.
        // :core uses only the lightweight API (no Security.addProvider call), so the Android
        // platform's own provider does not conflict. However, having two bcprov jars can cause
        // duplicate class errors in D8. We therefore keep bcprov only in :core's compileClasspath
        // and exclude it from the app's runtimeClasspath.
        exclude(group = "org.bouncycastle", module = "bcprov-jdk18on")
    }
    // Re-add BouncyCastle as an implementation dependency of :app so it IS bundled in the APK,
    // but only once. This is the cleanest way: one copy, no JCE provider registration.
    implementation(libs.bouncycastle)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.vm)
    implementation(libs.androidx.activity)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Location (optional for demo; allow runtime unavailability)
    implementation(libs.play.location)

    // Compose
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.extended)
    implementation(libs.compose.foundation)
    debugImplementation(libs.compose.ui.tooling)
}
