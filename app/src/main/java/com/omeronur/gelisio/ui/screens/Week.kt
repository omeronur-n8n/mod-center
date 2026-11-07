@file:OptIn(ExperimentalLayoutApi::class)

package com.omeronur.gelisio.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.omeronur.gelisio.data.PrincipleContext
import com.omeronur.gelisio.data.Quotes
import com.omeronur.gelisio.data.Review
import com.omeronur.gelisio.data.RoleGoal
import com.omeronur.gelisio.data.Store
import com.omeronur.gelisio.data.TR_DAYS
import com.omeronur.gelisio.data.Week
import com.omeronur.gelisio.data.deepHours
import com.omeronur.gelisio.data.isDone
import com.omeronur.gelisio.data.isScheduled
import com.omeronur.gelisio.data.q2Pct
import com.omeronur.gelisio.data.quarterKey
import com.omeronur.gelisio.data.status
import com.omeronur.gelisio.data.weekKey
import com.omeronur.gelisio.data.weekStart
import com.omeronur.gelisio.ui.Nav
import com.omeronur.gelisio.ui.Screen
import com.omeronur.gelisio.ui.SubPage
import com.omeronur.gelisio.ui.TabPage
import com.omeronur.gelisio.ui.components.BigNumber
import com.omeronur.gelisio.ui.components.Chip
import com.omeronur.gelisio.ui.components.Divider
import com.omeronur.gelisio.ui.components.GlassButton
import com.omeronur.gelisio.ui.components.GlassCard
import com.omeronur.gelisio.ui.components.GlassField
import com.omeronur.gelisio.ui.components.InputDialog
import com.omeronur.gelisio.ui.components.NavRow
import com.omeronur.gelisio.ui.components.PrimaryButton
import com.omeronur.gelisio.ui.components.SectionTitle
import com.omeronur.gelisio.ui.components.Tag
import com.omeronur.gelisio.ui.components.TextAction
import com.omeronur.gelisio.ui.components.pressable
import com.omeronur.gelisio.ui.components.trShort
import com.omeronur.gelisio.ui.icons.GelisioIcons
import com.omeronur.gelisio.ui.theme.G
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import kotlin.math.roundToInt

val QUADRANTS = mapOf(
    1 to ("I" to "Acil ve önemli"),
    2 to ("II" to "Acil değil, önemli"),
    3 to ("III" to "Acil, önemsiz"),
    4 to ("IV" to "Acil değil, önemsiz"),
)

private fun quadColor(q: Int) = when (q) {
    1 -> G.warning
    2 -> G.mint
    3 -> G.lavender
    else -> G.textSecondary
}

/** Pazar günü plan yapılan hafta: gelecek hafta. Diğer günlerde içinde bulunulan hafta. */
fun planningWeek(today: LocalDate = LocalDate.now()): LocalDate =
    if (today.dayOfWeek == DayOfWeek.SUNDAY) today.plusDays(1) else today

fun weekRange(anyDay: LocalDate): String {
    val s = weekStart(anyDay)
    return "${s.trShort()} – ${s.plusDays(6).trShort()}"
}

