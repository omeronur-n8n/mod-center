@file:OptIn(ExperimentalLayoutApi::class)

package com.omeronur.gelisio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.omeronur.gelisio.data.Dimension
import com.omeronur.gelisio.data.LeadMeasure
import com.omeronur.gelisio.data.Milestone
import com.omeronur.gelisio.data.Output
import com.omeronur.gelisio.data.PrincipleContext
import com.omeronur.gelisio.data.Project
import com.omeronur.gelisio.data.Quotes
import com.omeronur.gelisio.data.Session
import com.omeronur.gelisio.data.Store
import com.omeronur.gelisio.data.deepHours
import com.omeronur.gelisio.data.hobbyLevelUpReady
import com.omeronur.gelisio.data.milestonePct
import com.omeronur.gelisio.data.practiceMinutes
import com.omeronur.gelisio.data.weekKey
import com.omeronur.gelisio.data.weekStart
import com.omeronur.gelisio.data.weeklyPractice
import com.omeronur.gelisio.ui.HabitTemplate
import com.omeronur.gelisio.ui.Nav
import com.omeronur.gelisio.ui.Screen
import com.omeronur.gelisio.ui.SubPage
import com.omeronur.gelisio.ui.TabPage
import com.omeronur.gelisio.ui.components.Bars
import com.omeronur.gelisio.ui.components.BigNumber
import com.omeronur.gelisio.ui.components.Chip
import com.omeronur.gelisio.ui.components.ConfirmDialog
import com.omeronur.gelisio.ui.components.DateField
import com.omeronur.gelisio.ui.components.Divider
import com.omeronur.gelisio.ui.components.EditableList
import com.omeronur.gelisio.ui.components.GlassButton
import com.omeronur.gelisio.ui.components.GlassCard
import com.omeronur.gelisio.ui.components.GlassField
import com.omeronur.gelisio.ui.components.IconBadge
import com.omeronur.gelisio.ui.components.InputDialog
import com.omeronur.gelisio.ui.components.NavRow
import com.omeronur.gelisio.ui.components.PrimaryButton
import com.omeronur.gelisio.ui.components.ProgressRing
import com.omeronur.gelisio.ui.components.SectionTitle
import com.omeronur.gelisio.ui.components.Stepper
import com.omeronur.gelisio.ui.components.Tag
import com.omeronur.gelisio.ui.components.TextAction
import com.omeronur.gelisio.ui.components.pressable
import com.omeronur.gelisio.ui.components.trLong
import com.omeronur.gelisio.ui.components.trShort
import com.omeronur.gelisio.ui.icons.GelisioIcons
import com.omeronur.gelisio.ui.theme.G
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** Gönüllü yalnızlık ve yüksek kaliteli boş zaman şablonları (Dijital Minimalizm). */
val SOLITUDE_TEMPLATES = listOf(
    HabitTemplate("Telefonsuz yürüyüş", "Telefonu evde bırakıp 30 dakika yürümek", "Telefonsuz kapının önüne çıkmak"),
    HabitTemplate("Kendine mektup", "Haftada bir kendime düşüncelerimi yazmak", "Bir cümle yazmak"),
    HabitTemplate("Okuma", "30 dakika kâğıt kitap okumak", "1 sayfa okumak"),
    HabitTemplate("El becerisi", "Elle bir şey üretmek: çizim, tamir, yemek", "Malzemeyi masaya koymak"),
    HabitTemplate("Yüz yüze buluşma", "Haftada bir arkadaşla yüz yüze görüşmek", "Bir arkadaşa buluşma mesajı atmak"),
)

