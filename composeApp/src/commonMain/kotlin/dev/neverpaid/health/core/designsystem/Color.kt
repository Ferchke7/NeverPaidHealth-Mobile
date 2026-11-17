package dev.neverpaid.health.core.designsystem

import androidx.compose.ui.graphics.Color

// "Iron & Lime" gym palette: near-black canvas, soft tonal cards (no hairline outlines),
// one electric-lime accent for every primary action, orange reserved for streaks / records.

// Surfaces
val DarkBackground = Color(0xFF0B0C0F)       // App canvas
val GymWell = Color(0xFF101216)              // Recessed wells & inputs inside cards
val DarkSurface = Color(0xFF16181D)          // Cards, sheets, dialogs
val DarkSurfaceVariant = Color(0xFF16181D)   // Card surface alias
val DarkSurfaceHigh = Color(0xFF1F2228)      // Chips, inner tiles, progress tracks
val DarkSurfaceElevated = Color(0xFF282C34)  // Raised neutral controls

// Electric lime accent
val AccentLime = Color(0xFFC8FF3A)           // Primary CTA fill, active icons, highlights
val AccentLimePressed = Color(0xFFA9E01F)    // Pressed state
val AccentLimeDark = Color(0xFF4A5E12)       // Active border / strong container
val AccentLimeSubtle = Color(0xFF1B2310)     // Tinted container behind lime content
val AccentLimeGlow = Color(0x26C8FF3A)       // Translucent chart fills

// Legacy aliases (keep every screen on-palette)
val IndigoElectric = AccentLime
val IndigoDark = AccentLimeDark
val IndigoSubtle = AccentLimeSubtle

// Semantic status & metric tokens
val AmberWarm = Color(0xFFFF9F1C)            // Streaks, records, warm-up
val AmberGold = Color(0xFFFFC53D)
val AmberGoldSoft = Color(0xFFFFD66B)
val AmberDark = Color(0xFFB36B00)
val AmberSubtle = Color(0x26FF9F1C)

val InfoAccent = Color(0xFF4CC9F0)
val InfoAccentLight = Color(0xFF8FDDF6)
val CyanElectric = InfoAccent
val PurpleAccent = AccentLime

val CrimsonRed = Color(0xFFFF4D5E)
val CrimsonDark = Color(0xFF9B1C2A)
val CrimsonSubtle = Color(0x26FF4D5E)

// Typography
val TextPrimary = Color(0xFFF5F6F7)
val TextSoft = Color(0xFFD9DCE1)
val TextSecondary = Color(0xFF9AA0AA)
val TextMuted = Color(0xFF6B7280)

// Borders & dividers (deliberately faint; cards rely on tonal contrast, not outlines)
val BorderSubtle = Color(0xFF262932)
val BorderDivider = Color(0xFF1E2128)
val BorderHighlight = AccentLimeDark
val DarkBorder = BorderSubtle
val AmberWarning = AmberWarm
val BlueAccent = InfoAccent

// Third-party brand & hardware plate visualization colors (centralized in design system)
val GoogleBrandBlue = Color(0xFF4285F4)
val GoogleBrandRed = Color(0xFFEA4335)
val GoogleBrandYellow = Color(0xFFFBBC05)
val GoogleBrandGreen = Color(0xFF34A853)

val BarbellShaftColor = Color(0xFF525862)
val BarbellCollarColor = Color(0xFF737A86)
val BarbellSleeveColor = Color(0xFFA3A9B3)
val BarbellStrokeScrim = Color(0x66000000)
val LightPlateHexColor = 0xFFF8FAFC
val GymInkOnLight = Color(0xFF0B0C0F)        // Dark ink used on lime / white fills
