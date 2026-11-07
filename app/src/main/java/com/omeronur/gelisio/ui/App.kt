package com.omeronur.gelisio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.omeronur.gelisio.data.Backend
import com.omeronur.gelisio.data.UserRepository
import com.omeronur.gelisio.ui.components.GlassNavBar
import com.omeronur.gelisio.ui.components.NavItem
import com.omeronur.gelisio.ui.icons.GelisioIcons
import com.omeronur.gelisio.ui.screens.MeScreen
import com.omeronur.gelisio.ui.screens.RoadmapScreen
import com.omeronur.gelisio.ui.screens.SignInScreen
import com.omeronur.gelisio.ui.screens.TodayScreen
import com.omeronur.gelisio.ui.theme.G
import com.omeronur.gelisio.ui.theme.GelisioTheme

@Composable
fun GelisioApp() {
    GelisioTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(G.bgTop, G.bgBottom))),
        ) {
            val context = LocalContext.current
            if (Backend.isConfigured(context)) AuthGate() else SetupScreen()
        }
    }
}

@Composable
private fun AuthGate() {
    var user by remember { mutableStateOf(Backend.auth.currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { user = it.currentUser }
        Backend.auth.addAuthStateListener(listener)
        onDispose { Backend.auth.removeAuthStateListener(listener) }
    }
    val current = user
    if (current == null) {
        SignInScreen()
    } else {
        LaunchedEffect(current.uid) { UserRepository.ensureProfile(current) }
        HomeScreen(current)
    }
}

private val tabs = listOf(
    NavItem(GelisioIcons.today, "Bugün"),
    NavItem(GelisioIcons.week, "Hafta"),
    NavItem(GelisioIcons.focus, "Odak"),
    NavItem(GelisioIcons.growth, "Gelişim"),
    NavItem(GelisioIcons.me, "Ben"),
)

@Composable
private fun HomeScreen(user: FirebaseUser) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            when (tab) {
                0 -> TodayScreen(user)
                1 -> RoadmapScreen(
                    title = "Hafta",
                    subtitle = "Önce roller, sonra hafta",
                    icon = GelisioIcons.week,
                    items = listOf(
                        "Pazar planlama sihirbazı" to "Her rol için 2–3 hedef · 7 Alışkanlık",
                        "II. Kare yüzdesi" to "Acil olmayan ama önemli işlerin payı · 7 Alışkanlık",
                        "Haftalık gözden geçirme" to "Ne işe yaradı, ne yaramadı, neyi değiştiriyorum",
                    ),
                )
                2 -> RoadmapScreen(
                    title = "Odak",
                    subtitle = "Derin çalışma ve skor cetveli",
                    icon = GelisioIcons.focus,
                    items = listOf(
                        "Derin çalışma sayacı" to "Keşiş, çift modlu, ritmik veya gazeteci şablonu · Pürdikkat",
                        "Ritüel kontrolü" to "Yer, süre, kural, destek · Pürdikkat",
                        "Skor cetveli" to "Haftalık kılavuz ölçüt: derin saat · Pürdikkat",
                    ),
                )
                3 -> RoadmapScreen(
                    title = "Gelişim",
                    subtitle = "Projeler ve hobiler",
                    icon = GelisioIcons.growth,
                    items = listOf(
                        "Projeler" to "Kilometre taşları + kılavuz ölçüt · v1.1",
                        "Hobiler" to "Beceri seviyesi, pratik ısı haritası, üretim galerisi · Dijital Minimalizm",
                        "Seviye atlama" to "Goldilocks kuralı: 4 hafta hedef tutunca · Atomik Alışkanlıklar",
                    ),
                )
                else -> MeScreen(user)
            }
        }
        GlassNavBar(items = tabs, selected = tab, onSelect = { tab = it })
    }
}

@Composable
private fun SetupScreen() {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Firebase henüz bağlı değil", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = G.text)
        Spacer(Modifier.height(12.dp))
        Text(
            "app/google-services.json dosyasını Firebase konsolundan indirip koy, sonra uygulamayı yeniden derle. " +
                "Ayrıntılar README'de.",
            fontSize = 14.sp,
            color = G.textSecondary,
        )
    }
}
