package com.omeronur.gelisio.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseUser
import com.omeronur.gelisio.data.DayEntry
import com.omeronur.gelisio.data.DayRepository
import com.omeronur.gelisio.data.principleFor
import com.omeronur.gelisio.ui.components.GlassCard
import com.omeronur.gelisio.ui.components.ProgressRing
import com.omeronur.gelisio.ui.theme.G
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayScreen(user: FirebaseUser) {
    val today = remember { LocalDate.now() }
    val key = remember(today) { DayRepository.dayKey(today) }
    var entry by remember { mutableStateOf(DayEntry.EMPTY) }
    var editing by remember { mutableStateOf(false) }

    DisposableEffect(user.uid, key) {
        val reg = DayRepository.listen(user.uid, key) { entry = it }
        onDispose { reg.remove() }
    }

    val dateText = today.format(DateTimeFormatter.ofPattern("d MMMM EEEE", Locale.forLanguageTag("tr")))
    val principle = remember { principleFor(LocalDateTime.now()) }
    val progress = if (entry.vitalDone) 1f else 0f

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(dateText, color = G.textSecondary, fontSize = 14.sp)
        Text("Bugün", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = G.text)

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ProgressRing(value = progress) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${(progress * 100).toInt()}%", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = G.text)
                    Text("hayati iş", color = G.textSecondary, fontSize = 13.sp)
                }
            }
        }

        GlassCard(highlighted = true) {
            Text("En hayati iş", color = G.mint, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            if (entry.vitalTask.isBlank()) {
                Text("Bugün tek bir şey seç. Bu yapılmadan gün başarılı sayılmaz.", color = G.textSecondary)
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = { editing = true }) { Text("Seç", color = G.mint) }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = entry.vitalDone,
                        onCheckedChange = { DayRepository.setVitalDone(user.uid, key, it) },
                        colors = CheckboxDefaults.colors(checkedColor = G.mint, checkmarkColor = G.bgBottom),
                    )
                    Text(
                        entry.vitalTask,
                        color = G.text,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { editing = true }) { Text("Düzenle", color = G.textSecondary) }
                }
            }
        }

        GlassCard {
            Text("İlke · ${principle.source}", color = G.lavender, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(principle.text, color = G.text, fontSize = 15.sp)
        }

        GlassCard {
            Text("Yakında", color = G.textSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text("Alışkanlık takibi, bildirimler ve kart üstü seri sayacı v1.0'da.", color = G.textSecondary, fontSize = 14.sp)
        }
    }

    if (editing) {
        var draft by remember(entry.vitalTask) { mutableStateOf(entry.vitalTask) }
        AlertDialog(
            onDismissRequest = { editing = false },
            containerColor = G.bgTop,
            title = { Text("En hayati iş", color = G.text) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { if (it.length <= 120) draft = it },
                    placeholder = { Text("Tek cümle", color = G.textSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    DayRepository.setVitalTask(user.uid, key, draft)
                    editing = false
                }) { Text("Kaydet", color = G.mint) }
            },
            dismissButton = {
                TextButton(onClick = { editing = false }) { Text("Vazgeç", color = G.textSecondary) }
            },
        )
    }
}
