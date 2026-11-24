package dev.neverpaid.health

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import dev.neverpaid.health.core.designsystem.NeverPaidHealthTheme
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import dev.neverpaid.health.navigation.NeverPaidHealthApp
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val repository: NeverPaidRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        setContent {
            NeverPaidHealthTheme {
                NeverPaidHealthApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!repository.isGuestUser()) {
            lifecycleScope.launch {
                runCatching { repository.syncWithServer() }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.getStringExtra(EXTRA_NAV_TARGET) == NAV_TARGET_ACTIVE_WORKOUT) {
            PlatformBridge.markOpenActiveWorkoutPending()
        }
        val uri = intent?.data ?: return
        if (uri.scheme.equals("duda", ignoreCase = true)) {
            if (uri.host.equals("workout", ignoreCase = true)) {
                PlatformBridge.markOpenActiveWorkoutPending()
                return
            }
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

    companion object {
        const val EXTRA_NAV_TARGET = "extra_nav_target"
        const val NAV_TARGET_ACTIVE_WORKOUT = "active_workout"
    }
}
