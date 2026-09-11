package com.vivichi.app.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vivichi.app.ui.theme.*
import com.vivichi.app.util.SoundFx

private data class TutorialStep(val icon: ImageVector, val title: String, val body: String)

// Same icon vocabulary as the bottom nav bar, so the tutorial reads as part of the same
// designed system instead of a generic emoji slapped on top of it.
private val STEPS = listOf(
    TutorialStep(Icons.Filled.Home, "Home", "This is your buddy's home. See their health, level, and the habits ready to complete right now."),
    TutorialStep(Icons.Filled.CheckCircle, "Habits", "Tap here to see every habit you've set up, add new ones, and check them off as you finish them each day."),
    TutorialStep(Icons.Filled.BarChart, "Stats", "Come here to track your streaks and unlock achievement titles as you keep up your habits."),
    TutorialStep(Icons.Filled.Palette, "Style", "Change your pet's species, unlock outfits by leveling up, and equip seasonal themes — always free."),
    TutorialStep(Icons.Filled.Pets, "Play", "Visit the playground anytime to feed, pet, and play with your buddy just for fun."),
    TutorialStep(Icons.Filled.Park, "R.I.P.", "If your buddy's health hits zero they pass on — this is where you can see and remember past pets."),
    TutorialStep(Icons.Filled.Settings, "More", "Settings, notifications, habit schedules — and if Vivichi's ever brightened your day, you can support development any time from here.")
)

@Composable
fun TutorialOverlay(onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val current = STEPS[step]
    val isLast = step == STEPS.lastIndex

    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(10.dp),
            modifier = Modifier.fillMaxWidth().padding(28.dp)
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Purple, Pink))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(current.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
                }
                Text(current.title, fontSize = 19.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 14.dp))
                Text(
                    current.body, fontSize = 13.sp, color = SoftText, fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center, lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 18.dp)
                )
                Row(horizontalArrangement = Arrangement.Center) {
                    STEPS.indices.forEach { i ->
                        Box(
                            Modifier
                                .padding(3.dp)
                                .height(8.dp)
                                .width(if (i == step) 24.dp else 8.dp)
                                .background(if (i == step) Pink else MutedText, RoundedCornerShape(4.dp))
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { SoundFx.click(); onDone() }) { Text("Skip", color = SoftText, fontWeight = FontWeight.Bold) }
                    VivichiButton(
                        text = if (isLast) "Let's go!" else "Next",
                        onClick = { if (isLast) onDone() else step++ },
                        modifier = Modifier.width(140.dp)
                    )
                }
            }
        }
    }
}
