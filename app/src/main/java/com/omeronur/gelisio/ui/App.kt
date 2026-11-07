package com.omeronur.gelisio.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.omeronur.gelisio.data.Backend
import com.omeronur.gelisio.data.Store
import com.omeronur.gelisio.notify.Notifier
import com.omeronur.gelisio.ui.components.GIcon
import com.omeronur.gelisio.ui.components.GlassBackground
import com.omeronur.gelisio.ui.components.LocalHaze
import com.omeronur.gelisio.ui.components.glass
import com.omeronur.gelisio.ui.components.pressable
import com.omeronur.gelisio.ui.icons.GelisioIcons
import com.omeronur.gelisio.ui.screens.AboutScreen
import com.omeronur.gelisio.ui.screens.DeepBlocksScreen
import com.omeronur.gelisio.ui.screens.DeepSessionScreen
import com.omeronur.gelisio.ui.screens.FocusTab
import com.omeronur.gelisio.ui.screens.GoalEditorScreen
import com.omeronur.gelisio.ui.screens.GrowthTab
import com.omeronur.gelisio.ui.screens.HabitDetailScreen
import com.omeronur.gelisio.ui.screens.HabitWizardScreen
import com.omeronur.gelisio.ui.screens.IdentitiesScreen
import com.omeronur.gelisio.ui.screens.JustStartScreen
import com.omeronur.gelisio.ui.screens.MatrixScreen
import com.omeronur.gelisio.ui.screens.MeTab
import com.omeronur.gelisio.ui.screens.MissionScreen
import com.omeronur.gelisio.ui.screens.NotificationsScreen
import com.omeronur.gelisio.ui.screens.OnboardingScreen
import com.omeronur.gelisio.ui.screens.PhilosophyScreen
import com.omeronur.gelisio.ui.screens.PlanWizardScreen
import com.omeronur.gelisio.ui.screens.ProjectDetailScreen
import com.omeronur.gelisio.ui.screens.ProjectEditorScreen
import com.omeronur.gelisio.ui.screens.QuotesScreen
import com.omeronur.gelisio.ui.screens.RolesScreen
import com.omeronur.gelisio.ui.screens.ScorecardScreen
import com.omeronur.gelisio.ui.screens.SessionsScreen
import com.omeronur.gelisio.ui.screens.SignInScreen
import com.omeronur.gelisio.ui.screens.TodayTab
import com.omeronur.gelisio.ui.screens.ValuesScreen
import com.omeronur.gelisio.ui.screens.WeekTab
import com.omeronur.gelisio.ui.screens.WeeklyReviewScreen
import com.omeronur.gelisio.ui.theme.G
import com.omeronur.gelisio.ui.theme.GelisioTheme
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay

@Composable
fun GelisioApp() {
    GelisioTheme {
        val haze = remember { HazeState() }
        GlassBackground(haze) {
            CompositionLocalProvider(LocalHaze provides haze) {
                val context = LocalContext.current
                if (Backend.isConfigured(context)) AuthGate() else SetupScreen()
            }
        }
    }
}

@Composable
private fun AuthGate() {
    var user by remember { mutableStateOf(Backend.auth.currentUser) }
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { user = it.currentUser }
        Backend.auth.addAuthStateListener(listener)
        onDispose { Backend.auth.removeAuthStateListener(listener) }
    }
    val current = user
    if (current == null) {
        LaunchedEffect(Unit) { Store.stop(); Nav.stack.clear() }
        SignInScreen()
        return
    }
    LaunchedEffect(current.uid) {
        Store.onChange = { Notifier.requestReschedule(context.applicationContext) }
        Store.start(current.uid)
        Store.touchUser(current.displayName, current.email)
    }
    val loaded by Store.loaded.collectAsState()
    val profile by Store.profile.collectAsState()
    when {
        !loaded -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = G.mint, strokeWidth = 3.dp)
        }
        !profile.onboarded -> OnboardingScreen()
        else -> Main()
    }
    ErrorBanner()
}

@Composable
private fun ErrorBanner() {
    val err by Store.error.collectAsState()
    val e = err ?: return
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(G.cardRadius)).background(G.critical.copy(alpha = 0.9f)).pressable({ Store.error.value = null }).padding(16.dp)) {
            Text("Senkron hatası", style = MaterialTheme.typography.titleSmall, color = G.bgBottom)
            Text(e, style = MaterialTheme.typography.bodySmall, color = G.bgBottom)
        }
    }
}

private val tabs = listOf(
    GelisioIcons.today to "Bugün",
    GelisioIcons.week to "Hafta",
    GelisioIcons.focus to "Odak",
    GelisioIcons.growth to "Gelişim",
    GelisioIcons.me to "Ben",
)

