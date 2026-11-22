package dev.neverpaid.health.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// "Royal Violet & Clean White / Obsidian Dark" Design System Tokens
@Immutable
data class DudaColorPalette(
    val royalCanvas: Color,
    val royalWell: Color,
    val surfaceWhite: Color,
    val surfaceLilac: Color,
    val surfaceLilacPressed: Color,
    val borderLilac: Color,
    val borderLilacStrong: Color,
    val textPrimary: Color,
    val textSoft: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val emeraldBg: Color,
    val emeraldBorder: Color,
    val amberSubtle: Color,
    val infoAccentLight: Color,
    val crimsonSubtle: Color,
    val chartNavyBar: Color,
    val violetInk: Color
)

val LightDudaPalette = DudaColorPalette(
    royalCanvas = Color(0xFFF4F3FA),
    royalWell = Color(0xFFF8F7FC),
    surfaceWhite = Color(0xFFFFFFFF),
    surfaceLilac = Color(0xFFF0EFFE),
    surfaceLilacPressed = Color(0xFFE5E1FC),
    borderLilac = Color(0xFFECEAF6),
    borderLilacStrong = Color(0xFFDDD6FE),
    textPrimary = Color(0xFF1E1B4B),
    textSoft = Color(0xFF374151),
    textSecondary = Color(0xFF6B7280),
    textMuted = Color(0xFF9CA3AF),
    emeraldBg = Color(0xFFECFDF5),
    emeraldBorder = Color(0xFFA7F3D0),
    amberSubtle = Color(0xFFFEF3C7),
    infoAccentLight = Color(0xFFECFEFF),
    crimsonSubtle = Color(0xFFFEF2F2),
    chartNavyBar = Color(0xFF1B1145),
    violetInk = Color(0xFF6D28D9)
)

val DarkDudaPalette = DudaColorPalette(
    royalCanvas = Color(0xFF090812),
    royalWell = Color(0xFF1B1830),
    surfaceWhite = Color(0xFF131122),
    surfaceLilac = Color(0xFF241D44),
    surfaceLilacPressed = Color(0xFF2E2457),
    borderLilac = Color(0xFF2A2447),
    borderLilacStrong = Color(0xFF43367A),
    textPrimary = Color(0xFFF5F3FF),
    textSoft = Color(0xFFD6D0F0),
    textSecondary = Color(0xFFA59EC8),
    textMuted = Color(0xFF7B73A1),
    emeraldBg = Color(0xFF0F2D24),
    emeraldBorder = Color(0xFF1D5A47),
    amberSubtle = Color(0xFF2D2212),
    infoAccentLight = Color(0xFF0E2C36),
    crimsonSubtle = Color(0xFF33151B),
    chartNavyBar = Color(0xFFA78BFA),
    violetInk = Color(0xFFA78BFA)
)

val LocalDudaColors = staticCompositionLocalOf { LightDudaPalette }

// Dynamic Light/Dark Canvas, Surface, Border & Typography Inks
val RoyalCanvas: Color @Composable @ReadOnlyComposable get() = LocalDudaColors.current.royalCanvas
val RoyalWell: Color @Composable @ReadOnlyComposable get() = LocalDudaColors.current.royalWell
val SurfaceWhite: Color @Composable @ReadOnlyComposable get() = LocalDudaColors.current.surfaceWhite
val SurfaceLilac: Color @Composable @ReadOnlyComposable get() = LocalDudaColors.current.surfaceLilac
val SurfaceLilacPressed: Color @Composable @ReadOnlyComposable get() = LocalDudaColors.current.surfaceLilacPressed
val BorderLilac: Color @Composable @ReadOnlyComposable get() = LocalDudaColors.current.borderLilac
val BorderLilacStrong: Color @Composable @ReadOnlyComposable get() = LocalDudaColors.current.borderLilacStrong

// Royal Violet Core Accents (Shared Brand Identity)
val VioletDeep = Color(0xFF1B0552)            // Deep hero start
val VioletRoyal = Color(0xFF3B0CA3)           // Deep hero mid
val VioletPrimary = Color(0xFF6D28D9)         // Primary brand accent & CTA end
val VioletElectric = Color(0xFF8B5CF6)        // Electric gradient CTA start
val VioletSoft = Color(0xFFA78BFA)            // Secondary soft violet
val VioletGlow = Color(0x266D28D9)            // Translucent chart fill
val HeroMuted = Color(0xFFDDD6FE)             // Muted subtitle on dark violet hero cards
val InkWhite = Color(0xFFFFFFFF)              // Crisp white ink on violet/indigo/teal/emerald
val RoyalVioletToolbarHex = "#3B0CA3"

