package dev.neverpaid.health.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = AccentLime,
    onPrimary = GymInkOnLight,
    primaryContainer = AccentLimeSubtle,
    onPrimaryContainer = AccentLime,
    secondary = AccentLime,
    onSecondary = GymInkOnLight,
    secondaryContainer = DarkSurfaceHigh,
    onSecondaryContainer = TextPrimary,
    tertiary = AmberWarm,
    onTertiary = GymInkOnLight,
    error = CrimsonRed,
    onError = TextPrimary,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = BorderDivider
)

val GymShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun NeverPaidHealthTheme(
    darkTheme: Boolean = true, // Gym UI is dark-only
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AthleticTypography,
        shapes = GymShapes,
        content = content
    )
}
