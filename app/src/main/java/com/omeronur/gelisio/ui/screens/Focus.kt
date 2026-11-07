@file:OptIn(ExperimentalLayoutApi::class)

package com.omeronur.gelisio.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.omeronur.gelisio.data.DeepBlock
import com.omeronur.gelisio.data.Goal
import com.omeronur.gelisio.data.LeadMeasure
import com.omeronur.gelisio.data.PrincipleContext
import com.omeronur.gelisio.data.Quotes
import com.omeronur.gelisio.data.Session
import com.omeronur.gelisio.data.Store
import com.omeronur.gelisio.data.TR_DAYS
import com.omeronur.gelisio.data.ValueCheck
import com.omeronur.gelisio.data.WorkPhilosophy
import com.omeronur.gelisio.data.deepHours
import com.omeronur.gelisio.data.quarterKey
import com.omeronur.gelisio.data.weekKey
import com.omeronur.gelisio.data.weekStart
import com.omeronur.gelisio.notify.Notifier
import com.omeronur.gelisio.ui.Nav
import com.omeronur.gelisio.ui.Screen
import com.omeronur.gelisio.ui.SubPage
import com.omeronur.gelisio.ui.TabPage
import com.omeronur.gelisio.ui.components.Bars
import com.omeronur.gelisio.ui.components.BigNumber
import com.omeronur.gelisio.ui.components.Chip
import com.omeronur.gelisio.ui.components.ConfirmDialog
import com.omeronur.gelisio.ui.components.Divider
import com.omeronur.gelisio.ui.components.GlassButton
import com.omeronur.gelisio.ui.components.GlassCard
import com.omeronur.gelisio.ui.components.GlassField
import com.omeronur.gelisio.ui.components.IconBadge
import com.omeronur.gelisio.ui.components.NavRow
import com.omeronur.gelisio.ui.components.PrimaryButton
import com.omeronur.gelisio.ui.components.ProgressRing
import com.omeronur.gelisio.ui.components.SectionTitle
import com.omeronur.gelisio.ui.components.Stepper
import com.omeronur.gelisio.ui.components.Tag
import com.omeronur.gelisio.ui.components.TextAction
import com.omeronur.gelisio.ui.components.TimeField
import com.omeronur.gelisio.ui.components.ToggleRow
import com.omeronur.gelisio.ui.components.pressable
import com.omeronur.gelisio.ui.components.trShort
import com.omeronur.gelisio.ui.icons.GelisioIcons
import com.omeronur.gelisio.ui.theme.G
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** Süren seans cihazda saklanır: ekrandan çıkınca veya uygulama kapanınca kaybolmaz. */
object RunningSession {
    private const val P = "gelisio_session"
    data class State(val start: Long, val minutes: Int, val projectId: String, val ritual: List<String>)

    fun get(ctx: Context): State? = runCatching { read(ctx) }.getOrNull()

    private fun read(ctx: Context): State? {
        val sp = ctx.getSharedPreferences(P, Context.MODE_PRIVATE)
        val start = sp.getLong("start", 0)
        if (start == 0L) return null
        return State(start, sp.getInt("minutes", 90), sp.getString("projectId", "").orEmpty(), sp.getString("ritual", "").orEmpty().split(",").filter { it.isNotBlank() })
    }

    fun set(ctx: Context, s: State?) {
        val e = ctx.getSharedPreferences(P, Context.MODE_PRIVATE).edit()
        if (s == null) e.clear() else e.putLong("start", s.start).putInt("minutes", s.minutes).putString("projectId", s.projectId).putString("ritual", s.ritual.joinToString(","))
        e.apply()
    }
}

