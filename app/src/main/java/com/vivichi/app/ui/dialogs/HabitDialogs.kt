@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.vivichi.app.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.vivichi.app.data.Habit
import com.vivichi.app.ui.components.EmojiGlyph
import com.vivichi.app.ui.theme.*
import com.vivichi.app.util.SoundFx
import com.vivichi.app.util.formatHabitTime

private val COMMON_EMOJIS = listOf(
    "🪥","💧","🍳","🥗","📚","🏃","📖","😴","🎵","🧘","💧","🧴","🍎","🐾","🎨","🎯","🎮","🎵","🎹","🧹","🌿","📓","📝","⏰","🐱","🐻","🎨","💼","🥤","🍵","😴","🧠","❤️","⭐","✅","🔥","💪","📷","🌸","🌿","☀️","⚡","🎯","💊","📞","📅","☕","🪴","🐾","🎵","☕","🚿","🧴","📖","🎧","🍎","🥳","🥰","😄","🤩","😎","🎺","🎸","🎹","🎤","🔥","🎁"
).distinct()

@Composable
fun AddEditHabitDialog(
    existing: Habit?,
    onDismiss: () -> Unit,
    onSave: (name: String, icon: String, xp: Int, time: String) -> Unit,
    use24h: Boolean = false
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var icon by remember { mutableStateOf(existing?.icon ?: "✨") }
    var xp by remember { mutableIntStateOf(existing?.xp ?: 3) }
    var time by remember { mutableStateOf(existing?.time ?: "09:00") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(26.dp))
                .background(Color.White)
                .padding(20.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (existing != null) "Edit Habit" else "New Habit", fontWeight = FontWeight.Black, fontSize = 16.sp)
                IconButton(onClick = { SoundFx.click(); onDismiss() }) { Icon(Icons.Filled.Close, null, tint = SoftText) }
            }
            Spacer(Modifier.height(12.dp))

            FieldLabel("Habit name")
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                placeholder = { Text("e.g. Drink water") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            )
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    FieldLabel("Icon")
                    OutlinedButton(
                        onClick = { SoundFx.click(); showEmojiPicker = true },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { EmojiGlyph(raw = icon, size = 20.dp) }
                }
                Column(Modifier.weight(1.2f)) {
                    FieldLabel("Unlock time")
                    OutlinedButton(
                        onClick = { SoundFx.click(); showTimePicker = true },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(formatHabitTime(time, use24h), fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(12.dp))

            FieldLabel("Intensity")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                IntensityChip("3 XP", "Low", xp == 3, Modifier.weight(1f)) { SoundFx.click(); xp = 3 }
                IntensityChip("5 XP", "Medium", xp == 5, Modifier.weight(1f)) { SoundFx.click(); xp = 5 }
                IntensityChip("10 XP", "High", xp == 10, Modifier.weight(1f)) { SoundFx.click(); xp = 10 }
            }
            if (existing == null) {
                Text(
                    "New habits give XP right away and start earning coins tomorrow.",
                    fontSize = 11.sp, color = SoftText, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            Spacer(Modifier.height(16.dp))

            VivichiButton(
                text = if (existing != null) "Save Changes" else "Add Habit",
                onClick = { if (name.isNotBlank()) onSave(name.trim(), icon, xp, time) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showEmojiPicker) {
        EmojiPickerDialog(onPick = { icon = it; showEmojiPicker = false }, onDismiss = { showEmojiPicker = false })
    }
    if (showTimePicker) {
        TimePickerDialog(initial = time, use24h = use24h, onPick = { time = it; showTimePicker = false }, onDismiss = { showTimePicker = false })
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = SoftText, modifier = Modifier.padding(bottom = 5.dp))
}

@Composable
private fun IntensityChip(value: String, label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Color(0xFFFFF0F5) else BgPink)
            .then(Modifier)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
            TextButton(onClick = onClick, contentPadding = PaddingValues(0.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = TextDark)
                    Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SoftText)
                }
            }
        }
    }
}

@Composable
private fun EmojiPickerDialog(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White).padding(16.dp)) {
            Text("Pick an icon", fontWeight = FontWeight.Black, fontSize = 14.sp, modifier = Modifier.padding(bottom = 10.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(6), modifier = Modifier.heightIn(max = 320.dp)) {
                items(COMMON_EMOJIS) { emo ->
                    TextButton(onClick = { SoundFx.click(); onPick(emo) }) { EmojiGlyph(raw = emo, size = 22.dp) }
                }
            }
        }
    }
}

@Composable
private fun TimePickerDialog(initial: String, use24h: Boolean = false, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val parts = initial.split(":")
    var hour by remember { mutableIntStateOf(parts.getOrNull(0)?.toIntOrNull() ?: 9) }
    var minute by remember { mutableIntStateOf(parts.getOrNull(1)?.toIntOrNull() ?: 0) }
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = use24h)
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TimePicker(state = state)
            Spacer(Modifier.height(10.dp))
            VivichiButton(text = "Set time", onClick = {
                val h = state.hour.toString().padStart(2, '0')
                val m = state.minute.toString().padStart(2, '0')
                onPick("$h:$m")
            })
        }
    }
}

@Composable
fun EditTimesDialog(habits: List<Habit>, onDismiss: () -> Unit, onSave: (Map<String, String>) -> Unit, use24h: Boolean = false) {
    val times = remember { mutableStateMapOf<String, String>().apply { habits.forEach { put(it.id, it.time) } } }
    var editingId by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(26.dp))
                .background(Color.White)
                .padding(20.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Edit Times", fontWeight = FontWeight.Black, fontSize = 16.sp)
                IconButton(onClick = { SoundFx.click(); onDismiss() }) { Icon(Icons.Filled.Close, null, tint = SoftText) }
            }
            Spacer(Modifier.height(10.dp))
            LazyColumn(Modifier.heightIn(max = 400.dp)) {
                items(habits) { h ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(BgPink, RoundedCornerShape(14.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EmojiGlyph(raw = h.icon, size = 16.dp)
                        Text(h.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = { SoundFx.click(); editingId = h.id }, shape = RoundedCornerShape(10.dp)) {
                            Text(formatHabitTime(times[h.id] ?: h.time, use24h), fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            VivichiButton(text = "Save Changes", onClick = { onSave(times.toMap()) }, modifier = Modifier.fillMaxWidth())
        }
    }

    editingId?.let { id ->
        TimePickerDialog(initial = times[id] ?: "09:00", use24h = use24h, onPick = { times[id] = it; editingId = null }, onDismiss = { editingId = null })
    }
}
