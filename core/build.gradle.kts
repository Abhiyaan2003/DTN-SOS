// :core — pure Kotlin/JVM library.
// RULE: zero Android imports. All protocol logic lives here and is testable on the JVM.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Crypto: BouncyCastle lightweight API (NOT the JCE provider).
    // Ed25519Signer is used directly from org.bouncycastle.crypto.signers.Ed25519Signer.
    // Do not call Security.addProvider(BouncyCastleProvider()) anywhere in :core.
    implementation(libs.bouncycastle)

    // Test
    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit.platform)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = true
    }
}
