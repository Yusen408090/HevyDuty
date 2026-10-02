package com.example.myapplication.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Lime = Color(0xFFC5F36B)
val Ink = Color(0xFF101211)
val Panel = Color(0xFF1B1E1C)
val Muted = Color(0xFF9BABA0)

private val HeavyColors = darkColorScheme(
    primary = Lime, onPrimary = Ink, primaryContainer = Color(0xFF2B3B1E),
    onPrimaryContainer = Lime, secondary = Color(0xFF8ACCB1),
    secondaryContainer = Color(0xFF2B3B1E), onSecondaryContainer = Lime,
    background = Ink, onBackground = Color(0xFFF1F4EF),
    surface = Ink, onSurface = Color(0xFFF1F4EF),
    surfaceVariant = Panel, onSurfaceVariant = Muted,
    surfaceContainer = Panel, surfaceContainerHigh = Color(0xFF242925),
    outline = Color(0xFF3C4740), outlineVariant = Color(0xFF343D36),
    inverseSurface = Color(0xFF303A31), inverseOnSurface = Color(0xFFF1F4EF),
    error = Color(0xFFFFADA4)
)

@Composable
fun HeavyDutyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HeavyColors,
        typography = Typography(
            headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = (-1).sp),
            headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = (-0.6).sp),
            titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 21.sp),
            titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
            labelSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)
        ),
        content = content
    )
}
