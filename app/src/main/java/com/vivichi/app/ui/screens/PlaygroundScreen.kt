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
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.CircleShape
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
import com.vivichi.app.ui.components.*
import com.vivichi.app.ui.components.FloatingSparkles
import com.vivichi.app.ui.components.bounceClick
import com.vivichi.app.ui.components.enterFromBelow
import com.vivichi.app.ui.theme.*
import com.vivichi.app.util.SoundFx
import kotlinx.coroutines.delay
import androidx.compose.animation.togetherWith
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
    var burst by remember { mutableStateOf<Pair<Int, String>?>(null) }

    // Every tap pops the pet up and springs it back (a toggle would shrink on every other tap).
    val pop = remember { Animatable(1f) }
    LaunchedEffect(bump) {
        if (bump > 0) {
            pop.animateTo(1.16f, tween(110))
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
    }
    val scale = pop.value

    // Closed-eyes "satisfied" face for a moment after every action; a new tap restarts the timer.
    var happy by remember { mutableStateOf(false) }
    LaunchedEffect(bump) {
        if (bump > 0) {
            happy = true
            delay(2000)
            happy = false
        }
    }
    var quote by remember { mutableStateOf<com.vivichi.app.data.Quote?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageHeader(
            title = "Playground",
            subtitle = "Spend some time with ${state.pet.name.ifBlank { "your buddy" }}",
            emoji = "🎾",
            accent = seasonalGradient(state.pet.outfit)
        )

        // Play-mat scene: themed backdrop, the pet front and centre, reactions pop out of it.
        Column(
            Modifier
                .padding(16.dp, 12.dp, 16.dp, 4.dp)
                .fillMaxWidth()
                .enterFromBelow(1)
                .card(radius = 26.dp)
        ) {
            val theme = seasonalGradient(state.pet.outfit)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                androidx.compose.ui.graphics.lerp(theme.first(), Color.White, 0.7f),
                                androidx.compose.ui.graphics.lerp(theme.last(), Color.White, 0.85f)
                            )
                        )
                    )
            ) {
                androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                    // round play rug under the pet
                    drawOval(
                        Color.White.copy(alpha = 0.7f),
                        topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height * 0.74f),
                        size = androidx.compose.ui.geometry.Size(size.width * 0.64f, size.height * 0.2f)
                    )
                    drawOval(
                        theme.first().copy(alpha = 0.25f),
                        topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.26f, size.height * 0.78f),
                        size = androidx.compose.ui.geometry.Size(size.width * 0.48f, size.height * 0.12f)
                    )
                }
                FloatingSparkles(Modifier.matchParentSize(), color = Color.White, count = 12, seed = 21)
                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.scale(scale)) {
                        CharacterView(species = state.pet.species, mood = Mood.HAPPY, outfit = state.pet.outfit, health = state.pet.health, size = 180.dp, happy = happy)
                    }
                    burst?.let { (id, emoji) -> EmojiBurst(trigger = id, emoji = emoji, modifier = Modifier.size(180.dp)) }
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = reaction != null,
                    modifier = Modifier.align(Alignment.TopEnd).padding(14.dp),
                    enter = androidx.compose.animation.scaleIn(spring(dampingRatio = 0.45f)) + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut()
                ) {
                    Box(
                        Modifier.size(58.dp).cardShadow(18.dp, 4.dp).clip(RoundedCornerShape(18.dp)).background(Color.White),
                        contentAlignment = Alignment.Center
                    ) { reaction?.let { EmojiGlyph(raw = it, size = 32.dp) } }
                }
            }
            Box(Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) {
                androidx.compose.animation.AnimatedContent(
                    targetState = message,
                    transitionSpec = { androidx.compose.animation.scaleIn(initialScale = 0.9f) + androidx.compose.animation.fadeIn() togetherWith androidx.compose.animation.fadeOut() },
                    label = "msg"
                ) { m ->
                    Text(
                        m, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 19.sp,
                        color = TextDark,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFF8F3FC)).padding(14.dp, 12.dp)
                    )
                }
            }
        }

        SectionTitle("Play together", "🐾")

        val rows = ACTIONS.chunked(2)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEachIndexed { rowIndex, row ->
                Row(Modifier.enterFromBelow(rowIndex + 1), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { action ->
                        PlayButton(action, Modifier.weight(1f), enabled = !cooldown) {
                            if (cooldown) return@PlayButton
                            cooldown = true
                            val msgs = PLAY_MESSAGES[action.key].orEmpty()
                            message = if (msgs.isNotEmpty()) msgs[Random.nextInt(msgs.size)] else "..."
                            reaction = REACT_EMOJI[action.key]
                            bump++
                            burst = bump to action.emoji
                            SoundFx.playgroundAction(action.key)
                        }
                    }
                }
            }
            WisdomButton(Modifier.enterFromBelow(rows.size + 1)) {
                SoundFx.click()
                quote = pickQuote(quote)
                bump++
            }
        }
        Spacer(Modifier.height(20.dp))
    }

    quote?.let { q ->
        WisdomDialog(
            quote = q,
            species = state.pet.species,
            petName = state.pet.name.ifBlank { "your buddy" },
            onAnother = { SoundFx.click(); quote = pickQuote(q); bump++ },
            onDismiss = { quote = null }
        )
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

private fun pickQuote(current: com.vivichi.app.data.Quote?): com.vivichi.app.data.Quote {
    val pool = com.vivichi.app.data.WISDOM_QUOTES
    var q = pool[Random.nextInt(pool.size)]
    while (q == current && pool.size > 1) q = pool[Random.nextInt(pool.size)]
    return q
}

private val ActionCaptions = mapOf(
    "feed" to "Snack time", "pet" to "Gentle pats", "play" to "Fetch!",
    "hug" to "Big squeeze", "sing" to "La la la", "tickle" to "Hehehe"
)

/**
 * Chunky "game button": a raised face on a darker lip (so it looks pressable), a glossy highlight,
 * decorative bubbles, and the icon sitting in its own white badge.
 */
@Composable
private fun PlayButton(action: PlayAction, modifier: Modifier = Modifier, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val top = action.colors.first()
    val bottom = action.colors.last()
    val lip = androidx.compose.ui.graphics.lerp(bottom, Color(0xFF3D2E4E), 0.28f)
    Box(
        modifier
            .cardShadow(22.dp, 5.dp)
            .bounceClick(shape, enabled = enabled, pressedScale = 0.92f, onClick = onClick)
            .background(lip)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 5.dp)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(androidx.compose.ui.graphics.lerp(top, Color.White, 0.18f), bottom)))
        ) {
            androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                // gloss band + bubbles
                drawRoundRect(
                    Color.White.copy(alpha = 0.18f),
                    topLeft = androidx.compose.ui.geometry.Offset(8.dp.toPx(), 5.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(size.width - 16.dp.toPx(), size.height * 0.34f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                )
                drawCircle(Color.White.copy(alpha = 0.14f), size.height * 0.55f, androidx.compose.ui.geometry.Offset(size.width * 0.95f, size.height * 0.95f))
                drawCircle(Color.White.copy(alpha = 0.12f), 7.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width * 0.72f, size.height * 0.28f))
                drawCircle(Color.White.copy(alpha = 0.10f), 4.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width * 0.84f, size.height * 0.52f))
            }
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .shadow(4.dp, CircleShape, clip = false, spotColor = lip, ambientColor = lip)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) { EmojiGlyph(raw = action.emoji, size = 28.dp) }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(action.label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text(ActionCaptions[action.key] ?: "", color = Color.White.copy(alpha = 0.88f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private val WisdomTop = Color(0xFF3B2F7A)
private val WisdomBottom = Color(0xFF6A3FA0)
private val Gold = Color(0xFFFFD166)

@Composable
private fun WisdomButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier
            .fillMaxWidth()
            .cardShadow(24.dp, 6.dp)
            .bounceClick(shape, pressedScale = 0.95f, onClick = onClick)
            .background(Color(0xFF2A1F5C))
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 5.dp)
                .clip(shape)
                .background(Brush.linearGradient(listOf(WisdomTop, WisdomBottom)))
        ) {
            FloatingSparkles(Modifier.matchParentSize(), color = Gold, count = 12, seed = 77)
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Gold.copy(alpha = 0.55f), Color.Transparent))),
                    contentAlignment = Alignment.Center
                ) { EmojiGlyph(raw = "🔮", size = 34.dp, modifier = Modifier.floating(3.dp, 1800)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Wisdom", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Text("Words from real people who made it", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    Modifier.clip(RoundedCornerShape(12.dp)).background(Gold).padding(horizontal = 12.dp, vertical = 7.dp)
                ) { Text("Reveal", color = Color(0xFF3B2F7A), fontSize = 12.sp, fontWeight = FontWeight.Black) }
            }
        }
    }
}

