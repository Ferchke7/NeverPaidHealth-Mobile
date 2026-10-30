package dev.neverpaid.health

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import dev.neverpaid.health.core.designsystem.NeverPaidHealthTheme
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import dev.neverpaid.health.navigation.NeverPaidHealthApp
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val repository: NeverPaidRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleAuthIntent(intent)

        setContent {
            NeverPaidHealthTheme {
                NeverPaidHealthApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthIntent(intent)
    }

    private fun handleAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme.equals("duda", ignoreCase = true)) {
            val token = uri.getQueryParameter("token") ?: uri.getQueryParameter("access_token")
            val idToken = uri.getQueryParameter("id_token")
            if (!token.isNullOrBlank()) {
                lifecycleScope.launch {
                    runCatching { repository.authenticateWithAccessToken(token) }
                }
            } else if (!idToken.isNullOrBlank()) {
                lifecycleScope.launch {
                    runCatching { repository.authenticateWithGoogleIdToken(idToken) }
                }
            }
        }
    }
}