@Composable
fun GrowthTab() {
    val today = LocalDate.now()
    val projects by Store.projects.collectAsState()
    val sessions by Store.sessions.collectAsState()
    val habits by Store.habits.collectAsState()
    val logs by Store.logIndex.collectAsState()
    val goals by Store.goals.collectAsState()
    val custom by Store.quotes.collectAsState()
    val ctx = LocalContext.current
    val quote = remember(custom) { Quotes.pick(ctx, PrincipleContext.HOBBY, custom) }
    val active = projects.filter { !it.archived }

    TabPage("Gelişim", subtitle = "Projeler ve hobiler") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Proje", { Nav.push(Screen.ProjectEditor(kind = "project")) }, Modifier.weight(1f), icon = GelisioIcons.project)
            GlassButton("Hobi", { Nav.push(Screen.ProjectEditor(kind = "hobby")) }, Modifier.weight(1f), icon = GelisioIcons.hobby, color = G.mint)
        }

        SectionTitle("Projeler")
        val ps = active.filter { !it.isHobby }
        if (ps.isEmpty()) Text("Proje bir hedefe hizmet eder ve bittiği gün kapanır.", style = MaterialTheme.typography.bodySmall)
        ps.forEach { p ->
            val target = p.leadMeasure.weeklyTarget
            val h = deepHours(sessions, today, p.id)
            GlassCard(onClick = { Nav.push(Screen.ProjectDetail(p.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(milestonePct(p).toFloat(), diameter = 62.dp, strokeWidth = 7.dp) {
                        Text("%${(milestonePct(p) * 100).roundToInt()}", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.title, style = MaterialTheme.typography.titleMedium)
                        goals.firstOrNull { it.id == p.goalId }?.let { Text(it.title, style = MaterialTheme.typography.bodySmall, maxLines = 1) }
                        Spacer(Modifier.height(4.dp))
                        Text("${p.milestones.count { it.done }}/${p.milestones.size} taş · bu hafta ${"%.1f".format(h)}/${"%.0f".format(target)} sa", style = MaterialTheme.typography.bodySmall, color = G.textSecondary)
                    }
                    p.dueDate?.let { Tag(it.trShort(), if (it.isBefore(today)) G.critical else G.textSecondary) }
                }
            }
        }

        SectionTitle("Hobiler")
        val hs = active.filter { it.isHobby }
        if (hs.isEmpty()) Text("Hobi bitmez, seviye atlar. Boş zaman tüketerek değil, üreterek dolar.", style = MaterialTheme.typography.bodySmall)
        hs.forEach { p ->
            val wk = weeklyPractice(p, today, sessions, habits, logs)
            GlassCard(onClick = { Nav.push(Screen.ProjectDetail(p.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(wk.toFloat() / p.weeklyPracticeMin.coerceAtLeast(1), diameter = 62.dp, strokeWidth = 7.dp) {
                        Text("${p.skillLevel}", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.title, style = MaterialTheme.typography.titleMedium)
                        Text("Seviye ${p.skillLevel}/10 · ${p.dimension.label}", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(6.dp))
                        Heat(p, 4, sessions, habits, logs, today, cell = 9)
                    }
                    if (hobbyLevelUpReady(p, today, sessions, habits, logs)) Tag("Seviye atla", G.mint)
                }
            }
        }

        GlassCard {
            Text("İlke: ${quote.source}", style = MaterialTheme.typography.labelMedium, color = G.lavender)
            Spacer(Modifier.height(6.dp))
            Text(quote.text, style = MaterialTheme.typography.titleMedium)
        }

        SectionTitle("Gönüllü yalnızlık şablonları")
        GlassCard(padding = 10.dp) {
            SOLITUDE_TEMPLATES.forEachIndexed { i, t ->
                NavRow(GelisioIcons.digitalDetox, t.name, t.full, onClick = { Nav.push(Screen.HabitWizard(template = t)) })
                if (i < SOLITUDE_TEMPLATES.lastIndex) Divider()
            }
        }
        val archived = projects.count { it.archived }
        if (archived > 0) Text("$archived arşivlenmiş kayıt var.", style = MaterialTheme.typography.bodySmall, color = G.textTertiary)
    }
}

/** Isı haritası: günlük pratik dakikası (seans + bağlı alışkanlık). */
@Composable
fun Heat(
    p: Project,
    weeks: Int,
    sessions: List<Session>,
    habits: List<com.omeronur.gelisio.data.Habit>,
    logs: com.omeronur.gelisio.data.LogIndex,
    today: LocalDate,
    cell: Int = 14,
) {
    val start = weekStart(today).minusWeeks((weeks - 1).toLong())
    val dailyTarget = (p.weeklyPracticeMin / 7.0).coerceAtLeast(5.0)
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        for (w in 0 until weeks) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                for (d in 0 until 7) {
                    val date = start.plusDays((w * 7 + d).toLong())
                    val m = if (date.isAfter(today)) -1 else practiceMinutes(p, date, sessions, habits, logs)
                    val a = when {
                        m < 0 -> 0.03f
                        m == 0 -> 0.08f
                        else -> (0.3f + 0.7f * (m / dailyTarget).toFloat()).coerceAtMost(1f)
                    }
                    Box(Modifier.width(cell.dp).height(cell.dp).clip(RoundedCornerShape(3.dp)).background(if (m > 0) G.mint.copy(alpha = a) else Color.White.copy(alpha = a)))
                }
            }
        }
    }
}

