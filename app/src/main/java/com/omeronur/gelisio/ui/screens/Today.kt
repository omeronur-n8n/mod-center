@file:OptIn(ExperimentalLayoutApi::class)

package com.omeronur.gelisio.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omeronur.gelisio.data.Cell
import com.omeronur.gelisio.data.DeepBlock
import com.omeronur.gelisio.data.Dimension
import com.omeronur.gelisio.data.Goldilocks
import com.omeronur.gelisio.data.Habit
import com.omeronur.gelisio.data.LogIndex
import com.omeronur.gelisio.data.LogStatus
import com.omeronur.gelisio.data.PrincipleContext
import com.omeronur.gelisio.data.Quotes
import com.omeronur.gelisio.data.Store
import com.omeronur.gelisio.data.TR_DAYS
import com.omeronur.gelisio.data.cell
import com.omeronur.gelisio.data.goldilocks
import com.omeronur.gelisio.data.isDone
import com.omeronur.gelisio.data.isScheduled
import com.omeronur.gelisio.data.missedYesterday
import com.omeronur.gelisio.data.parseTime
import com.omeronur.gelisio.data.status
import com.omeronur.gelisio.ui.Nav
import com.omeronur.gelisio.ui.Screen
import com.omeronur.gelisio.ui.TabPage
import com.omeronur.gelisio.ui.components.Chip
import com.omeronur.gelisio.ui.components.GIcon
import com.omeronur.gelisio.ui.components.GlassButton
import com.omeronur.gelisio.ui.components.GlassCard
import com.omeronur.gelisio.ui.components.IconBadge
import com.omeronur.gelisio.ui.components.IconGlassButton
import com.omeronur.gelisio.ui.components.PrimaryButton
import com.omeronur.gelisio.ui.components.ProgressRing
import com.omeronur.gelisio.ui.components.SectionTitle
import com.omeronur.gelisio.ui.components.TextAction
import com.omeronur.gelisio.ui.components.glass
import com.omeronur.gelisio.ui.components.pressable
import com.omeronur.gelisio.ui.components.trLong
import com.omeronur.gelisio.ui.icons.GelisioIcons
import com.omeronur.gelisio.ui.theme.G
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.roundToInt

