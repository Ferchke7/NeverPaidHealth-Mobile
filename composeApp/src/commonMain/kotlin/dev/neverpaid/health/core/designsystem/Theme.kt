package dev.neverpaid.health.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldNeon,
    onPrimary = DarkBackground,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = TextPrimary,
    secondary = IndigoElectric,
    onSecondary = DarkBackground,
    secondaryContainer = IndigoDark,
    onSecondaryContainer = TextPrimary,
    tertiary = AmberWarm,
    onTertiary = DarkBackground,
    error = CrimsonRed,
    onError = DarkBackground,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle
)

@Composable
fun NeverPaidHealthTheme(
    darkTheme: Boolean = true, // Athletic OLED default
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AthleticTypography,
        content = content
    )
}
