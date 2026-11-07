package com.omeronur.gelisio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.omeronur.gelisio.data.Cue
import com.omeronur.gelisio.data.DayDoc
import com.omeronur.gelisio.data.DeepBlock
import com.omeronur.gelisio.data.Dimension
import com.omeronur.gelisio.data.Goal
import com.omeronur.gelisio.data.Habit
import com.omeronur.gelisio.data.HabitLog
import com.omeronur.gelisio.data.Identity
import com.omeronur.gelisio.data.LeadMeasure
import com.omeronur.gelisio.data.LogStatus
import com.omeronur.gelisio.data.Milestone
import com.omeronur.gelisio.data.Profile
import com.omeronur.gelisio.data.Project
import com.omeronur.gelisio.data.RoleGoal
import com.omeronur.gelisio.data.Session
import com.omeronur.gelisio.data.Store
import com.omeronur.gelisio.data.Week
import com.omeronur.gelisio.data.WorkPhilosophy
import com.omeronur.gelisio.data.index
import com.omeronur.gelisio.data.quarterKey
import com.omeronur.gelisio.data.weekKey
import com.omeronur.gelisio.ui.Nav
import com.omeronur.gelisio.ui.Tabs
import com.omeronur.gelisio.ui.components.GlassBackground
import com.omeronur.gelisio.ui.components.LocalHaze
import com.omeronur.gelisio.ui.screens.DeepSessionScreen
import com.omeronur.gelisio.ui.screens.HabitDetailScreen
import com.omeronur.gelisio.ui.screens.HabitWizardScreen
import com.omeronur.gelisio.ui.screens.ProjectDetailScreen
import com.omeronur.gelisio.ui.theme.GelisioTheme
import dev.chrisbanes.haze.HazeState
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Cihazsız ekran görüntüleri: sahte veriyle her sekme ve ana alt ekranlar. */
class ScreensTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6.copy(softButtons = false),
        theme = "android:Theme.Material.NoActionBar",
        maxPercentDifference = 1.0,
    )

    private val today = LocalDate.now()

    @Before
    fun data() {
        Nav.stack.clear()
        Store.profile.value = Profile(
            mission = "İşletmelere insan gibi büyümeyi öğretmek.",
            roles = listOf("Girişimci", "Partner", "Sporcu"),
            values = listOf("Dürüstlük", "Ustalık"),
            workPhilosophy = WorkPhilosophy.RHYTHMIC,
            deepBlocks = (1..5).map { DeepBlock(it, "08:00", 90, "p1") },
            onboarded = true,
        )
        Store.identities.value = listOf(Identity("i1", "her gün okuyan biriyim"), Identity("i2", "sağlığına yatırım yapan biriyim"))
        val created = today.minusDays(40)
        Store.habits.value = listOf(
            Habit("h1", "Okumak", "i1", "30 dakika okumak", "1 sayfa okumak", Cue("time", "07:30", "Salondaki koltuk"), "Kahve", createdAt = created),
            Habit("h2", "Yürüyüş", "i2", "45 dakika telefonsuz yürüyüş", "Ayakkabıları giymek", Cue("time", "18:00", "Sahil"), createdAt = created),
            Habit("h3", "Günlük", "i1", "Sayfa dolusu yazmak", "Bir cümle yazmak", Cue("stack", afterHabitId = "h1"), createdAt = created),
        )
        val logs = mutableListOf<HabitLog>()
        for (i in 1..39) {
            val d = today.minusDays(i.toLong())
            if (i % 9 != 0) logs += HabitLog("h1", d, if (i % 5 == 0) LogStatus.MIN else LogStatus.FULL)
            if (i % 4 != 0 && i != 1 && i != 2) logs += HabitLog("h2", d, LogStatus.FULL)
            if (i % 3 != 0) logs += HabitLog("h3", d, LogStatus.FULL)
        }
        logs += HabitLog("h1", today, LogStatus.FULL)
        Store.logs.value = logs
        Store.javaClass.getDeclaredField("_logIndex").apply { isAccessible = true }
            .let { (it.get(Store) as kotlinx.coroutines.flow.MutableStateFlow<Any>).value = logs.index() }
        Store.goals.value = listOf(Goal("g1", "Q4'te 3 partnerlik paketi sat", quarterKey(today), "Girişimci", LeadMeasure("derin saat", 10.0)))
        Store.projects.value = listOf(
            Project("p1", "project", "Excentri lansmanı", "g1", "Girişimci",
                listOf(Milestone("Teklif şablonu", true), Milestone("İlk 5 görüşme", true), Milestone("Pilot müşteri"), Milestone("Fiyat testi")),
                LeadMeasure("derin saat", 6.0), today.plusDays(38)),
            Project("p2", "hobby", "Suluboya", identityId = "i1", dimension = Dimension.SPIRITUAL, skillLevel = 3, weeklyPracticeMin = 120, outputType = "Tablo"),
        )
        val zone = ZoneId.systemDefault()
        val sessions = mutableListOf<Session>()
        for (w in 0..7) for (d in 0..3) {
            val date = today.minusWeeks(w.toLong()).minusDays(d.toLong())
            sessions += Session("s$w$d", if (d % 2 == 0) "p1" else "", date.atTime(8, 0).atZone(zone).toInstant().toEpochMilli(), 90 + (w * 13) % 60)
        }
        for (d in 0..60 step 2) sessions += Session("pr$d", "p2", today.minusDays(d.toLong()).atTime(20, 0).atZone(zone).toInstant().toEpochMilli(), 20 + d % 40, kind = "practice")
        Store.sessions.value = sessions.sortedByDescending { it.startMillis }
        val wk = weekKey(today)
        Store.weeks.value = mapOf(wk to Week(wk, listOf(
            RoleGoal("a", "Girişimci", "Partnerlik teklif setini bitir", 2, 2, true),
            RoleGoal("b", "Girişimci", "3 keşif görüşmesi", 2, 3),
            RoleGoal("c", "Partner", "Cumartesi akşamı birlikte yemek", 2, 6),
            RoleGoal("d", "Sporcu", "3 kuvvet antrenmanı", 2, 0),
            RoleGoal("e", "Girişimci", "Fatura takibi", 1, 1, true),
        )))
        Store.days.value = mapOf(today to DayDoc(today, setOf(Dimension.PHYSICAL)))
        Store.loaded.value = true
    }

    @Composable
    private fun Frame(content: @Composable () -> Unit) {
        GelisioTheme {
            val haze = remember { HazeState() }
            GlassBackground(haze) { CompositionLocalProvider(LocalHaze provides haze) { content() } }
        }
    }

    private fun tab(i: Int) {
        Nav.tab.intValue = i
        paparazzi.snapshot { Frame { Tabs() } }
    }

    @Test fun t0_today() = tab(0)
    @Test fun t1_week() = tab(1)
    @Test fun t2_focus() = tab(2)
    @Test fun t3_growth() = tab(3)
    @Test fun t4_me() = tab(4)
    @Test fun s_habitDetail() = paparazzi.snapshot { Frame { HabitDetailScreen("h1") } }
    @Test fun s_wizard() = paparazzi.snapshot { Frame { HabitWizardScreen(null, null) } }
    @Test fun s_project() = paparazzi.snapshot { Frame { ProjectDetailScreen("p1") } }
    @Test fun s_hobby() = paparazzi.snapshot { Frame { ProjectDetailScreen("p2") } }
    @Test fun s_deep() = paparazzi.snapshot { Frame { DeepSessionScreen(null) } }
}
