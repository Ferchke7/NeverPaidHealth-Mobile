package dev.neverpaid.health.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

private val RoyalLightColorScheme = lightColorScheme(
    primary = VioletPrimary,
    onPrimary = InkWhite,
    primaryContainer = LightDudaPalette.surfaceLilac,
    onPrimaryContainer = LightDudaPalette.textPrimary,
    secondary = VioletElectric,
    onSecondary = InkWhite,
    secondaryContainer = LightDudaPalette.surfaceLilac,
    onSecondaryContainer = LightDudaPalette.textPrimary,
    tertiary = EmeraldSuccess,
    onTertiary = InkWhite,
    error = CrimsonRed,
    onError = InkWhite,
    background = LightDudaPalette.royalCanvas,
    onBackground = LightDudaPalette.textPrimary,
    surface = LightDudaPalette.surfaceWhite,
    onSurface = LightDudaPalette.textPrimary,
    surfaceVariant = LightDudaPalette.royalWell,
    onSurfaceVariant = LightDudaPalette.textSecondary,
    outline = LightDudaPalette.borderLilacStrong,
    outlineVariant = LightDudaPalette.borderLilac
)

private val RoyalDarkColorScheme = darkColorScheme(
    primary = VioletElectric,
    onPrimary = InkWhite,
    primaryContainer = DarkDudaPalette.surfaceLilac,
    onPrimaryContainer = DarkDudaPalette.textPrimary,
    secondary = VioletSoft,
    onSecondary = InkWhite,
    secondaryContainer = DarkDudaPalette.surfaceLilac,
    onSecondaryContainer = DarkDudaPalette.textPrimary,
    tertiary = EmeraldSuccess,
    onTertiary = InkWhite,
    error = CrimsonRed,
    onError = InkWhite,
    background = DarkDudaPalette.royalCanvas,
    onBackground = DarkDudaPalette.textPrimary,
    surface = DarkDudaPalette.surfaceWhite,
    onSurface = DarkDudaPalette.textPrimary,
    surfaceVariant = DarkDudaPalette.royalWell,
    onSurfaceVariant = DarkDudaPalette.textSecondary,
    outline = DarkDudaPalette.borderLilacStrong,
    outlineVariant = DarkDudaPalette.borderLilac
)

val GymShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun NeverPaidHealthTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    DudaRoyalTheme(darkTheme = darkTheme, content = content)
}

@Composable
fun DudaRoyalTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkDudaPalette else LightDudaPalette
    CompositionLocalProvider(LocalDudaColors provides palette) {
        MaterialTheme(
            colorScheme = if (darkTheme) RoyalDarkColorScheme else RoyalLightColorScheme,
            typography = AthleticTypography,
            shapes = GymShapes,
            content = content
        )
    }
}