@Composable
fun WeekTab() {
    val today = LocalDate.now()
    val key = weekKey(today)
    val weeks by Store.weeks.collectAsState()
    val profile by Store.profile.collectAsState()
    val sessions by Store.sessions.collectAsState()
    val goals by Store.goals.collectAsState()
    val habits by Store.habits.collectAsState()
    val logs by Store.logIndex.collectAsState()
    val week = weeks[key] ?: Week(key)
    val q2 = q2Pct(week)
    val qGoal = goals.firstOrNull { it.quarter == quarterKey(today) }
    val hours = deepHours(sessions, today)
    var editing by remember { mutableStateOf<RoleGoal?>(null) }
    var addingFor by remember { mutableStateOf<String?>(null) }

    // Haftanın alışkanlık tutarlılığı (bugüne kadar)
    val start = weekStart(today)
    var sched = 0
    var done = 0
    habits.filter { !it.archived }.forEach { h ->
        var d = start
        while (!d.isAfter(today)) {
            if (h.isScheduled(d) && !(d == today && !logs.status(h.id, d).isDone())) {
                sched++
                if (logs.status(h.id, d).isDone()) done++
            }
            d = d.plusDays(1)
        }
    }

    TabPage("Hafta", subtitle = "${key.substringAfter("-W")}. hafta · ${weekRange(today)}") {
        GlassCard(strong = true) {
            Row {
                BigNumber(q2?.let { "%$it" } ?: "—", "II. Kare", Modifier.weight(1f), G.mint)
                BigNumber("${week.goals.count { it.done }}/${week.goals.size}", "Hedef", Modifier.weight(1f))
                BigNumber(if (sched == 0) "—" else "%${done * 100 / sched}", "Alışkanlık", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Derin çalışma: ${"%.1f".format(hours)} / ${qGoal?.leadMeasure?.weeklyTarget?.let { "%.0f".format(it) } ?: "—"} saat",
                style = MaterialTheme.typography.bodyMedium, color = G.textSecondary,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Pazar planı", { Nav.push(Screen.PlanWizard) }, Modifier.weight(1f), icon = GelisioIcons.week)
            GlassButton("Gözden geçir", { Nav.push(Screen.WeeklyReview) }, Modifier.weight(1f), icon = GelisioIcons.review)
        }

        SectionTitle("Roller × hedefler", action = "Matris", onAction = { Nav.push(Screen.Matrix) })
        if (profile.roles.isEmpty()) {
            GlassCard(onClick = { Nav.push(Screen.Roles) }) {
                Text("Önce rollerini yaz", style = MaterialTheme.typography.titleMedium)
                Text("Girişimci, eş, baba, sporcu… En fazla 7 rol. Hafta rollere göre planlanır.", style = MaterialTheme.typography.bodySmall)
            }
        }
        val roles = profile.roles + week.goals.map { it.role }.filter { it.isNotBlank() && it !in profile.roles }.distinct()
        roles.forEach { role ->
            val rg = week.goals.filter { it.role == role }
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(role, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Text("${rg.size}/3", style = MaterialTheme.typography.labelMedium, color = if (rg.size in 2..3) G.mint else G.textTertiary)
                }
                Spacer(Modifier.height(6.dp))
                rg.forEach { g -> GoalLine(g, onToggle = { Store.saveWeek(week.upsert(g.copy(done = !g.done))) }, onEdit = { editing = g }) }
                TextAction("+ Hedef ekle", { addingFor = role })
            }
        }
        val loose = week.goals.filter { it.role.isBlank() }
        if (loose.isNotEmpty()) {
            GlassCard {
                Text("Rolsüz görevler", style = MaterialTheme.typography.titleMedium)
                loose.forEach { g -> GoalLine(g, onToggle = { Store.saveWeek(week.upsert(g.copy(done = !g.done))) }, onEdit = { editing = g }) }
            }
        }
        GlassCard(padding = 10.dp) {
            NavRow(GelisioIcons.ritual, "Zaman yönetimi matrisi", "Görevleri 4 kareye ayır; II. Kareyi büyüt", onClick = { Nav.push(Screen.Matrix) })
        }
    }

    editing?.let { g -> RoleGoalDialog(g, onSave = { Store.saveWeek(week.upsert(it)) }, onDelete = { Store.saveWeek(week.copy(goals = week.goals.filter { x -> x.id != g.id })) }, onDismiss = { editing = null }) }
    addingFor?.let { role ->
        InputDialog("$role için hedef", placeholder = "Hafta sonu teklif setini bitir", onConfirm = {
            Store.saveWeek(week.upsert(RoleGoal(UUID.randomUUID().toString(), role, it)))
        }, onDismiss = { addingFor = null })
    }
}

fun Week.upsert(g: RoleGoal): Week = copy(goals = if (goals.any { it.id == g.id }) goals.map { if (it.id == g.id) g else it } else goals + g)

@Composable
fun GoalLine(g: RoleGoal, onToggle: () -> Unit, onEdit: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.layout.Box(Modifier.pressable(onToggle, pressedScale = 0.85f)) {
            StatusDot(if (g.done) com.omeronur.gelisio.data.LogStatus.FULL else null, size = 26.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).pressable(onEdit).padding(vertical = 6.dp)) {
            Text(
                g.title, style = MaterialTheme.typography.bodyLarge,
                color = if (g.done) G.textSecondary else G.text,
                textDecoration = if (g.done) TextDecoration.LineThrough else null,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Tag("${QUADRANTS[g.quadrant]?.first}. Kare", quadColor(g.quadrant))
                if (g.day in 1..7) Tag(TR_DAYS[g.day - 1], G.textSecondary)
            }
        }
    }
}

