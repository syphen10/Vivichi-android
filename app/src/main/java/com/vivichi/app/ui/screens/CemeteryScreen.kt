package com.vivichi.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.vivichi.app.data.CemeteryEntry
import com.vivichi.app.data.petEmoji
import com.vivichi.app.ui.VivichiViewModel
import com.vivichi.app.ui.components.EmojiGlyph
import com.vivichi.app.ui.theme.*
import com.vivichi.app.util.SoundFx

@Composable
fun CemeteryScreen(viewModel: VivichiViewModel) {
    val state by viewModel.state.collectAsState()
    var detail by remember { mutableStateOf<CemeteryEntry?>(null) }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF2D2D5A), RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
                .padding(16.dp, 20.dp, 16.dp, 16.dp)
        ) {
            Text("Pet Cemetery", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
            Text("Those who lived on, in memory", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 3.dp))
        }

        if (state.cemetery.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(40.dp, 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                EmojiGlyph(raw = "🌱", size = 48.dp)
                Spacer(Modifier.height(10.dp))
                Text("No pets here yet", fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text("Keep your buddy healthy and they will never end up here!", fontSize = 12.sp, color = SoftText, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
            }
        } else {
            LazyColumn(Modifier.padding(13.dp)) {
                items(state.cemetery, key = { it.id }) { entry ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 11.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .clickable { SoundFx.click(); detail = entry }
                            .padding(16.dp, 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(58.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF3F0FF)),
                            contentAlignment = Alignment.Center
                        ) { EmojiGlyph(raw = petEmoji(entry.species), size = 30.dp) }
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(entry.name, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            Text("${entry.species.replaceFirstChar { it.uppercase() }} · Lv.${entry.level}", fontSize = 11.sp, color = SoftText, fontWeight = FontWeight.SemiBold)
                            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                MiniStat(emoji = "🔥", value = "${entry.streak}")
                                MiniStat(emoji = "✅", value = "${entry.totalDone}")
                                MiniStat(emoji = "⭐", value = "${entry.totalXP}")
                            }
                        }
                        Box(Modifier.clip(RoundedCornerShape(20.dp)).background(BgPink).padding(9.dp, 4.dp)) {
                            Text("${entry.daysAlive}d", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MutedText)
                        }
                    }
                }
            }
        }
    }

    detail?.let { entry ->
        Dialog(onDismissRequest = { detail = null }) {
            Column(
                Modifier.clip(RoundedCornerShape(26.dp)).background(Color.White).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Pet Memorial", fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp))
                EmojiGlyph(raw = petEmoji(entry.species), size = 60.dp)
                Text(entry.name, fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 6.dp))
                Text("${entry.species} | Born: ${entry.bornAt} | Died: ${entry.diedAt}", fontSize = 12.sp, color = SoftText, fontWeight = FontWeight.SemiBold)
                Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailStat("${entry.daysAlive}d", "Days Alive")
                    DetailStat("Lv.${entry.level}", "Level")
                    DetailStat("${entry.streak}", "Best Streak")
                }
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailStat("${entry.totalDone}", "Habits Done")
                    DetailStat("${entry.totalXP}", "Total XP")
                    DetailStat(entry.outfit, "Last Outfit")
                }
            }
        }
    }
}

@Composable
private fun MiniStat(emoji: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        EmojiGlyph(raw = emoji, size = 11.dp)
        Spacer(Modifier.width(3.dp))
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Black, color = SoftText)
    }
}

@Composable
private fun DetailStat(value: String, label: String) {
    Column(
        Modifier
            .width(88.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(BgPink)
            .padding(vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Black, color = PinkDark)
        Text(label, fontSize = 9.sp, color = SoftText, fontWeight = FontWeight.Bold)
    }
}
