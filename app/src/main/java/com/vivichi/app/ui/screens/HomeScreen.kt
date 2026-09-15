package com.vivichi.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vivichi.app.data.xpForLevel
import com.vivichi.app.domain.GameLogic
import com.vivichi.app.domain.HabitStatus
import com.vivichi.app.ui.VivichiViewModel
import com.vivichi.app.ui.components.AnimatedBar
import com.vivichi.app.ui.components.CharacterView
import com.vivichi.app.ui.components.EmojiGlyph
import com.vivichi.app.ui.components.FloatingSparkles
import com.vivichi.app.ui.components.bounceClick
import com.vivichi.app.ui.components.enterFromBelow
import com.vivichi.app.ui.components.pulsing
import com.vivichi.app.ui.components.wiggling
import com.vivichi.app.ui.dialogs.ShareStreakDialog
import com.vivichi.app.ui.theme.*
import com.vivichi.app.util.SoundFx

@Composable
fun HomeScreen(viewModel: VivichiViewModel, onNavigateHabits: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val mood = GameLogic.characterMood(state)
    val activeTitle = GameLogic.activeTitle(state)
    val health = state.pet.health
    var showShare by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(16.dp, 12.dp, 16.dp, 0.dp).enterFromBelow(0),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(GameLogic.greeting(), fontSize = 20.sp, fontWeight = FontWeight.Black)
                Row(
                    Modifier
                        .bounceClick(RoundedCornerShape(22.dp)) { SoundFx.click(); showShare = true }
                        .background(Brush.horizontalGradient(listOf(Yellow, Orange)))
                        .padding(horizontal = 13.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EmojiGlyph(raw = "🔥", size = 15.dp, modifier = if (state.streak > 0) Modifier.wiggling(10f, 520) else Modifier)
                    Spacer(Modifier.width(4.dp))
                    Text("${state.streak}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
            }
        }

        if (health < 75) {
            item {
                val (bg, fg, msg) = when {
                    health < 25 -> Triple(Color(0xFFFF8080), Color.White, "CRITICAL — Complete habits NOW or ${state.pet.name} will die!")
                    health < 50 -> Triple(Color(0xFFFFE5E5), Color(0xFFC0392B), "${state.pet.name} is getting weaker. Do not miss any more habits!")
                    else -> Triple(Color(0xFFFFF3CD), Color(0xFF8B6914), "${state.pet.name} needs more care today.")
                }
                Text(
                    msg, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .padding(13.dp, 8.dp, 13.dp, 0.dp)
                        .enterFromBelow(1)
                        .then(if (health < 25) Modifier.pulsing(1.025f, 700) else Modifier)
                        .fillMaxWidth()
                        .background(bg, RoundedCornerShape(18.dp))
                        .padding(14.dp, 9.dp)
                )
            }
        }

        item {
            val glow = seasonalGradient(state.pet.outfit)
            Box(
                Modifier
                    .padding(13.dp, 11.dp, 13.dp, 0.dp)
                    .enterFromBelow(1)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.White)
            ) {
                // Soft seasonal glow behind the pet plus drifting sparkles: the card feels alive
                // even when nothing is changing.
                Box(
                    Modifier
                        .matchParentSize()
                        .background(
                            Brush.radialGradient(
                                listOf(glow.first().copy(alpha = 0.28f), glow.last().copy(alpha = 0.10f), Color.Transparent)
                            )
                        )
                )
                FloatingSparkles(Modifier.matchParentSize(), color = glow.first().copy(alpha = 0.9f), count = 10, seed = state.pet.species.hashCode())
                Column(
                    Modifier.fillMaxWidth().padding(16.dp, 18.dp, 16.dp, 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.pet.name, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    activeTitle?.let {
                        Box(
                            Modifier
                                .padding(top = 4.dp, bottom = 2.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.horizontalGradient(listOf(Yellow, Orange)))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text("${it.emoji} ${it.name}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    CharacterView(species = state.pet.species, mood = mood, outfit = state.pet.outfit, health = health, size = 180.dp)
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        val label = when { health >= 75 -> "Healthy 💚"; health >= 50 -> "Tired 💛"; health >= 25 -> "Weak 🧡"; else -> "Frail ❤️" }
                        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Black, color = SoftText, modifier = Modifier.width(70.dp))
                        val barColor by animateColorAsState(
                            when { health >= 75 -> GreenDark; health >= 50 -> Orange; health >= 25 -> Red; else -> Color(0xFFCC0000) },
                            tween(600), label = "health"
                        )
                        AnimatedBar(health / 100f, SolidColor(barColor), Modifier.weight(1f), height = 7.dp)
                        Text("$health%", fontSize = 10.sp, fontWeight = FontWeight.Black, color = SoftText, modifier = Modifier.width(34.dp), textAlign = TextAlign.End)
                    }
                }
            }
        }

        item {
            Row(
                Modifier
                    .padding(13.dp, 8.dp, 13.dp, 0.dp)
                    .enterFromBelow(2)
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(18.dp))
                    .padding(13.dp, 10.dp),
                verticalAlignment = Alignment.Top
            ) {
                EmojiGlyph(raw = "🐾", size = 19.dp, modifier = Modifier.padding(top = 1.dp, end = 9.dp))
                Column {
                    Text("${state.pet.name} SAYS", fontSize = 9.sp, fontWeight = FontWeight.Black, color = MutedText)
                    AnimatedContent(
                        targetState = GameLogic.buddyMessage(state),
                        transitionSpec = { (fadeIn(tween(300)) + slideInVertically { it / 2 }) togetherWith (fadeOut(tween(150)) + slideOutVertically { -it / 2 }) },
                        label = "says"
                    ) { msg ->
                        Text(msg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 17.sp)
                    }
                }
            }
        }

        item {
            val need = xpForLevel(state.pet.level)
            Column(Modifier.padding(13.dp, 9.dp, 13.dp, 2.dp).enterFromBelow(3)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Level ${state.pet.level}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = PinkDark)
                    Text("${state.pet.xp}/$need XP", fontSize = 11.sp, color = SoftText, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(6.dp))
                AnimatedBar(
                    (state.pet.xp.toFloat() / need).coerceIn(0f, 1f),
                    Brush.horizontalGradient(seasonalGradient(state.pet.outfit)),
                    height = 10.dp
                )
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(13.dp, 11.dp, 13.dp, 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ready Now", fontSize = 14.sp, fontWeight = FontWeight.Black)
                TextButton(onClick = { SoundFx.click(); onNavigateHabits() }) { Text("All", color = Pink, fontWeight = FontWeight.Black, fontSize = 12.sp) }
            }
        }

        val available = state.habits.filter { it.enabled && GameLogic.habitStatus(state, it) != HabitStatus.DISABLED }.take(5)
        if (available.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(14.dp, 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No habits ready right now", fontSize = 12.sp, color = SoftText, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    Text("Check back at your scheduled times", fontSize = 11.sp, color = SoftText.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            itemsIndexed(available, key = { _, h -> h.id }) { index, habit ->
                HabitCard(
                    habit = habit,
                    status = GameLogic.habitStatus(state, habit),
                    modifier = Modifier.padding(13.dp, 0.dp, 13.dp, 9.dp).enterFromBelow(4 + index)
                ) {
                    viewModel.completeHabit(habit.id)
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }

    if (showShare) {
        ShareStreakDialog(state = state, onDismiss = { showShare = false })
    }
}

@Composable
fun HabitCard(
    habit: com.vivichi.app.data.Habit,
    status: HabitStatus,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val done = status == HabitStatus.DONE
    val expired = status == HabitStatus.EXPIRED
    val clickable = status == HabitStatus.AVAILABLE

    val cardBg by animateColorAsState(
        when {
            done -> Color(0xFFF0FFF7)
            expired -> Color(0xFFFFF5F5)
            else -> Color.White
        },
        tween(450), label = "cardBg"
    )
    val nameColor by animateColorAsState(if (done) MutedText else TextDark, tween(450), label = "name")
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier
            .fillMaxWidth()
            .then(
                if (clickable) Modifier.bounceClick(shape, pressedScale = 0.96f) { SoundFx.complete(); onClick() }
                else Modifier.clip(shape)
            )
            .background(cardBg)
            .padding(14.dp, 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(if (done) Color(0xFFDDF7EA) else BgPink),
            contentAlignment = Alignment.Center
        ) { EmojiGlyph(raw = habit.icon, size = 19.dp) }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(habit.name, fontSize = 13.sp, fontWeight = FontWeight.Black, color = nameColor)
            val intLbl = when (habit.intensity) { "medium" -> "Medium"; "high" -> "High"; else -> "Low" }
            val sub = when {
                done -> "Completed"
                expired -> "Missed — resets tomorrow at ${habit.time}"
                else -> "+${habit.xp} XP | $intLbl | ${habit.time}"
            }
            Text(sub, fontSize = 11.sp, color = SoftText, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
        }
        // The status badge swaps with a springy pop, so completing a habit visibly "lands".
        AnimatedContent(
            targetState = status,
            transitionSpec = {
                (scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), initialScale = 0.2f) + fadeIn()) togetherWith fadeOut(tween(90))
            },
            label = "status"
        ) { st ->
            when {
                st == HabitStatus.DONE -> Box(
                    Modifier.size(27.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(Green, GreenDark))),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                st == HabitStatus.EXPIRED -> Box(
                    Modifier.clip(RoundedCornerShape(7.dp)).background(Red.copy(alpha = 0.1f)).padding(horizontal = 7.dp, vertical = 2.dp)
                ) { Text("⏰ Expired", fontSize = 10.sp, color = Red, fontWeight = FontWeight.Black) }
                // With an edit action the pencil takes this slot itself; the empty "to do" circle is
                // only a decoration and doubling them up made the row look cluttered and misaligned.
                onEdit != null -> Spacer(Modifier.size(0.dp))
                else -> Box(
                    Modifier
                        .size(27.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.5.dp, BorderPink, CircleShape)
                )
            }
        }
        if (onEdit != null) {
            Box(
                Modifier
                    .padding(start = if (done || expired) 6.dp else 0.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable { SoundFx.click(); onEdit() },
                contentAlignment = Alignment.Center
            ) { EmojiGlyph(raw = "✏️", size = 17.dp) }
        }
    }
}