@Composable
fun TodayTab() {
    val today = LocalDate.now()
    val habits by Store.habits.collectAsState()
    val logs by Store.logIndex.collectAsState()
    val profile by Store.profile.collectAsState()
    val days by Store.days.collectAsState()
    val custom by Store.quotes.collectAsState()
    val ctx = LocalContext.current

    val active = habits.filter { !it.archived }
    val todays = active.filter { it.isScheduled(today) }
    val done = todays.count { logs.status(it.id, today).isDone() }
    val missedAny = active.any { missedYesterday(it, logs, today) }
    val quote = remember(missedAny, custom) {
        Quotes.pick(ctx, Quotes.dayContext(LocalDateTime.now(), missedAny), custom)
    }

    TabPage(title = "Bugün", subtitle = today.trLong(), actions = {
        IconGlassButton(GelisioIcons.add, { Nav.push(Screen.HabitWizard()) })
    }) {
        // Günün tamamlanma halkası
        GlassCard(strong = true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val pct = if (todays.isEmpty()) 0f else done.toFloat() / todays.size
                ProgressRing(pct, diameter = 128.dp, strokeWidth = 12.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${(pct * 100).roundToInt()}%", style = MaterialTheme.typography.headlineMedium)
                        Text("bugün", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("$done / ${todays.size} alışkanlık", style = MaterialTheme.typography.titleMedium)
                    Text("Minimum da sayılır. Kötü günde bile kimliğine oy ver.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(2.dp))
                    GlassButton("Sadece başla", { Nav.push(Screen.JustStart()) }, icon = GelisioIcons.play, color = G.mint)
                }
            }
        }

        // Goldilocks önerileri
        active.forEach { h ->
            when (goldilocks(h, logs, today)) {
                Goldilocks.LEVEL_UP -> GoldilocksCard(h, up = true)
                Goldilocks.SHRINK -> GoldilocksCard(h, up = false)
                null -> Unit
            }
        }

        SectionTitle("Alışkanlıklar", action = if (active.isNotEmpty()) "Skor kartı" else null, onAction = { Nav.push(Screen.Scorecard) })
        if (todays.isEmpty()) {
            GlassCard {
                Text(
                    if (active.isEmpty()) "İlk alışkanlığını kur. Önce kimlik, sonra davranış." else "Bugün planlı alışkanlık yok.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Alışkanlık kur", { Nav.push(Screen.HabitWizard()) }, Modifier.fillMaxWidth(), icon = GelisioIcons.add)
            }
        } else {
            Text("Dokun: tam · Uzun bas: minimum · Sola kaydır: atla", style = MaterialTheme.typography.bodySmall, color = G.textTertiary)
            todays.forEach { h -> HabitRow(h, logs, today, active) }
        }

        // Sıradaki derin blok
        nextDeepBlock(profile.deepBlocks)?.let { (whenText, b) ->
            val proj = Store.projects.value.firstOrNull { it.id == b.projectId }
            GlassCard(onClick = { Nav.push(Screen.DeepSession(b.projectId.ifBlank { null })) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(GelisioIcons.timer, G.lavender)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Sıradaki derin blok", style = MaterialTheme.typography.labelMedium, color = G.textSecondary)
                        Text("$whenText · ${b.minutes} dk", style = MaterialTheme.typography.titleMedium)
                        if (proj != null) Text(proj.title, style = MaterialTheme.typography.bodySmall)
                    }
                    GIcon(GelisioIcons.forward, size = 16.sp, color = G.textTertiary)
                }
            }
        }

        // Günün cümlesi
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GIcon(GelisioIcons.quote, size = 14.sp, color = G.lavender)
                Spacer(Modifier.width(8.dp))
                Text("Günün cümlesi · İlke: ${quote.source}", style = MaterialTheme.typography.labelMedium, color = G.lavender)
            }
            Spacer(Modifier.height(10.dp))
            Text(quote.text, style = MaterialTheme.typography.titleMedium)
        }

        // Günlük Özel Zafer: 1 saatlik yenilenme bloğu
        val renewal = days[today]?.renewal.orEmpty()
        GlassCard {
            Text("Günlük Özel Zafer", style = MaterialTheme.typography.titleMedium)
            Text("Bugün 1 saat yenilen: fiziksel, ruhsal, zihinsel. İlke: 7 Alışkanlık", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Dimension.PHYSICAL to GelisioIcons.physical,
                    Dimension.SPIRITUAL to GelisioIcons.spiritual,
                    Dimension.MENTAL to GelisioIcons.mental,
                ).forEach { (d, icon) ->
                    Chip(d.label, d in renewal, {
                        Store.setRenewal(today, if (d in renewal) renewal - d else renewal + d)
                    }, icon = icon)
                }
            }
        }
    }
}

private fun nextDeepBlock(blocks: List<DeepBlock>): Pair<String, DeepBlock>? {
    if (blocks.isEmpty()) return null
    val now = LocalDateTime.now()
    for (offset in 0..7) {
        val d = now.toLocalDate().plusDays(offset.toLong())
        val todays = blocks.filter { it.day == d.dayOfWeek.value }.sortedBy { it.time }
        val b = todays.firstOrNull { offset > 0 || (parseTime(it.time) ?: LocalTime.MIN).isAfter(now.toLocalTime()) } ?: continue
        val label = when (offset) {
            0 -> "Bugün ${b.time}"
            1 -> "Yarın ${b.time}"
            else -> "${TR_DAYS[d.dayOfWeek.value - 1]} ${b.time}"
        }
        return label to b
    }
    return null
}

@Composable
private fun GoldilocksCard(h: Habit, up: Boolean) {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(if (up) GelisioIcons.levelUp else GelisioIcons.minus, if (up) G.mint else G.warning)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(if (up) "Seviye atlama zamanı" else "Küçültme zamanı", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (up) "${h.name}: son 14 günde %95+ tutarlılık. Zorluğu bir kademe artır."
                    else "${h.name}: son 14 günde %60 altı. 2 dakikalık sürüme in, zinciri koru.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextAction(if (up) "Zorluğu artır" else "Sürümleri düzenle", {
                Store.saveHabit(h.copy(goldilocksAt = LocalDate.now(), difficulty = if (up) h.difficulty + 1 else h.difficulty))
                Nav.push(Screen.HabitWizard(h.id))
            })
            TextAction("Şimdi değil", { Store.saveHabit(h.copy(goldilocksAt = LocalDate.now())) }, color = G.textSecondary)
        }
    }
}

