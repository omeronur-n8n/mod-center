@file:OptIn(ExperimentalLayoutApi::class)

package com.omeronur.gelisio.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.omeronur.gelisio.data.Cell
import com.omeronur.gelisio.data.Cue
import com.omeronur.gelisio.data.Goldilocks
import com.omeronur.gelisio.data.Habit
import com.omeronur.gelisio.data.Identity
import com.omeronur.gelisio.data.LogStatus
import com.omeronur.gelisio.data.ScoreItem
import com.omeronur.gelisio.data.Store
import com.omeronur.gelisio.data.TR_DAYS
import com.omeronur.gelisio.data.cell
import com.omeronur.gelisio.data.consistency
import com.omeronur.gelisio.data.goldilocks
import com.omeronur.gelisio.data.reps
import com.omeronur.gelisio.data.stageOf
import com.omeronur.gelisio.data.status
import com.omeronur.gelisio.data.streak
import com.omeronur.gelisio.data.votes
import com.omeronur.gelisio.ui.HabitTemplate
import com.omeronur.gelisio.ui.Nav
import com.omeronur.gelisio.ui.Screen
import com.omeronur.gelisio.ui.SubPage
import com.omeronur.gelisio.ui.components.BigNumber
import com.omeronur.gelisio.ui.components.Chip
import com.omeronur.gelisio.ui.components.ConfirmDialog
import com.omeronur.gelisio.ui.components.DaysPicker
import com.omeronur.gelisio.ui.components.Divider
import com.omeronur.gelisio.ui.components.GlassButton
import com.omeronur.gelisio.ui.components.GlassCard
import com.omeronur.gelisio.ui.components.GlassField
import com.omeronur.gelisio.ui.components.IconGlassButton
import com.omeronur.gelisio.ui.components.InputDialog
import com.omeronur.gelisio.ui.components.PrimaryButton
import com.omeronur.gelisio.ui.components.SectionTitle
import com.omeronur.gelisio.ui.components.Tag
import com.omeronur.gelisio.ui.components.TextAction
import com.omeronur.gelisio.ui.components.TimeField
import com.omeronur.gelisio.ui.components.ToggleRow
import com.omeronur.gelisio.ui.components.pressable
import com.omeronur.gelisio.ui.components.trShort
import com.omeronur.gelisio.ui.icons.GelisioIcons
import com.omeronur.gelisio.ui.theme.G
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun HabitDetailScreen(habitId: String) {
    val habits by Store.habits.collectAsState()
    val logs by Store.logIndex.collectAsState()
    val identities by Store.identities.collectAsState()
    val h = habits.firstOrNull { it.id == habitId } ?: run {
        SubPage("Alışkanlık") { Text("Bu alışkanlık silinmiş.", style = MaterialTheme.typography.bodyLarge) }
        return
    }
    val today = LocalDate.now()
    val identity = identities.firstOrNull { it.id == h.identityId }
    val c30 = consistency(h, logs, today)
    val r = reps(h, logs)
    val stage = stageOf(r)
    var confirmDelete by remember { mutableStateOf(false) }

    SubPage(h.name, subtitle = identity?.statement, actions = {
        IconGlassButton(GelisioIcons.edit, { Nav.push(Screen.HabitWizard(h.id)) })
    }) {
        GlassCard(strong = true) {
            Row {
                BigNumber(c30?.let { "%${(it * 100).roundToInt()}" } ?: "—", "Tutarlılık · 30 gün", Modifier.weight(1f), G.mint)
                BigNumber("${streak(h, logs, today)}", "Seri (kırılgan)", Modifier.weight(1f))
                BigNumber("$r", "Tekrar", Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Tag(stage.label, G.mint)
                Spacer(Modifier.width(8.dp))
                Text(stage.range + " · tekrar süreden önemlidir", style = MaterialTheme.typography.bodySmall)
            }
        }

        when (goldilocks(h, logs, today)) {
            Goldilocks.LEVEL_UP -> GlassCard {
                Text("Goldilocks: seviye atla", style = MaterialTheme.typography.titleSmall, color = G.mint)
                Text("Son 14 günde %95+. Tam sürümü bir kademe zorlaştır.", style = MaterialTheme.typography.bodySmall)
                TextAction("Zorluğu artır", {
                    Store.saveHabit(h.copy(difficulty = h.difficulty + 1, goldilocksAt = today))
                    Nav.push(Screen.HabitWizard(h.id))
                })
            }
            Goldilocks.SHRINK -> GlassCard {
                Text("Goldilocks: küçült", style = MaterialTheme.typography.titleSmall, color = G.warning)
                Text("Son 14 günde %60 altı. 2 dakikalık sürüme in; önce zinciri tut.", style = MaterialTheme.typography.bodySmall)
                TextAction("Tamam, küçültüyorum", {
                    Store.saveHabit(h.copy(goldilocksAt = today))
                    Nav.push(Screen.HabitWizard(h.id))
                })
            }
            null -> Unit
        }

        SectionTitle("Zincir · son 5 hafta")
        GlassCard {
            Row(Modifier.fillMaxWidth()) {
                TR_DAYS.forEach { Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = G.textTertiary, textAlign = TextAlign.Center) }
            }
            Spacer(Modifier.height(8.dp))
            val start = com.omeronur.gelisio.data.weekStart(today).minusWeeks(4)
            for (w in 0 until 5) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (d in 0 until 7) {
                        val date = start.plusDays((w * 7 + d).toLong())
                        val c = cell(h, logs, date, today)
                        val editable = !date.isAfter(today) && c != Cell.OFF
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(8.dp)).background(cellColor(c))
                                .pressable(if (editable) ({ cycle(h, date, logs.status(h.id, date)) }) else null, pressedScale = 0.9f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${date.dayOfMonth}", style = MaterialTheme.typography.labelSmall,
                                color = if (c == Cell.FULL || c == Cell.MISS || c == Cell.MISS_TWICE) G.bgBottom else G.textTertiary)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text("Geçmiş güne dokun: tam → minimum → atlandı → boş. Sarı: tek kaçırma, kırmızı: art arda ikinci.", style = MaterialTheme.typography.bodySmall)
        }

        SectionTitle("Tasarım")
        GlassCard {
            Info("Tam sürüm", h.fullVersion)
            Divider()
            Info("2 dakikalık sürüm", h.twoMinVersion)
            Divider()
            Info(if (h.cue.type == "stack") "İstifleme" else "Uygulamaya koyma niyeti", cueText(h, habits))
            if (h.reward.isNotBlank()) {
                Divider()
                Info("Ödül", h.reward)
            }
            Divider()
            Info("Günler", if (h.days.size == 7) "Her gün" else h.days.joinToString(" ") { TR_DAYS[it - 1] })
            Divider()
            Info("Zorluk", "Kademe ${h.difficulty}")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton(if (h.archived) "Arşivden çıkar" else "Arşivle", { Store.saveHabit(h.copy(archived = !h.archived)); Nav.pop() }, Modifier.weight(1f))
            GlassButton("Sil", { confirmDelete = true }, Modifier.weight(1f), icon = GelisioIcons.delete, color = G.critical)
        }
    }
    if (confirmDelete) {
        ConfirmDialog("Alışkanlığı sil", "Tüm kayıtları da silinir. Bunun yerine arşivlemek geçmişi korur.", onConfirm = {
            Store.deleteHabit(h.id)
            Nav.pop()
        }, onDismiss = { confirmDelete = false })
    }
}

