package com.vivichi.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vivichi.app.data.DefaultContent
import com.vivichi.app.data.PETS
import com.vivichi.app.ui.VivichiViewModel
import com.vivichi.app.ui.components.EmojiGlyph
import com.vivichi.app.ui.components.FloatingSparkles
import com.vivichi.app.ui.components.bounceClick
import com.vivichi.app.ui.components.floating
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import com.vivichi.app.ui.dialogs.VivichiButton
import com.vivichi.app.ui.theme.*
import com.vivichi.app.util.SoundFx

@Composable
fun OnboardingScreen(viewModel: VivichiViewModel) {
    var step by remember { mutableIntStateOf(0) }
    var petName by remember { mutableStateOf("") }
    var species by remember { mutableStateOf("cat") }
    val times = remember { mutableStateMapOf<String, String>().apply { DefaultContent.defaultHabits.forEach { put(it.id, it.time) } } }
    val enabled = remember { mutableStateMapOf<String, Boolean>().apply { DefaultContent.defaultHabits.forEach { put(it.id, it.enabled) } } }
    var notifChoice by remember { mutableStateOf(true) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFFFE2EE), Color(0xFFEDE4FF), Color(0xFFE2F0FF)))),
        contentAlignment = Alignment.TopCenter
    ) {
        FloatingSparkles(color = Color(0xFFFF9CC0), count = 16, seed = 3)
        // safeDrawing = status bar + nav bar + cutout + keyboard, so content clears the system
        // bars under edge-to-edge and the name field isn't hidden by the keyboard.
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp)) {
            Spacer(Modifier.height(20.dp))
            Text(
                "Vivichi",
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                color = PinkDark,
                modifier = Modifier.fillMaxWidth().floating(amplitude = 4.dp, periodMs = 2200),
                textAlign = TextAlign.Center
            )
            Text(
                "Your daily life companion",
                fontSize = 12.sp,
                color = SoftText,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(5) { i ->
                    val w by animateDpAsState(if (i == step) 24.dp else 8.dp, tween(350), label = "dot")
                    Box(
                        Modifier
                            .padding(3.dp)
                            .height(8.dp)
                            .width(w)
                            .background(if (i == step) Pink else MutedText, RoundedCornerShape(4.dp))
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(6.dp),
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        (slideInHorizontally(tween(320)) { it / 3 } + fadeIn(tween(260))) togetherWith
                            (slideOutHorizontally(tween(260)) { -it / 3 } + fadeOut(tween(180)))
                    },
                    label = "step"
                ) { current ->
                Column(Modifier.padding(24.dp)) {
                    when (current) {
                        0 -> StepName(petName, { petName = it }) { if (petName.isNotBlank()) step = 1 }
                        1 -> StepSpecies(species, { species = it }) { step = 2 }
                        2 -> StepSchedule(times, enabled) { step = 3 }
                        3 -> StepNotify(
                            onEnable = { notifChoice = true; step = 4 },
                            onSkip = { notifChoice = false; step = 4 }
                        )
                        else -> StepStatusPanel(
                            petName = petName.trim(),
                            species = species,
                            onEnable = {
                                viewModel.finishOnboarding(petName.trim(), species, times, enabled, notifOptIn = notifChoice, statusPanelOptIn = true)
                            },
                            onSkip = {
                                viewModel.finishOnboarding(petName.trim(), species, times, enabled, notifOptIn = notifChoice, statusPanelOptIn = false)
                            }
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun StepName(name: String, onChange: (String) -> Unit, onNext: () -> Unit) {
    Text("Name your buddy!", fontSize = 19.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    Text("They will live with you every single day", fontSize = 12.sp, color = SoftText, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 20.dp))
    OutlinedTextField(
        value = name, onValueChange = onChange,
        placeholder = { Text("e.g. Mochi, Luna, Boba", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)
    )
    VivichiButton(text = "Next", onClick = onNext, modifier = Modifier.fillMaxWidth().padding(top = 14.dp))
}

@Composable
private fun StepSpecies(species: String, onChange: (String) -> Unit, onNext: () -> Unit) {
    Text("Pick your buddy!", fontSize = 19.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    Text("Start with one of these four — unlock more buddies with coins later!", fontSize = 12.sp, color = SoftText, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp))
    val choices = PETS.filter { it.price == 0 }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in choices.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { p ->
                    val selected = species == p.id
                    Box(Modifier.weight(1f)) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (selected) Color(0xFFFFE0EC) else BgPink)
                                .border(if (selected) 2.5.dp else 0.dp, Pink, RoundedCornerShape(18.dp))
                                .clickable { SoundFx.click(); onChange(p.id) }
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            EmojiGlyph(raw = p.emoji, size = 34.dp)
                            Text(p.name, fontSize = 10.sp, fontWeight = FontWeight.Black, color = TextDark, modifier = Modifier.padding(top = 2.dp))
                        }
                        if (selected) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Pink),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(12.dp)) }
                        }
                    }
                }
            }
        }
    }
    VivichiButton(text = "Next", onClick = onNext, modifier = Modifier.fillMaxWidth().padding(top = 14.dp))
}

