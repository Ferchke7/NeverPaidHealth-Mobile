package dev.neverpaid.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.neverpaid.health.core.designsystem.NeverPaidHealthTheme
import dev.neverpaid.health.navigation.NeverPaidHealthApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NeverPaidHealthTheme {
                NeverPaidHealthApp()
            }
        }
    }
}
