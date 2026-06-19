import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// --- Client-side identifiers: Clerk *publishable* key + Reown (WalletConnect) *project ID*. ---
// These are PUBLIC client IDs (not secrets). Provide them in local.properties (gitignored):
//     clerk.publishableKey=pk_test_xxxxxxxx
//     walletconnect.projectId=xxxxxxxxxxxx
// ...or paste them as the "" fallbacks below. local.properties takes precedence.
val livanaLocalProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val clerkPublishableKey: String = livanaLocalProperties.getProperty("clerk.publishableKey") ?: ""
val walletConnectProjectId: String = livanaLocalProperties.getProperty("walletconnect.projectId") ?: ""

android {
    namespace = "com.livana.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.livana.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        // Auth / wallet client identifiers — same across environments, so they live in
        // defaultConfig (both debug + release inherit them). Public client IDs, not secrets.
        // Empty until provided via local.properties (or the fallbacks in this file).
        buildConfigField("String", "CLERK_PUBLISHABLE_KEY", "\"$clerkPublishableKey\"")
        buildConfigField("String", "WALLETCONNECT_PROJECT_ID", "\"$walletConnectProjectId\"")
    }

    packaging {
        resources {
            excludes += "META-INF/DISCLAIMER"
        }
    }

    // Environment config (Anvil now, testnet later) lives in build types / flavors.
    // All chain/backend values are injected here — never hardcoded in code.
    buildTypes {
        debug {
            isDebuggable = true
            // Local Anvil + backend (emulator host = 10.0.2.2)
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080\"")
            buildConfigField("String", "RPC_URL", "\"http://10.0.2.2:8545\"")
            buildConfigField("long", "CHAIN_ID", "31337L")
            buildConfigField("String", "IPFS_GATEWAY", "\"https://gateway.pinata.cloud/ipfs/\"")
            buildConfigField("String", "USDC_ADDRESS", "\"0x5FbDB2315678afecb367f032d93F642f64180aa3\"")
            buildConfigField("String", "FACTORY_ADDRESS", "\"0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512\"")
            buildConfigField("String", "SBT_ADDRESS", "\"0xCafac3dD18aC6c6e92c921884f9E4176737C052c\"")
            buildConfigField("String", "EXPLORER_BASE", "\"\"") // none on Anvil
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // TODO: fill in testnet (Fuji) / mainnet values when promoting environments.
            buildConfigField("String", "BASE_URL", "\"https://TBD\"")
            buildConfigField("String", "RPC_URL", "\"https://TBD\"")
            buildConfigField("long", "CHAIN_ID", "43113L")
            buildConfigField("String", "IPFS_GATEWAY", "\"https://gateway.pinata.cloud/ipfs/\"")
            buildConfigField("String", "USDC_ADDRESS", "\"0xTBD\"")
            buildConfigField("String", "FACTORY_ADDRESS", "\"0xTBD\"")
            buildConfigField("String", "SBT_ADDRESS", "\"0xTBD\"")
            buildConfigField("String", "EXPLORER_BASE", "\"https://testnet.snowtrace.io/tx/\"")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

// Force the kotlin-metadata-jvm reader (used by Hilt's annotation processor) up to the Kotlin
// version we compile with, so it can read our 2.4.0 class metadata. See note in dependencies{}.
configurations.configureEach {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-metadata-jvm:2.4.0")
    }
}

dependencies {
    // Hilt's annotation processor reads compiled-class metadata via kotlin-metadata-jvm. The
    // version bundled with Hilt 2.58 supports metadata up to 2.3.0, but our classes compile to
    // 2.4.0 metadata (Kotlin 2.4.0). Force the reader to 2.4.0 so Hilt can process our components
    // without forcing the AGP 9 / Gradle 9 migration that Hilt 2.59+ would require.
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.coil.compose)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.web3j.core)

    // Clerk native Android SDK: API (auth/session) + prebuilt Compose UI (AuthView).
    // clerk-android-ui pulls clerk-android-api transitively; both pinned for clarity.
    implementation(libs.clerk.android.api)
    implementation(libs.clerk.android.ui)

    // Reown AppKit (WalletConnect) for wallet linking. BOM-managed core + appkit, plus the
    // material bottom-sheet navigation host required to display AppKit's connect modal.
    implementation(platform(libs.reown.bom))
    implementation(libs.reown.core)
    implementation(libs.reown.appkit)
    implementation(libs.androidx.compose.material.navigation)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
}
