@file:OptIn(ExperimentalLayoutApi::class)

package com.omeronur.gelisio.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.omeronur.gelisio.R
import com.omeronur.gelisio.data.Backend
import com.omeronur.gelisio.data.CustomQuote
import com.omeronur.gelisio.data.Identity
import com.omeronur.gelisio.data.PrincipleContext
import com.omeronur.gelisio.data.Quotes
import com.omeronur.gelisio.data.Store
import com.omeronur.gelisio.data.principles
import com.omeronur.gelisio.data.votes
import com.omeronur.gelisio.notify.Notifier
import com.omeronur.gelisio.ui.Nav
import com.omeronur.gelisio.ui.Screen
import com.omeronur.gelisio.ui.SubPage
import com.omeronur.gelisio.ui.TabPage
import com.omeronur.gelisio.ui.components.Chip
import com.omeronur.gelisio.ui.components.ConfirmDialog
import com.omeronur.gelisio.ui.components.Divider
import com.omeronur.gelisio.ui.components.EditableList
import com.omeronur.gelisio.ui.components.GlassButton
import com.omeronur.gelisio.ui.components.GlassCard
import com.omeronur.gelisio.ui.components.GlassField
import com.omeronur.gelisio.ui.components.IconGlassButton
import com.omeronur.gelisio.ui.components.InputDialog
import com.omeronur.gelisio.ui.components.NavRow
import com.omeronur.gelisio.ui.components.PrimaryButton
import com.omeronur.gelisio.ui.components.SectionTitle
import com.omeronur.gelisio.ui.components.Stepper
import com.omeronur.gelisio.ui.components.Tag
import com.omeronur.gelisio.ui.components.TextAction
import com.omeronur.gelisio.ui.components.TimeField
import com.omeronur.gelisio.ui.components.ToggleRow
import com.omeronur.gelisio.ui.icons.GelisioIcons
import com.omeronur.gelisio.ui.theme.G

@Composable
fun MeTab() {
    val user = runCatching { Backend.auth.currentUser }.getOrNull()
    val profile by Store.profile.collectAsState()
    val identities by Store.identities.collectAsState()
    val habits by Store.habits.collectAsState()
    val logs by Store.logIndex.collectAsState()
    var confirmOut by remember { mutableStateOf(false) }

    TabPage("Ben", subtitle = user?.email) {
        SectionTitle("Kimlikler ve oylar", action = "Düzenle", onAction = { Nav.push(Screen.Identities) })
        GlassCard(strong = true, onClick = { Nav.push(Screen.Identities) }) {
            if (identities.isEmpty()) Text("Henüz kimlik yok. İlk alışkanlıkla birlikte yazılır.", style = MaterialTheme.typography.bodyMedium, color = G.textSecondary)
            identities.forEachIndexed { i, id ->
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Ben ${id.statement.removePrefix("Ben ").removePrefix("ben ")}", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Text("${votes(id.id, habits, logs)} oy", style = MaterialTheme.typography.titleMedium, color = G.mint)
                }
                if (i < identities.lastIndex) Divider()
            }
        }

        SectionTitle("Yön")
        GlassCard(padding = 10.dp) {
            NavRow(GelisioIcons.mission, "Misyon", profile.mission.ifBlank { "Kişisel misyon bildirgen" }, onClick = { Nav.push(Screen.Mission) })
            Divider()
            NavRow(GelisioIcons.social, "Roller", profile.roles.joinToString(", ").ifBlank { "En fazla 7 rol" }, trailing = "${profile.roles.size}/7", onClick = { Nav.push(Screen.Roles) })
            Divider()
            NavRow(GelisioIcons.values, "Değerler", profile.values.joinToString(", ").ifBlank { "Mücadeleni seç" }, onClick = { Nav.push(Screen.Values) })
            Divider()
            NavRow(GelisioIcons.focus, "Çalışma felsefesi", profile.workPhilosophy?.label ?: "Seçilmedi", onClick = { Nav.push(Screen.Philosophy) })
            Divider()
            NavRow(GelisioIcons.scoreboard, "Alışkanlık Skor Kartı", "Mevcut rutinini +, −, = ile işaretle", onClick = { Nav.push(Screen.Scorecard) })
        }

        SectionTitle("Bildirim ve cümleler")
        GlassCard(padding = 10.dp) {
            NavRow(GelisioIcons.bell, "Bildirimler", "Günde en fazla ${profile.notifBudget} · sessiz ${profile.quietStart}–${profile.quietEnd}", onClick = { Nav.push(Screen.Notifications) })
            Divider()
            NavRow(GelisioIcons.quote, "Motivasyon cümleleri", "Bağlama göre; kendi cümleni ekle", onClick = { Nav.push(Screen.Quotes) })
        }

        SectionTitle("Hesap")
        GlassCard(padding = 10.dp) {
            NavRow(GelisioIcons.settings, "Hakkında", "Sürüm, ikon atfı, Firestore kuralları", onClick = { Nav.push(Screen.About) })
        }
        GlassButton("Çıkış yap", { confirmOut = true }, Modifier.fillMaxWidth(), icon = GelisioIcons.signOut, color = G.critical)
    }
    if (confirmOut) ConfirmDialog("Çıkış yap", "Veriler bulutta kalır; tekrar girişte geri gelir.", confirm = "Çıkış", onConfirm = {
        Store.stop()
        Backend.auth.signOut()
    }, onDismiss = { confirmOut = false })
}