@Composable
private fun Main() {
    val top = Nav.stack.lastOrNull()
    var lastDepth by remember { mutableStateOf(0) }
    BackHandler(enabled = Nav.stack.isNotEmpty() || Nav.tab.intValue != 0) {
        if (Nav.stack.isNotEmpty()) Nav.pop() else Nav.tab.intValue = 0
    }
    Box(Modifier.fillMaxSize()) {
        val depth = Nav.stack.size
        AnimatedContent(
            targetState = top,
            transitionSpec = {
                val forward = depth >= lastDepth
                if (forward) {
                    (slideInHorizontally(G.motion()) { it / 3 } + fadeIn(G.motion())) togetherWith fadeOut(G.motion(200))
                } else {
                    fadeIn(G.motion()) togetherWith (slideOutHorizontally(G.motion()) { it / 3 } + fadeOut(G.motion(200)))
                }
            },
            label = "nav",
        ) { screen ->
            if (screen == null) Tabs() else ScreenFor(screen)
        }
        LaunchedEffect(depth) { lastDepth = depth }
        Toast()
    }
}

@Composable
internal fun Tabs() {
    Box(Modifier.fillMaxSize()) {
        Crossfade(Nav.tab.intValue, animationSpec = G.motion(), label = "tab") { t ->
            when (t) {
                0 -> TodayTab()
                1 -> WeekTab()
                2 -> FocusTab()
                3 -> GrowthTab()
                else -> MeTab()
            }
        }
        NavBar(Modifier.align(Alignment.BottomCenter))
    }
}

/** Yüzen cam alt çubuk; seçili sekme gradyan kapsülle işaretlenir. */
@Composable
private fun NavBar(modifier: Modifier) {
    Row(
        modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .fillMaxWidth()
            .glass(RoundedCornerShape(30.dp), strong = true)
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        tabs.forEachIndexed { i, (icon, label) ->
            val selected = Nav.tab.intValue == i
            val bg by animateColorAsState(if (selected) Color(0x1FFFFFFF) else Color.Transparent, G.motion(), label = "tabbg")
            val fg by animateColorAsState(if (selected) G.mint else G.textSecondary, G.motion(), label = "tabfg")
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(bg)
                    .pressable({ Nav.tab.intValue = i }, pressedScale = 0.92f)
                    .padding(vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                GIcon(icon, size = 19.sp, color = fg)
                Spacer(Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = fg, maxLines = 1)
            }
        }
    }
}

@Composable
private fun Toast() {
    val msg = Nav.toast.value
    LaunchedEffect(msg) {
        if (msg != null) {
            delay(2600)
            Nav.toast.value = null
        }
    }
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(top = 8.dp, start = 24.dp, end = 24.dp), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = msg != null,
            enter = slideInVertically(G.motion()) { -it } + fadeIn(G.motion()),
            exit = slideOutVertically(G.motion(200)) { -it } + fadeOut(G.motion(200)),
        ) {
            var last by remember { mutableStateOf("") }
            if (msg != null) last = msg
            Row(
                Modifier.glass(RoundedCornerShape(50), strong = true).padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GIcon(GelisioIcons.done, size = 15.sp, color = G.mint)
                Spacer(Modifier.width(10.dp))
                Text(last, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ScreenFor(s: Screen) {
    when (s) {
        Screen.Onboarding -> OnboardingScreen()
        is Screen.HabitWizard -> HabitWizardScreen(s.habitId, s.template)
        is Screen.HabitDetail -> HabitDetailScreen(s.habitId)
        is Screen.JustStart -> JustStartScreen(s.habitId)
        Screen.Scorecard -> ScorecardScreen()
        Screen.PlanWizard -> PlanWizardScreen()
        Screen.WeeklyReview -> WeeklyReviewScreen()
        Screen.Matrix -> MatrixScreen()
        is Screen.GoalEditor -> GoalEditorScreen(s.goalId)
        is Screen.DeepSession -> DeepSessionScreen(s.projectId)
        Screen.DeepBlocks -> DeepBlocksScreen()
        Screen.Sessions -> SessionsScreen()
        is Screen.ProjectEditor -> ProjectEditorScreen(s.projectId, s.kind)
        is Screen.ProjectDetail -> ProjectDetailScreen(s.projectId)
        Screen.Mission -> MissionScreen()
        Screen.Roles -> RolesScreen()
        Screen.Values -> ValuesScreen()
        Screen.Identities -> IdentitiesScreen()
        Screen.Philosophy -> PhilosophyScreen()
        Screen.Notifications -> NotificationsScreen()
        Screen.Quotes -> QuotesScreen()
        Screen.About -> AboutScreen()
    }
}

@Composable
private fun SetupScreen() {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Firebase henüz bağlı değil", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Text(
            "app/google-services.json dosyasını Firebase konsolundan indirip koy, sonra uygulamayı yeniden derle.",
            style = MaterialTheme.typography.bodyMedium,
            color = G.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
