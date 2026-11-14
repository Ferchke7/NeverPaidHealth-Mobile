package dev.neverpaid.health.core.designsystem

import androidx.compose.ui.graphics.Color

// Obsidian Architecture Dark Palette (1:1 match with duda.uz Figma Obsidian Design System)
val DarkBackground = Color(0xFF0A0A0A)       // #0a0a0a Canvas background & recessed inputs
val ObsidianWell = Color(0xFF0A0A0A)         // #0a0a0a Inner wells & inputs inside cards
val DarkSurface = Color(0xFF171717)          // #171717 Elevated cards, sheets, dialogs
val DarkSurfaceVariant = Color(0xFF171717)   // #171717 Card surface
val DarkSurfaceHigh = Color(0xFF262626)      // #262626 Tonal hover/wells & progress tracks
val DarkSurfaceElevated = Color(0xFF262626)  // #262626 Elevated neutral controls

// Obsidian Emerald Accent (#34d399 Emerald-400 / #022c22 Emerald-950 / #064e3b Emerald-900)
val EmeraldNeon = Color(0xFF34D399)          // Primary CTA fill, active highlights & lines
val EmeraldLight = Color(0xFF34D399)         // Active nav / chip foreground
val EmeraldPressed = Color(0xFF10B981)       // Emerald-500 pressed state
val EmeraldDark = Color(0xFF064E3B)          // #064e3b Active Emerald-900 border/container
val EmeraldSubtle = Color(0xFF022C22)        // #022c22 Solid Emerald-950 active container
val EmeraldGlow = Color(0x2634D399)          // Subtle translucent chart curve fill

// Legacy aliases mapped cleanly onto Obsidian Emerald / Neutral (prevents off-palette leaks)
val IndigoElectric = EmeraldLight
val IndigoDark = EmeraldDark
val IndigoSubtle = EmeraldSubtle

// Semantic Status & Metric Tokens
val AmberWarm = Color(0xFFF59E0B)
val AmberGold = Color(0xFFFBBF24)
val AmberGoldSoft = Color(0xFFFFD54F)
val AmberDark = Color(0xFFB45309)
val AmberSubtle = Color(0x26F59E0B)

val InfoAccent = Color(0xFF38BDF8)
val InfoAccentLight = Color(0xFF90CAF9)
val CyanElectric = InfoAccent
val PurpleAccent = EmeraldLight

val CrimsonRed = Color(0xFFEF4444)
val CrimsonDark = Color(0xFF991B1B)
val CrimsonSubtle = Color(0x26EF4444)

// Obsidian High-Contrast Neutral Typography
val TextPrimary = Color(0xFFFAFAFA)          // #fafafa Neutral-50
val TextSecondary = Color(0xFFA3A3A3)        // #a3a3a3 Neutral-400
val TextMuted = Color(0xFF737373)            // #737373 Neutral-500

// Crisp 1dp Obsidian Hairline Borders & Dividers
val BorderSubtle = Color(0xFF404040)         // #404040 Card & input 1dp border
val BorderDivider = Color(0xFF262626)        // #262626 Internal card dividers & bar hairlines
val BorderHighlight = Color(0xFF064E3B)      // #064e3b Active emerald card & chip border
val DarkBorder = BorderSubtle
val AmberWarning = AmberWarm
val BlueAccent = InfoAccent

// Third-party Brand & Hardware Plate Visualization Colors (centralized in design system)
val GoogleBrandBlue = Color(0xFF4285F4)
val GoogleBrandRed = Color(0xFFEA4335)
val GoogleBrandYellow = Color(0xFFFBBC05)
val GoogleBrandGreen = Color(0xFF34A853)

val BarbellShaftColor = Color(0xFF525252)
val BarbellCollarColor = Color(0xFF737373)
val BarbellSleeveColor = Color(0xFFA3A3A3)
val BarbellStrokeScrim = Color(0x66000000)
val LightPlateHexColor = 0xFFF8FAFC
val ObsidianInkOnLight = Color(0xFF0A0A0A)
