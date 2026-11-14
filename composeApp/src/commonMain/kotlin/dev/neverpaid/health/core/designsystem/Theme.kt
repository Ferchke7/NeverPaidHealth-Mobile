package dev.neverpaid.health.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldNeon,
    onPrimary = DarkBackground,
    primaryContainer = EmeraldSubtle,
    onPrimaryContainer = EmeraldLight,
    secondary = EmeraldLight,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceHigh,
    onSecondaryContainer = TextPrimary,
    tertiary = AmberWarm,
    onTertiary = DarkBackground,
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

val ObsidianShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun NeverPaidHealthTheme(
    darkTheme: Boolean = true, // Obsidian Architecture dark default
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AthleticTypography,
        shapes = ObsidianShapes,
        content = content
    )
}
