@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.omeronur.gelisio.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omeronur.gelisio.data.TR_DAYS
import com.omeronur.gelisio.data.parseTime
import com.omeronur.gelisio.ui.icons.GelisioIcons
import com.omeronur.gelisio.ui.theme.G
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

val TR: Locale = Locale.forLanguageTag("tr")
fun LocalDate.trLong(): String = format(DateTimeFormatter.ofPattern("d MMMM EEEE", TR))
fun LocalDate.trShort(): String = format(DateTimeFormatter.ofPattern("d MMM", TR))

private val dialogShape = RoundedCornerShape(28.dp)
private val dialogColor = Color(0xFF161B28)

/** Saat seçici alanı: "HH:mm" metni tutar, dokununca 24 saatlik seçici açılır. */
@Composable
fun TimeField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = G.textSecondary)
        Spacer(Modifier.height(7.dp))
        Row(
            Modifier.fillMaxWidth().pressable({ open = true }).glass(RoundedCornerShape(G.buttonRadius), strong = true)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GIcon(GelisioIcons.clock, size = 15.sp, color = G.mint)
            Spacer(Modifier.width(10.dp))
            Text(value.ifBlank { "Saat seç" }, style = MaterialTheme.typography.bodyLarge, color = if (value.isBlank()) G.textTertiary else G.text)
        }
    }
    if (open) {
        val t = parseTime(value)
        val state = rememberTimePickerState(t?.hour ?: 8, t?.minute ?: 0, is24Hour = true)
        AlertDialog(
            onDismissRequest = { open = false },
            shape = dialogShape,
            containerColor = dialogColor,
            confirmButton = {
                TextAction("Tamam", {
                    onChange("%02d:%02d".format(state.hour, state.minute))
                    open = false
                })
            },
            dismissButton = { TextAction("Vazgeç", { open = false }, color = G.textSecondary) },
            text = {
                TimePicker(
                    state = state,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = Color(0x14FFFFFF),
                        selectorColor = G.mint,
                        timeSelectorSelectedContainerColor = G.mint.copy(alpha = 0.25f),
                        timeSelectorUnselectedContainerColor = Color(0x14FFFFFF),
                        timeSelectorSelectedContentColor = G.text,
                        timeSelectorUnselectedContentColor = G.textSecondary,
                        clockDialSelectedContentColor = G.bgBottom,
                        clockDialUnselectedContentColor = G.text,
                    ),
                )
            },
        )
    }
}

@Composable
fun DateField(label: String, value: LocalDate?, onChange: (LocalDate?) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = G.textSecondary)
        Spacer(Modifier.height(7.dp))
        Row(
            Modifier.fillMaxWidth().pressable({ open = true }).glass(RoundedCornerShape(G.buttonRadius), strong = true)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GIcon(GelisioIcons.week, size = 15.sp, color = G.mint)
            Spacer(Modifier.width(10.dp))
            Text(value?.trLong() ?: "Tarih seç", style = MaterialTheme.typography.bodyLarge, color = if (value == null) G.textTertiary else G.text)
        }
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = value?.atStartOfDay()?.toInstant(ZoneOffset.UTC)?.toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { open = false },
            shape = dialogShape,
            colors = DatePickerDefaults.colors(containerColor = dialogColor),
            confirmButton = {
                TextAction("Tamam", {
                    onChange(state.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() })
                    open = false
                })
            },
            dismissButton = { TextAction("Temizle", { onChange(null); open = false }, color = G.textSecondary) },
        ) {
            DatePicker(
                state = state,
                colors = DatePickerDefaults.colors(
                    containerColor = dialogColor,
                    selectedDayContainerColor = G.mint,
                    selectedDayContentColor = G.bgBottom,
                    todayDateBorderColor = G.lavender,
                    todayContentColor = G.lavender,
                ),
            )
        }
    }
}

/** Haftanın günleri (1 = Pazartesi). */
@Composable
fun DaysPicker(days: List<Int>, onChange: (List<Int>) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TR_DAYS.forEachIndexed { i, d ->
            val day = i + 1
            Chip(d, day in days, {
                val next = if (day in days) days - day else days + day
                if (next.isNotEmpty()) onChange(next.sorted())
            })
        }
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String = "Sil", onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = dialogShape,
        containerColor = dialogColor,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium, color = G.textSecondary) },
        confirmButton = { TextAction(confirm, { onConfirm(); onDismiss() }, color = G.critical) },
        dismissButton = { TextAction("Vazgeç", onDismiss, color = G.textSecondary) },
    )
}

/** Tek alanlı metin girişi penceresi. */
@Composable
fun InputDialog(
    title: String,
    initial: String = "",
    placeholder: String = "",
    confirm: String = "Kaydet",
    singleLine: Boolean = true,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var v by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = dialogShape,
        containerColor = dialogColor,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { GlassField(v, { v = it }, placeholder = placeholder, singleLine = singleLine, maxLength = 300) },
        confirmButton = {
            TextAction(confirm, {
                if (v.isNotBlank()) {
                    onConfirm(v.trim())
                    onDismiss()
                }
            })
        },
        dismissButton = { TextAction("Vazgeç", onDismiss, color = G.textSecondary) },
    )
}

/** Düzenlenebilir metin listesi (roller, değerler, kilometre taşları). */
@Composable
fun EditableList(
    items: List<String>,
    onChange: (List<String>) -> Unit,
    addLabel: String,
    placeholder: String,
    max: Int = 50,
) {
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Int?>(null) }
    GlassCard(padding = 8.dp) {
        if (items.isEmpty()) {
            Text("Henüz yok.", Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium, color = G.textSecondary)
        }
        items.forEachIndexed { i, s ->
            Row(Modifier.fillMaxWidth().padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(s, Modifier.weight(1f).pressable({ editing = i }).padding(vertical = 12.dp), style = MaterialTheme.typography.bodyLarge)
                IconGlassButton(GelisioIcons.close, { onChange(items.filterIndexed { j, _ -> j != i }) }, size = 34.dp, color = G.textSecondary)
            }
            if (i < items.lastIndex) Divider()
        }
    }
    if (items.size < max) GlassButton(addLabel, { adding = true }, Modifier.fillMaxWidth(), icon = GelisioIcons.add)
    if (adding) InputDialog(addLabel, placeholder = placeholder, onConfirm = { onChange(items + it) }, onDismiss = { adding = false })
    editing?.let { idx ->
        InputDialog("Düzenle", initial = items.getOrNull(idx).orEmpty(), onConfirm = { v ->
            onChange(items.mapIndexed { j, s -> if (j == idx) v else s })
        }, onDismiss = { editing = null })
    }
}