@Composable
fun MissionScreen() {
    val profile by Store.profile.collectAsState()
    var text by rememberSaveable { mutableStateOf(profile.mission) }
    SubPage("Misyon", subtitle = "Kişisel misyon bildirgesi", bottom = {
        PrimaryButton("Kaydet", { Store.saveProfile(profile.copy(mission = text.trim())); Nav.pop() }, Modifier.fillMaxWidth(), icon = GelisioIcons.check)
    }) {
        Lead("Sonu düşünerek başla", "Kim olmak, ne yapmak istiyorsun ve hangi ilkelere göre? Kısa olsun; Pazar planında her hafta okunur.")
        GlassField(text, { text = it }, placeholder = "İnsanlara ve işletmelere…", singleLine = false, maxLength = 800)
    }
}

@Composable
fun RolesScreen() {
    val profile by Store.profile.collectAsState()
    SubPage("Roller", subtitle = "${profile.roles.size}/7") {
        Lead("Rollerin", "Haftalık plan rollere göre yapılır. En fazla 7: girişimci, eş, arkadaş, sporcu…")
        EditableList(profile.roles, { Store.saveProfile(profile.copy(roles = it.take(7))) }, "Rol ekle", "Girişimci", max = 7)
    }
}

@Composable
fun ValuesScreen() {
    val profile by Store.profile.collectAsState()
    SubPage("Değerler", subtitle = "Mücadeleni seç") {
        Lead("İyi değerler", "Gerçeğe dayalı, sosyal açıdan yapıcı, kontrol edilebilir. Hedef ve alışkanlık açarken bu üç soru sorulur.")
        EditableList(profile.values, { Store.saveProfile(profile.copy(values = it)) }, "Değer ekle", "Dürüstlük")
    }
}

@Composable
fun IdentitiesScreen() {
    val identities by Store.identities.collectAsState()
    val habits by Store.habits.collectAsState()
    val logs by Store.logIndex.collectAsState()
    var editing by remember { mutableStateOf<Identity?>(null) }
    var adding by remember { mutableStateOf(false) }
    var del by remember { mutableStateOf<Identity?>(null) }
    SubPage("Kimlikler", subtitle = "Her tamamlama bir oy") {
        Lead("Kim olmak istiyorsun?", "Alışkanlıklar kimliğe oy verir. Minimum işaret de oy sayılır.")
        identities.forEach { id ->
            val hs = habits.filter { it.identityId == id.id }
            GlassCard(onClick = { editing = id }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(id.statement, style = MaterialTheme.typography.titleMedium)
                        Text(if (hs.isEmpty()) "Bağlı alışkanlık yok" else hs.joinToString(", ") { it.name }, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("${votes(id.id, habits, logs)}", style = MaterialTheme.typography.headlineMedium, color = G.mint)
                    Spacer(Modifier.width(4.dp))
                    Text("oy", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(8.dp))
                    IconGlassButton(GelisioIcons.delete, { del = id }, size = 34.dp, color = G.textTertiary)
                }
            }
        }
        PrimaryButton("Kimlik ekle", { adding = true }, Modifier.fillMaxWidth(), icon = GelisioIcons.add)
    }
    editing?.let { id -> InputDialog("Ben ___ olan biriyim", initial = id.statement, onConfirm = { Store.saveIdentity(id.copy(statement = it)) }, onDismiss = { editing = null }) }
    if (adding) InputDialog("Ben ___ olan biriyim", placeholder = "her gün yazan biriyim", onConfirm = { Store.saveIdentity(Identity(statement = it)) }, onDismiss = { adding = false })
    del?.let { id -> ConfirmDialog("Kimliği sil", "Bağlı alışkanlıklar kalır ama oyları bu kimliğe yazılmaz.", onConfirm = { Store.deleteIdentity(id.id) }, onDismiss = { del = null }) }
}

