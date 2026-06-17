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
        // WalletConnect / web3j artifacts (jitpack) — uncomment if needed
        // maven("https://jitpack.io")
    }
}

rootProject.name = "Livana"
include(":app")
