package com.livana.app

import android.app.Application
import com.clerk.api.Clerk
import com.livana.app.core.chain.wallet.ReownAppKitInitializer
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LivanaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Clerk init is non-blocking. The publishable key is a public client ID supplied via
        // local.properties; it is empty until the user provides it. Guard against a blank key so
        // a fresh checkout doesn't crash — auth simply stays signed-out until a key is present.
        val publishableKey = BuildConfig.CLERK_PUBLISHABLE_KEY
        if (publishableKey.isNotBlank()) {
            Clerk.initialize(this, publishableKey = publishableKey)
        }

        // Reown AppKit (WalletConnect). Self-guards on a blank Project ID, so this is a no-op until
        // a Project ID is provided via local.properties.
        ReownAppKitInitializer.initialize(this)
    }
}
