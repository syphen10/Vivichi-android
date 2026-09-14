package com.vivichi.app.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.vivichi.app.data.CemeteryEntry
import com.vivichi.app.data.petEmoji
import com.vivichi.app.ui.components.CharacterView
import com.vivichi.app.ui.components.EmojiGlyph
import com.vivichi.app.domain.Mood
import com.vivichi.app.ui.theme.*
import com.vivichi.app.util.SoundFx

@Composable
fun DeathScreen(entry: CemeteryEntry, onNewBuddy: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF2D2D5A), Color(0xFF4A2D6A))))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CharacterView(species = entry.species, mood = Mood.SAD, outfit = entry.outfit, health = 0, size = 120.dp)
            Spacer(Modifier.height(16.dp))
            Text("${entry.name} has passed away", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "They lived for ${entry.daysAlive} day${if (entry.daysAlive != 1) "s" else ""} and completed ${entry.totalDone} habits.\nTheir memory lives on in the cemetery.",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DeathStat("Lv.${entry.level}", "Level")
                DeathStat("${entry.streak}", "Best Streak")
                DeathStat("${entry.totalXP}", "XP")
                DeathStat("${entry.daysAlive}d", "Days Alive")
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { SoundFx.click(); onNewBuddy() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B6BAA)),
                shape = RoundedCornerShape(18.dp)
            ) { Text("Create a new buddy", fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun DeathStat(value: String, label: String) {
    Column(
        Modifier
            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(label, color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MissedYouOverlay(days: Int, petName: String, species: String, streak: Int, onDismiss: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFFFE2EE), Color(0xFFEDE4FF))))
            .padding(30.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            EmojiGlyph(raw = petEmoji(species), size = 70.dp)
            Spacer(Modifier.height(12.dp))
            Text(
                (if (days >= 7) "Where have you been?! " else "") + "$petName missed you!",
                fontSize = 22.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "You have been away for $days day${if (days != 1) "s" else ""}. Lets get back on track!",
                fontSize = 13.sp, color = SoftText, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = 19.sp
            )
            Spacer(Modifier.height(20.dp))
            Column(
                Modifier
                    .background(Color.White, RoundedCornerShape(18.dp))
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Current streak", fontSize = 11.sp, color = SoftText, fontWeight = FontWeight.Bold)
                Text("$streak", fontSize = 32.sp, fontWeight = FontWeight.Black, color = PinkDark)
            }
            Spacer(Modifier.height(20.dp))
            VivichiButton(text = "Let's go!", onClick = onDismiss)
        }
    }
}

@Composable
fun LevelUpDialog(level: Int, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(26.dp))
                .background(Color.White)
                .padding(38.dp, 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                EmojiGlyph(raw = "🌟", size = 30.dp)
                EmojiGlyph(raw = "✨", size = 30.dp)
                EmojiGlyph(raw = "🌟", size = 30.dp)
            }
            Spacer(Modifier.height(6.dp))
            Text("Level $level!", fontSize = 23.sp, fontWeight = FontWeight.Black, color = PinkDark)
            Spacer(Modifier.height(4.dp))
            Text("You are on fire!", fontSize = 12.sp, color = SoftText, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            VivichiButton(text = "Awesome!", onClick = onDismiss)
        }
    }
}

@Composable
fun EarlyConfirmDialog(habitName: String, minutesAhead: Int, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val hrs = minutesAhead / 60
    val mins = minutesAhead % 60
    val aheadText = if (hrs > 0) "${hrs}h ${mins}m ahead of schedule" else "${mins}m ahead of schedule"
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Completing early ⏰", fontWeight = FontWeight.Black) },
        text = { Text("$habitName is $aheadText.\nComplete it now or wait for the scheduled time?", fontSize = 13.sp) },
        confirmButton = { TextButton(onClick = { SoundFx.click(); onConfirm() }) { Text("Yes, do it! 💪", color = PinkDark, fontWeight = FontWeight.Black) } },
        dismissButton = { TextButton(onClick = { SoundFx.click(); onCancel() }) { Text("Not yet", color = SoftText, fontWeight = FontWeight.Bold) } }
    )
}

@Composable
fun BoxScope.XpToast(xp: Int, intensity: String, coins: Int = 0) {
    val label = when (intensity) { "medium" -> "Medium"; "high" -> "High"; else -> "Low" }
    Row(
        Modifier
            .align(Alignment.Center)
            .background(Brush.horizontalGradient(listOf(PurpleDark, Pink)), RoundedCornerShape(18.dp))
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("+$xp XP · $label", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black)
        if (coins > 0) {
            Text("  ·  +$coins ", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black)
            EmojiGlyph(raw = "🪙", size = 20.dp)
        }
    }
}

@Composable
fun VivichiButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = { SoundFx.click(); onClick() },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = PinkDark),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)
    ) {
        Text(text, fontWeight = FontWeight.Black, fontSize = 16.sp)
    }
}
