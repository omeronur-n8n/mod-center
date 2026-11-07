package com.omeronur.gelisio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Gelisio tasarım tokenları (koyu, cam yüzeyli). */
object G {
    val bgTop = Color(0xFF1A1F2E)
    val bgBottom = Color(0xFF07090F)
    val mint = Color(0xFF7CF5D2)
    val lavender = Color(0xFF8B7CFF)
    val warning = Color(0xFFFFC56B)
    val critical = Color(0xFFFF6B81)
    val text = Color(0xFFF2F4F8)
    val textSecondary = Color(0x99F2F4F8)
    val glassTop = Color(0x17FFFFFF)
    val glassBottom = Color(0x08FFFFFF)
    val glassBorder = Color(0x1AFFFFFF)
}

private val GelisioScheme = darkColorScheme(
    primary = G.mint,
    onPrimary = G.bgBottom,
    secondary = G.lavender,
    onSecondary = G.bgBottom,
    background = G.bgBottom,
    onBackground = G.text,
    surface = G.bgBottom,
    onSurface = G.text,
    error = G.critical,
    onError = G.bgBottom,
)

@Composable
fun GelisioTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = GelisioScheme, content = content)
}