@Composable
fun RoleGoalDialog(g: RoleGoal, onSave: (RoleGoal) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(g.title) }
    var q by remember { mutableIntStateOf(g.quadrant) }
    var day by remember { mutableIntStateOf(g.day) }
    val roles = Store.profile.value.roles
    var role by remember { mutableStateOf(g.role) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        containerColor = Color(0xFF161B28),
        title = { Text("Hedef", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassField(title, { title = it }, maxLength = 120)
                Text("Kare", style = MaterialTheme.typography.labelMedium, color = G.textSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    QUADRANTS.forEach { (k, v) -> Chip("${v.first} · ${v.second}", q == k, { q = k }) }
                }
                Text("Gün", style = MaterialTheme.typography.labelMedium, color = G.textSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip("—", day == 0, { day = 0 })
                    TR_DAYS.forEachIndexed { i, d -> Chip(d, day == i + 1, { day = i + 1 }) }
                }
                if (roles.isNotEmpty()) {
                    Text("Rol", style = MaterialTheme.typography.labelMedium, color = G.textSecondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Chip("Rolsüz", role.isBlank(), { role = "" })
                        roles.forEach { r -> Chip(r, role == r, { role = r }) }
                    }
                }
            }
        },
        confirmButton = { TextAction("Kaydet", { if (title.isNotBlank()) onSave(g.copy(title = title.trim(), quadrant = q, day = day, role = role)); onDismiss() }) },
        dismissButton = { TextAction("Sil", { onDelete(); onDismiss() }, color = G.critical) },
    )
}

