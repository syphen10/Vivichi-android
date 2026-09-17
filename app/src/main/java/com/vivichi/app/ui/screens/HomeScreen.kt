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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vivichi.app.data.Habit
import com.vivichi.app.domain.GameLogic
import com.vivichi.app.domain.HabitStatus
import com.vivichi.app.ui.VivichiViewModel
import com.vivichi.app.ui.components.*
import com.vivichi.app.ui.dialogs.ShareStreakDialog
import com.vivichi.app.ui.theme.*
import com.vivichi.app.util.SoundFx
import com.vivichi.app.util.formatHabitTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(viewModel: VivichiViewModel, onNavigateHabits: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val mood = GameLogic.characterMood(state)
    val health = state.pet.health
    var showShare by remember { mutableStateOf(false) }
    val (done, left, missed) = rememberTodayCounts(state)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            PageHeader(
                title = GameLogic.greeting(),
                subtitle = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())),
                emoji = "🐾",
                accent = seasonalGradient(state.pet.outfit),
                stats = listOf(
                    HeaderStat("$done", "Done", GreenDark, "✅"),
                    HeaderStat("$left", "To do", Orange, "⏰"),
                    HeaderStat("${state.streak}", "Streak", PinkDark, "🔥")
                ),
                trailing = {
                    Row(
                        Modifier
                            .cardShadow(20.dp, 3.dp)
                            .bounceClick(RoundedCornerShape(20.dp)) { SoundFx.click(); showShare = true }
                            .background(Brush.horizontalGradient(listOf(Yellow, Orange)))
                            .padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Share", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            )
        }

        if (health < 75) {
            item {
                val (bg, fg, msg) = when {
                    health < 25 -> Triple(Color(0xFFFF6B6B), Color.White, "CRITICAL — complete habits now or ${state.pet.name} will die!")
                    health < 50 -> Triple(Color(0xFFFFE5E5), Color(0xFFC0392B), "${state.pet.name} is getting weaker. Don't miss any more habits!")
                    else -> Triple(Color(0xFFFFF3CD), Color(0xFF8B6914), "${state.pet.name} needs a little more care today.")
                }
                Row(
                    Modifier
                        .padding(16.dp, 10.dp, 16.dp, 0.dp)
                        .enterFromBelow(1)
                        .then(if (health < 25) Modifier.pulsing(1.02f, 700) else Modifier)
                        .fillMaxWidth()
                        .card(radius = 16.dp, color = bg, elevation = 2.dp)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EmojiGlyph(raw = "⚠️", size = 16.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(msg, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        item { PetScene(state = state, mood = mood) }

        item {
            SectionTitle(
                title = "Today's habits",
                emoji = "✅",
                trailing = if (missed > 0) "$missed missed · See all" else "See all",
                onTrailing = onNavigateHabits
            )
        }

        // Open habits first, then missed, then done — what needs doing sits at the top.
        val order = mapOf(HabitStatus.AVAILABLE to 0, HabitStatus.EXPIRED to 1, HabitStatus.DONE to 2)
        val list = state.habits
            .filter { it.enabled }
            .sortedBy { order[GameLogic.habitStatus(state, it)] ?: 9 }
            .take(6)
        if (list.isEmpty()) {
            item { EmptyHint(emoji = "🌱", title = "No habits yet", body = "Add your first habit in the Habits tab.") }
        } else {
            itemsIndexed(list, key = { _, h -> h.id }) { index, habit ->
                HabitCard(
                    habit = habit,
                    status = GameLogic.habitStatus(state, habit),
                    use24h = state.use24h,
                    modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 10.dp).enterFromBelow(3 + index)
                ) {
                    viewModel.completeHabit(habit.id)
                }
            }
        }
    }

    if (showShare) {
        ShareStreakDialog(state = state, onDismiss = { showShare = false })
    }
}

/**
 * The pet's little "room": a themed sky, a soft hill it stands on, drifting sparkles, and a
 * speech bubble. Health and XP live in the HUD above, so this card is pure character.
 */
@Composable
private fun PetScene(state: com.vivichi.app.data.AppState, mood: com.vivichi.app.domain.Mood) {
    val theme = seasonalGradient(state.pet.outfit)
    val sky = listOf(lerp(theme.first(), Color.White, 0.72f), lerp(theme.last(), Color.White, 0.86f))
    Column(
        Modifier
            .padding(16.dp, 12.dp, 16.dp, 0.dp)
            .enterFromBelow(2)
            .fillMaxWidth()
            .card(radius = 26.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(206.dp)
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(Brush.verticalGradient(sky))
        ) {
            Canvas(Modifier.matchParentSize()) {
                // sun/moon glow + two layered hills
                drawCircle(Color.White.copy(alpha = 0.55f), size.minDimension * 0.34f, Offset(size.width * 0.82f, size.height * 0.2f))
                drawOval(
                    lerp(theme.last(), Color.White, 0.55f),
                    topLeft = Offset(-size.width * 0.25f, size.height * 0.72f),
                    size = Size(size.width * 0.9f, size.height * 0.6f)
                )
                drawOval(
                    lerp(theme.first(), Color.White, 0.45f),
                    topLeft = Offset(size.width * 0.3f, size.height * 0.78f),
                    size = Size(size.width * 1.0f, size.height * 0.55f)
                )
                // contact shadow under the pet
                drawOval(
                    Color(0x223D2E4E),
                    topLeft = Offset(size.width * 0.5f - 56.dp.toPx(), size.height * 0.86f),
                    size = Size(112.dp.toPx(), 16.dp.toPx())
                )
            }
            FloatingSparkles(Modifier.matchParentSize(), color = Color.White, count = 10, seed = state.pet.species.hashCode())
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp)) {
                CharacterView(species = state.pet.species, mood = mood, outfit = state.pet.outfit, health = state.pet.health, size = 170.dp)
            }
        }
        // Speech bubble with a little tail pointing up at the pet.
        Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF8F3FC))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EmojiGlyph(raw = "🐾", size = 18.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("${state.pet.name.uppercase()} SAYS", fontSize = 9.sp, fontWeight = FontWeight.Black, color = MutedText, letterSpacing = 0.6.sp)
                    AnimatedContent(
                        targetState = GameLogic.buddyMessage(state),
                        transitionSpec = { (fadeIn(tween(300)) + slideInVertically { it / 2 }) togetherWith (fadeOut(tween(150)) + slideOutVertically { -it / 2 }) },
                        label = "says"
                    ) { msg ->
                        Text(msg, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark, lineHeight = 18.sp)
                    }
                }
            }
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-7).dp)
                    .size(width = 16.dp, height = 8.dp)
                    .background(Color(0xFFF8F3FC), GenericShape { s, _ -> moveTo(0f, s.height); lineTo(s.width / 2f, 0f); lineTo(s.width, s.height); close() })
            )
        }
    }
}