// Hero & KPI Gradients
val RoyalHeroBrush: Brush = Brush.linearGradient(
    colors = listOf(VioletDeep, VioletRoyal, VioletPrimary)
)
val VioletButtonBrush: Brush = Brush.linearGradient(
    colors = listOf(VioletElectric, VioletPrimary)
)
val IndigoCardBrush: Brush = Brush.linearGradient(
    colors = listOf(Color(0xFF1E1B4B), Color(0xFF312E81))
)
val TealCardBrush: Brush = Brush.linearGradient(
    colors = listOf(Color(0xFF0E7490), Color(0xFF0891B2))
)

// Semantic Status & Metric Tokens
val EmeraldSuccess = Color(0xFF10B981)        // Receipt checkmark, PRs, completed sets
val EmeraldDark = Color(0xFF059669)
val EmeraldBg: Color @Composable @ReadOnlyComposable get() = LocalDudaColors.current.emeraldBg
val EmeraldBorder: Color @Composable @ReadOnlyComposable get() = LocalDudaColors.current.emeraldBorder

val AmberWarm = Color(0xFFF59E0B)             // Streaks, warm-ups
val AmberGold = Color(0xFFFBBF24)
val AmberGoldSoft = Color(0xFFFDE68A)
val AmberDark = Color(0xFFD97706)
val AmberSubtle: Color @Composable @ReadOnlyComposable get() = LocalDudaColors.current.amberSubtle

val InfoAccent = Color(0xFF0891B2)            // Cyan/teal hydration & volume accents
val InfoAccentLight: Color @Composable @ReadOnlyComposable get() = LocalDudaColors.current.infoAccentLight
val CyanElectric = InfoAccent
val PurpleAccent = VioletPrimary

val CrimsonRed = Color(0xFFEF4444)            // Errors, discard, fatigue alert
val CrimsonDark = Color(0xFFDC2626)
val CrimsonSubtle: Color @Composable @ReadOnlyComposable get() = LocalDudaColors.current.crimsonSubtle

// Chart Dual-Bar Palette
val ChartNavyBar: Color @Composable @ReadOnlyComposable get() = LocalDudaColors.current.chartNavyBar
val ChartVioletBar = Color(0xFF7C3AED)

// Typography Inks (Theme-Aware High Contrast)
val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalDudaColors.current.textPrimary
val TextSoft: Color @Composable @ReadOnlyComposable get() = LocalDudaColors.current.textSoft
val TextSecondary: Color @Composable @ReadOnlyComposable get() = LocalDudaColors.current.textSecondary
val TextMuted: Color @Composable @ReadOnlyComposable get() = LocalDudaColors.current.textMuted
val VioletInk: Color @Composable @ReadOnlyComposable get() = LocalDudaColors.current.violetInk

// Semantic Compatibility Aliases
val DarkBackground: Color @Composable @ReadOnlyComposable get() = RoyalCanvas
val GymWell: Color @Composable @ReadOnlyComposable get() = RoyalWell
val DarkSurface: Color @Composable @ReadOnlyComposable get() = SurfaceWhite
val DarkSurfaceVariant: Color @Composable @ReadOnlyComposable get() = SurfaceWhite
val DarkSurfaceHigh: Color @Composable @ReadOnlyComposable get() = SurfaceLilac
val DarkSurfaceElevated: Color @Composable @ReadOnlyComposable get() = SurfaceLilacPressed
val AccentLime = VioletPrimary
val AccentLimePressed = VioletRoyal
val AccentLimeDark: Color @Composable @ReadOnlyComposable get() = BorderLilacStrong
val AccentLimeSubtle: Color @Composable @ReadOnlyComposable get() = SurfaceLilac
val AccentLimeGlow = VioletGlow
val IndigoElectric = VioletElectric
val IndigoDark: Color @Composable @ReadOnlyComposable get() = BorderLilacStrong
val IndigoSubtle: Color @Composable @ReadOnlyComposable get() = SurfaceLilac
val BorderSubtle: Color @Composable @ReadOnlyComposable get() = BorderLilac
val BorderDivider: Color @Composable @ReadOnlyComposable get() = BorderLilac
val BorderHighlight: Color @Composable @ReadOnlyComposable get() = BorderLilacStrong
val DarkBorder: Color @Composable @ReadOnlyComposable get() = BorderLilac
val AmberWarning = AmberWarm
val BlueAccent = InfoAccent
val GymInkOnLight = InkWhite

// Third-party brand & hardware plate visualization colors
val GoogleBrandBlue = Color(0xFF4285F4)
val GoogleBrandRed = Color(0xFFEA4335)
val GoogleBrandYellow = Color(0xFFFBBC05)
val GoogleBrandGreen = Color(0xFF34A853)

val BarbellShaftColor = Color(0xFF64748B)
val BarbellCollarColor = Color(0xFF475569)
val BarbellSleeveColor = Color(0xFF94A3B8)
val BarbellStrokeScrim = Color(0x400F172A)
val LightPlateHexColor = 0xFFF8FAFC
