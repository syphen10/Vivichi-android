package com.vivichi.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.ui.text.TextStyle as UiTextStyle
import com.vivichi.app.ui.components.CountUpText
import com.vivichi.app.ui.components.FloatingSparkles
import com.vivichi.app.ui.components.bounceClick
import com.vivichi.app.ui.components.enterFromBelow
import com.vivichi.app.ui.components.wiggling
import kotlinx.coroutines.delay
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
        item { Text("Stats", fontSize = 21.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp).enterFromBelow(0)) }

        item {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .enterFromBelow(1)
                    .fillMaxWidth()
                    .bounceClick(RoundedCornerShape(26.dp)) { SoundFx.click(); showShare = true }
                    .background(Brush.linearGradient(listOf(Yellow, Orange)))
            ) {
                FloatingSparkles(Modifier.matchParentSize(), color = Color.White, count = 12, seed = 5)
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EmojiGlyph(raw = "🔥", size = 16.dp, modifier = Modifier.wiggling(10f, 520))
                        Text(" Day Streak", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    CountUpText(state.streak, UiTextStyle(color = Color.White, fontSize = 54.sp, fontWeight = FontWeight.Black, lineHeight = 54.sp))
                    Text("Tap to share!", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp).enterFromBelow(2), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                StatCard(state.bestStreak, "Best Streak", Modifier.weight(1f))
                StatCard(state.pet.level, "Level", Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(bottom = 5.dp).enterFromBelow(3), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                StatCard(state.totalXP, "Total XP", Modifier.weight(1f))
                StatCard(state.totalDone, "Habits Done", Modifier.weight(1f))
            }
        }

        item {
            Text("Last 7 Days", fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp, bottom = 10.dp))
        }
        item {
            // Row must fit the tallest bar (4 + 58 = 62dp) plus spacer and day label (~16dp);
            // at 72dp with 66dp bars the labels under full bars were pushed out and clipped.
            Row(Modifier.fillMaxWidth().height(90.dp).enterFromBelow(4), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                for (i in 6 downTo 0) {
                    val day = today.minusDays(i.toLong())
                    val pct = if (i == 0) GameLogic.completionPct(state) else (state.history.getOrNull(i - 1)?.pct ?: 0)
                    // Bars grow up from the baseline one after another, oldest day first.
                    val grow = remember { Animatable(0f) }
                    LaunchedEffect(pct) {
                        delay((6 - i) * 70L + 150L)
                        grow.animateTo(pct / 100f, spring(dampingRatio = 0.6f, stiffness = 180f))
                    }
                    val barHeight = (4 + grow.value.coerceIn(0f, 1.1f) * 58).dp
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
        itemsIndexed(earned) { index, (title, isEarned) ->
            Box(Modifier.enterFromBelow(5 + index)) {
                TitleRow(title, isEarned, state.activeTitle == title.id) { viewModel.equipTitle(title.id) }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }

    if (showShare) {
        ShareStreakDialog(state = state, onDismiss = { showShare = false })
    }
}

@Composable
private fun StatCard(value: Int, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CountUpText(value, UiTextStyle(fontSize = 26.sp, fontWeight = FontWeight.Black, color = Pink))
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
