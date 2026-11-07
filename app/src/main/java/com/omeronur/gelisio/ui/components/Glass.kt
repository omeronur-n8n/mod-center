@file:OptIn(ExperimentalFoundationApi::class)

package com.omeronur.gelisio.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omeronur.gelisio.ui.icons.GelisioIconFont
import com.omeronur.gelisio.ui.theme.G
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

/** Arka plan bulanıklık kaynağı. Cam yüzeyler bu durumdan okur. */
val LocalHaze = staticCompositionLocalOf<HazeState?> { null }

private val glassStyle = HazeStyle(
    backgroundColor = G.bgBottom,
    tint = HazeTint(Color(0x14FFFFFF)),
    blurRadius = 24.dp,
    noiseFactor = 0.06f,
    fallbackTint = HazeTint(Color(0x22FFFFFF)),
)

private val glassStrongStyle = HazeStyle(
    backgroundColor = G.bgBottom,
    tint = HazeTint(Color(0x22FFFFFF)),
    blurRadius = 30.dp,
    noiseFactor = 0.06f,
    fallbackTint = HazeTint(Color(0x30FFFFFF)),
)

/** Cam kenarı: üstte parlak, altta sönük — Apple tarzı ışık vurgusu. */
private val rimBrush = Brush.verticalGradient(listOf(Color(0x40FFFFFF), Color(0x14FFFFFF), Color(0x0AFFFFFF)))

/** Liquid Glass yüzeyi: arka planı 24 dp bulanıklaştırır, %6 beyaz tint, 1 px ışıklı kenar. */
fun Modifier.glass(shape: Shape, strong: Boolean = false): Modifier = composed {
    val state = LocalHaze.current
    val base = this.clip(shape)
    val blurred = if (state != null) {
        base.hazeChild(state, shape, if (strong) glassStrongStyle else glassStyle)
    } else {
        base.background(if (strong) G.glassStrong else G.glass)
    }
    blurred.border(1.dp, rimBrush, shape)
}

/** Basınca hafif küçülen, dokunuşta titreşim veren tıklama. */
fun Modifier.pressable(
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
): Modifier = composed {
    if (onClick == null && onLongClick == null) return@composed this
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) pressedScale else 1f, G.motion(200), label = "press")
    val view = LocalView.current
    this
        .scale(scale)
        .combinedClickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick?.invoke()
            },
            onLongClick = onLongClick?.let {
                {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    it()
                }
            },
        )
}

@Composable
fun GlassBackground(state: HazeState, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(G.bgBottom)) {
        val t = rememberInfiniteTransition(label = "orbs")
        val drift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Reverse), label = "drift")
        // Bulanıklık kaynağı: gradyan zemin + yavaş süzülen renk küreleri (camın arkasında görünen ışık).
        Canvas(Modifier.fillMaxSize().haze(state)) {
            drawRect(Brush.verticalGradient(listOf(G.bgTop, G.bgBottom)))
            val w = size.width
            val h = size.height
            fun orb(c: Color, x: Float, y: Float, r: Float, a: Float) = drawCircle(
                Brush.radialGradient(listOf(c.copy(alpha = a), Color.Transparent), center = Offset(x, y), radius = r),
                radius = r,
                center = Offset(x, y),
            )
            orb(G.mint, w * (0.10f + 0.12f * drift), h * 0.12f, w * 0.75f, 0.30f)
            orb(G.lavender, w * (0.95f - 0.15f * drift), h * (0.38f + 0.05f * drift), w * 0.85f, 0.32f)
            orb(G.mint, w * (0.25f + 0.2f * drift), h * 0.78f, w * 0.7f, 0.16f)
            orb(G.lavender, w * 0.85f, h * (0.95f - 0.06f * drift), w * 0.6f, 0.22f)
        }
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    padding: Dp = 18.dp,
    strong: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(G.cardRadius)
    Column(
        modifier
            .fillMaxWidth()
            .pressable(onClick, onLongClick)
            .glass(shape, strong)
            .padding(padding),
        content = content,
    )
}

@Composable
fun GIcon(icon: Char, modifier: Modifier = Modifier, size: TextUnit = 18.sp, color: Color = G.text) {
    Text(icon.toString(), modifier, fontFamily = GelisioIconFont, fontSize = size, color = color, lineHeight = size)
}

/** Ana buton: nane → lavanta gradyan, iç ışık, renkli gölge. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Char? = null,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(G.buttonRadius)
    Box(
        modifier
            .heightIn(min = 54.dp)
            .pressable(if (enabled) onClick else null, pressedScale = 0.96f)
            .shadow(if (enabled) 18.dp else 0.dp, shape, ambientColor = G.mint, spotColor = G.lavender)
            .clip(shape)
            .background(if (enabled) G.accentH else SolidColor(Color(0x22FFFFFF)))
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(Color(0x40FFFFFF), Color.Transparent), endY = size.height * 0.55f))
            }
            .border(1.dp, Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x10FFFFFF))), shape)
            .padding(horizontal = 22.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        val fg = if (enabled) G.bgBottom else G.textTertiary
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                GIcon(icon, size = 16.sp, color = fg)
                Spacer(Modifier.width(10.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, color = fg)
        }
    }
}

/** İkincil buton: cam kapsül. */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Char? = null,
    color: Color = G.text,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(G.buttonRadius)
    Box(
        modifier
            .heightIn(min = 50.dp)
            .pressable(if (enabled) onClick else null, pressedScale = 0.96f)
            .glass(shape, strong = true)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        val fg = if (enabled) color else G.textTertiary
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                GIcon(icon, size = 15.sp, color = fg)
                if (text.isNotEmpty()) Spacer(Modifier.width(9.dp))
            }
            if (text.isNotEmpty()) Text(text, style = MaterialTheme.typography.labelLarge, color = fg)
        }
    }
}

