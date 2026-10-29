pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    // libs.versions.toml is auto-discovered from gradle/libs.versions.toml (Gradle 7.4+).
}

rootProject.name = "dtn-sos"
include(":core")
include(":app")
