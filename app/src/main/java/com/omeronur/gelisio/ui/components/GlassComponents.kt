package com.omeronur.gelisio.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omeronur.gelisio.ui.icons.GelisioIconFont
import com.omeronur.gelisio.ui.theme.G

/** Cam yüzey: yarı saydam dikey gradyan + ince kenar. Blur yerine saydamlıkla yaklaşılır. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(G.glassTop, G.glassBottom)))
            .border(1.dp, if (highlighted) G.mint.copy(alpha = 0.55f) else G.glassBorder, shape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(20.dp),
        content = content,
    )
}

/** Logonun diliyle ilerleme halkası: nane → lavanta, yuvarlak uçlu. */
@Composable
fun ProgressRing(
    value: Float,
    diameter: Dp = 176.dp,
    strokeWidth: Dp = 14.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val animated by animateFloatAsState(
        targetValue = value.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "ring",
    )
    Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2
            val arc = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = Color.White.copy(alpha = 0.08f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arc,
                style = Stroke(width = stroke),
            )
            if (animated > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(G.mint, G.lavender, G.mint),
                        center = Offset(this.size.width / 2, this.size.height / 2),
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arc,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

data class NavItem(val icon: Char, val label: String)

/** Alt cam sekme çubuğu. */
@Composable
fun GlassNavBar(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(G.glassTop, G.glassBottom)))
            .border(1.dp, G.glassBorder, shape)
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        items.forEachIndexed { index, item ->
            NavButton(
                item = item,
                selected = index == selected,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(index) },
            )
        }
    }
}

@Composable
private fun NavButton(item: NavItem, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val tint = if (selected) G.mint else G.textSecondary
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color.White.copy(alpha = 0.07f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = item.icon.toString(), fontFamily = GelisioIconFont, fontSize = 20.sp, color = tint)
        Spacer(Modifier.height(4.dp))
        Text(
            text = item.label,
            fontSize = 11.sp,
            maxLines = 1,
            color = if (selected) G.text else G.textSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
