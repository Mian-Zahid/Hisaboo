package com.mianzahid.expenseentry.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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

// Core Dark Obsidian & Emerald FinTech Palette
val BgObsidian = Color(0xFF090E0C)
val SurfaceCard = Color(0xFF111916)
val BorderGlass = Color(0xFF243630)
val SurfaceGlass = Color(0xFF16221E)

val EmeraldPrimary = Color(0xFF10B981)
val OnEmeraldPrimary = Color(0xFF03261C)
val EmeraldGlowTint = Color(0x3310B981)

val CashInGreen = Color(0xFF34D399)
val CashInTint = Color(0x2434D399)
val CashOutRed = Color(0xFFF87171)
val CashOutTint = Color(0x26F87171)
val AmberAccent = Color(0xFFFBBF24)
val AmberTint = Color(0x24FBBF24)
val BlueAccent = Color(0xFF38BDF8)
val BlueTint = Color(0x2438BDF8)

val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// Hero Wallet Card Mesh Gradient
val HeroWalletGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF064E3B),
        Color(0xFF047857),
        Color(0xFF0D9488)
    )
)

val HeroCardBorder = Color(0x29FFFFFF) // 16% white alpha

private val HisabooDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = OnEmeraldPrimary,
    primaryContainer = SurfaceGlass,
    onPrimaryContainer = CashInGreen,
    secondary = CashInGreen,
    onSecondary = OnEmeraldPrimary,
    secondaryContainer = SurfaceGlass,
    onSecondaryContainer = TextPrimary,
    tertiary = AmberAccent,
    onTertiary = OnEmeraldPrimary,
    background = BgObsidian,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceGlass,
    onSurfaceVariant = TextSecondary,
    outline = BorderGlass,
    outlineVariant = Color(0xFF1C2B26),
    error = CashOutRed,
    onError = Color(0xFF2B0808)
)

val HisabooTypography = Typography(
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
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BgObsidian.toArgb()
            window.navigationBarColor = BgObsidian.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = HisabooDarkColorScheme,
        typography = HisabooTypography,
        shapes = HisabooShapes,
        content = content
    )
}
