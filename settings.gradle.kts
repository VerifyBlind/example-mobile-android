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
}

rootProject.name = "example-mobile-android"
include(":app")

// SDK modülünü yerel klasörden dahil et
include(":verifyblind")
project(":verifyblind").projectDir = file("../sdk-android/verifyblind")
