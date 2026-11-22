package dev.neverpaid.health.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val RoyalLightColorScheme = lightColorScheme(
    primary = VioletPrimary,
    onPrimary = InkWhite,
    primaryContainer = SurfaceLilac,
    onPrimaryContainer = TextPrimary,
    secondary = VioletElectric,
    onSecondary = InkWhite,
    secondaryContainer = SurfaceLilac,
    onSecondaryContainer = TextPrimary,
    tertiary = EmeraldSuccess,
    onTertiary = InkWhite,
    error = CrimsonRed,
    onError = InkWhite,
    background = RoyalCanvas,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = RoyalWell,
    onSurfaceVariant = TextSecondary,
    outline = BorderLilacStrong,
    outlineVariant = BorderLilac
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
    DudaRoyalTheme(content = content)
}

@Composable
fun DudaRoyalTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RoyalLightColorScheme,
        typography = AthleticTypography,
        shapes = GymShapes,
        content = content
    )
}