/** İşaretleme: dokun tam, uzun bas minimum, sola kaydır atla. Aynı işarete tekrar basmak geri alır. */
fun mark(h: Habit, date: LocalDate, current: LogStatus?, next: LogStatus, all: List<Habit>, logs: LogIndex) {
    val target = if (current == next) null else next
    Store.setLog(h.id, date, target)
    if (target == LogStatus.FULL || target == LogStatus.MIN) {
        val stacked = all.firstOrNull { it.cue.type == "stack" && it.cue.afterHabitId == h.id && !logs.status(it.id, date).isDone() && it.isScheduled(date) }
        val identity = Store.identities.value.firstOrNull { it.id == h.identityId }
        Nav.toast(
            when {
                stacked != null -> "Sırada: ${stacked.name}"
                identity != null -> "+1 oy: ${identity.statement}"
                else -> if (target == LogStatus.MIN) "Minimum işaretlendi, zincir korundu" else "Tamamlandı"
            },
        )
    } else if (target == LogStatus.SKIP) {
        Nav.toast("Atlandı. Bir kez kaçırmak kaza; yarın 2 dakika yeter.")
    }
}

@Composable
private fun HabitRow(h: Habit, logs: LogIndex, today: LocalDate, all: List<Habit>) {
    val st = logs.status(h.id, today)
    val recovering = !st.isDone() && missedYesterday(h, logs, today)
    val offset = remember(h.id) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var width = remember { floatArrayOf(1f) }
    val shape = RoundedCornerShape(G.cardRadius)

    Box(Modifier.fillMaxWidth().onSizeChanged { width[0] = it.width.toFloat() }) {
        // Kaydırınca altta görünen "atla" katmanı
        Box(
            Modifier.matchParentSize().clip(shape).background(G.warning.copy(alpha = 0.18f)).padding(end = 22.dp),
            contentAlignment = Alignment.CenterEnd,
        ) { Text("Atla", style = MaterialTheme.typography.labelLarge, color = G.warning) }

        Row(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .fillMaxWidth()
                .pointerInput(h.id, st) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (offset.value < -width[0] * 0.33f) mark(h, today, st, LogStatus.SKIP, all, logs)
                                offset.animateTo(0f, G.motion(220))
                            }
                        },
                        onDragCancel = { scope.launch { offset.animateTo(0f, G.motion(220)) } },
                    ) { change, drag ->
                        change.consume()
                        scope.launch { offset.snapTo((offset.value + drag).coerceIn(-width[0] * 0.6f, 0f)) }
                    }
                }
                .pressable(
                    onClick = { mark(h, today, st, LogStatus.FULL, all, logs) },
                    onLongClick = { mark(h, today, st, LogStatus.MIN, all, logs) },
                )
                .glass(shape, strong = st.isDone())
                .padding(start = 14.dp, end = 10.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(st)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(h.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = when {
                    st == LogStatus.MIN -> "Minimum: ${h.twoMinVersion}"
                    recovering -> "Dün kaçtı. Bugün 2 dakika yeter: ${h.twoMinVersion}"
                    else -> cueText(h, all)
                }
                Text(
                    sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (recovering) G.warning else G.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                MiniChain(h, logs, today)
            }
            Spacer(Modifier.width(6.dp))
            IconGlassButton(GelisioIcons.forward, { Nav.push(Screen.HabitDetail(h.id)) }, size = 36.dp, color = G.textSecondary)
        }
    }
}

fun cueText(h: Habit, all: List<Habit>): String = if (h.cue.type == "stack") {
    val prev = all.firstOrNull { it.id == h.cue.afterHabitId }?.name ?: "önceki alışkanlık"
    "$prev sonrası"
} else {
    listOf(h.cue.time, h.cue.place).filter { it.isNotBlank() }.joinToString(" · ")
}

