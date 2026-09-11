package com.vivichi.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vivichi.app.data.TitleInfo
import com.vivichi.app.domain.GameLogic
import com.vivichi.app.ui.VivichiViewModel
import com.vivichi.app.ui.components.EmojiGlyph
import com.vivichi.app.ui.dialogs.ShareStreakDialog
import com.vivichi.app.ui.theme.*
import com.vivichi.app.util.SoundFx
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StatsScreen(viewModel: VivichiViewModel) {
    val state by viewModel.state.collectAsState()
    val today = LocalDate.now()
    var showShare by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 13.dp)) {
        item { Text("Stats", fontSize = 21.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 20.dp)) }

        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.horizontalGradient(listOf(Yellow, Orange)))
                    .clickable { SoundFx.click(); showShare = true }
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Day Streak", color = Color.White.copy(alpha = 0.88f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("${state.streak}", color = Color.White, fontSize = 54.sp, fontWeight = FontWeight.Black, lineHeight = 54.sp)
                Text("Tap to share!", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
            }
        }

        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                StatCard("${state.bestStreak}", "Best Streak", Modifier.weight(1f))
                StatCard("${state.pet.level}", "Level", Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(bottom = 5.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                StatCard("${state.totalXP}", "Total XP", Modifier.weight(1f))
                StatCard("${state.totalDone}", "Habits Done", Modifier.weight(1f))
            }
        }

        item {
            Text("Last 7 Days", fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp, bottom = 10.dp))
        }
        item {
            Row(Modifier.fillMaxWidth().height(72.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                for (i in 6 downTo 0) {
                    val day = today.minusDays(i.toLong())
                    val pct = if (i == 0) GameLogic.completionPct(state) else (state.history.getOrNull(i - 1)?.pct ?: 0)
                    val barHeight = (4 + (pct / 100f) * 62).dp
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(barHeight)
                                .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                .background(Brush.verticalGradient(if (i == 0) listOf(Pink, PinkDark) else listOf(Purple, PurpleDark)))
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(1), fontSize = 9.sp, fontWeight = FontWeight.Black, color = SoftText)
                    }
                }
            }
        }

        item { Text("Titles", fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 18.dp, bottom = 10.dp)) }
        val earned = GameLogic.earnedTitles(state)
        items(earned) { (title, isEarned) ->
            TitleRow(title, isEarned, state.activeTitle == title.id) { viewModel.equipTitle(title.id) }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }

    if (showShare) {
        ShareStreakDialog(state = state, onDismiss = { showShare = false })
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.Black, color = Pink)
        Text(label, fontSize = 10.sp, color = SoftText, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun TitleRow(title: TitleInfo, earned: Boolean, active: Boolean, onEquip: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 7.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(13.dp)
            .then(if (!earned) Modifier else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(36.dp)) { EmojiGlyph(raw = title.emoji, size = 20.dp) }
        Column(Modifier.weight(1f)) {
            Text(title.name, fontSize = 13.sp, fontWeight = FontWeight.Black, color = if (earned) TextDark else MutedText)
            Text(if (earned) "✓ Earned" else "🔒 ${title.requirement}", fontSize = 11.sp, color = SoftText, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 1.dp))
        }
        if (earned) {
            Button(
                onClick = { SoundFx.click(); onEquip() },
                colors = ButtonDefaults.buttonColors(containerColor = if (active) GreenDark else PinkDark),
                shape = RoundedCornerShape(9.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) { Text(if (active) "Equipped" else "Equip", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White) }
        }
    }
}
