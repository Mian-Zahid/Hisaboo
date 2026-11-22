package com.mianzahid.expenseentry.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.mianzahid.expenseentry.R

// Jameel Noori Nastaliq Font Family for Urdu Text
val JameelNooriNastaliq = FontFamily(
    Font(R.font.jameel_noori_nastaliq, FontWeight.Normal),
    Font(R.font.jameel_noori_nastaliq, FontWeight.Medium),
    Font(R.font.jameel_noori_nastaliq, FontWeight.SemiBold),
    Font(R.font.jameel_noori_nastaliq, FontWeight.Bold)
)

// Global Theme State (Default: Light Mode)
var isHisabooDarkTheme by mutableStateOf(false)

// Dynamic Theme-Aware Palette (Switches seamlessly between Light & High-Contrast Dark)
val BgObsidian: Color
    get() = if (isHisabooDarkTheme) Color(0xFF0B1114) else Color(0xFFF3F7F5)

val SurfaceCard: Color
    get() = if (isHisabooDarkTheme) Color(0xFF141E22) else Color(0xFFFFFFFF)

val BorderGlass: Color
    get() = if (isHisabooDarkTheme) Color(0xFF283A42) else Color(0xFFD5E2DC)

val SurfaceGlass: Color
    get() = if (isHisabooDarkTheme) Color(0xFF1B282E) else Color(0xFFE9F1ED)

val EmeraldPrimary: Color
    get() = if (isHisabooDarkTheme) Color(0xFF10B981) else Color(0xFF059669)

val OnEmeraldPrimary: Color
    get() = Color(0xFFFFFFFF)

val EmeraldGlowTint: Color
    get() = if (isHisabooDarkTheme) Color(0x2E10B981) else Color(0x1F059669)

val CashInGreen: Color
    get() = if (isHisabooDarkTheme) Color(0xFF34D399) else Color(0xFF059669)

val CashInTint: Color
    get() = if (isHisabooDarkTheme) Color(0x2434D399) else Color(0x1A059669)

val CashOutRed: Color
    get() = if (isHisabooDarkTheme) Color(0xFFF87171) else Color(0xFFDC2626)

val CashOutTint: Color
    get() = if (isHisabooDarkTheme) Color(0x26F87171) else Color(0x1ADC2626)

val AmberAccent: Color
    get() = if (isHisabooDarkTheme) Color(0xFFFBBF24) else Color(0xFFD97706)

val AmberTint: Color
    get() = if (isHisabooDarkTheme) Color(0x24FBBF24) else Color(0x1AD97706)

val BlueAccent: Color
    get() = if (isHisabooDarkTheme) Color(0xFF38BDF8) else Color(0xFF0284C7)

val BlueTint: Color
    get() = if (isHisabooDarkTheme) Color(0x2438BDF8) else Color(0x1A0284C7)

val TextPrimary: Color
    get() = if (isHisabooDarkTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A)

val TextSecondary: Color
    get() = if (isHisabooDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)

val TextMuted: Color
    get() = if (isHisabooDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

// Hero Wallet Card Mesh Gradient
val HeroWalletGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF065F46),
        Color(0xFF047857),
        Color(0xFF0D9488)
    )
)

val HeroCardBorder = Color(0x29FFFFFF)

private fun buildDarkColorScheme() = darkColorScheme(
    primary = Color(0xFF10B981),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF1B282E),
    onPrimaryContainer = Color(0xFF34D399),
    secondary = Color(0xFF34D399),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFF0B1114),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF141E22),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1B282E),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF283A42),
    error = Color(0xFFF87171)
)

private fun buildLightColorScheme() = lightColorScheme(
    primary = Color(0xFF059669),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE9F1ED),
    onPrimaryContainer = Color(0xFF059669),
    secondary = Color(0xFF059669),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF3F7F5),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE9F1ED),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFD5E2DC),
    error = Color(0xFFDC2626)
)

@Composable
private fun buildHisabooTypography() = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 38.sp,
        lineHeight = 44.sp,
        color = TextPrimary,
        fontFeatureSettings = "tnum"
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        color = TextPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        color = TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = TextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = TextSecondary
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        color = TextPrimary
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = TextSecondary
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = TextMuted
    )
)

val HisabooShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun HisabooTheme(
    darkTheme: Boolean = isHisabooDarkTheme,
    content: @Composable () -> Unit
) {
    val bgColor = if (darkTheme) Color(0xFF0B1114) else Color(0xFFF3F7F5)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = bgColor.toArgb()
            window.navigationBarColor = bgColor.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = if (darkTheme) buildDarkColorScheme() else buildLightColorScheme(),
        typography = buildHisabooTypography(),
        shapes = HisabooShapes,
        content = content
    )
}
