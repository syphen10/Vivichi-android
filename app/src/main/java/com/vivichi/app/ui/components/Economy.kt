package com.vivichi.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vivichi.app.data.PETS
import com.vivichi.app.data.PetSpecies
import com.vivichi.app.data.Rarity
import com.vivichi.app.ui.dialogs.VivichiButton
import com.vivichi.app.ui.theme.*
import com.vivichi.app.util.SoundFx

val PremiumGradient = listOf(Color(0xFFFFC857), Color(0xFFFF7EB3), Color(0xFFB388FF))

fun rarityColor(r: Rarity): Color = when (r) {
    Rarity.FREE -> Color(0xFF4DD9AC)
    Rarity.COMMON -> Color(0xFF6FB1FF)
    Rarity.RARE -> Color(0xFFB388FF)
    Rarity.LEGENDARY -> Color(0xFFFFA726)
}

/** Coin balance pill. The number counts up/down when it changes. */
@Composable
fun CoinChip(coins: Int, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val shown by animateIntAsState(coins, animationSpec = tween(700, easing = FastOutSlowInEasing), label = "coins")
    var lastCoins by remember { mutableIntStateOf(coins) }
    val bump = remember { Animatable(1f) }
    LaunchedEffect(coins) {
        if (coins > lastCoins) {
            bump.animateTo(1.18f, tween(140))
            bump.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        lastCoins = coins
    }
    Row(
        modifier
            .graphicsLayer { scaleX = bump.value; scaleY = bump.value }
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.5.dp, Color(0xFFFFE0A3), RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable { SoundFx.click(); onClick() } else Modifier)
            .padding(start = 8.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EmojiGlyph(raw = "🪙", size = 18.dp)
        Spacer(Modifier.width(5.dp))
        Text("$shown", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFFB7791F))
    }
}

/** Always-visible Premium entry point, with a slow shimmer so it catches the eye without nagging. */
@Composable
fun PremiumButton(isPremium: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shimmer = rememberInfiniteTransition(label = "premium")
    val shift by shimmer.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse), label = "shift")
    val colors = if (isPremium) listOf(Color(0xFFFFD27A), Color(0xFFFFB3C6)) else PremiumGradient
    Row(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(colors, start = androidx.compose.ui.geometry.Offset(0f + shift * 120f, 0f), end = androidx.compose.ui.geometry.Offset(260f + shift * 120f, 80f)))
            .clickable { SoundFx.click(); onClick() }
            .padding(start = 9.dp, end = 13.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EmojiGlyph(raw = "💎", size = 17.dp)
        Spacer(Modifier.width(5.dp))
        Text(if (isPremium) "Premium" else "Go Premium", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
    }
}

/** Global top bar: coins on the left, Premium on the right. */
@Composable
fun EconomyBar(coins: Int, isPremium: Boolean, onCoins: () -> Unit, onPremium: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        CoinChip(coins, onClick = onCoins)
        PremiumButton(isPremium, onClick = onPremium)
    }
}

@Composable
fun PremiumDialog(isPremium: Boolean, priceLabel: String?, onBuy: () -> Unit, onRestore: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(PremiumGradient))
                    .padding(top = 22.dp, bottom = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val pulse = rememberInfiniteTransition(label = "gem")
                    val s by pulse.animateFloat(1f, 1.12f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "s")
                    EmojiGlyph(raw = "💎", size = 58.dp, modifier = Modifier.graphicsLayer { scaleX = s; scaleY = s })
                    Text("Vivichi Premium", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.padding(top = 8.dp))
                    Text(if (isPremium) "You're Premium — thank you! 💗" else "One-time unlock, yours forever", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.92f))
                }
                IconButton(onClick = { SoundFx.click(); onDismiss() }, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(Icons.Filled.Close, null, tint = Color.White)
                }
            }
            Column(Modifier.padding(20.dp)) {
                listOf(
                    "🦄" to "Every animal unlocked — all ${PETS.size} buddies",
                    "🌈" to "Exclusive themes & outfits",
                    "🪙" to "+25 bonus coins every day",
                    "📺" to "No ads, ever"
                ).forEach { (emoji, text) ->
                    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(BgPink), contentAlignment = Alignment.Center) {
                            EmojiGlyph(raw = emoji, size = 20.dp)
                        }
                        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark, modifier = Modifier.padding(start = 12.dp))
                    }
                }
                Spacer(Modifier.height(14.dp))
                if (isPremium) {
                    VivichiButton(text = "All unlocked ✨", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
                } else {
                    VivichiButton(text = if (priceLabel != null) "Unlock for $priceLabel" else "Unlock Premium", onClick = onBuy, modifier = Modifier.fillMaxWidth())
                    TextButton(onClick = { SoundFx.click(); onRestore() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Restore purchase", color = SoftText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/** Confirm spending coins; if short, explains how to earn more. */
@Composable
fun BuyDialog(
    emoji: String,
    name: String,
    price: Int,
    coins: Int,
    subtitle: String,
    onBuy: () -> Unit,
    onGetPremium: () -> Unit,
    onDismiss: () -> Unit
) {
    val affordable = coins >= price
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.clip(RoundedCornerShape(26.dp)).background(Color.White).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val bob = rememberInfiniteTransition(label = "bob")
            val y by bob.animateFloat(0f, -8f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "y")
            EmojiGlyph(raw = emoji, size = 72.dp, modifier = Modifier.graphicsLayer { translationY = y })
            Text(name, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp))
            Text(subtitle, fontSize = 12.sp, color = SoftText, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Row(Modifier.padding(top = 14.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                EmojiGlyph(raw = "🪙", size = 22.dp)
                Text(" $price", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFFB7791F))
            }
            Text("You have $coins coins", fontSize = 11.sp, color = SoftText, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            if (affordable) {
                VivichiButton(text = "Unlock", onClick = onBuy, modifier = Modifier.fillMaxWidth())
            } else {
                Text(
                    "You need ${price - coins} more coins. Earn them by completing habits${" or watching ads"}.",
                    fontSize = 12.sp, color = TextDark, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                PremiumButton(isPremium = false, modifier = Modifier.align(Alignment.CenterHorizontally), onClick = onGetPremium)
            }
            TextButton(onClick = { SoundFx.click(); onDismiss() }) { Text("Not now", color = SoftText, fontWeight = FontWeight.Bold) }
        }
    }
}

/** Blocking picker shown when the current pet's species has become locked. */
@Composable
fun ForcePickPetDialog(petName: String, owned: List<PetSpecies>, onPick: (String) -> Unit) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
        Column(
            Modifier.clip(RoundedCornerShape(26.dp)).background(Color.White).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Choose ${petName.ifBlank { "your buddy" }}'s look", fontSize = 19.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Text(
                "This animal is now an unlockable buddy. Pick one to continue — your name, level, streak and progress all stay.",
                fontSize = 12.sp, color = SoftText, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = 17.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
            )
            owned.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                    row.forEach { p ->
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(BgPink)
                                .clickable { SoundFx.click(); onPick(p.id) }
                                .padding(vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            EmojiGlyph(raw = p.emoji, size = 44.dp)
                            Text(p.name, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