@Composable
fun NotificationsScreen() {
    val ctx = LocalContext.current
    val profile by Store.profile.collectAsState()
    var granted by remember { mutableStateOf(Notifier.canPost(ctx)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    SubPage("Bildirimler", subtitle = "Hatırlatır, utandırmaz") {
        if (!granted) {
            GlassCard {
                Text("Bildirim izni kapalı", style = MaterialTheme.typography.titleSmall, color = G.warning)
                Text("Niyet hatırlatmaları ve derin blok uyarıları için izin gerekli.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(10.dp))
                PrimaryButton("İzin ver", { if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }, Modifier.fillMaxWidth())
            }
        }
        GlassCard {
            Text("Günlük bütçe", style = MaterialTheme.typography.titleSmall)
            Text("Akşam kapanışı, Pazar planı ve derin bloklara yer ayrılır; kalan bütçe alışkanlıklara.", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
            Stepper("${profile.notifBudget} / gün", { Store.saveProfile(profile.copy(notifBudget = (profile.notifBudget - 1).coerceAtLeast(1))) },
                { Store.saveProfile(profile.copy(notifBudget = (profile.notifBudget + 1).coerceAtMost(8))) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TimeField("Sessiz başlangıç", profile.quietStart, { Store.saveProfile(profile.copy(quietStart = it)) }, Modifier.weight(1f))
            TimeField("Sessiz bitiş", profile.quietEnd, { Store.saveProfile(profile.copy(quietEnd = it)) }, Modifier.weight(1f))
        }
        TimeField("Akşam kapanışı", profile.eveningTime, { Store.saveProfile(profile.copy(eveningTime = it)) })
        GlassCard {
            ToggleRow("Pazar planlaması", profile.sundayPlanning, { Store.saveProfile(profile.copy(sundayPlanning = it)) }, "Pazar 19:00 · gözden geçirme + sihirbaz")
        }
        SectionTitle("Ne zaman ne gelir")
        GlassCard {
            listOf(
                "Niyet hatırlatması" to "Alışkanlığın saatinde; Tam / Minimum düğmeleriyle",
                "Kurtarma" to "Kaçırmadan sonraki gün aynı saatte: 2 dakika yeter",
                "İstifleme zinciri" to "Uygulama içinde \"Sırada: …\" (bütçeden yemez)",
                "Derin blok" to "Blok başında ritüel, bitişte süre dolunca",
                "Akşam kapanışı" to "Yarın planlı mı; sonra sessizlik",
                "Günün cümlesi" to "Uygulama içi kart; bildirim değil",
            ).forEachIndexed { i, (a, b) ->
                Column(Modifier.padding(vertical = 6.dp)) {
                    Text(a, style = MaterialTheme.typography.titleSmall)
                    Text(b, style = MaterialTheme.typography.bodySmall)
                }
                if (i < 5) Divider()
            }
        }
        GlassButton("Deneme bildirimi gönder", {
            Notifier.ensureChannels(ctx)
            if (Notifier.canPost(ctx)) {
                try {
                    NotificationManagerCompat.from(ctx).notify(
                        4242,
                        NotificationCompat.Builder(ctx, Notifier.CH_HABITS).setSmallIcon(R.drawable.ic_stat_gelisio)
                            .setColor(0xFF7CF5D2.toInt()).setContentTitle("Gelisio").setContentText("Bildirimler çalışıyor.").setAutoCancel(true).build(),
                    )
                } catch (_: SecurityException) {
                }
            } else {
                Nav.toast("Önce bildirim izni ver")
            }
        }, Modifier.fillMaxWidth(), icon = GelisioIcons.bell)
    }
}

@Composable
fun QuotesScreen() {
    val custom by Store.quotes.collectAsState()
    var ctxSel by remember { mutableStateOf(PrincipleContext.MORNING) }
    var adding by remember { mutableStateOf(false) }
    SubPage("Motivasyon cümleleri", subtitle = "Bağlama göre, 14 gün tekrarsız") {
        Text("Kitaplardan alıntı yok: her cümle bir ilkenin kendi ifademizle hâli ve \"İlke: <kitap>\" etiketiyle durur.", style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PrincipleContext.entries.forEach { c -> Chip(c.label, ctxSel == c, { ctxSel = c }) }
        }
        GlassCard {
            val defaults = principles.filter { it.context == ctxSel }
            defaults.forEach { p ->
                Column(Modifier.padding(vertical = 6.dp)) {
                    Text(p.text, style = MaterialTheme.typography.bodyLarge)
                    Text("İlke: ${p.source}", style = MaterialTheme.typography.bodySmall, color = G.lavender)
                }
                Divider()
            }
            custom.filter { PrincipleContext.of(it.context) == ctxSel }.forEach { q ->
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(q.text, style = MaterialTheme.typography.bodyLarge)
                        Text("Kendi cümlen", style = MaterialTheme.typography.bodySmall, color = G.mint)
                    }
                    IconGlassButton(GelisioIcons.delete, { Store.deleteQuote(q.id) }, size = 32.dp, color = G.textTertiary)
                }
                Divider()
            }
            Text("Havuz: ${Quotes.pool(ctxSel, custom).size} cümle", style = MaterialTheme.typography.bodySmall, color = G.textTertiary)
        }
        PrimaryButton("${ctxSel.label} için cümle ekle", { adding = true }, Modifier.fillMaxWidth(), icon = GelisioIcons.add)
    }
    if (adding) InputDialog("Kendi cümlen", singleLine = false, onConfirm = { Store.saveQuote(CustomQuote(context = ctxSel.key, text = it)) }, onDismiss = { adding = false })
}

const val FIRESTORE_RULES = """rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{uid}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == uid;
    }
  }
}"""

@Composable
fun AboutScreen() {
    val clip = LocalClipboardManager.current
    SubPage("Hakkında") {
        GlassCard(strong = true) {
            Text("Gelisio", style = MaterialTheme.typography.headlineMedium)
            Text("Sürüm 1.0.0 · kişisel araç", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
            Text("Kimlik → hedef → haftalık plan → günlük alışkanlık → derin çalışma → gözden geçirme.", style = MaterialTheme.typography.bodyMedium)
        }
        GlassCard {
            Text("Atıflar", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Text("Uicons by Flaticon", style = MaterialTheme.typography.bodyLarge)
            Text("İkonlar: Flaticon UIcons, Regular Rounded. Ücretsiz lisans, atıf şartıyla.", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Text("Inter yazı tipi · SIL Open Font License", style = MaterialTheme.typography.bodySmall)
            Text("Cam efekti: Haze (Apache 2.0)", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Text("İlkeler: Atomik Alışkanlıklar, Etkili İnsanların 7 Alışkanlığı, Pürdikkat, Dijital Minimalizm, Kafaya Takmama Sanatı. Uygulamada kitap metni yok; yalnızca yöntem.", style = MaterialTheme.typography.bodySmall)
        }
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Firestore kuralları", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                TextAction("Kopyala", { clip.setText(AnnotatedString(FIRESTORE_RULES)); Nav.toast("Kopyalandı") })
            }
            Text("Firebase konsolu → Firestore → Kurallar sekmesine yapıştır, Yayınla.", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            SelectionContainer { Text(FIRESTORE_RULES, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp), color = G.textSecondary) }
        }
        Tag("Veriler: users/{uid} altında, yalnızca sana açık", G.mint)
    }
}