@Composable
fun ProjectEditorScreen(projectId: String?, kind: String) {
    val projects by Store.projects.collectAsState()
    val goals by Store.goals.collectAsState()
    val identities by Store.identities.collectAsState()
    val profile by Store.profile.collectAsState()
    val existing = projects.firstOrNull { it.id == projectId }
    var k by rememberSaveable { mutableStateOf(existing?.kind ?: kind) }
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var goalId by rememberSaveable { mutableStateOf(existing?.goalId ?: "") }
    var role by rememberSaveable { mutableStateOf(existing?.role ?: "") }
    var milestones by remember { mutableStateOf(existing?.milestones ?: emptyList()) }
    var weekly by remember { mutableIntStateOf(existing?.leadMeasure?.weeklyTarget?.roundToInt() ?: 5) }
    var due by remember { mutableStateOf(existing?.dueDate) }
    var identityId by rememberSaveable { mutableStateOf(existing?.identityId ?: "") }
    var dim by remember { mutableStateOf(existing?.dimension ?: Dimension.MENTAL) }
    var level by remember { mutableIntStateOf(existing?.skillLevel ?: 1) }
    var practice by remember { mutableIntStateOf(existing?.weeklyPracticeMin ?: 120) }
    var outputType by rememberSaveable { mutableStateOf(existing?.outputType ?: "") }
    val hobby = k == "hobby"

    SubPage(if (existing == null) (if (hobby) "Yeni hobi" else "Yeni proje") else "Düzenle", bottom = {
        PrimaryButton("Kaydet", {
            val id = Store.saveProject(
                (existing ?: Project()).copy(
                    kind = k, title = title, goalId = goalId, role = role, milestones = milestones,
                    leadMeasure = LeadMeasure("derin saat", weekly.toDouble()), dueDate = due,
                    identityId = identityId, dimension = dim, skillLevel = level, weeklyPracticeMin = practice, outputType = outputType,
                ),
            )
            Nav.pop()
            if (existing == null) Nav.push(Screen.ProjectDetail(id))
        }, Modifier.fillMaxWidth(), enabled = title.isNotBlank(), icon = GelisioIcons.check)
    }) {
        if (existing == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Proje", !hobby, { k = "project" }, icon = GelisioIcons.project)
                Chip("Hobi", hobby, { k = "hobby" }, icon = GelisioIcons.hobby)
            }
        }
        GlassField(title, { title = it }, label = "Ad", placeholder = if (hobby) "Suluboya" else "Excentri lansmanı", maxLength = 80)
        if (!hobby) {
            if (goals.isNotEmpty()) {
                Text("Hizmet ettiği hayati hedef", style = MaterialTheme.typography.labelMedium, color = G.textSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Yok", goalId.isBlank(), { goalId = "" })
                    goals.forEach { g -> Chip("${g.quarter} · ${g.title}", goalId == g.id, { goalId = g.id }) }
                }
            }
            if (profile.roles.isNotEmpty()) {
                Text("Rol", style = MaterialTheme.typography.labelMedium, color = G.textSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    profile.roles.forEach { r -> Chip(r, role == r, { role = if (role == r) "" else r }) }
                }
            }
            SectionTitle("Kilometre taşları")
            EditableList(milestones.map { it.title }, { titles ->
                milestones = titles.map { t -> milestones.firstOrNull { it.title == t } ?: Milestone(t) }
            }, "Taş ekle", "Teklif şablonu hazır")
            GlassCard {
                Text("Kılavuz ölçüt", style = MaterialTheme.typography.titleSmall)
                Text("Bu projeye haftada kaç derin saat?", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(10.dp))
                Stepper("$weekly saat", { weekly = (weekly - 1).coerceAtLeast(1) }, { weekly = (weekly + 1).coerceAtMost(60) })
            }
            DateField("Bitiş tarihi", due, { due = it })
        } else {
            if (identities.isNotEmpty()) {
                Text("Kimlik", style = MaterialTheme.typography.labelMedium, color = G.textSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    identities.forEach { i -> Chip(i.statement, identityId == i.id, { identityId = if (identityId == i.id) "" else i.id }) }
                }
            }
            Text("Boyut", style = MaterialTheme.typography.labelMedium, color = G.textSecondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Dimension.entries.forEach { d -> Chip(d.label, dim == d, { dim = d }) }
            }
            GlassCard {
                Text("Beceri seviyesi", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Stepper("$level / 10", { level = (level - 1).coerceAtLeast(1) }, { level = (level + 1).coerceAtMost(10) })
                Spacer(Modifier.height(14.dp))
                Text("Haftalık pratik hedefi", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Stepper("$practice dk", { practice = (practice - 15).coerceAtLeast(15) }, { practice = (practice + 15).coerceAtMost(1200) })
            }
            GlassField(outputType, { outputType = it }, label = "Üretim türü", placeholder = "Tablo, eskiz, tarif…", maxLength = 40)
        }
    }
}

@Composable
fun ProjectDetailScreen(projectId: String) {
    val today = LocalDate.now()
    val projects by Store.projects.collectAsState()
    val sessions by Store.sessions.collectAsState()
    val habits by Store.habits.collectAsState()
    val logs by Store.logIndex.collectAsState()
    val goals by Store.goals.collectAsState()
    val outputs by Store.outputs.collectAsState()
    val identities by Store.identities.collectAsState()
    val p = projects.firstOrNull { it.id == projectId } ?: run {
        SubPage("Kayıt") { Text("Bu kayıt silinmiş.", style = MaterialTheme.typography.bodyLarge) }
        return
    }
    var addMilestone by remember { mutableStateOf(false) }
    var addOutput by remember { mutableStateOf(false) }
    var practiceOpen by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    val linked = habits.filter { it.projectId == p.id && !it.archived }

    SubPage(p.title, subtitle = if (p.isHobby) "Hobi · ${p.dimension.label}" else goals.firstOrNull { it.id == p.goalId }?.title ?: "Proje", actions = {
        com.omeronur.gelisio.ui.components.IconGlassButton(GelisioIcons.edit, { Nav.push(Screen.ProjectEditor(p.id, p.kind)) })
    }) {
        if (!p.isHobby) {
            val target = p.leadMeasure.weeklyTarget
            val h = deepHours(sessions, today, p.id)
            GlassCard(strong = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(milestonePct(p).toFloat(), diameter = 110.dp, strokeWidth = 11.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("%${(milestonePct(p) * 100).roundToInt()}", style = MaterialTheme.typography.headlineMedium)
                            Text("sonuç", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.width(18.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BigNumber("${p.milestones.count { it.done }}/${p.milestones.size}", "Kilometre taşı")
                        BigNumber("${"%.1f".format(h)}/${"%.0f".format(target)}", "Bu hafta derin saat", color = if (h >= target) G.mint else G.text)
                    }
                }
                p.dueDate?.let {
                    Spacer(Modifier.height(10.dp))
                    val left = ChronoUnit.DAYS.between(today, it)
                    Text("Bitiş: ${it.trLong()} · ${if (left >= 0) "$left gün kaldı" else "${-left} gün geçti"}", style = MaterialTheme.typography.bodySmall, color = if (left < 0) G.critical else G.textSecondary)
                }
            }
            SectionTitle("Süreç · haftalık derin saat")
            GlassCard {
                val weeks = (7 downTo 0).map { today.minusWeeks(it.toLong()) }
                Bars(weeks.map { deepHours(sessions, it, p.id) }, weeks.map { weekKey(it).substringAfter("W") }, target)
                Spacer(Modifier.height(8.dp))
                Text("Sonuç gecikmeli gelir; çubuklar kontrolündeki girdiyi gösterir.", style = MaterialTheme.typography.bodySmall)
            }
            PrimaryButton("Bu projeye derin seans", { Nav.push(Screen.DeepSession(p.id)) }, Modifier.fillMaxWidth(), icon = GelisioIcons.timer)
            SectionTitle("Kilometre taşları", action = "Ekle", onAction = { addMilestone = true })
            GlassCard(padding = 10.dp) {
                if (p.milestones.isEmpty()) Text("Taş yok.", Modifier.padding(8.dp), style = MaterialTheme.typography.bodyMedium, color = G.textSecondary)
                p.milestones.forEachIndexed { i, m ->
                    Row(
                        Modifier.fillMaxWidth().pressable({
                            Store.saveProject(p.copy(milestones = p.milestones.mapIndexed { j, x -> if (j == i) x.copy(done = !x.done) else x }))
                        }).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StatusDot(if (m.done) com.omeronur.gelisio.data.LogStatus.FULL else null, size = 24.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(m.title, style = MaterialTheme.typography.bodyLarge, color = if (m.done) G.textSecondary else G.text)
                    }
                    if (i < p.milestones.lastIndex) Divider()
                }
            }
        } else {
            val wk = weeklyPractice(p, today, sessions, habits, logs)
            GlassCard(strong = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(wk.toFloat() / p.weeklyPracticeMin.coerceAtLeast(1), diameter = 110.dp, strokeWidth = 11.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$wk", style = MaterialTheme.typography.headlineMedium)
                            Text("/ ${p.weeklyPracticeMin} dk", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.width(18.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BigNumber("${p.skillLevel}/10", "Beceri seviyesi", color = G.mint)
                        BigNumber("${outputs.count { it.projectId == p.id }}", "Üretim")
                    }
                }
                identities.firstOrNull { it.id == p.identityId }?.let {
                    Spacer(Modifier.height(10.dp))
                    Tag(it.statement, G.lavender)
                }
            }
            if (hobbyLevelUpReady(p, today, sessions, habits, logs)) {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(GelisioIcons.levelUp, G.mint)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Seviye atlama önerisi", style = MaterialTheme.typography.titleSmall)
                            Text("4 hafta üst üste pratik hedefini tuttun.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    TextAction("Seviye ${p.skillLevel + 1}'e geç", {
                        Store.saveProject(p.copy(skillLevel = p.skillLevel + 1))
                        Nav.toast("Seviye ${p.skillLevel + 1}")
                    })
                }
            }
            SectionTitle("Pratik · son 12 hafta")
            GlassCard {
                Heat(p, 12, sessions, habits, logs, today, cell = 17)
                Spacer(Modifier.height(10.dp))
                Text("Bağlı alışkanlık tamamlaması 15 dk sayılır.", style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Pratik kaydet", { practiceOpen = true }, Modifier.weight(1f), icon = GelisioIcons.add)
                GlassButton("Sayaçla", { Nav.push(Screen.DeepSession(p.id)) }, Modifier.weight(1f), icon = GelisioIcons.timer)
            }
            SectionTitle("Üretim galerisi", action = "Ekle", onAction = { addOutput = true })
            val outs = outputs.filter { it.projectId == p.id }
            if (outs.isEmpty()) Text("Ürettiğin her şeyi yaz: ${p.outputType.ifBlank { "eser" }}, tarih, not.", style = MaterialTheme.typography.bodySmall)
            outs.forEach { o ->
                GlassCard(padding = 14.dp, onLongClick = { Store.deleteOutput(o.id) }) {
                    Text(o.date.trLong(), style = MaterialTheme.typography.labelMedium, color = G.textSecondary)
                    Text(o.note, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        if (linked.isNotEmpty()) {
            SectionTitle("Bağlı alışkanlıklar")
            GlassCard(padding = 10.dp) {
                linked.forEachIndexed { i, h ->
                    NavRow(GelisioIcons.streak, h.name, h.fullVersion, onClick = { Nav.push(Screen.HabitDetail(h.id)) })
                    if (i < linked.lastIndex) Divider()
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton(if (p.archived) "Arşivden çıkar" else if (p.isHobby) "Arşivle" else "Projeyi kapat", {
                Store.saveProject(p.copy(archived = !p.archived)); Nav.pop()
            }, Modifier.weight(1f))
            GlassButton("Sil", { confirm = true }, Modifier.weight(1f), icon = GelisioIcons.delete, color = G.critical)
        }
    }
    if (addMilestone) InputDialog("Kilometre taşı", placeholder = "İlk müşteri görüşmesi", onConfirm = {
        Store.saveProject(p.copy(milestones = p.milestones + Milestone(it)))
    }, onDismiss = { addMilestone = false })
    if (addOutput) InputDialog("Üretim notu", placeholder = "${p.outputType.ifBlank { "Eser" }}: ne yaptın?", singleLine = false, onConfirm = {
        Store.addOutput(Output(projectId = p.id, note = it, date = today))
    }, onDismiss = { addOutput = false })
    if (practiceOpen) {
        var mins by remember { mutableIntStateOf(30) }
        AlertDialog(
            onDismissRequest = { practiceOpen = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color(0xFF161B28),
            title = { Text("Bugünkü pratik", style = MaterialTheme.typography.titleLarge) },
            text = { Stepper("$mins dk", { mins = (mins - 5).coerceAtLeast(5) }, { mins = (mins + 5).coerceAtMost(600) }) },
            confirmButton = {
                TextAction("Kaydet", {
                    val start = today.atTime(java.time.LocalTime.now()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - mins * 60_000L
                    Store.addSession(Session(projectId = p.id, startMillis = start, minutes = mins, kind = "practice"))
                    practiceOpen = false
                    Nav.toast("$mins dk pratik yazıldı")
                })
            },
            dismissButton = { TextAction("Vazgeç", { practiceOpen = false }, color = G.textSecondary) },
        )
    }
    if (confirm) ConfirmDialog("Sil", "Seanslar ve üretim kayıtları kalır; kayıt silinir.", onConfirm = { Store.deleteProject(p.id); Nav.pop() }, onDismiss = { confirm = false })
}

@Suppress("unused")
private fun centered() = TextAlign.Center

@Suppress("unused")
private val ratio = Modifier.aspectRatio(1f)