@Composable
fun FocusTab() {
    val today = LocalDate.now()
    val goals by Store.goals.collectAsState()
    val sessions by Store.sessions.collectAsState()
    val profile by Store.profile.collectAsState()
    val projects by Store.projects.collectAsState()
    val custom by Store.quotes.collectAsState()
    val ctx = LocalContext.current
    val goal = goals.firstOrNull { it.quarter == quarterKey(today) }
    val target = goal?.leadMeasure?.weeklyTarget
    val hours = deepHours(sessions, today)
    val quote = remember(custom) { Quotes.pick(ctx, PrincipleContext.DEEP_WORK, custom) }
    val running = RunningSession.get(ctx)

    TabPage("Odak", subtitle = "Derin çalışma · ${quarterKey(today).replace("-", " ")}") {
        if (running != null) {
            GlassCard(strong = true, onClick = { Nav.push(Screen.DeepSession(running.projectId.ifBlank { null })) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(GelisioIcons.timer, G.mint)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Seans sürüyor", style = MaterialTheme.typography.titleMedium, color = G.mint)
                        Text("${running.minutes} dk planlandı · dokun ve dön", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // 1. disiplin: en hayati hedef
        if (goal == null) {
            GlassCard {
                Text("Çeyreğin hayati hedefi", style = MaterialTheme.typography.labelMedium, color = G.textSecondary)
                Spacer(Modifier.height(4.dp))
                Text("Bu çeyrek için tek bir hayati hedef seç. Az hedef, derin iş.", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Hayati hedef belirle", { Nav.push(Screen.GoalEditor()) }, Modifier.fillMaxWidth(), icon = GelisioIcons.mission)
            }
        } else {
            GlassCard(onClick = { Nav.push(Screen.GoalEditor(goal.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(GelisioIcons.mission, G.lavender)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Hayati hedef · ${goal.quarter}", style = MaterialTheme.typography.labelMedium, color = G.textSecondary)
                        Text(goal.title, style = MaterialTheme.typography.titleMedium)
                        Text("Kılavuz ölçüt: haftada ${"%.0f".format(goal.leadMeasure.weeklyTarget)} ${goal.leadMeasure.unit}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (!goal.valueCheck.passes) {
                    Spacer(Modifier.height(8.dp))
                    Text("Değer filtresinde \"hayır\" var: hedefi kontrolündeki bir ölçüye çek.", style = MaterialTheme.typography.bodySmall, color = G.warning)
                }
            }
        }

        // 3. disiplin: skor cetveli
        GlassCard(strong = true, onClick = { Nav.push(Screen.Sessions) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Skor cetveli · bu hafta", style = MaterialTheme.typography.labelMedium, color = G.textSecondary)
                    Text("${"%.1f".format(hours)} / ${target?.let { "%.0f".format(it) } ?: "—"} derin saat", style = MaterialTheme.typography.headlineMedium)
                }
                if (target != null) {
                    ProgressRing((hours / target).toFloat(), diameter = 64.dp, strokeWidth = 7.dp) {
                        Text("%${((hours / target) * 100).roundToInt().coerceAtMost(999)}", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            val weeks = (7 downTo 0).map { today.minusWeeks(it.toLong()) }
            Bars(weeks.map { deepHours(sessions, it) }, weeks.map { weekKey(it).substringAfter("W") }, target)
        }

        PrimaryButton("Derin seans başlat", { Nav.push(Screen.DeepSession()) }, Modifier.fillMaxWidth(), icon = GelisioIcons.play)

        GlassCard {
            Text("İlke: ${quote.source}", style = MaterialTheme.typography.labelMedium, color = G.lavender)
            Spacer(Modifier.height(6.dp))
            Text(quote.text, style = MaterialTheme.typography.titleMedium)
        }

        // 4. disiplin: düzenli icraat takvimi
        SectionTitle("Derin blok planı")
        GlassCard(padding = 10.dp) {
            NavRow(
                GelisioIcons.week,
                profile.workPhilosophy?.let { "Felsefe: ${it.label}" } ?: "Çalışma felsefeni seç",
                if (profile.deepBlocks.isEmpty()) "Henüz blok yok" else profile.deepBlocks.sortedWith(compareBy({ it.day }, { it.time }))
                    .take(4).joinToString(" · ") { "${TR_DAYS[it.day - 1]} ${it.time}" },
                trailing = "${profile.deepBlocks.sumOf { it.minutes } / 60} sa/hf",
                onClick = { Nav.push(Screen.DeepBlocks) },
            )
        }

        SectionTitle("Son seanslar", action = if (sessions.isNotEmpty()) "Tümü" else null, onAction = { Nav.push(Screen.Sessions) })
        if (sessions.isEmpty()) {
            Text("Henüz seans yok. İlk seans skor cetveline yazılır.", style = MaterialTheme.typography.bodySmall)
        } else {
            GlassCard(padding = 14.dp) {
                sessions.take(4).forEachIndexed { i, s -> SessionLine(s, projects.firstOrNull { it.id == s.projectId }?.title) ; if (i < 3 && i < sessions.lastIndex) Divider() }
            }
        }
    }
}

@Composable
fun SessionLine(s: Session, project: String?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(project ?: if (s.kind == "practice") "Pratik" else "Derin seans", style = MaterialTheme.typography.bodyLarge)
            val t = Instant.ofEpochMilli(s.startMillis).atZone(ZoneId.systemDefault())
            Text("${t.toLocalDate().trShort()} · %02d:%02d".format(t.hour, t.minute), style = MaterialTheme.typography.bodySmall)
        }
        Text("${s.minutes} dk", style = MaterialTheme.typography.titleSmall, color = G.mint)
    }
}

/** Hayati hedef: kılavuz ölçüt + 3 soruluk değer filtresi. */
@Composable
fun GoalEditorScreen(goalId: String?) {
    val goals by Store.goals.collectAsState()
    val profile by Store.profile.collectAsState()
    val existing = goals.firstOrNull { it.id == goalId }
    val today = LocalDate.now()
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var quarter by rememberSaveable { mutableStateOf(existing?.quarter ?: quarterKey(today)) }
    var role by rememberSaveable { mutableStateOf(existing?.role ?: "") }
    var weekly by remember { mutableDoubleStateOf(existing?.leadMeasure?.weeklyTarget ?: 10.0) }
    var reality by rememberSaveable { mutableStateOf(existing?.valueCheck?.reality ?: true) }
    var constructive by rememberSaveable { mutableStateOf(existing?.valueCheck?.constructive ?: true) }
    var controllable by rememberSaveable { mutableStateOf(existing?.valueCheck?.controllable ?: true) }
    var confirm by remember { mutableStateOf(false) }
    val clash = goals.any { it.quarter == quarter && it.id != goalId }

    SubPage(if (existing == null) "Hayati hedef" else "Hedefi düzenle", subtitle = "Pürdikkat · 1. ve 2. disiplin", actions = {
        if (existing != null) TextAction("Sil", { confirm = true }, color = G.critical)
    }, bottom = {
        PrimaryButton("Kaydet", {
            Store.saveGoal(Goal(existing?.id ?: "", title, quarter, role, LeadMeasure("derin saat", weekly), ValueCheck(reality, constructive, controllable)))
            Nav.pop()
        }, Modifier.fillMaxWidth(), enabled = title.isNotBlank(), icon = GelisioIcons.check)
    }) {
        Lead("Tek hedef", "Çeyrekte bir hayati hedef. Sonuç gecikmeli gelir; haftalık ilerlemeyi kontrolündeki girdi gösterir.")
        GlassField(title, { title = it }, label = "Hedef", placeholder = "Q1'de 3 yeni partnerlik paketi sat", maxLength = 120)
        Text("Çeyrek", style = MaterialTheme.typography.labelMedium, color = G.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val q1 = quarterKey(today)
            val q2 = quarterKey(today.plusMonths(3))
            Chip(q1, quarter == q1, { quarter = q1 })
            Chip(q2, quarter == q2, { quarter = q2 })
        }
        if (clash) Text("Bu çeyrekte zaten bir hayati hedef var. İkinci hedef odağı böler.", style = MaterialTheme.typography.bodySmall, color = G.warning)
        if (profile.roles.isNotEmpty()) {
            Text("Rol", style = MaterialTheme.typography.labelMedium, color = G.textSecondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                profile.roles.forEach { r -> Chip(r, role == r, { role = if (role == r) "" else r }) }
            }
        }
        GlassCard {
            Text("Kılavuz ölçüt", style = MaterialTheme.typography.titleSmall)
            Text("Haftalık derin çalışma saati. Skor cetveli bunu sayar.", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
            Stepper("${"%.0f".format(weekly)} saat", { weekly = (weekly - 1).coerceAtLeast(1.0) }, { weekly = (weekly + 1).coerceAtMost(60.0) })
        }
        GlassCard {
            Text("Değer filtresi", style = MaterialTheme.typography.titleSmall)
            ToggleRow("Gerçeğe dayalı mı?", reality, { reality = it })
            ToggleRow("Sosyal açıdan yapıcı mı?", constructive, { constructive = it })
            ToggleRow("Kontrolümde mi?", controllable, { controllable = it })
            if (!(reality && constructive && controllable)) {
                Text("Uyarı: en az bir \"hayır\". Kontrol edemediğin bir ölçüyü hedef edinme.", style = MaterialTheme.typography.bodySmall, color = G.warning)
            }
        }
    }
    if (confirm && existing != null) ConfirmDialog("Hedefi sil", "Seanslar kalır, hedef silinir.", onConfirm = { Store.deleteGoal(existing.id); Nav.pop() }, onDismiss = { confirm = false })
}

private val RITUAL = listOf(
    "yer" to ("Yer" to "Nerede çalışacağın belli; masa hazır"),
    "sure" to ("Süre" to "Bitiş saati belli; sayaç kurulu"),
    "kural" to ("Kural" to "İnternet, telefon, e-posta kuralı net"),
    "destek" to ("Destek" to "Su, kahve, notlar elinin altında"),
)

/** Derin çalışma sayacı: seans öncesi ritüel kontrolü, bitişte süre skor cetveline yazılır. */
@Composable
fun DeepSessionScreen(projectId: String?) {
    val ctx = LocalContext.current
    val projects by Store.projects.collectAsState()
    val custom by Store.quotes.collectAsState()
    var running by remember { mutableStateOf(RunningSession.get(ctx)) }
    var checks by remember { mutableStateOf(setOf<String>()) }
    var minutes by remember { mutableIntStateOf(Store.profile.value.deepBlocks.firstOrNull()?.minutes ?: 90) }
    var project by remember { mutableStateOf(projectId ?: "") }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var confirmCancel by remember { mutableStateOf(false) }
    val quote = remember(custom) { Quotes.pick(ctx, PrincipleContext.DEEP_WORK, custom) }

    val r = running
    if (r != null) {
        val view = LocalView.current
        DisposableEffect(Unit) {
            view.keepScreenOn = true
            onDispose { view.keepScreenOn = false }
        }
        LaunchedEffect(r.start) {
            while (true) {
                now = System.currentTimeMillis()
                delay(500)
            }
        }
    }

    fun finish() {
        val s = running ?: return
        val mins = ((System.currentTimeMillis() - s.start) / 60_000L).toInt()
        if (mins >= 1) {
            Store.addSession(Session(projectId = s.projectId, startMillis = s.start, minutes = mins, ritual = s.ritual, kind = "deep"))
            Nav.toast("$mins dk skor cetveline yazıldı")
        }
        Notifier.cancelSessionEnd(ctx)
        RunningSession.set(ctx, null)
        running = null
        Nav.pop()
    }

    SubPage("Derin seans", subtitle = if (r == null) "Ritüel → odak → kayıt" else "Sığ işe dönme") {
        if (r == null) {
            GlassCard {
                Text("İlke: ${quote.source}", style = MaterialTheme.typography.labelMedium, color = G.lavender)
                Spacer(Modifier.height(6.dp))
                Text(quote.text, style = MaterialTheme.typography.titleMedium)
            }
            SectionTitle("Ritüel kontrolü")
            GlassCard(padding = 8.dp) {
                RITUAL.forEachIndexed { i, (k, v) ->
                    Row(
                        Modifier.fillMaxWidth().pressable({ checks = if (k in checks) checks - k else checks + k }).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StatusDot(if (k in checks) com.omeronur.gelisio.data.LogStatus.FULL else null, size = 26.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(v.first, style = MaterialTheme.typography.titleSmall)
                            Text(v.second, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (i < RITUAL.lastIndex) Divider()
                }
            }
            SectionTitle("Süre")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(25, 50, 90, 120, 180, 240).forEach { m -> Chip(if (m < 60) "$m dk" else "${m / 60} sa${if (m % 60 > 0) " ${m % 60} dk" else ""}", minutes == m, { minutes = m }) }
            }
            val active = projects.filter { !it.archived }
            if (active.isNotEmpty()) {
                SectionTitle("Hangi projeye yazılsın")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Genel", project.isBlank(), { project = "" })
                    active.forEach { p -> Chip(p.title, project == p.id, { project = p.id }, icon = if (p.isHobby) GelisioIcons.hobby else GelisioIcons.project) }
                }
            }
            PrimaryButton(
                if (checks.size == RITUAL.size) "Başlat" else "Ritüeli tamamla (${checks.size}/4)",
                {
                    val s = RunningSession.State(System.currentTimeMillis(), minutes, project, checks.toList())
                    RunningSession.set(ctx, s)
                    Notifier.scheduleSessionEnd(ctx, s.start + minutes * 60_000L, projects.firstOrNull { it.id == project }?.title ?: "Derin seans")
                    running = s
                },
                Modifier.fillMaxWidth(),
                enabled = checks.size == RITUAL.size,
                icon = GelisioIcons.play,
            )
        } else {
            val total = r.minutes * 60_000L
            val elapsed = now - r.start
            val left = (total - elapsed).coerceAtLeast(0)
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ProgressRing((elapsed.toFloat() / total), diameter = 260.dp, strokeWidth = 16.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (left > 0) "%d:%02d:%02d".format(left / 3_600_000, (left / 60_000) % 60, (left / 1000) % 60) else "Süre doldu",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = if (left > 0) MaterialTheme.typography.displayLarge.fontSize else MaterialTheme.typography.headlineMedium.fontSize))
                        Text("${elapsed / 60_000} / ${r.minutes} dk", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            projects.firstOrNull { it.id == r.projectId }?.let { Text(it.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.CenterHorizontally)) }
            Spacer(Modifier.height(10.dp))
            PrimaryButton(if (left > 0) "Bitir ve kaydet" else "Seansı kaydet", { finish() }, Modifier.fillMaxWidth(), icon = GelisioIcons.check)
            GlassButton("İptal et (kaydetme)", { confirmCancel = true }, Modifier.fillMaxWidth(), color = G.textSecondary)
            Text("Ekran açık kalır. Uygulamadan çıksan da sayaç devam eder; süre dolunca bildirim gelir.", style = MaterialTheme.typography.bodySmall)
        }
    }
    if (confirmCancel) ConfirmDialog("Seansı iptal et", "Bu seans skor cetveline yazılmaz.", confirm = "İptal et", onConfirm = {
        Notifier.cancelSessionEnd(ctx)
        RunningSession.set(ctx, null)
        running = null
        Nav.pop()
    }, onDismiss = { confirmCancel = false })
}

fun templateFor(p: WorkPhilosophy): List<DeepBlock> = when (p) {
    WorkPhilosophy.MONASTIC -> (1..5).map { DeepBlock(it, "08:00", 240) } + DeepBlock(6, "09:00", 180)
    WorkPhilosophy.BIMODAL -> listOf(1, 2).flatMap { listOf(DeepBlock(it, "09:00", 240), DeepBlock(it, "14:00", 180)) }
    WorkPhilosophy.RHYTHMIC -> (1..5).map { DeepBlock(it, "08:00", 90) }
    WorkPhilosophy.JOURNALISTIC -> emptyList()
}

/** Çalışma felsefesi → derin blok şablonu; bloklar elle düzenlenir. */
@Composable
fun DeepBlocksScreen() {
    val profile by Store.profile.collectAsState()
    val projects by Store.projects.collectAsState()
    var editing by remember { mutableStateOf<Pair<Int, DeepBlock>?>(null) }
    SubPage("Derin blok planı", subtitle = profile.workPhilosophy?.let { "Felsefe: ${it.label}" } ?: "Felsefe seçilmedi") {
        GlassCard(padding = 10.dp) {
            NavRow(GelisioIcons.values, "Çalışma felsefesi", profile.workPhilosophy?.desc ?: "Keşiş, çift modlu, ritmik veya gazeteci", onClick = { Nav.push(Screen.Philosophy) })
        }
        profile.workPhilosophy?.let { p ->
            if (p != WorkPhilosophy.JOURNALISTIC) {
                GlassButton("${p.label} şablonunu uygula", {
                    Store.saveProfile(profile.copy(deepBlocks = templateFor(p)))
                    Nav.toast("Şablon uygulandı")
                }, Modifier.fillMaxWidth(), icon = GelisioIcons.review)
            } else {
                Text("Gazeteci yaklaşımında sabit blok yok: boşluk buldukça Odak → Derin seans başlat.", style = MaterialTheme.typography.bodySmall)
            }
        }
        SectionTitle("Bloklar · haftada ${"%.1f".format(profile.deepBlocks.sumOf { it.minutes } / 60.0)} saat")
        val sorted = profile.deepBlocks.withIndex().sortedWith(compareBy({ it.value.day }, { it.value.time }))
        if (sorted.isEmpty()) Text("Blok yok.", style = MaterialTheme.typography.bodyMedium, color = G.textSecondary)
        sorted.forEach { (i, b) ->
            GlassCard(onClick = { editing = i to b }, padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(TR_DAYS[b.day - 1], Modifier.width(48.dp), style = MaterialTheme.typography.titleMedium, color = G.mint)
                    Column(Modifier.weight(1f)) {
                        Text("${b.time} · ${b.minutes} dk", style = MaterialTheme.typography.bodyLarge)
                        projects.firstOrNull { it.id == b.projectId }?.let { Text(it.title, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
        PrimaryButton("Blok ekle", { editing = -1 to DeepBlock(1, "09:00", 90) }, Modifier.fillMaxWidth(), icon = GelisioIcons.add)
    }
    editing?.let { (idx, b) ->
        var day by remember(idx) { mutableIntStateOf(b.day) }
        var time by remember(idx) { mutableStateOf(b.time) }
        var mins by remember(idx) { mutableIntStateOf(b.minutes) }
        var proj by remember(idx) { mutableStateOf(b.projectId) }
        AlertDialog(
            onDismissRequest = { editing = null },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color(0xFF161B28),
            title = { Text("Derin blok", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        TR_DAYS.forEachIndexed { i, d -> Chip(d, day == i + 1, { day = i + 1 }) }
                    }
                    TimeField("Başlangıç", time, { time = it })
                    Stepper("$mins dk", { mins = (mins - 15).coerceAtLeast(15) }, { mins = (mins + 15).coerceAtMost(480) })
                    if (projects.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Chip("Genel", proj.isBlank(), { proj = "" })
                            projects.filter { !it.archived }.forEach { p -> Chip(p.title, proj == p.id, { proj = p.id }) }
                        }
                    }
                }
            },
            confirmButton = {
                TextAction("Kaydet", {
                    val nb = DeepBlock(day, time, mins, proj)
                    val list = profile.deepBlocks.toMutableList()
                    if (idx >= 0) list[idx] = nb else list += nb
                    Store.saveProfile(profile.copy(deepBlocks = list))
                    editing = null
                })
            },
            dismissButton = {
                TextAction(if (idx >= 0) "Sil" else "Vazgeç", {
                    if (idx >= 0) Store.saveProfile(profile.copy(deepBlocks = profile.deepBlocks.filterIndexed { j, _ -> j != idx }))
                    editing = null
                }, color = if (idx >= 0) G.critical else G.textSecondary)
            },
        )
    }
}

@Composable
fun PhilosophyScreen() {
    val profile by Store.profile.collectAsState()
    SubPage("Çalışma felsefesi", subtitle = "Pürdikkat") {
        Lead("Derinliği nasıl planlıyorsun?", "Seçim derin blok şablonunu belirler. Sonra blokları elle değiştirebilirsin.")
        WorkPhilosophy.entries.forEach { p ->
            val sel = profile.workPhilosophy == p
            GlassCard(strong = sel, onClick = {
                Store.saveProfile(profile.copy(workPhilosophy = p, deepBlocks = if (profile.deepBlocks.isEmpty()) templateFor(p) else profile.deepBlocks))
                Nav.toast("Felsefe: ${p.label}")
            }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.label, style = MaterialTheme.typography.titleMedium, color = if (sel) G.mint else G.text)
                        Text(p.desc, style = MaterialTheme.typography.bodySmall)
                    }
                    if (sel) Tag("Seçili", G.mint)
                }
            }
        }
    }
}

@Composable
fun SessionsScreen() {
    val sessions by Store.sessions.collectAsState()
    val projects by Store.projects.collectAsState()
    var del by remember { mutableStateOf<Session?>(null) }
    SubPage("Skor cetveli", subtitle = "Tüm seanslar") {
        val byWeek = sessions.groupBy { weekKey(it.date) }
        if (byWeek.isEmpty()) Text("Henüz seans yok.", style = MaterialTheme.typography.bodyMedium, color = G.textSecondary)
        byWeek.forEach { (wk, list) ->
            SectionTitle("$wk · ${"%.1f".format(list.filter { it.kind == "deep" }.sumOf { it.minutes } / 60.0)} saat")
            GlassCard(padding = 14.dp) {
                list.forEachIndexed { i, s ->
                    Box(Modifier.pressable(null, onLongClick = { del = s })) { SessionLine(s, projects.firstOrNull { it.id == s.projectId }?.title) }
                    if (i < list.lastIndex) Divider()
                }
            }
        }
        if (sessions.isNotEmpty()) Text("Silmek için seansa uzun bas.", style = MaterialTheme.typography.bodySmall, color = G.textTertiary)
    }
    del?.let { s -> ConfirmDialog("Seansı sil", "${s.minutes} dk skor cetvelinden çıkar.", onConfirm = { Store.deleteSession(s.id) }, onDismiss = { del = null }) }
}

@Suppress("unused")
private fun unusedWeekStart(d: LocalDate) = weekStart(d)

@Suppress("unused")
@Composable
private fun Bar(value: Float) = LinearProgressIndicator(progress = { value }, Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)), color = G.mint, trackColor = Color(0x14FFFFFF))

@Suppress("unused")
private fun Goal.label() = title
