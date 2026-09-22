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
import androidx.compose.ui.draw.drawBehind
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
    val clock = LocalAmbientTime.current
    val colors = if (isPremium) listOf(Color(0xFFFFD27A), Color(0xFFFFB3C6)) else PremiumGradient
    Row(
        modifier
            .clip(RoundedCornerShape(20.dp))
            // Shimmer is read in the draw phase only, so the button repaints without recomposing.
            .drawBehind {
                val shift = pingPongLinear(clock.value, 2600)
                drawRect(Brush.linearGradient(colors, start = androidx.compose.ui.geometry.Offset(shift * 120f, 0f), end = androidx.compose.ui.geometry.Offset(260f + shift * 120f, 80f)))
            }
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
        PremiumButton(isPremium, modifier = Modifier.pulsing(1.04f, 1400), onClick = onPremium)
    }
}

@Composable
fun PremiumDialog(
    isPremium: Boolean,
    priceLabel: String?,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit,
    message: String? = null
) {
    // Confetti only when Premium switches on while the dialog is open (i.e. a purchase just landed).
    val wasPremium = remember { isPremium }
    Dialog(onDismissRequest = onDismiss) {
      Box(contentAlignment = Alignment.Center) {
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
                    EmojiGlyph(raw = "💎", size = 58.dp, modifier = Modifier.graphicsLayer { scaleX = s; scaleY = s }.wiggling(6f, 1300))
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
                ).forEachIndexed { i, (emoji, text) ->
                    Row(Modifier.padding(vertical = 6.dp).enterFromBelow(i + 1, distance = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(BgPink), contentAlignment = Alignment.Center) {
                            EmojiGlyph(raw = emoji, size = 20.dp)
                        }
                        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark, modifier = Modifier.padding(start = 12.dp))
                    }
                }
                Spacer(Modifier.height(14.dp))
                // Result of the last buy/restore attempt, right where the user is looking.
                androidx.compose.animation.AnimatedVisibility(visible = message != null) {
                    Text(
                        message.orEmpty(),
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B6914), textAlign = TextAlign.Center, lineHeight = 18.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFFF3CD))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
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
        ConfettiBurst(trigger = if (isPremium && !wasPremium) true else null, modifier = Modifier.matchParentSize(), count = 100, colors = PremiumGradient + ConfettiColors)
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
    onDismiss: () -> Unit,
    ad: AdOffer? = null
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
                    "You need ${price - coins} more coins. Earn them by completing habits${if (ad != null) " or watching ads" else ""}.",
                    fontSize = 12.sp, color = TextDark, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                if (ad != null) {
                    WatchAdButton(ad, Modifier.fillMaxWidth().padding(bottom = 10.dp))
                }
                PremiumButton(isPremium = false, modifier = Modifier.align(Alignment.CenterHorizontally), onClick = onGetPremium)
            }
            TextButton(onClick = { SoundFx.click(); onDismiss() }) { Text("Not now", color = SoftText, fontWeight = FontWeight.Bold) }
        }
    }
}

/** Celebration after spending coins: the item bursts in on a spinning sunburst with confetti. */
@Composable
fun UnlockedDialog(emoji: String, name: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        val appear = remember { Animatable(0f) }
        LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 260f)) }
        val spin = rememberInfiniteTransition(label = "rays")
        val rot by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "rot")
        Box(contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Unlocked!", fontSize = 24.sp, fontWeight = FontWeight.Black, color = PinkDark, modifier = Modifier.pulsing(1.05f, 800))
                Box(Modifier.size(170.dp), contentAlignment = Alignment.Center) {
                    // Rotating sunburst rays behind the item.
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = rot; alpha = appear.value.coerceIn(0f, 1f) }) {
                        val c = center
                        val r = size.minDimension / 2
                        for (i in 0 until 12) {
                            val a0 = Math.toRadians((i * 30).toDouble())
                            val a1 = Math.toRadians((i * 30 + 13).toDouble())
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(c.x, c.y)
                                lineTo(c.x + (r * kotlin.math.cos(a0)).toFloat(), c.y + (r * kotlin.math.sin(a0)).toFloat())
                                lineTo(c.x + (r * kotlin.math.cos(a1)).toFloat(), c.y + (r * kotlin.math.sin(a1)).toFloat())
                                close()
                            }
                            drawPath(path, Brush.radialGradient(listOf(Color(0xFFFFD166).copy(alpha = 0.55f), Color.Transparent), center = c, radius = r))
                        }
                    }
                    EmojiGlyph(
                        raw = emoji, size = 92.dp,
                        modifier = Modifier.graphicsLayer {
                            val s = appear.value
                            scaleX = s; scaleY = s
                            rotationZ = (1f - s) * -120f
                        }.floating(5.dp, 1500)
                    )
                }
                Text(name, fontSize = 20.sp, fontWeight = FontWeight.Black, color = TextDark)
                Text("is yours to keep — and already equipped!", fontSize = 12.sp, color = SoftText, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 2.dp, bottom = 14.dp))
                VivichiButton(text = "Yay!", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
            ConfettiBurst(trigger = name, modifier = Modifier.matchParentSize(), count = 80, origin = androidx.compose.ui.geometry.Offset(0.5f, 0.4f))
        }
    }
}