private fun cycle(h: Habit, date: LocalDate, s: LogStatus?) {
    val next = when (s) {
        null -> LogStatus.FULL
        LogStatus.FULL -> LogStatus.MIN
        LogStatus.MIN -> LogStatus.SKIP
        LogStatus.SKIP -> null
    }
    Store.setLog(h.id, date, next)
}

@Composable
fun Info(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = G.textSecondary)
        Spacer(Modifier.height(2.dp))
        Text(value.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * 6 adımlı alışkanlık kurulumu (dört yasa sırasıyla):
 * 1 kimlik · 2 değer filtresi · 3 tam + 2 dakikalık sürüm · 4 işaret · 5 ödül · 6 özet.
 */
@Composable
fun HabitWizardScreen(habitId: String?, template: HabitTemplate?) {
    val habits by Store.habits.collectAsState()
    val identities by Store.identities.collectAsState()
    val projects by Store.projects.collectAsState()
    val existing = habits.firstOrNull { it.id == habitId }

    var step by rememberSaveable { mutableIntStateOf(0) }
    var identityId by rememberSaveable { mutableStateOf(existing?.identityId ?: "") }
    var newIdentity by rememberSaveable { mutableStateOf("") }
    var reality by rememberSaveable { mutableStateOf(true) }
    var constructive by rememberSaveable { mutableStateOf(true) }
    var controllable by rememberSaveable { mutableStateOf(true) }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: template?.name ?: "") }
    var full by rememberSaveable { mutableStateOf(existing?.fullVersion ?: template?.full ?: "") }
    var twoMin by rememberSaveable { mutableStateOf(existing?.twoMinVersion ?: template?.twoMin ?: "") }
    var cueType by rememberSaveable { mutableStateOf(existing?.cue?.type ?: "time") }
    var time by rememberSaveable { mutableStateOf(existing?.cue?.time ?: "") }
    var place by rememberSaveable { mutableStateOf(existing?.cue?.place ?: template?.place ?: "") }
    var after by rememberSaveable { mutableStateOf(existing?.cue?.afterHabitId ?: "") }
    var reward by rememberSaveable { mutableStateOf(existing?.reward ?: "") }
    var days by rememberSaveable { mutableStateOf(existing?.days ?: listOf(1, 2, 3, 4, 5, 6, 7)) }
    var projectId by rememberSaveable { mutableStateOf(existing?.projectId ?: "") }

    val titles = listOf("Kimlik", "Değer filtresi", "Sürümler", "İşaret", "Ödül", "Özet")
    val valid = when (step) {
        0 -> identityId.isNotBlank() || newIdentity.isNotBlank()
        2 -> name.isNotBlank() && full.isNotBlank() && twoMin.isNotBlank()
        3 -> if (cueType == "time") time.isNotBlank() && place.isNotBlank() else after.isNotBlank()
        else -> true
    }

    SubPage(
        if (existing == null) "Yeni alışkanlık" else "Alışkanlığı düzenle",
        subtitle = "Adım ${step + 1}/6 · ${titles[step]}",
        bottom = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (step > 0) GlassButton("Geri", { step-- }, Modifier.weight(1f))
                if (step < 5) {
                    PrimaryButton("Devam", { step++ }, Modifier.weight(2f), enabled = valid)
                } else {
                    PrimaryButton("Kaydet", {
                        val idId = if (identityId.isBlank()) Store.saveIdentity(Identity(statement = newIdentity)) else identityId
                        Store.saveHabit(
                            (existing ?: Habit()).copy(
                                name = name, identityId = idId, fullVersion = full, twoMinVersion = twoMin,
                                cue = Cue(cueType, if (cueType == "time") time else "", if (cueType == "time") place else "", if (cueType == "stack") after else ""),
                                reward = reward, days = days, projectId = projectId,
                            ),
                        )
                        Nav.toast(if (existing == null) "Alışkanlık kuruldu" else "Kaydedildi")
                        Nav.pop()
                    }, Modifier.weight(2f), icon = GelisioIcons.check)
                }
            }
        },
    ) {
        StepBar(step, 6)
        AnimatedContent(step, transitionSpec = { fadeIn(G.motion()) togetherWith fadeOut(G.motion(200)) }, label = "step") { s ->
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (s) {
                    0 -> {
                        Lead("Kimlik", "Sonuç değil, olmak istediğin kişi. Her tekrar bu kimliğe verilmiş bir oy.")
                        if (identities.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                identities.forEach { i -> Chip(i.statement, i.id == identityId, { identityId = i.id; newIdentity = "" }) }
                            }
                            Text("ya da yeni kimlik", style = MaterialTheme.typography.bodySmall)
                        }
                        GlassField(newIdentity, { newIdentity = it; if (it.isNotBlank()) identityId = "" }, label = "Ben ___ olan biriyim",
                            placeholder = "her gün okuyan biriyim")
                    }
                    1 -> {
                        Lead("Değer filtresi", "İyi değerler gerçeğe dayanır, yapıcıdır ve kontrolündedir.")
                        GlassCard {
                            ToggleRow("Gerçeğe dayalı mı?", reality, { reality = it })
                            Divider()
                            ToggleRow("Sosyal açıdan yapıcı mı?", constructive, { constructive = it })
                            Divider()
                            ToggleRow("Kontrolümde mi?", controllable, { controllable = it })
                        }
                        if (!(reality && constructive && controllable)) {
                            GlassCard { Text("Uyarı: en az bir cevap \"hayır\". Bu alışkanlık kontrol edemediğin bir sonuca bağlı olabilir. Devam edebilirsin ama ölçüyü kontrolündeki bir davranışa çek.", style = MaterialTheme.typography.bodyMedium, color = G.warning) }
                        }
                    }
                    2 -> {
                        Lead("Kolaylaştır", "Tam sürüm hedefin, 2 dakikalık sürüm kötü gündeki minimum. Minimum zinciri korur.")
                        GlassField(name, { name = it }, label = "Alışkanlık adı", placeholder = "Okumak", maxLength = 60)
                        GlassField(full, { full = it }, label = "Tam sürüm", placeholder = "30 dakika okumak", maxLength = 120)
                        GlassField(twoMin, { twoMin = it }, label = "2 dakikalık sürüm (zorunlu)", placeholder = "1 sayfa okumak", maxLength = 120)
                    }
                    3 -> {
                        Lead("Görünür kıl", "Tetiksiz alışkanlık kaydedilmez: ya niyet (saat + yer) ya istifleme.")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Chip("Niyet: saat + yer", cueType == "time", { cueType = "time" })
                            Chip("İstifleme", cueType == "stack", { cueType = "stack" }, )
                        }
                        if (cueType == "time") {
                            TimeField("Saat", time, { time = it })
                            GlassField(place, { place = it }, label = "Yer", placeholder = "Salondaki koltuk", maxLength = 60)
                            if (time.isNotBlank() && place.isNotBlank()) Text("Niyet: ${name.ifBlank { "Bu alışkanlığı" }} saat $time'da $place'de yapacağım.", style = MaterialTheme.typography.bodySmall, color = G.mint)
                        } else {
                            val others = habits.filter { it.id != habitId && !it.archived }
                            if (others.isEmpty()) {
                                Text("İstiflemek için önce başka bir alışkanlık kur. Şimdilik niyet kullan.", style = MaterialTheme.typography.bodyMedium, color = G.warning)
                            } else {
                                Text("Şundan sonra:", style = MaterialTheme.typography.labelMedium, color = G.textSecondary)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    others.forEach { o -> Chip(o.name, o.id == after, { after = o.id }) }
                                }
                            }
                        }
                        Text("Günler", style = MaterialTheme.typography.labelMedium, color = G.textSecondary)
                        DaysPicker(days) { days = it }
                        if (projects.isNotEmpty()) {
                            Text("Proje veya hobiye bağla (isteğe bağlı)", style = MaterialTheme.typography.labelMedium, color = G.textSecondary)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Chip("Yok", projectId.isBlank(), { projectId = "" })
                                projects.filter { !it.archived }.forEach { p -> Chip(p.title, p.id == projectId, { projectId = p.id }, icon = if (p.isHobby) GelisioIcons.hobby else GelisioIcons.project) }
                            }
                        }
                    }
                    4 -> {
                        Lead("Cazip kıl", "Sevdiğin bir şeyi alışkanlığın hemen arkasına bağla. Boş bırakabilirsin.")
                        GlassField(reward, { reward = it }, label = "Alışkanlıktan sonra…", placeholder = "Kahvemi içerim", maxLength = 80)
                    }
                    else -> {
                        Lead("Tatmin edici kıl", "Dokun = tam, uzun bas = minimum, kaydır = atlandı. Her işaret kimliğine oy yazar.")
                        GlassCard {
                            Info("Kimlik", identities.firstOrNull { it.id == identityId }?.statement ?: newIdentity)
                            Divider()
                            Info("Tam / minimum", "$full  ·  $twoMin")
                            Divider()
                            Info("İşaret", if (cueType == "time") "$time · $place" else "${habits.firstOrNull { it.id == after }?.name} sonrası")
                            if (reward.isNotBlank()) {
                                Divider()
                                Info("Ödül", reward)
                            }
                            Divider()
                            Info("Günler", if (days.size == 7) "Her gün" else days.joinToString(" ") { TR_DAYS[it - 1] })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepBar(step: Int, total: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            Box(
                Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(if (i <= step) G.accentH else androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color(0x1AFFFFFF))),
            )
        }
    }
}

