package com.shejan.financebuddy.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// === Global Theme Switcher State ===
var currentThemeModeState by mutableStateOf("DARK")
var isDarkModeGlobal: Boolean
    get() = currentThemeModeState != "LIGHT"
    set(value) {
        currentThemeModeState = if (value) "DARK" else "LIGHT"
    }

// === Dynamic Background & Surface (Deep Charcoal/Black & Surface Gray Palette) ===
val BackgroundDark: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xFF000000) // Pure OLED Pitch Black
    "LIGHT"  -> Color(0xFFF1F5F9)
    else     -> Color(0xFF0A0D14) // Deep Charcoal / Dark Canvas
}

val SurfaceDark: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xFF080808) // Refined AMOLED Surface for Drawers & Sheets
    "LIGHT"  -> Color(0xFFFFFFFF)
    else     -> Color(0xFF121620)
}

val CardDark: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xFF121212) // Material Elevation Level 1 for AMOLED Cards
    "LIGHT"  -> Color(0xFFFFFFFF)
    else     -> Color(0xFF171B26)
}

val CardDarker: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xFF0A0A0A) // Recessed Card / Input Container for AMOLED
    "LIGHT"  -> Color(0xFFE2E8F0)
    else     -> Color(0xFF0E111A)
}

// === Accent Colors ===
val AccentTeal: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xFF00E5B3)
    "LIGHT"  -> Color(0xFF0D9488)
    else     -> Color(0xFF00D4AA)
}

val AccentBlue: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xFF389BFF)
    "LIGHT"  -> Color(0xFF2563EB)
    else     -> Color(0xFF0096FF)
}

val AccentPurple: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xFF906BFF)
    "LIGHT"  -> Color(0xFF7C3AED)
    else     -> Color(0xFF7C5CFC)
}

// === Dynamic Text ===
val TextPrimary: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xFFFFFFFF) // 100% High Contrast
    "LIGHT"  -> Color(0xFF0F172A)
    else     -> Color(0xFFF3F5F9) // Crisp High Contrast Off-White
}

val TextSecondary: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xD9FFFFFF) // 85%
    "LIGHT"  -> Color(0xFF475569)
    else     -> Color(0xFF949EB8) // Legible Secondary Text
}

val TextMuted: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0x99FFFFFF) // 60%
    "LIGHT"  -> Color(0xFF64748B)
    else     -> Color(0xFF767F9D) // Improved Contrast Ratio (> 4.5:1 WCAG AA)
}

// === Semantic (Income, Expense, Transfer) ===
val IncomeGreen: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xFF00E5B3) // Preserved Semantic Green for AMOLED
    "LIGHT"  -> Color(0xFF059669)
    else     -> Color(0xFF00C897)
}

val ExpenseRed: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xFFFF5272) // Preserved Semantic Red for AMOLED
    "LIGHT"  -> Color(0xFFE11D48)
    else     -> Color(0xFFFF5C7C)
}

val TransferYellow: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xFFFFC033) // Preserved Semantic Yellow for AMOLED
    "LIGHT"  -> Color(0xFFD97706)
    else     -> Color(0xFFFFBD2E)
}

// === Gradient Stops ===
val GradientStart: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xFF00E5B3)
    "LIGHT"  -> Color(0xFF0D9488)
    else     -> Color(0xFF00D4AA)
}

val GradientEnd: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xFF389BFF)
    "LIGHT"  -> Color(0xFF2563EB)
    else     -> Color(0xFF0096FF)
}

// === Dynamic Divider / Border ===
val DividerColor: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0x26FFFFFF) // Thin crisp light white border (15% white)
    "LIGHT"  -> Color(0xFFCBD5E1)
    else     -> Color(0x26FFFFFF) // Thin crisp light white border for Dark Mode (15% white)
}

// === On-Accent ===
val OnAccent: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xFF000000)
    "LIGHT"  -> Color(0xFFFFFFFF)
    else     -> Color(0xFF0A0D14)
}

// === Chart Tokens ===
val ChartGridLine: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0x1AFFFFFF) // 10%
    "LIGHT"  -> Color(0x2694A3B8)
    else     -> Color(0x1AFFFFFF)
}

val ChartLabel: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0x80FFFFFF) // 50%
    "LIGHT"  -> Color(0xFF64748B)
    else     -> Color(0x80FFFFFF)
}

val ChartSurface: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xFF000000)
    "LIGHT"  -> Color(0x0F0F172A)
    else     -> Color(0x0FFFFFFF)
}

// === Scrim (bottom sheet / dialog overlay) ===
val ScrimColor: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xCC000000)
    "LIGHT"  -> Color(0x59000000)
    else     -> Color(0xA6000000)
}

// === Toggle / Switch Colors ===
val SwitchTrackUnchecked: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0x33FFFFFF)
    "LIGHT"  -> Color(0xFFCBD5E1)
    else     -> Color(0xFF22283A)
}

val SwitchThumbUnchecked: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0x80FFFFFF)
    "LIGHT"  -> Color(0xFF64748B)
    else     -> Color(0xFF8C96B0)
}

val SwitchBorderUnchecked: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0x40FFFFFF)
    "LIGHT"  -> Color(0xFF94A3B8)
    else     -> Color(0xFF2F3750)
}