@file:OptIn(ExperimentalLayoutApi::class)

package com.omeronur.gelisio.ui.screens

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.omeronur.gelisio.R
import com.omeronur.gelisio.data.Backend
import com.omeronur.gelisio.data.Identity
import com.omeronur.gelisio.data.Store
import com.omeronur.gelisio.data.WorkPhilosophy
import com.omeronur.gelisio.ui.components.Chip
import com.omeronur.gelisio.ui.components.EditableList
import com.omeronur.gelisio.ui.components.GlassButton
import com.omeronur.gelisio.ui.components.GlassCard
import com.omeronur.gelisio.ui.components.GlassField
import com.omeronur.gelisio.ui.components.PrimaryButton
import com.omeronur.gelisio.ui.components.Tag
import com.omeronur.gelisio.ui.components.TextAction
import com.omeronur.gelisio.ui.icons.GelisioIcons
import com.omeronur.gelisio.ui.theme.G

@Composable
fun SignInScreen() {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val client = remember {
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(Backend.webClientId(context).orEmpty())
                .requestEmail()
                .build(),
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            busy = false
            return@rememberLauncherForActivityResult
        }
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken.orEmpty(), null)
            Backend.auth.signInWithCredential(credential).addOnFailureListener {
                busy = false
                error = it.message ?: "Giriş başarısız"
            }
        } catch (e: ApiException) {
            busy = false
            error = "Google girişi başarısız (kod ${e.statusCode})"
        }
    }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(R.drawable.logo_mark), contentDescription = "Gelisio", Modifier.size(140.dp))
        Spacer(Modifier.height(12.dp))
        Text("Gelisio", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(8.dp))
        Text("Kimlik, hedef, hafta, alışkanlık, derin iş.\nTek döngü, tek uygulama.", style = MaterialTheme.typography.bodyLarge, color = G.textSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(40.dp))
        PrimaryButton(if (busy) "Bağlanıyor…" else "Google ile giriş yap", {
            busy = true
            error = null
            launcher.launch(client.signInIntent)
        }, Modifier.fillMaxWidth(), enabled = !busy)
        error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = G.critical, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(18.dp))
        Text("Telefon ve bilgisayar aynı hesapla senkron olur.", style = MaterialTheme.typography.bodySmall, color = G.textTertiary)
    }
}

/** Kurulum: misyon, en fazla 7 rol, ilk kimlik, çalışma felsefesi, bildirim izni. Her adım atlanabilir. */
@Composable
fun OnboardingScreen() {
    val ctx = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    var mission by rememberSaveable { mutableStateOf(Store.profile.value.mission) }
    var roles by remember { mutableStateOf(Store.profile.value.roles) }
    var identity by rememberSaveable { mutableStateOf("") }
    var philosophy by remember { mutableStateOf(Store.profile.value.workPhilosophy) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun finish() {
        val p = Store.profile.value
        if (identity.isNotBlank()) Store.saveIdentity(Identity(statement = identity))
        Store.saveProfile(
            p.copy(
                mission = mission.trim(), roles = roles.take(7), workPhilosophy = philosophy,
                deepBlocks = if (p.deepBlocks.isEmpty() && philosophy != null) templateFor(philosophy!!) else p.deepBlocks,
                onboarded = true,
            ),
        )
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Kurulum ${step + 1}/5", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = G.textSecondary)
            TextAction("Sonra", { finish() }, color = G.textSecondary)
        }
        Column(Modifier.padding(horizontal = 20.dp)) { StepBar(step, 5) }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AnimatedContent(step, transitionSpec = { fadeIn(G.motion()) togetherWith fadeOut(G.motion(200)) }, label = "onb") { s ->
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    when (s) {
                        0 -> {
                            Lead("Misyon", "Kişisel misyon bildirgen: kim olmak istiyorsun, neye hizmet ediyorsun? Kısa tut; sonra değiştirebilirsin.")
                            GlassField(mission, { mission = it }, placeholder = "İşletmelere insan gibi büyümeyi…", singleLine = false, maxLength = 800)
                        }
                        1 -> {
                            Lead("Roller", "En fazla 7. Haftalık plan rollere göre yapılır.")
                            EditableList(roles, { roles = it.take(7) }, "Rol ekle", "Girişimci", max = 7)
                            if (roles.isEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("Girişimci", "Partner", "Sporcu").forEach { r -> Chip(r, false, { roles = roles + r }) }
                                }
                            }
                        }
                        2 -> {
                            Lead("İlk kimlik", "Sonuç değil kimlik: \"Ben ___ olan biriyim\". Her alışkanlık bir kimliğe oy verir.")
                            GlassField(identity, { identity = it }, label = "Ben ___ olan biriyim", placeholder = "her gün derin çalışan biriyim", maxLength = 100)
                        }
                        3 -> {
                            Lead("Çalışma felsefesi", "Derin blok şablonun buna göre kurulur.")
                            WorkPhilosophy.entries.forEach { p ->
                                GlassCard(strong = philosophy == p, onClick = { philosophy = p }) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(p.label, style = MaterialTheme.typography.titleMedium, color = if (philosophy == p) G.mint else G.text)
                                            Text(p.desc, style = MaterialTheme.typography.bodySmall)
                                        }
                                        if (philosophy == p) Tag("Seçili", G.mint)
                                    }
                                }
                            }
                        }
                        else -> {
                            Lead("Bildirimler", "Günde en fazla 4, sessiz saatler 22:00–07:00. Hatırlatır, utandırmaz; kayıp dili yok.")
                            GlassCard {
                                Text("Niyet saatinde hatırlatma, kaçırmadan sonra kurtarma, derin blok başlangıcı, akşam kapanışı ve Pazar planı.", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (Build.VERSION.SDK_INT >= 33) {
                                GlassButton("Bildirim iznini aç", { permission.launch(Manifest.permission.POST_NOTIFICATIONS) }, Modifier.fillMaxWidth(), icon = GelisioIcons.bell, color = G.mint)
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step > 0) GlassButton("Geri", { step-- }, Modifier.weight(1f))
            if (step < 4) PrimaryButton("Devam", { step++ }, Modifier.weight(2f))
            else PrimaryButton("Başla", { finish() }, Modifier.weight(2f), icon = GelisioIcons.check)
        }
    }
    @Suppress("UNUSED_VARIABLE") val unused = ctx
}
