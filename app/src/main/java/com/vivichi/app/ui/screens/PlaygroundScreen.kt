package com.vivichi.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vivichi.app.data.PLAY_MESSAGES
import com.vivichi.app.data.REACT_EMOJI
import com.vivichi.app.data.petEmoji
import com.vivichi.app.domain.Mood
import com.vivichi.app.ui.VivichiViewModel
import com.vivichi.app.ui.components.CharacterView
import com.vivichi.app.ui.components.EmojiGlyph
import com.vivichi.app.ui.theme.*
import com.vivichi.app.util.SoundFx
import kotlinx.coroutines.delay
import kotlin.random.Random

private data class PlayAction(val key: String, val emoji: String, val label: String, val colors: List<Color>)

private val ACTIONS = listOf(
    PlayAction("feed", "🍖", "Feed", listOf(Yellow, Orange)),
    PlayAction("pet", "💗", "Pet", listOf(Pink, PinkDark)),
    PlayAction("play", "🎾", "Play", listOf(Purple, PurpleDark)),
    PlayAction("hug", "🫂", "Hug", listOf(Green, GreenDark)),
    PlayAction("sing", "🎵", "Sing", listOf(Color(0xFFB8C4FF), Color(0xFF6C5CE7))),
    PlayAction("tickle", "🪶", "Tickle", listOf(PinkLight, Pink))
)

@Composable
fun PlaygroundScreen(viewModel: VivichiViewModel) {
    val state by viewModel.state.collectAsState()
    var message by remember(state.pet.species) { mutableStateOf("Hey! Come play with me! ${petEmoji(state.pet.species)}") }
    var reaction by remember { mutableStateOf<String?>(null) }
    var bump by remember { mutableIntStateOf(0) }
    var cooldown by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(if (bump % 2 == 1) 1.12f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "bump")

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(seasonalGradient(state.pet.outfit)), RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
                .padding(16.dp, 24.dp, 16.dp, 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Playground 🐾", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text("Spend time with your buddy!", color = Color.White.copy(alpha = 0.88f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
            }
        }

        Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.TopStart) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.scale(scale)) {
                    CharacterView(species = state.pet.species, mood = Mood.HAPPY, outfit = state.pet.outfit, health = state.pet.health, size = 180.dp)
                }
            }
        }
        Box(
            Modifier.padding(18.dp, 4.dp).fillMaxWidth().height(64.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            androidx.compose.animation.AnimatedVisibility(visible = reaction != null) {
                Box(
                    Modifier.padding(end = 16.dp).size(64.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) { reaction?.let { EmojiGlyph(raw = it, size = 34.dp) } }
            }
        }

        Box(
            Modifier
                .padding(18.dp, 10.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(20.dp, 15.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(message, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 19.sp)
        }

        val rows = ACTIONS.chunked(2)
        Column(Modifier.padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { action ->
                        PlayButton(action, Modifier.weight(1f), enabled = !cooldown) {
                            if (cooldown) return@PlayButton
                            cooldown = true
                            val msgs = PLAY_MESSAGES[action.key].orEmpty()
                            message = if (msgs.isNotEmpty()) msgs[Random.nextInt(msgs.size)] else "..."
                            reaction = REACT_EMOJI[action.key]
                            bump++
                            SoundFx.playgroundAction(action.key)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }

    LaunchedEffect(reaction) {
        if (reaction != null) {
            delay(1200)
            reaction = null
        }
    }
    LaunchedEffect(cooldown) {
        if (cooldown) {
            delay(800)
            cooldown = false
        }
    }
}

@Composable
private fun PlayButton(action: PlayAction, modifier: Modifier = Modifier, enabled: Boolean, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(action.colors))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 17.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmojiGlyph(raw = action.emoji, size = 30.dp)
        Spacer(Modifier.height(7.dp))
        Text(action.label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}