/** Pazar planlama sihirbazı: gözden geçir → misyon ve roller → rol başına 2–3 hedef → programla. */
@Composable
fun PlanWizardScreen() {
    val today = LocalDate.now()
    val target = planningWeek(today)
    val targetKey = weekKey(target)
    val lastKey = weekKey(target.minusWeeks(1))
    val weeks by Store.weeks.collectAsState()
    val profile by Store.profile.collectAsState()
    val custom by Store.quotes.collectAsState()
    val ctx = LocalContext.current
    val quote = remember(custom) { Quotes.pick(ctx, PrincipleContext.WEEKLY_PLAN, custom) }
    var step by rememberSaveable { mutableIntStateOf(0) }
    var plan by remember { mutableStateOf(weeks[targetKey] ?: Week(targetKey)) }
    val last = weeks[lastKey] ?: Week(lastKey)
    var worked by rememberSaveable { mutableStateOf(last.review.worked) }
    var didnt by rememberSaveable { mutableStateOf(last.review.didnt) }
    var change by rememberSaveable { mutableStateOf(last.review.change) }
    var addingFor by remember { mutableStateOf<String?>(null) }
    val titles = listOf("Geçen hafta", "Misyon ve roller", "Hedefler", "Programla")

    SubPage("Pazar planı", subtitle = "Adım ${step + 1}/4 · ${titles[step]} · ${weekRange(target)}", bottom = {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step > 0) GlassButton("Geri", { step-- }, Modifier.weight(1f))
            if (step < 3) {
                PrimaryButton("Devam", {
                    if (step == 0 && lastKey != targetKey) Store.saveWeek(last.copy(review = Review(worked, didnt, change)))
                    step++
                }, Modifier.weight(2f))
            } else {
                PrimaryButton("Haftayı kaydet", {
                    Store.saveWeek(plan)
                    Nav.toast("Hafta planlandı · II. Kare %${q2Pct(plan) ?: 0}")
                    Nav.pop()
                }, Modifier.weight(2f), icon = GelisioIcons.check)
            }
        }
    }) {
        StepBar(step, 4)
        AnimatedContent(step, transitionSpec = { fadeIn(G.motion()) togetherWith fadeOut(G.motion(200)) }, label = "plan") { s ->
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (s) {
                    0 -> {
                        Lead("Geçen haftaya bak", "Kendini yargılamadan: ne işe yaradı, ne yaramadı, neyi değiştiriyorsun?")
                        GlassCard {
                            Text("İlke: ${quote.source}", style = MaterialTheme.typography.labelMedium, color = G.lavender)
                            Text(quote.text, style = MaterialTheme.typography.titleMedium)
                        }
                        ReviewStats(target.minusWeeks(1))
                        GlassField(worked, { worked = it }, label = "Ne işe yaradı?", singleLine = false, maxLength = 400)
                        GlassField(didnt, { didnt = it }, label = "Ne yaramadı?", singleLine = false, maxLength = 400)
                        GlassField(change, { change = it }, label = "Bu hafta neyi değiştiriyorum?", singleLine = false, maxLength = 400)
                    }
                    1 -> {
                        Lead("Önce roller, sonra hafta", "Misyonunu oku. Her rolün bu hafta ne istediğini düşün.")
                        GlassCard(onClick = { Nav.push(Screen.Mission) }) {
                            Text("Misyon", style = MaterialTheme.typography.labelMedium, color = G.textSecondary)
                            Text(profile.mission.ifBlank { "Henüz yazılmadı. Dokun ve yaz." }, style = MaterialTheme.typography.bodyLarge)
                        }
                        GlassCard(onClick = { Nav.push(Screen.Roles) }) {
                            Text("Roller (${profile.roles.size}/7)", style = MaterialTheme.typography.labelMedium, color = G.textSecondary)
                            Spacer(Modifier.height(8.dp))
                            if (profile.roles.isEmpty()) Text("Rol yok. Dokun ve ekle.", style = MaterialTheme.typography.bodyLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                profile.roles.forEach { Tag(it, G.mint) }
                            }
                        }
                    }
                    2 -> {
                        Lead("Rol başına 2–3 hedef", "Çoğu II. Kareden olsun: acil değil ama önemli. Acil olan zaten kendini hatırlatır.")
                        (profile.roles.ifEmpty { listOf("") }).forEach { role ->
                            val rg = plan.goals.filter { it.role == role }
                            GlassCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(role.ifBlank { "Genel" }, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                    Text("${rg.size}", style = MaterialTheme.typography.labelMedium, color = if (rg.size in 2..3) G.mint else if (rg.size > 3) G.warning else G.textTertiary)
                                }
                                rg.forEach { g ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(g.title, Modifier.weight(1f).padding(vertical = 8.dp), style = MaterialTheme.typography.bodyLarge)
                                        TextAction("Çıkar", { plan = plan.copy(goals = plan.goals - g) }, color = G.textTertiary)
                                    }
                                }
                                if (rg.size > 3) Text("3'ten fazla hedef odağı dağıtır.", style = MaterialTheme.typography.bodySmall, color = G.warning)
                                TextAction("+ Hedef", { addingFor = role })
                            }
                        }
                    }
                    else -> {
                        Lead("Programla", "Her hedefe bir gün ve kare ver. Önemli olanı takvime koymazsan acil olan yer.")
                        Text("II. Kare: %${q2Pct(plan) ?: 0}", style = MaterialTheme.typography.titleLarge, color = G.mint)
                        plan.goals.forEach { g ->
                            GlassCard {
                                Text(g.title, style = MaterialTheme.typography.titleSmall)
                                if (g.role.isNotBlank()) Text(g.role, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(8.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TR_DAYS.forEachIndexed { i, d -> Chip(d, g.day == i + 1, { plan = plan.upsert(g.copy(day = if (g.day == i + 1) 0 else i + 1)) }) }
                                }
                                Spacer(Modifier.height(6.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    QUADRANTS.forEach { (k, v) -> Chip("${v.first}. Kare", g.quadrant == k, { plan = plan.upsert(g.copy(quadrant = k)) }) }
                                }
                            }
                        }
                        if (plan.goals.isEmpty()) Text("Hedef eklenmedi. Geri dönüp rol başına hedef seç.", style = MaterialTheme.typography.bodyMedium, color = G.warning)
                    }
                }
            }
        }
    }
    addingFor?.let { role ->
        InputDialog(if (role.isBlank()) "Hedef" else "$role için hedef", placeholder = "Ne olursa bu hafta başarılı sayılır?", onConfirm = {
            plan = plan.upsert(RoleGoal(UUID.randomUUID().toString(), role, it))
        }, onDismiss = { addingFor = null })
    }
}