@Composable
fun StatusDot(st: LogStatus?, size: androidx.compose.ui.unit.Dp = 30.dp) {
    val m = Modifier.size(size).clip(CircleShape)
    when (st) {
        LogStatus.FULL -> Box(m.background(G.accent), contentAlignment = Alignment.Center) {
            GIcon(GelisioIcons.check, size = (size.value * 0.45f).sp, color = G.bgBottom)
        }
        LogStatus.MIN -> Box(m.background(G.mint.copy(alpha = 0.28f)), contentAlignment = Alignment.Center) {
            Text("2'", style = MaterialTheme.typography.labelSmall, color = G.mint)
        }
        LogStatus.SKIP -> Box(m.background(G.warning.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            GIcon(GelisioIcons.close, size = (size.value * 0.45f).sp, color = G.warning)
        }
        null -> Box(m.background(Color(0x14FFFFFF)))
    }
}

fun cellColor(c: Cell): Color = when (c) {
    Cell.FULL -> G.mint
    Cell.MIN -> G.mint.copy(alpha = 0.45f)
    Cell.MISS -> G.warning
    Cell.MISS_TWICE -> G.critical
    Cell.TODAY_OPEN -> Color(0x33FFFFFF)
    Cell.OFF -> Color(0x0DFFFFFF)
    Cell.FUTURE -> Color(0x08FFFFFF)
}

@Composable
fun MiniChain(h: Habit, logs: LogIndex, today: LocalDate, days: Int = 14) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in days - 1 downTo 0) {
            val c = cell(h, logs, today.minusDays(i.toLong()), today)
            Box(Modifier.size(width = 10.dp, height = 10.dp).clip(RoundedCornerShape(3.dp)).background(cellColor(c)))
        }
    }
}

/** "Bir şeyler yap" prensibi: 2 dakikalık sürüm + 5 dakikalık sayaç. */
@Composable
fun JustStartScreen(habitId: String?) {
    val habits by Store.habits.collectAsState()
    val logs by Store.logIndex.collectAsState()
    val custom by Store.quotes.collectAsState()
    val ctx = LocalContext.current
    val today = LocalDate.now()
    val open = habits.filter { !it.archived && it.isScheduled(today) && !logs.status(it.id, today).isDone() }
    var selectedId by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(habitId ?: open.firstOrNull()?.id) }
    val h = habits.firstOrNull { it.id == selectedId }
    val quote = remember(custom) { Quotes.pick(ctx, PrincipleContext.NO_MOTIVATION, custom) }
    var startedAt by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(0L) }
    var now by remember { androidx.compose.runtime.mutableLongStateOf(System.currentTimeMillis()) }
    androidx.compose.runtime.LaunchedEffect(startedAt) {
        while (startedAt > 0) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(250)
        }
    }
    val total = 5 * 60_000L
    val left = if (startedAt == 0L) total else (total - (now - startedAt)).coerceAtLeast(0)
    val finished = startedAt > 0 && left == 0L

    com.omeronur.gelisio.ui.SubPage("Sadece başla", subtitle = "Motivasyon eylemden sonra gelir") {
        GlassCard {
            Text("İlke: ${quote.source}", style = MaterialTheme.typography.labelMedium, color = G.lavender)
            Spacer(Modifier.height(6.dp))
            Text(quote.text, style = MaterialTheme.typography.titleMedium)
        }
        if (open.isNotEmpty()) {
            SectionTitle("Hangisi")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                open.forEach { o -> Chip(o.name, o.id == selectedId, { selectedId = o.id }) }
            }
        }
        GlassCard(strong = true) {
            Text("2 dakikalık sürüm", style = MaterialTheme.typography.labelMedium, color = G.textSecondary)
            Spacer(Modifier.height(6.dp))
            Text(h?.twoMinVersion?.ifBlank { null } ?: "Bugün yapılacak tek küçük adımı seç.", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(20.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ProgressRing(1f - left.toFloat() / total, diameter = 200.dp) {
                    Text("%d:%02d".format(left / 60000, (left / 1000) % 60), style = MaterialTheme.typography.displayLarge)
                }
            }
            Spacer(Modifier.height(20.dp))
            when {
                startedAt == 0L -> PrimaryButton("5 dakikalık sayacı başlat", { startedAt = System.currentTimeMillis() }, Modifier.fillMaxWidth(), icon = GelisioIcons.play)
                !finished -> GlassButton("Sayacı sıfırla", { startedAt = 0L }, Modifier.fillMaxWidth(), icon = GelisioIcons.stop)
                else -> Text("Başladın. Gerisi arkasından gelir.", style = MaterialTheme.typography.titleMedium, color = G.mint)
            }
        }
        if (h != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton("Minimum işaretle", {
                    mark(h, today, logs.status(h.id, today), LogStatus.MIN, habits, logs)
                    Nav.pop()
                }, Modifier.weight(1f))
                PrimaryButton("Tamamını yaptım", {
                    mark(h, today, logs.status(h.id, today), LogStatus.FULL, habits, logs)
                    Nav.pop()
                }, Modifier.weight(1f))
            }
        }
    }
}