@Composable
private fun StepSchedule(times: MutableMap<String, String>, enabled: MutableMap<String, Boolean>, onNext: () -> Unit) {
    Text("Your daily schedule", fontSize = 19.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    Text("Habits unlock at set times and expire 2.5 hrs later!", fontSize = 12.sp, color = SoftText, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp))
    LazyColumn(Modifier.heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        items(DefaultContent.defaultHabits) { h ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(BgPink, RoundedCornerShape(14.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EmojiGlyph(raw = h.icon, size = 16.dp)
                Text(h.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(com.vivichi.app.util.formatHabitTime(times[h.id] ?: h.time, false), fontSize = 12.sp, fontWeight = FontWeight.Black, color = SoftText)
                Switch(
                    checked = enabled[h.id] ?: h.enabled,
                    onCheckedChange = { SoundFx.click(); enabled[h.id] = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = Pink)
                )
            }
        }
    }
    VivichiButton(text = "Looks good!", onClick = onNext, modifier = Modifier.fillMaxWidth().padding(top = 14.dp))
}

@Composable
private fun StepNotify(onEnable: () -> Unit, onSkip: () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { EmojiGlyph(raw = "🔔", size = 54.dp) }
    Text("Stay on track!", fontSize = 19.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
    Text(
        "Get notified when habits unlock and when your streak is at risk.",
        fontSize = 12.sp, color = SoftText, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp)
    )
    VivichiButton(text = "Yes, remind me!", onClick = onEnable, modifier = Modifier.fillMaxWidth())
    OutlinedButton(
        onClick = { SoundFx.click(); onSkip() },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        shape = RoundedCornerShape(18.dp)
    ) { Text("Maybe later", color = SoftText, fontWeight = FontWeight.Bold) }
}

@Composable
private fun StepStatusPanel(petName: String, species: String, onEnable: () -> Unit, onSkip: () -> Unit) {
    Text("Keep ${petName.ifBlank { "your buddy" }} close", fontSize = 19.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    Text(
        "Show a live panel in your notification shade with your pet's health and a countdown to your next habit. You can turn it off anytime in More.",
        fontSize = 12.sp, color = SoftText, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = 17.sp,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp)
    )

    // Static preview mirroring the real notification layout, so the choice isn't abstract.
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFFFE6F0), Color(0xFFEADEFF))))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EmojiGlyph(raw = PETS.find { it.id == species }?.emoji ?: "🐱", size = 44.dp)
        Column(Modifier.weight(1f).padding(start = 10.dp, end = 8.dp)) {
            Text(petName.ifBlank { "Your buddy" }, fontSize = 13.sp, fontWeight = FontWeight.Black, color = TextDark, maxLines = 1)
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.7f))
                ) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(0.86f).background(GreenDark, RoundedCornerShape(6.dp)))
                }
                Text("86%", fontSize = 10.sp, fontWeight = FontWeight.Black, color = SoftText, modifier = Modifier.padding(start = 6.dp))
            }
            Row(Modifier.padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                EmojiGlyph(raw = "💧", size = 12.dp)
                Text(" Drink Water", fontSize = 11.sp, color = TextDark, maxLines = 1)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("28:12", fontSize = 16.sp, fontWeight = FontWeight.Black, color = PinkDark)
            Text("left to do", fontSize = 9.sp, color = SoftText)
        }
    }

    VivichiButton(text = "Yes, show it!", onClick = onEnable, modifier = Modifier.fillMaxWidth().padding(top = 18.dp))
    OutlinedButton(
        onClick = { SoundFx.click(); onSkip() },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        shape = RoundedCornerShape(18.dp)
    ) { Text("No thanks", color = SoftText, fontWeight = FontWeight.Bold) }
}
