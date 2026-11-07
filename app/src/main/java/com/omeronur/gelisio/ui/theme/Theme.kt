package com.omeronur.gelisio.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omeronur.gelisio.R

/** Gelisio tasarım tokenları (spec: Arayüz tablosu). */
object G {
    val bgTop = Color(0xFF1A1F2E)
    val bgBottom = Color(0xFF07090F)
    val mint = Color(0xFF7CF5D2)
    val lavender = Color(0xFF8B7CFF)
    val warning = Color(0xFFFFC56B)
    val critical = Color(0xFFFF6B81)
    val text = Color(0xFFF2F4F8)
    val textSecondary = Color(0x99F2F4F8)
    val textTertiary = Color(0x59F2F4F8)
    val glass = Color(0x0FFFFFFF) // beyaz %6
    val glassStrong = Color(0x1AFFFFFF)
    val stroke = Color(0x1AFFFFFF) // beyaz %10
    val strokeHighlight = Color(0x40FFFFFF)
    val accent = Brush.linearGradient(listOf(mint, lavender))
    val accentH = Brush.horizontalGradient(listOf(mint, lavender))

    val cardRadius = 24.dp
    val buttonRadius = 16.dp

    /** Hareket: 200–250 ms, ease-out. */
    val easeOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    fun <T> motion(ms: Int = 240) = tween<T>(durationMillis = ms, easing = easeOut)
}

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun inter(weight: Int) = Font(
    R.font.inter,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Inter = FontFamily(inter(400), inter(500), inter(600), inter(700))

/** Rakamlar tabular: sayaçlar ve yüzdeler titremez. */
val Tabular = TextStyle(fontFeatureSettings = "tnum")

private val base = TextStyle(fontFamily = Inter, color = G.text, fontFeatureSettings = "tnum")

private val GelisioTypography = Typography(
    displayLarge = base.copy(fontSize = 44.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
    headlineLarge = base.copy(fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp),
    headlineMedium = base.copy(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp),
    titleLarge = base.copy(fontSize = 19.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleMedium = base.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = base.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = base.copy(fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = base.copy(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = base.copy(fontSize = 12.sp, lineHeight = 16.sp, color = G.textSecondary),
    labelLarge = base.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = base.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
    labelSmall = base.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp),
)

private val GelisioScheme = darkColorScheme(
    primary = G.mint,
    onPrimary = G.bgBottom,
    secondary = G.lavender,
    onSecondary = G.bgBottom,
    background = G.bgBottom,
    onBackground = G.text,
    surface = G.bgTop,
    onSurface = G.text,
    surfaceVariant = Color(0xFF242A3B),
    onSurfaceVariant = G.textSecondary,
    surfaceContainerHigh = Color(0xFF1F2433),
    error = G.critical,
    onError = G.bgBottom,
    outline = G.stroke,
)

@Composable
fun GelisioTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = GelisioScheme, typography = GelisioTypography, content = content)
}