@Composable
fun Lead(title: String, text: String) {
    Column {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = G.textSecondary)
    }
}

/** Alışkanlık Skor Kartı: mevcut günlük rutini +, − veya = olarak işaretle. */
@Composable
fun ScorecardScreen() {
    val items by Store.scorecard.collectAsState()
    var adding by remember { mutableStateOf(false) }
    SubPage("Alışkanlık Skor Kartı", subtitle = "Başlangıç denetimi") {
        Lead("Farkında ol", "Günün akışındaki davranışlarını sırayla yaz. Her birini iyi (+), kötü (−) veya nötr (=) olarak işaretle. Yargılama, sadece gör.")
        GlassCard(padding = 10.dp) {
            if (items.isEmpty()) Text("Örnek: Uyanırım · Telefona bakarım · Kahve yaparım…", Modifier.padding(8.dp), style = MaterialTheme.typography.bodyMedium, color = G.textSecondary)
            items.forEachIndexed { i, item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(item.text, Modifier.weight(1f).padding(start = 6.dp), style = MaterialTheme.typography.bodyLarge)
                    listOf("+" to G.mint, "−" to G.critical, "=" to G.textSecondary).forEach { (m, c) ->
                        val sel = item.mark == m
                        Box(
                            Modifier.padding(start = 6.dp).size(34.dp).clip(RoundedCornerShape(10.dp))
                                .background(if (sel) c.copy(alpha = 0.25f) else androidx.compose.ui.graphics.Color(0x0FFFFFFF))
                                .pressable({ Store.saveScoreItem(item.copy(mark = m)) }, pressedScale = 0.9f),
                            contentAlignment = Alignment.Center,
                        ) { Text(m, style = MaterialTheme.typography.titleMedium, color = if (sel) c else G.textTertiary) }
                    }
                    IconGlassButton(GelisioIcons.close, { Store.deleteScoreItem(item.id) }, Modifier.padding(start = 6.dp), size = 32.dp, color = G.textTertiary)
                }
                if (i < items.lastIndex) Divider()
            }
        }
        GlassButton("Satır ekle", { adding = true }, Modifier.fillMaxWidth(), icon = GelisioIcons.add)
        if (items.isNotEmpty()) {
            Text("+ ${items.count { it.mark == "+" }}   − ${items.count { it.mark == "−" }}   = ${items.count { it.mark == "=" }}", style = MaterialTheme.typography.titleMedium, color = G.textSecondary)
        }
    }
    if (adding) InputDialog("Davranış", placeholder = "Telefona bakarım", onConfirm = {
        Store.saveScoreItem(ScoreItem(text = it, mark = "=", order = (items.maxOfOrNull { s -> s.order } ?: 0) + 1))
    }, onDismiss = { adding = false })
}

@Composable
fun IdentityVotes(identity: Identity, habits: List<Habit>, logs: com.omeronur.gelisio.data.LogIndex) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(identity.statement, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text("${votes(identity.id, habits, logs)} oy", style = MaterialTheme.typography.titleSmall, color = G.mint)
    }
}

@Suppress("unused")
private fun LocalDate.label() = trShort()