@Composable
private fun WisdomDialog(
    quote: com.vivichi.app.data.Quote,
    species: String,
    petName: String,
    onAnother: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(WisdomTop, WisdomBottom)))
        ) {
            Box(Modifier.fillMaxWidth()) {
                FloatingSparkles(Modifier.matchParentSize(), color = Gold, count = 16, seed = 5)
                Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${petName.uppercase()} SHARES SOME WISDOM", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    CharacterView(species = species, mood = Mood.HAPPY, outfit = "default", health = 100, size = 110.dp, happy = true)
                    Spacer(Modifier.height(6.dp))
                    androidx.compose.animation.AnimatedContent(
                        targetState = quote,
                        transitionSpec = {
                            (androidx.compose.animation.fadeIn(tween(260)) + androidx.compose.animation.slideInVertically { it / 6 }) togetherWith
                                androidx.compose.animation.fadeOut(tween(120))
                        },
                        label = "quote"
                    ) { q ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("“", color = Gold, fontSize = 52.sp, fontWeight = FontWeight.Black, lineHeight = 40.sp)
                            Text(q.text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 25.sp)
                            Spacer(Modifier.height(12.dp))
                            Box(Modifier.width(34.dp).height(2.dp).background(Gold.copy(alpha = 0.6f)))
                            Spacer(Modifier.height(10.dp))
                            Text("— ${q.author}", color = Gold, fontSize = 14.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier
                                .weight(1f)
                                .bounceClick(RoundedCornerShape(16.dp)) { SoundFx.click(); onDismiss() }
                                .background(Color.White.copy(alpha = 0.14f))
                                .padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center
                        ) { Text("Close", color = Color.White, fontWeight = FontWeight.Black) }
                        Box(
                            Modifier
                                .weight(1f)
                                .bounceClick(RoundedCornerShape(16.dp), onClick = onAnother)
                                .background(Gold)
                                .padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center
                        ) { Text("Another one", color = WisdomTop, fontWeight = FontWeight.Black) }
                    }
                }
            }
        }
    }
}
