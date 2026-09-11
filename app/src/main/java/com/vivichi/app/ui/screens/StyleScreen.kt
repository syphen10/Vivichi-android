package com.vivichi.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vivichi.app.data.*
import com.vivichi.app.domain.GameLogic
import com.vivichi.app.domain.Mood
import com.vivichi.app.ui.VivichiViewModel
import com.vivichi.app.ui.components.CharacterView
import com.vivichi.app.ui.components.EmojiGlyph
import com.vivichi.app.ui.theme.*
import com.vivichi.app.util.SoundFx
import java.time.LocalDate

private fun currentSeason(): String = when (LocalDate.now().monthValue) {
    in 3..5 -> "spring"; in 6..8 -> "summer"; in 9..11 -> "autumn"; else -> "winter"
}

@Composable
fun StyleScreen(viewModel: VivichiViewModel) {
    val state by viewModel.state.collectAsState()
    val season = currentSeason()

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        item {
            Column(Modifier.padding(top = 20.dp, bottom = 8.dp)) {
                Text("Style", fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text("Outfits unlock with levels. All seasons always free!", fontSize = 12.sp, color = SoftText, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 3.dp))
            }
        }
        item {
            Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                CharacterView(species = state.pet.species, mood = GameLogic.mood(state), outfit = state.pet.outfit, health = state.pet.health, size = 140.dp)
            }
        }
        item {
            Text("Choose your buddy", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MutedText, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        item {
            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.height(((PETS.size / 3 + 1) * 90).dp), verticalArrangement = Arrangement.spacedBy(7.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(PETS) { p ->
                    val selected = state.pet.species == p.id
                    Column(
                        Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (selected) Color(0xFFFFF0F5) else Color.White)
                            .clickable { SoundFx.click(); viewModel.pickSpecies(p.id) }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        EmojiGlyph(raw = p.emoji, size = 34.dp)
                        Text(p.name, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

        item { Text("Outfits", fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 18.dp, bottom = 9.dp)) }
        item {
            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.height(((OUTFITS.size / 3 + 1) * 92).dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(OUTFITS) { o ->
                    val locked = state.pet.level < o.level
                    val active = state.pet.outfit == o.id
                    OutfitCard(emoji = o.emoji, name = o.name, sub = if (locked) "Lv.${o.level}" else "Tap to wear", locked = locked, active = active) {
                        if (!locked) viewModel.pickOutfit(o.id)
                    }
                }
            }
        }

        item {
            Row(Modifier.padding(top = 18.dp, bottom = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Seasonal ", fontSize = 14.sp, fontWeight = FontWeight.Black)
                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(seasonBg(season)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text(season.replaceFirstChar { it.uppercase() }, fontSize = 10.sp, fontWeight = FontWeight.Black, color = seasonFg(season))
                }
            }
        }
        item {
            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.height(((SEASONAL_OUTFITS.size / 3 + 1) * 92).dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SEASONAL_OUTFITS) { o ->
                    val active = state.pet.outfit == o.id
                    val isCurrent = o.season == season
                    OutfitCard(emoji = o.emoji, name = o.name, sub = if (isCurrent) "Current season" else "Available", locked = false, active = active, dim = !isCurrent) {
                        viewModel.pickOutfit(o.id)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

private fun seasonBg(s: String) = when (s) { "spring" -> Color(0xFFEAFAF1); "summer" -> Color(0xFFFEF9E7); "autumn" -> Color(0xFFFEF0E6); else -> Color(0xFFEBF5FB) }
private fun seasonFg(s: String) = when (s) { "spring" -> Color(0xFF27AE60); "summer" -> Color(0xFFF39C12); "autumn" -> Color(0xFFD35400); else -> Color(0xFF2980B9) }

@Composable
private fun OutfitCard(emoji: String, name: String, sub: String, locked: Boolean, active: Boolean, dim: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .clickable(enabled = !locked, onClick = { SoundFx.click(); onClick() })
                .padding(vertical = 11.dp)
                .alpha(if (locked) 0.45f else if (dim) 0.6f else 1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EmojiGlyph(raw = emoji, size = 24.dp)
            Text(name, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 4.dp))
            Text(sub, fontSize = 9.sp, color = if (active) PinkDark else SoftText, fontWeight = FontWeight.SemiBold)
        }
        if (locked) {
            val infinite = rememberInfiniteTransition(label = "lock")
            val wobble by infinite.animateFloat(
                initialValue = -8f, targetValue = 8f,
                animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "wobble"
            )
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .graphicsLayer { rotationZ = wobble }
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) { EmojiGlyph(raw = "🔒", size = 12.dp) }
        }
    }
}
