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
import com.vivichi.app.ui.components.CountUpText
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
    var pendingExpiredAdd by remember { mutableStateOf<PendingHabit?>(null) }

    val pct = GameLogic.completionPct(state)
    val order = mapOf(HabitStatus.AVAILABLE to 0, HabitStatus.EXPIRED to 1, HabitStatus.DONE to 2, HabitStatus.DISABLED to 3)
    val active = state.habits.filter { it.enabled }.sortedBy { order[GameLogic.habitStatus(state, it)] ?: 9 }
    val inactive = state.habits.filterNot { it.enabled }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Box(
                Modifier
                    .padding(horizontal = 13.dp)
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.linearGradient(listOf(Pink, PurpleDark)))
            ) {
                FloatingSparkles(Modifier.matchParentSize(), color = Color.White, count = 14, seed = 11)
                Column(
                    Modifier.fillMaxWidth().padding(16.dp, 18.dp, 16.dp, 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())),
                        color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                    Text("Today's Goals", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 3.dp))
                    ProgressRing(pct)
                }
            }
        }

        item { SectionLabel("Active", Modifier.padding(top = 14.dp, bottom = 6.dp)) }
        itemsIndexed(active, key = { _, h -> h.id }) { index, habit ->
            HabitRowWithActions(
                habit = habit,
                index = index,
                use24h = state.use24h,
                status = GameLogic.habitStatus(state, habit),
                onComplete = { viewModel.completeHabit(habit.id) },
                onEdit = { editHabit = habit }
            )
        }

        if (inactive.isNotEmpty()) {
            item { SectionLabel("Inactive", Modifier.padding(top = 12.dp, bottom = 6.dp)) }
            items(inactive, key = { it.id }) { habit ->
                Row(
                    Modifier
                        .padding(13.dp, 0.dp, 13.dp, 7.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.55f))
                        .padding(14.dp, 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EmojiGlyph(raw = habit.icon, size = 19.dp)
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(habit.name, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        Text("Disabled", fontSize = 11.sp, color = SoftText, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { SoundFx.click(); viewModel.toggleHabit(habit.id, true) },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenDark),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 11.dp, vertical = 5.dp)
                    ) { Text("Enable", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White) }
                }
            }
        }

        item {
            Box(
                Modifier
                    .padding(13.dp, 12.dp, 13.dp, 24.dp)
                    .fillMaxWidth()
                    .bounceClick(RoundedCornerShape(18.dp)) { SoundFx.click(); showAdd = true }
                    .border(2.dp, BorderPink, RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.6f))
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) { Text("+ Add custom habit", color = Pink, fontWeight = FontWeight.Black) }
        }
    }

    if (showAdd) {
        AddEditHabitDialog(
            existing = null,
            onDismiss = { showAdd = false },
            onSave = { name, icon, xp, time ->
                val ok = viewModel.addOrUpdateHabit(null, name, icon, xp, time)
                if (!ok) pendingExpiredAdd = PendingHabit(name, icon, xp, time)
                showAdd = false
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
    pendingExpiredAdd?.let { pending ->
        AlertDialog(
            onDismissRequest = { pendingExpiredAdd = null },
            title = { Text("This time has passed!", fontWeight = FontWeight.Black) },
            text = { Text("${pending.name} at ${com.vivichi.app.util.formatHabitTime(pending.time, state.use24h)} already expired today. It will unlock next tomorrow.", fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = {
                    SoundFx.click()
                    viewModel.forceAddHabit(pending.name, pending.icon, pending.xp, pending.time)
                    pendingExpiredAdd = null
                }) { Text("Add anyway", color = PinkDark, fontWeight = FontWeight.Black) }
            },
            dismissButton = {
                TextButton(onClick = { SoundFx.click(); pendingExpiredAdd = null }) { Text("Cancel", color = SoftText, fontWeight = FontWeight.Bold) }
            }
        )
    }
}

/** Ring that sweeps round to today's completion, with the number rolling up alongside it. */
@Composable
private fun ProgressRing(pct: Int) {
    var target by remember { mutableStateOf(0f) }
    LaunchedEffect(pct) { target = pct / 100f }
    val sweep by animateFloatAsState(target, tween(1100, easing = FastOutSlowInEasing), label = "ring")
    Box(Modifier.size(86.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(Color.White.copy(alpha = 0.25f), 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            drawArc(Color.White, -90f, 360f * sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CountUpText(pct, TextStyle(color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black), suffix = "%")
            Text("done", color = Color.White.copy(alpha = 0.85f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private data class PendingHabit(val name: String, val icon: String, val xp: Int, val time: String)

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Row(modifier.padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = MutedText)
        Spacer(Modifier.width(6.dp))
        Box(Modifier.weight(1f).height(1.dp).background(BorderPink))
    }
}

@Composable
private fun HabitRowWithActions(habit: Habit, status: HabitStatus, index: Int, use24h: Boolean, onComplete: () -> Unit, onEdit: () -> Unit) {
    HabitCard(
        habit = habit,
        status = status,
        modifier = Modifier.padding(13.dp, 0.dp, 13.dp, 9.dp).enterFromBelow(index + 1),
        onEdit = onEdit,
        use24h = use24h,
        onClick = onComplete
    )
}
