package dev.neverpaid.health

import androidx.compose.ui.window.ComposeUIViewController
import dev.neverpaid.health.core.designsystem.NeverPaidHealthTheme
import dev.neverpaid.health.core.di.initKoin
import dev.neverpaid.health.navigation.NeverPaidHealthApp

fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin()
    }
) {
    NeverPaidHealthTheme {
        NeverPaidHealthApp()
    }
}
