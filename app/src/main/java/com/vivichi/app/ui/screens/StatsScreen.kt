package com.vivichi.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle as UiTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vivichi.app.data.TitleInfo
import com.vivichi.app.domain.GameLogic
import com.vivichi.app.ui.VivichiViewModel
import com.vivichi.app.ui.components.*
import com.vivichi.app.ui.dialogs.ShareStreakDialog
import com.vivichi.app.ui.theme.*
import com.vivichi.app.util.SoundFx
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StatsScreen(viewModel: VivichiViewModel) {
    val state by viewModel.state.collectAsState()
    val today = LocalDate.now()
    var showShare by remember { mutableStateOf(false) }
    val earned = GameLogic.earnedTitles(state)
    val earnedCount = earned.count { it.second }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 20.dp)) {
        item {
            PageHeader(
                title = "Stats",
                subtitle = "$earnedCount of ${earned.size} titles earned",
                emoji = "🏆",
                accent = listOf(Yellow, Orange)
            )
        }

        item {
            Box(
                Modifier
                    .padding(16.dp, 12.dp, 16.dp, 4.dp)
                    .enterFromBelow(1)
                    .fillMaxWidth()
                    .cardShadow(26.dp, 6.dp)
                    .bounceClick(RoundedCornerShape(26.dp)) { SoundFx.click(); showShare = true }
                    .background(Brush.linearGradient(listOf(Color(0xFFFFC857), Color(0xFFFF8A3D), Color(0xFFFF6B8B))))
            ) {
                FloatingSparkles(Modifier.matchParentSize(), color = Color.White, count = 12, seed = 5)
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("CURRENT STREAK", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Row(verticalAlignment = Alignment.Bottom) {
                            CountUpText(state.streak, UiTextStyle(color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Black, lineHeight = 52.sp))
                            Text(" days", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        Text("Best: ${state.bestStreak} days · Tap to share", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    EmojiGlyph(raw = "🔥", size = 64.dp, modifier = Modifier.wiggling(6f, 700).floating(4.dp, 1600))
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth().padding(16.dp, 10.dp, 16.dp, 0.dp).enterFromBelow(2), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(state.pet.level, "Level", "⭐", PurpleDark, Modifier.weight(1f))
                StatCard(state.totalXP, "Total XP", "✨", Orange, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(16.dp, 10.dp, 16.dp, 0.dp).enterFromBelow(3), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(state.totalDone, "Habits done", "✅", GreenDark, Modifier.weight(1f))
                StatCard(state.cemetery.size, "Buddies lost", "🌸", PinkDark, Modifier.weight(1f))
            }
        }

        item { SectionTitle("Last 7 days", "🎯", trailing = "Today ${GameLogic.completionPct(state)}%") }
        item {
            Row(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .enterFromBelow(4)
                    .card(radius = 22.dp)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .height(90.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
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
                                .clip(RoundedCornerShape(6.dp))
                                .background(Brush.verticalGradient(if (i == 0) listOf(Pink, PinkDark) else listOf(Purple, PurpleDark)))
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(1),
                            fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (i == 0) PinkDark else SoftText
                        )
                    }
                }
            }
        }

        item { SectionTitle("Titles", "👑", trailing = "$earnedCount/${earned.size}") }
        itemsIndexed(earned) { index, (title, isEarned) ->
            Box(Modifier.padding(horizontal = 16.dp).enterFromBelow(5 + index)) {
                TitleRow(title, isEarned, state.activeTitle == title.id) { viewModel.equipTitle(title.id) }
            }
        }
    }

    if (showShare) {
        ShareStreakDialog(state = state, onDismiss = { showShare = false })
    }
}

@Composable
private fun StatCard(value: Int, label: String, emoji: String, accent: Color, modifier: Modifier = Modifier) {
    Row(
        modifier
            .card(radius = 20.dp)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) { EmojiGlyph(raw = emoji, size = 20.dp) }
        Spacer(Modifier.width(10.dp))
        Column {
            CountUpText(value, UiTextStyle(fontSize = 22.sp, fontWeight = FontWeight.Black, color = TextDark))
            Text(label, fontSize = 10.sp, color = SoftText, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TitleRow(title: TitleInfo, earned: Boolean, active: Boolean, onEquip: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .card(radius = 18.dp, color = if (earned) Color.White else Color.White.copy(alpha = 0.7f), elevation = if (earned) 3.dp else 1.dp)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (earned) Brush.linearGradient(listOf(Color(0xFFFFE9A8), Color(0xFFFFC56B)))
                    else Brush.linearGradient(listOf(Color(0xFFF1EDF6), Color(0xFFE7E1EF)))
                ),
            contentAlignment = Alignment.Center
        ) { EmojiGlyph(raw = if (earned) title.emoji else "🔒", size = 20.dp) }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title.name, fontSize = 14.sp, fontWeight = FontWeight.Black, color = if (earned) TextDark else SoftText)
            Text(
                if (earned) "Earned" else title.requirement,
                fontSize = 11.sp, color = if (earned) GreenDark else MutedText, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
        if (earned) {
            Box(
                Modifier
                    .bounceClick(RoundedCornerShape(12.dp)) { SoundFx.click(); onEquip() }
                    .background(if (active) Brush.linearGradient(listOf(Green, GreenDark)) else Brush.linearGradient(listOf(Pink, PinkDark)))
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) { Text(if (active) "Equipped" else "Equip", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White) }
        }
    }
}