/** Everything a "watch an ad" button needs. Null wherever ads don't apply (Premium users). */
data class AdOffer(val adsLeft: Int, val ready: Boolean, val onWatch: () -> Unit)

@Composable
fun WatchAdButton(ad: AdOffer, modifier: Modifier = Modifier) {
    // The ad is fetched only while this button is on screen (see RewardedAds.offerShown).
    if (ad.adsLeft > 0) {
        val context = androidx.compose.ui.platform.LocalContext.current
        androidx.compose.runtime.DisposableEffect(Unit) {
            com.vivichi.app.monetize.RewardedAds.offerShown(context)
            onDispose { com.vivichi.app.monetize.RewardedAds.offerHidden() }
        }
    }
    val enabled = ad.adsLeft > 0 && ad.ready
    val label = when {
        ad.adsLeft <= 0 -> "Daily ad limit reached"
        !ad.ready -> "Loading ad…"
        else -> "Watch ad  +${com.vivichi.app.domain.GameLogic.COINS_PER_AD}"
    }
    Row(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) Brush.linearGradient(listOf(Color(0xFFFFC857), Color(0xFFFFA726))) else Brush.linearGradient(listOf(Color(0xFFEDE7F6), Color(0xFFEDE7F6))))
            .clickable(enabled = enabled) { SoundFx.click(); ad.onWatch() }
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        EmojiGlyph(raw = "📺", size = 18.dp)
        Text("  $label", fontSize = 15.sp, fontWeight = FontWeight.Black, color = if (enabled) Color.White else SoftText)
        if (enabled) {
            Spacer(Modifier.width(2.dp))
            EmojiGlyph(raw = "🪙", size = 16.dp)
        }
    }
}

/** Opened from the coin chip: balance, the ad button, and every way to earn. */
@Composable
fun CoinsDialog(coins: Int, isPremium: Boolean, ad: AdOffer?, onGetPremium: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.clip(RoundedCornerShape(26.dp)).background(Color.White).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Bob + tilt rather than a full flip: a flip passes through zero width and the coin
            // blinks out of existence every half second.
            EmojiGlyph(raw = "🪙", size = 64.dp, modifier = Modifier.floating(6.dp, 1500).wiggling(9f, 1100))
            val shown by animateIntAsState(coins, tween(700), label = "bal")
            Text("$shown coins", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFFB7791F), modifier = Modifier.padding(top = 6.dp))
            Text("Spend them on buddies and themes in Style", fontSize = 12.sp, color = SoftText, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)

            Spacer(Modifier.height(16.dp))
            if (ad != null) {
                WatchAdButton(ad, Modifier.fillMaxWidth())
                Text(
                    "${ad.adsLeft} of ${com.vivichi.app.domain.GameLogic.MAX_ADS_PER_DAY} ads left today",
                    fontSize = 11.sp, color = SoftText, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp)
                )
                Spacer(Modifier.height(14.dp))
            }

            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(BgPink).padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text("Ways to earn", fontSize = 12.sp, fontWeight = FontWeight.Black, color = TextDark)
                Text(
                    "Habits pay up to ${com.vivichi.app.domain.GameLogic.MAX_HABIT_COINS_PER_DAY} coins a day. New habits start paying the day after you add them.",
                    fontSize = 10.sp, color = SoftText, fontWeight = FontWeight.SemiBold, lineHeight = 14.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                )
                listOf(
                    "✅" to "Easy habit" to "+5",
                    "💪" to "Medium habit" to "+8",
                    "🔥" to "Hard habit" to "+15",
                    "🥳" to "Finish every habit today" to "+${com.vivichi.app.domain.GameLogic.COINS_ALL_DONE_BONUS}",
                    "💎" to (if (isPremium) "Premium daily bonus (active)" else "Premium daily bonus") to "+${com.vivichi.app.domain.GameLogic.PREMIUM_DAILY_BONUS}"
                ).forEach { (pair, amount) ->
                    val (emoji, text) = pair
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        EmojiGlyph(raw = emoji, size = 16.dp)
                        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark, modifier = Modifier.padding(start = 8.dp).weight(1f))
                        Text(amount, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFFB7791F))
                    }
                }
            }
            if (!isPremium) {
                Spacer(Modifier.height(12.dp))
                PremiumButton(isPremium = false, onClick = onGetPremium)
            }
            TextButton(onClick = { SoundFx.click(); onDismiss() }) { Text("Close", color = SoftText, fontWeight = FontWeight.Bold) }
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