@Composable
fun TextAction(text: String, onClick: () -> Unit, color: Color = G.mint, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier.pressable(onClick).padding(horizontal = 6.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = color,
    )
}

@Composable
fun IconGlassButton(icon: Char, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 44.dp, color: Color = G.text) {
    Box(
        modifier.size(size).pressable(onClick, pressedScale = 0.9f).glass(CircleShape, strong = true),
        contentAlignment = Alignment.Center,
    ) { GIcon(icon, size = (size.value * 0.36f).sp, color = color) }
}

/** Seçilebilir kapsül çip; seçili olan gradyanla dolar. */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, icon: Char? = null) {
    val shape = RoundedCornerShape(50)
    val m = modifier.pressable(onClick, pressedScale = 0.94f)
    Row(
        (if (selected) m.clip(shape).background(G.accentH) else m.glass(shape)).padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fg = if (selected) G.bgBottom else G.text
        if (icon != null) {
            GIcon(icon, size = 13.sp, color = fg)
            Spacer(Modifier.width(7.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1)
    }
}

@Composable
fun ProgressRing(
    value: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 176.dp,
    strokeWidth: Dp = 14.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val animated by animateFloatAsState(value.coerceIn(0f, 1f), G.motion(600), label = "ring")
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val arc = Size(size.width - stroke, size.height - stroke)
            val tl = Offset(stroke / 2, stroke / 2)
            drawArc(Color(0x14FFFFFF), 0f, 360f, false, tl, arc, style = Stroke(stroke))
            if (animated > 0f) {
                drawArc(
                    Brush.sweepGradient(listOf(G.mint, G.lavender, G.mint)),
                    -90f, 360f * animated, false, tl, arc,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(TR), style = MaterialTheme.typography.labelSmall, color = G.textSecondary, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) TextAction(action, onAction)
    }
}

@Composable
fun Tag(text: String, color: Color = G.lavender) {
    Text(
        text,
        Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.16f)).padding(horizontal = 9.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = color,
    )
}

/** Cam metin alanı. */
@Composable
fun GlassField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    singleLine: Boolean = true,
    maxLength: Int = 200,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    Column(modifier.fillMaxWidth()) {
        if (label != null) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = G.textSecondary)
            Spacer(Modifier.height(7.dp))
        }
        val shape = RoundedCornerShape(G.buttonRadius)
        BasicTextField(
            value = value,
            onValueChange = { if (it.length <= maxLength) onValueChange(it) },
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = G.text),
            cursorBrush = SolidColor(G.mint),
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            modifier = Modifier.fillMaxWidth().glass(shape, strong = true).padding(horizontal = 16.dp, vertical = 14.dp),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = G.textTertiary)
                    inner()
                }
            },
        )
    }
}

@Composable
fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, subtitle: String? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = G.bgBottom,
                checkedTrackColor = G.mint,
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = G.textSecondary,
                uncheckedTrackColor = Color(0x22FFFFFF),
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

/** +/− sayaç. */
@Composable
fun Stepper(value: String, onMinus: () -> Unit, onPlus: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        IconGlassButton(com.omeronur.gelisio.ui.icons.GelisioIcons.minus, onMinus, size = 40.dp)
        Text(value, Modifier.width(84.dp), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        IconGlassButton(com.omeronur.gelisio.ui.icons.GelisioIcons.add, onPlus, size = 40.dp)
    }
}

/** Liste satırı: ikon + başlık + açıklama + ok. Dokunulabilir. */
@Composable
fun NavRow(icon: Char, title: String, subtitle: String? = null, trailing: String? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().pressable(onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.labelMedium, color = G.textSecondary)
            Spacer(Modifier.width(6.dp))
        }
        GIcon(com.omeronur.gelisio.ui.icons.GelisioIcons.forward, size = 16.sp, color = G.textTertiary)
    }
}

@Composable
fun IconBadge(icon: Char, color: Color = G.mint, size: Dp = 38.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) { GIcon(icon, size = (size.value * 0.42f).sp, color = color) }
}

@Composable
fun BigNumber(value: String, label: String, modifier: Modifier = Modifier, color: Color = G.text) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = color)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun Divider() {
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(Color(0x12FFFFFF)))
}

/** Basit çubuk grafik (skor cetveli). Hedef çizgisi lavanta. */
@Composable
fun Bars(values: List<Double>, labels: List<String>, target: Double?, modifier: Modifier = Modifier, height: Dp = 120.dp) {
    val max = (values + listOfNotNull(target)).maxOrNull()?.takeIf { it > 0 } ?: 1.0
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(height)) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                values.forEachIndexed { i, v ->
                    val frac = (v / max).toFloat().coerceIn(0f, 1f)
                    val hit = target != null && v >= target
                    Box(
                        Modifier.weight(1f).fillMaxHeight(frac.coerceAtLeast(0.02f))
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(if (hit) G.accent else SolidColor(if (i == values.lastIndex) Color(0x55FFFFFF) else Color(0x26FFFFFF))),
                    )
                }
            }
            if (target != null) {
                val tf = (target / max).toFloat().coerceIn(0f, 1f)
                Canvas(Modifier.fillMaxSize()) {
                    val y = size.height * (1 - tf)
                    drawLine(G.lavender, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            labels.forEach { Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = G.textTertiary, textAlign = TextAlign.Center) }
        }
    }
}

@Composable
fun RowScope.Weighted(content: @Composable () -> Unit) = Box(Modifier.weight(1f)) { content() }

val ContentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 140.dp)
