package com.vivichi.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import com.vivichi.app.ui.components.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.alpha
import com.vivichi.app.ui.components.FloatingSparkles
import com.vivichi.app.ui.components.bounceClick
import com.vivichi.app.ui.components.enterFromBelow
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
import com.vivichi.app.data.Habit
import com.vivichi.app.domain.GameLogic
import com.vivichi.app.domain.HabitStatus
import com.vivichi.app.ui.VivichiViewModel
import com.vivichi.app.ui.components.EmojiGlyph
import com.vivichi.app.ui.dialogs.AddEditHabitDialog
import com.vivichi.app.ui.theme.*
import com.vivichi.app.util.SoundFx
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HabitsScreen(viewModel: VivichiViewModel) {
    val state by viewModel.state.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var editHabit by remember { mutableStateOf<Habit?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val pct = GameLogic.completionPct(state)
    val order = mapOf(HabitStatus.AVAILABLE to 0, HabitStatus.EXPIRED to 1, HabitStatus.DONE to 2, HabitStatus.DISABLED to 3)
    val active = state.habits.filter { it.enabled }.sortedBy { order[GameLogic.habitStatus(state, it)] ?: 9 }
    val inactive = state.habits.filterNot { it.enabled }

    val (doneCount, leftCount, missedCount) = rememberTodayCounts(state)
    val todo = active.filter { GameLogic.habitStatus(state, it) == HabitStatus.AVAILABLE }
    val tomorrow = active.filter { GameLogic.startsTomorrow(state, it) }
    val missed = active.filter { GameLogic.habitStatus(state, it) == HabitStatus.EXPIRED && it !in tomorrow }
    val done = active.filter { GameLogic.habitStatus(state, it) == HabitStatus.DONE }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 20.dp)) {
        item {
            PageHeader(
                title = "Today's goals",
                subtitle = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())),
                emoji = "✅",
                accent = listOf(Pink, PurpleDark),
                stats = listOf(
                    HeaderStat("$doneCount", "Done", GreenDark),
                    HeaderStat("$leftCount", "To do", Orange),
                    HeaderStat("$missedCount", "Missed", Red)
                ),
                trailing = { ProgressRing(pct) }
            )
        }

        var index = 0
        fun section(title: String, emoji: String, habits: List<Habit>) {
            if (habits.isEmpty()) return
            item(key = "h_$title") { SectionTitle(title, emoji, trailing = "${habits.size}") }
            habits.forEach { habit ->
                val i = index++
                item(key = habit.id) {
                    HabitRowWithActions(
                        habit = habit,
                        index = i,
                        use24h = state.use24h,
                        status = GameLogic.habitStatus(state, habit),
                        startsTomorrow = habit in tomorrow,
                        onComplete = { viewModel.completeHabit(habit.id) },
                        onEdit = { editHabit = habit }
                    )
                }
            }
        }
        section("To do", "⏰", todo)
        section("Missed", "⚠️", missed)
        section("Done", "🏆", done)
        section("Starts tomorrow", "🌱", tomorrow)

        if (active.isEmpty()) {
            item { EmptyHint(emoji = "🌱", title = "No active habits", body = "Add one below or turn on a paused habit.") }
        }

        if (inactive.isNotEmpty()) {
            item(key = "h_paused") { SectionTitle("Paused", "😴", trailing = "${inactive.size}") }
            items(inactive, key = { it.id }) { habit ->
                Row(
                    Modifier
                        .padding(16.dp, 0.dp, 16.dp, 10.dp)
                        .fillMaxWidth()
                        .card(radius = 18.dp, color = Color.White.copy(alpha = 0.75f), elevation = 1.dp)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFFF1EDF6)),
                        contentAlignment = Alignment.Center
                    ) { EmojiGlyph(raw = habit.icon, size = 19.dp, modifier = Modifier.alpha(0.6f)) }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(habit.name, fontSize = 13.sp, fontWeight = FontWeight.Black, color = SoftText)
                        Text("Paused · not counted today", fontSize = 11.sp, color = MutedText, fontWeight = FontWeight.SemiBold)
                    }
                    Box(
                        Modifier
                            .bounceClick(RoundedCornerShape(12.dp)) { SoundFx.click(); viewModel.toggleHabit(habit.id, true) }
                            .background(Brush.horizontalGradient(listOf(Green, GreenDark)))
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) { Text("Resume", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White) }
                }
            }
        }

        item {
            Row(
                Modifier
                    .padding(16.dp, 14.dp, 16.dp, 8.dp)
                    .fillMaxWidth()
                    .bounceClick(RoundedCornerShape(18.dp)) { SoundFx.click(); showAdd = true }
                    .background(Color.White.copy(alpha = 0.7f))
                    .dashedBorder(PinkLight, 18.dp)
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(24.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Pink, PinkDark))),
                    contentAlignment = Alignment.Center
                ) { Text("+", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp) }
                Spacer(Modifier.width(10.dp))
                Text("Add a custom habit", color = PinkDark, fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
        }
    }

    if (showAdd) {
        AddEditHabitDialog(
            existing = null,
            onDismiss = { showAdd = false },
            onSave = { name, icon, xp, time ->
                viewModel.addOrUpdateHabit(null, name, icon, xp, time)
                showAdd = false
                // Confirm visibly — the new habit lands in whichever section fits, maybe off-screen.
                val t = com.vivichi.app.util.formatHabitTime(time, state.use24h)
                val passed = com.vivichi.app.util.habitWindowPassed(time)
                android.widget.Toast.makeText(
                    context,
                    if (passed) "\"$name\" added — it starts tomorrow at $t" else "\"$name\" added for $t",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            , use24h = state.use24h
        )
    }
    editHabit?.let { h ->
        AddEditHabitDialog(
            existing = h,
            onDismiss = { editHabit = null },
            onSave = { name, icon, xp, time ->
                viewModel.addOrUpdateHabit(h.id, name, icon, xp, time)
                editHabit = null
            }
            , use24h = state.use24h
        )
    }
}

/** Ring that sweeps round to today's completion, with the number rolling up alongside it. */
@Composable
private fun ProgressRing(pct: Int) {
    var target by remember { mutableStateOf(0f) }
    LaunchedEffect(pct) { target = pct / 100f }
    val sweep by animateFloatAsState(target, tween(1100, easing = FastOutSlowInEasing), label = "ring")
    Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx()
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(PinkLight.copy(alpha = 0.3f), 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            drawArc(Brush.sweepGradient(listOf(Pink, PurpleDark, Pink)), -90f, 360f * sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CountUpText(pct, TextStyle(color = TextDark, fontSize = 15.sp, fontWeight = FontWeight.Black), suffix = "%")
            Text("done", color = SoftText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Row(modifier.padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = MutedText)
        Spacer(Modifier.width(6.dp))
        Box(Modifier.weight(1f).height(1.dp).background(BorderPink))
    }
}

@Composable
private fun HabitRowWithActions(habit: Habit, status: HabitStatus, index: Int, use24h: Boolean, startsTomorrow: Boolean, onComplete: () -> Unit, onEdit: () -> Unit) {
    HabitCard(
        habit = habit,
        status = status,
        startsTomorrow = startsTomorrow,
        modifier = Modifier.padding(13.dp, 0.dp, 13.dp, 9.dp).enterFromBelow(index + 1),
        onEdit = onEdit,
        use24h = use24h,
        onClick = onComplete
    )
}
