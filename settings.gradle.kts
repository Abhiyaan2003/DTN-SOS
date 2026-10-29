pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
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