@Composable
fun EmptyHint(emoji: String, title: String, body: String) {
    Column(
        Modifier
            .padding(16.dp, 4.dp, 16.dp, 8.dp)
            .fillMaxWidth()
            .card(radius = 20.dp, elevation = 2.dp)
            .padding(vertical = 22.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmojiGlyph(raw = emoji, size = 34.dp, modifier = Modifier.floating(4.dp, 1800))
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Black, color = TextDark, modifier = Modifier.padding(top = 8.dp))
        Text(body, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SoftText, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun HabitCard(
    habit: Habit,
    status: HabitStatus,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    use24h: Boolean = false,
    onClick: () -> Unit
) {
    val done = status == HabitStatus.DONE
    val expired = status == HabitStatus.EXPIRED
    val clickable = status == HabitStatus.AVAILABLE
    val accent = intensityColor(habit.intensity)

    val cardBg by animateColorAsState(
        when {
            done -> Color(0xFFF2FCF7)
            expired -> Color(0xFFFFF6F6)
            else -> Color.White
        },
        tween(450), label = "cardBg"
    )
    val nameColor by animateColorAsState(if (done) MutedText else TextDark, tween(450), label = "name")
    val stripe by animateColorAsState(
        when { done -> GreenDark; expired -> Red.copy(alpha = 0.5f); else -> accent },
        tween(450), label = "stripe"
    )
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .cardShadow(18.dp, 3.dp)
            .then(
                if (clickable) Modifier.bounceClick(shape, pressedScale = 0.96f) { SoundFx.complete(); onClick() }
                else Modifier.clip(shape)
            )
            .cardSurface(18.dp, cardBg)
    ) {
        // intensity stripe
        Box(Modifier.fillMaxHeight().width(5.dp).background(stripe))
        Row(Modifier.weight(1f).padding(start = 11.dp, end = 12.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = if (done) 0.08f else 0.14f)),
                contentAlignment = Alignment.Center
            ) { EmojiGlyph(raw = habit.icon, size = 22.dp) }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(habit.name, fontSize = 14.sp, fontWeight = FontWeight.Black, color = nameColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(5.dp))
                when {
                    done -> Text("Completed · +${habit.xp} XP earned", fontSize = 11.sp, color = GreenDark, fontWeight = FontWeight.Black)
                    expired -> Text("Missed · back tomorrow at ${formatHabitTime(habit.time, use24h)}", fontSize = 11.sp, color = Red, fontWeight = FontWeight.Bold)
                    else -> Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        InfoPill(formatHabitTime(habit.time, use24h), emoji = "⏰")
                        InfoPill("+${habit.xp} XP", tint = accent, bg = accent.copy(alpha = 0.12f))
                        if (GameLogic.habitEarnsCoinsToday(habit)) {
                            InfoPill("+${GameLogic.coinsForHabit(habit)}", emoji = "🪙", tint = Color(0xFFB7791F), bg = Color(0xFFFFF4D6))
                        }
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
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
                        Modifier.size(28.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Green, GreenDark))),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(17.dp)) }
                    st == HabitStatus.EXPIRED -> EmojiGlyph(raw = "⏰", size = 20.dp)
                    // With an edit action the pencil takes this slot; an empty circle beside it looked cluttered.
                    onEdit != null -> Spacer(Modifier.size(0.dp))
                    else -> Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(2.5.dp, accent.copy(alpha = 0.45f), CircleShape)
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
}
