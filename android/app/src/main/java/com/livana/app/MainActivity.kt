package com.livana.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.livana.app.core.chain.wallet.WalletConnector
import com.livana.app.core.designsystem.theme.LivanaTheme
import com.livana.app.core.navigation.LivanaNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var walletConnector: WalletConnector

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        handleWalletRedirect(intent)
        setContent {
            LivanaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LivanaNavHost()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWalletRedirect(intent)
    }

    /** Forward the wallet's redirect deep link back into the WalletConnect SDK. */
    private fun handleWalletRedirect(intent: Intent?) {
        val data = intent?.data?.toString() ?: return
        if (data.startsWith("livana-wc://")) {
            walletConnector.handleDeepLink(data)
        }
    }
}
