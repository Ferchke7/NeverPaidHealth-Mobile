package dev.neverpaid.health.core.designsystem

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// "Royal Violet & Clean White" E-Wallet Gym UI Kit
// Canvas #F4F3FA, Elevated White #FFFFFF, Royal Violet Hero #1B0552 -> #3B0CA3 -> #6D28D9,
// Soft Lilac Borders #ECEAF6 / #DDD6FE, Emerald Success #10B981, Crisp Ink #1E1B4B.

// Light Canvas & Surface Palette
val RoyalCanvas = Color(0xFFF4F3FA)           // App canvas
val RoyalWell = Color(0xFFF8F7FC)             // Recessed wells & inputs inside cards
val SurfaceWhite = Color(0xFFFFFFFF)          // Elevated cards, docks, sheets, dialogs
val SurfaceLilac = Color(0xFFF0EFFE)          // Soft lilac pills & icon tile backgrounds
val SurfaceLilacPressed = Color(0xFFE5E1FC)   // Raised / pressed lilac surface
val BorderLilac = Color(0xFFECEAF6)           // Subtle hairline card border
val BorderLilacStrong = Color(0xFFDDD6FE)     // Active highlight border

// Royal Violet Core Accents
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
val EmeraldBg = Color(0xFFECFDF5)
val EmeraldBorder = Color(0xFFA7F3D0)

val AmberWarm = Color(0xFFF59E0B)             // Streaks, warm-ups
val AmberGold = Color(0xFFFBBF24)
val AmberGoldSoft = Color(0xFFFDE68A)
val AmberDark = Color(0xFFD97706)
val AmberSubtle = Color(0xFFFEF3C7)

val InfoAccent = Color(0xFF0891B2)            // Cyan/teal hydration & volume accents
val InfoAccentLight = Color(0xFFECFEFF)
val CyanElectric = InfoAccent
val PurpleAccent = VioletPrimary

val CrimsonRed = Color(0xFFEF4444)            // Errors, discard, fatigue alert
val CrimsonDark = Color(0xFFDC2626)
val CrimsonSubtle = Color(0xFFFEF2F2)

// Chart Dual-Bar Palette (E-Wallet Statistic screen)
val ChartNavyBar = Color(0xFF1B1145)
val ChartVioletBar = Color(0xFF7C3AED)

// Typography Inks (Light Theme High Contrast)
val TextPrimary = Color(0xFF1E1B4B)           // Deep Royal Navy Ink
val TextSoft = Color(0xFF374151)              // Body copy
val TextSecondary = Color(0xFF6B7280)         // Muted labels
val TextMuted = Color(0xFF9CA3AF)             // Hint / disabled ink

// Semantic Compatibility Aliases (so existing feature call-sites flip cleanly to Royal Violet Light)
val DarkBackground = RoyalCanvas
val GymWell = RoyalWell
val DarkSurface = SurfaceWhite
val DarkSurfaceVariant = SurfaceWhite
val DarkSurfaceHigh = SurfaceLilac
val DarkSurfaceElevated = SurfaceLilacPressed
val AccentLime = VioletPrimary
val AccentLimePressed = VioletRoyal
val AccentLimeDark = BorderLilacStrong
val AccentLimeSubtle = SurfaceLilac
val AccentLimeGlow = VioletGlow
val IndigoElectric = VioletElectric
val IndigoDark = BorderLilacStrong
val IndigoSubtle = SurfaceLilac
val BorderSubtle = BorderLilac
val BorderDivider = BorderLilac
val BorderHighlight = BorderLilacStrong
val DarkBorder = BorderLilac
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
