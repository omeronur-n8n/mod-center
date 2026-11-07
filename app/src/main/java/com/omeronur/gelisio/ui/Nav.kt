package com.omeronur.gelisio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omeronur.gelisio.ui.components.IconGlassButton
import com.omeronur.gelisio.ui.icons.GelisioIcons
import com.omeronur.gelisio.ui.theme.G

/** Uygulama içi ekranlar. Sekmeler kökte; diğer her şey yığına itilir. */
sealed interface Screen {
    data object Onboarding : Screen
    data class HabitWizard(val habitId: String? = null, val template: HabitTemplate? = null) : Screen
    data class HabitDetail(val habitId: String) : Screen
    data class JustStart(val habitId: String? = null) : Screen
    data object Scorecard : Screen
    data object PlanWizard : Screen
    data object WeeklyReview : Screen
    data object Matrix : Screen
    data class GoalEditor(val goalId: String? = null) : Screen
    data class DeepSession(val projectId: String? = null) : Screen
    data object DeepBlocks : Screen
    data object Sessions : Screen
    data class ProjectEditor(val projectId: String? = null, val kind: String = "project") : Screen
    data class ProjectDetail(val projectId: String) : Screen
    data object Mission : Screen
    data object Roles : Screen
    data object Values : Screen
    data object Identities : Screen
    data object Philosophy : Screen
    data object Notifications : Screen
    data object Quotes : Screen
    data object About : Screen
}

/** Gönüllü yalnızlık ve benzeri hazır şablonlar alışkanlık sihirbazını önceden doldurur. */
data class HabitTemplate(val name: String, val full: String, val twoMin: String, val place: String = "")

object Nav {
    val stack = mutableStateListOf<Screen>()
    val tab = mutableIntStateOf(0)
    val toast = mutableStateOf<String?>(null)

    fun push(s: Screen) {
        stack.add(s)
    }

    fun pop() {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
    }

    fun replace(s: Screen) {
        pop()
        push(s)
    }

    fun toast(msg: String) {
        toast.value = msg
    }
}

/** Sekme sayfası: büyük başlık (Apple tarzı), kaydırılabilir içerik, alt çubuğa yer. */
@Composable
fun TabPage(
    title: String,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (subtitle != null) Text(subtitle.uppercase(com.omeronur.gelisio.ui.components.TR), style = MaterialTheme.typography.labelSmall, color = G.textSecondary)
                Text(title, style = MaterialTheme.typography.headlineLarge)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
        }
        content()
        Spacer(Modifier.height(130.dp))
    }
}

/** Alt sayfa: geri butonu + başlık; alt kısımda isteğe bağlı sabit eylem çubuğu. */
@Composable
fun SubPage(
    title: String,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconGlassButton(GelisioIcons.back, { Nav.pop() })
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            content()
            Spacer(Modifier.height(if (bottom == null) 40.dp else 16.dp))
        }
        if (bottom != null) {
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = bottom,
            )
        }
    }
}