@Composable
fun ReviewStats(anyDay: LocalDate) {
    val weeks by Store.weeks.collectAsState()
    val sessions by Store.sessions.collectAsState()
    val goals by Store.goals.collectAsState()
    val habits by Store.habits.collectAsState()
    val logs by Store.logIndex.collectAsState()
    val today = LocalDate.now()
    val w = weeks[weekKey(anyDay)]
    val start = weekStart(anyDay)
    var sched = 0
    var done = 0
    habits.filter { !it.archived }.forEach { h ->
        for (i in 0..6) {
            val d = start.plusDays(i.toLong())
            if (d.isAfter(today)) break
            if (h.isScheduled(d) && !(d == today && !logs.status(h.id, d).isDone())) {
                sched++
                if (logs.status(h.id, d).isDone()) done++
            }
        }
    }
    val target = goals.firstOrNull { it.quarter == quarterKey(anyDay) }?.leadMeasure?.weeklyTarget
    val hours = deepHours(sessions, anyDay)
    GlassCard {
        Row {
            BigNumber(if (sched == 0) "—" else "%${done * 100 / sched}", "Alışkanlık", Modifier.weight(1f), G.mint)
            BigNumber("${"%.1f".format(hours)}${target?.let { "/" + it.roundToInt() } ?: ""}", "Derin saat", Modifier.weight(1f))
            BigNumber("${w?.goals?.count { it.done } ?: 0}/${w?.goals?.size ?: 0}", "Hedef", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Text("II. Kare: ${q2Pct(w)?.let { "%$it" } ?: "—"}", style = MaterialTheme.typography.bodySmall)
    }
}

/** Haftalık gözden geçirme ve hesap (icraat takvimi). */
@Composable
fun WeeklyReviewScreen() {
    val today = LocalDate.now()
    val key = weekKey(today)
    val weeks by Store.weeks.collectAsState()
    val week = weeks[key] ?: Week(key)
    var worked by rememberSaveable { mutableStateOf(week.review.worked) }
    var didnt by rememberSaveable { mutableStateOf(week.review.didnt) }
    var change by rememberSaveable { mutableStateOf(week.review.change) }
    SubPage("Haftalık gözden geçirme", subtitle = weekRange(today), bottom = {
        PrimaryButton("Kaydet", {
            Store.saveWeek(week.copy(review = Review(worked, didnt, change)))
            Nav.toast("Gözden geçirme kaydedildi")
            Nav.pop()
        }, Modifier.fillMaxWidth(), icon = GelisioIcons.check)
    }) {
        Lead("Haftalık hesap", "Skor cetveline bak: kılavuz ölçütü tuttun mu? Sonuç değil, kontrolündeki girdi.")
        ReviewStats(today)
        if (week.goals.isNotEmpty()) {
            GlassCard {
                week.goals.forEach { g ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        StatusDot(if (g.done) com.omeronur.gelisio.data.LogStatus.FULL else null, size = 22.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(g.title, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        GlassField(worked, { worked = it }, label = "Ne işe yaradı?", singleLine = false, maxLength = 400)
        GlassField(didnt, { didnt = it }, label = "Ne yaramadı?", singleLine = false, maxLength = 400)
        GlassField(change, { change = it }, label = "Neyi değiştiriyorum?", singleLine = false, maxLength = 400)
    }
}

/** Zaman yönetimi matrisi: bu haftanın görevleri 4 karede. */
@Composable
fun MatrixScreen() {
    val today = LocalDate.now()
    val key = weekKey(today)
    val weeks by Store.weeks.collectAsState()
    val week = weeks[key] ?: Week(key)
    var editing by remember { mutableStateOf<RoleGoal?>(null) }
    var addingQ by remember { mutableStateOf<Int?>(null) }
    SubPage("Zaman yönetimi matrisi", subtitle = "II. Kare: ${q2Pct(week)?.let { "%$it" } ?: "—"}") {
        Text("Hedef: zamanın çoğu II. Karede (acil değil, önemli). I. Kare küçüldükçe kriz azalır; III ve IV kesilecek yerdir.", style = MaterialTheme.typography.bodySmall)
        listOf(listOf(1, 2), listOf(3, 4)).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { q ->
                    val items = week.goals.filter { it.quadrant == q }
                    GlassCard(Modifier.weight(1f).heightIn(min = 170.dp), strong = q == 2, padding = 14.dp) {
                        Text("${QUADRANTS[q]?.first}. Kare", style = MaterialTheme.typography.titleSmall, color = quadColor(q))
                        Text(QUADRANTS[q]?.second.orEmpty(), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        items.forEach { g ->
                            Text("• ${g.title}", Modifier.fillMaxWidth().pressable({ editing = g }).padding(vertical = 4.dp),
                                style = MaterialTheme.typography.bodyMedium, color = if (g.done) G.textTertiary else G.text, maxLines = 2)
                        }
                        TextAction("+ Ekle", { addingQ = q })
                    }
                }
            }
        }
        Divider()
        Text("Görevler haftalık plana yazılır; Hafta sekmesinde rolleriyle görünür.", style = MaterialTheme.typography.bodySmall)
    }
    editing?.let { g -> RoleGoalDialog(g, onSave = { Store.saveWeek(week.upsert(it)) }, onDelete = { Store.saveWeek(week.copy(goals = week.goals.filter { x -> x.id != g.id })) }, onDismiss = { editing = null }) }
    addingQ?.let { q ->
        InputDialog("${QUADRANTS[q]?.first}. Kare görevi", onConfirm = {
            Store.saveWeek(week.upsert(RoleGoal(UUID.randomUUID().toString(), "", it, quadrant = q)))
        }, onDismiss = { addingQ = null })
    }
}
