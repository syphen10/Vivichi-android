package com.vivichi.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vivichi.app.data.*
import com.vivichi.app.domain.GameLogic
import com.vivichi.app.ui.VivichiViewModel
import com.vivichi.app.ui.components.*
import com.vivichi.app.ui.components.CharacterView
import com.vivichi.app.ui.components.EmojiGlyph
import com.vivichi.app.ui.components.PremiumGradient
import com.vivichi.app.ui.components.rarityColor
import com.vivichi.app.ui.components.enterFromBelow
import com.vivichi.app.ui.components.bounceClick
import com.vivichi.app.ui.theme.*
import com.vivichi.app.util.SoundFx
import java.time.LocalDate

private fun currentSeason(): String = when (LocalDate.now().monthValue) {
    in 3..5 -> "spring"; in 6..8 -> "summer"; in 9..11 -> "autumn"; else -> "winter"
}

/** What the buy sheet is currently asking about. */
private sealed class Pending {
    data class Species(val p: PetSpecies) : Pending()
    data class Wearable(val o: OutfitInfo) : Pending()
}

@Composable
fun StyleScreen(viewModel: VivichiViewModel, adOffer: com.vivichi.app.ui.components.AdOffer?, onOpenPremium: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val season = currentSeason()
    val context = LocalContext.current
    var pending by remember { mutableStateOf<Pending?>(null) }
    var celebrate by remember { mutableStateOf<Pair<String, String>?>(null) }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        item {
            PageHeader(
                title = "Style",
                subtitle = "Dress up your buddy and your app",
                emoji = "🎨",
                accent = listOf(Purple, PurpleDark),
                horizontalPadding = 2.dp,
                stats = listOf(
                    HeaderStat("${PETS.count { GameLogic.ownsSpecies(state, it.id) }}/${PETS.size}", "Buddies", PinkDark),
                    HeaderStat("${THEMES.count { GameLogic.canWear(state, it) }}/${THEMES.size}", "Themes", PurpleDark),
                    HeaderStat("${state.coins}", "Coins", Orange)
                )
            )
        }
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 6.dp)
                    .cardShadow(26.dp, 4.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.White)
                    .background(Brush.linearGradient(seasonalGradient(state.pet.outfit).map { it.copy(alpha = 0.35f) }))
                    .border(1.dp, CardBorder, RoundedCornerShape(26.dp))
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                com.vivichi.app.ui.components.FloatingSparkles(Modifier.matchParentSize(), color = Color.White, count = 10, seed = 13)
                // Switching buddy or outfit pops the preview with a little spin.
                androidx.compose.animation.AnimatedContent(
                    targetState = state.pet.species to state.pet.outfit,
                    transitionSpec = {
                        (androidx.compose.animation.scaleIn(spring(dampingRatio = 0.45f, stiffness = 380f), initialScale = 0.5f) + androidx.compose.animation.fadeIn()) togetherWith
                            (androidx.compose.animation.scaleOut(targetScale = 1.2f) + androidx.compose.animation.fadeOut(tween(120)))
                    },
                    label = "preview"
                ) { (sp, outfit) ->
                    CharacterView(species = sp, mood = GameLogic.mood(state), outfit = outfit, health = state.pet.health, size = 130.dp)
                }
            }
        }

        item { SectionHeader("Buddies", "${PETS.count { GameLogic.ownsSpecies(state, it.id) }}/${PETS.size} unlocked") }
        gridRows("pets", PETS) { p ->
                val owned = GameLogic.ownsSpecies(state, p.id)
                BuddyCard(
                    pet = p,
                    owned = owned,
                    selected = state.pet.species == p.id,
                    onClick = {
                        if (owned) viewModel.pickSpecies(p.id) else pending = Pending.Species(p)
                    }
                )
            }

        item { SectionHeader("Outfits", "Level up or go Premium") }
        gridRows("outfits", OUTFITS) { o ->
                val canWear = GameLogic.canWear(state, o)
                val sub = when {
                    canWear && state.pet.outfit == o.id -> "Wearing"
                    canWear -> "Tap to wear"
                    o.premium -> "Premium"
                    else -> "Lv.${o.level}"
                }
                WearCard(o.emoji, o.name, sub, locked = !canWear, premiumLock = o.premium && !canWear, active = state.pet.outfit == o.id) {
                    when {
                        canWear -> viewModel.pickOutfit(o.id)
                        o.premium -> onOpenPremium()
                        else -> android.widget.Toast.makeText(context, "Reach level ${o.level} to unlock ${o.name}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }

        item {
            Row(Modifier.padding(top = 18.dp, bottom = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Seasons ", fontSize = 15.sp, fontWeight = FontWeight.Black)
                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(seasonBg(season)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("Always free", fontSize = 10.sp, fontWeight = FontWeight.Black, color = seasonFg(season))
                }
            }
        }
        gridRows("seasons", SEASONAL_OUTFITS, columns = 4) { o ->
                val isCurrent = o.season == season
                WearCard(o.emoji, o.name, if (isCurrent) "Now" else "Free", locked = false, active = state.pet.outfit == o.id, dim = !isCurrent) {
                    viewModel.pickOutfit(o.id)
                }
            }

        item { SectionHeader("Themes", "Recolour the whole app") }
        gridRows("themes", THEMES) { o ->
                val canWear = GameLogic.canWear(state, o)
                val sub = when {
                    canWear && state.pet.outfit == o.id -> "Active"
                    canWear -> "Tap to use"
                    o.premium -> "Premium"
                    else -> "${o.price}"
                }
                WearCard(
                    o.emoji, o.name, sub,
                    locked = !canWear,
                    premiumLock = o.premium && !canWear,
                    priceTag = !canWear && !o.premium,
                    active = state.pet.outfit == o.id,
                    swatch = seasonalGradient(o.id)
                ) {
                    when {
                        canWear -> viewModel.pickOutfit(o.id)
                        o.premium -> onOpenPremium()
                        else -> pending = Pending.Wearable(o)
                    }
                }
            }
        item { Spacer(Modifier.height(24.dp)) }
    }

    when (val p = pending) {
        is Pending.Species -> BuyDialog(
            emoji = p.p.emoji,
            name = p.p.name,
            price = p.p.price,
            coins = state.coins,
            subtitle = "${p.p.rarity.label} buddy",
            onBuy = {
                if (viewModel.buySpecies(p.p.id)) { SoundFx.complete(); celebrate = p.p.emoji to p.p.name }
                pending = null
            },
            onGetPremium = { pending = null; onOpenPremium() },
            onDismiss = { pending = null },
            ad = adOffer
        )
        is Pending.Wearable -> BuyDialog(
            emoji = p.o.emoji,
            name = p.o.name,
            price = p.o.price,
            coins = state.coins,
            subtitle = "Colour theme",
            onBuy = {
                if (viewModel.buyWearable(p.o.id)) { SoundFx.complete(); celebrate = p.o.emoji to p.o.name }
                pending = null
            },
            onGetPremium = { pending = null; onOpenPremium() },
            onDismiss = { pending = null },
            ad = adOffer
        )
        null -> Unit
    }
    celebrate?.let { (emoji, name) ->
        com.vivichi.app.ui.components.UnlockedDialog(emoji = emoji, name = name, onDismiss = { celebrate = null })
    }
}

@Composable
private fun SectionHeader(title: String, sub: String) {
    val emoji = when (title) { "Buddies" -> "🐾"; "Outfits" -> "🧣"; "Themes" -> "🌈"; else -> null }
    SectionTitle(title = title, emoji = emoji, trailing = sub, horizontalPadding = 2.dp)
}

/**
 * Emits one lazy-list item per row of [columns] cells. Rows are composed only as they scroll
 * into view (a whole 16-card grid as a single item composed every card at once, which stuttered).
 * Each row slides in once; the stagger runs across the row's cells.
 */
private fun <T> LazyListScope.gridRows(
    key: String,
    items: List<T>,
    columns: Int = 3,
    cell: @Composable (T) -> Unit
) {
    items.chunked(columns).forEachIndexed { r, row ->
        item(key = "$key-$r") {
            Row(
                Modifier.padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEachIndexed { c, item ->
                    Box(Modifier.weight(1f).enterFromBelow(c, distance = 18.dp)) { cell(item) }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun pressScale(selected: Boolean): Float =
    animateFloatAsState(if (selected) 1.04f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "sel").value

@Composable
private fun BuddyCard(pet: PetSpecies, owned: Boolean, selected: Boolean, onClick: () -> Unit) {
    val border by animateColorAsState(if (selected) PinkDark else CardBorder, label = "border")
    Box(Modifier.fillMaxWidth().scale(pressScale(selected))) {
        Column(
            Modifier
                .fillMaxWidth()
                .cardShadow(18.dp, 2.dp)
                .bounceClick(RoundedCornerShape(18.dp), pressedScale = 0.92f) { SoundFx.click(); onClick() }
                .background(if (selected) Color(0xFFFFF0F5) else Color.White)
                .border(2.dp, border, RoundedCornerShape(18.dp))
                .padding(top = 12.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.alpha(if (owned) 1f else 0.4f)) { EmojiGlyph(raw = pet.emoji, size = 36.dp) }
            Text(pet.name, fontSize = 10.sp, fontWeight = FontWeight.Black, color = TextDark, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(4.dp))
            if (owned) {
                Text(
                    if (selected) "With you" else pet.rarity.label,
                    fontSize = 9.sp, fontWeight = FontWeight.Black,
                    color = if (selected) PinkDark else rarityColor(pet.rarity)
                )
            } else {
                PriceChip(pet.price)
            }
        }
        if (!owned) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(rarityColor(pet.rarity))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) { Text(pet.rarity.label, fontSize = 7.sp, fontWeight = FontWeight.Black, color = Color.White) }
            LockBadge(Modifier.align(Alignment.TopEnd))
        } else if (selected) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(6.dp).size(18.dp).clip(CircleShape).background(PinkDark),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(12.dp)) }
        }
    }
}

@Composable
private fun PriceChip(price: Int) {
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFFFFF4D6)).padding(horizontal = 7.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EmojiGlyph(raw = "🪙", size = 11.dp)
        Text(" $price", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFB7791F))
    }
}

@Composable
private fun LockBadge(modifier: Modifier, premium: Boolean = false) {
    val infinite = rememberInfiniteTransition(label = "lock")
    val wobble by infinite.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "wobble"
    )
    Box(
        modifier
            .padding(6.dp)
            .size(22.dp)
            .graphicsLayer { rotationZ = wobble }
            .clip(RoundedCornerShape(8.dp))
            .background(if (premium) Brush.linearGradient(PremiumGradient) else Brush.linearGradient(listOf(Color.White, Color.White))),
        contentAlignment = Alignment.Center
    ) { EmojiGlyph(raw = if (premium) "💎" else "🔒", size = 12.dp) }
}

private fun seasonBg(s: String) = when (s) { "spring" -> Color(0xFFEAFAF1); "summer" -> Color(0xFFFEF9E7); "autumn" -> Color(0xFFFEF0E6); else -> Color(0xFFEBF5FB) }
private fun seasonFg(s: String) = when (s) { "spring" -> Color(0xFF27AE60); "summer" -> Color(0xFFF39C12); "autumn" -> Color(0xFFD35400); else -> Color(0xFF2980B9) }

@Composable
private fun WearCard(
    emoji: String,
    name: String,
    sub: String,
    locked: Boolean,
    active: Boolean,
    premiumLock: Boolean = false,
    priceTag: Boolean = false,
    dim: Boolean = false,
    swatch: List<Color>? = null,
    onClick: () -> Unit
) {
    val border by animateColorAsState(if (active) PinkDark else CardBorder, label = "border")
    Box(Modifier.fillMaxWidth().scale(pressScale(active))) {
        Column(
            Modifier
                .fillMaxWidth()
                .cardShadow(18.dp, 2.dp)
                .bounceClick(RoundedCornerShape(18.dp), pressedScale = 0.92f) { SoundFx.click(); onClick() }
                .background(Color.White)
                .border(2.dp, border, RoundedCornerShape(18.dp))
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (swatch != null) Brush.linearGradient(swatch) else Brush.linearGradient(listOf(BgPink, BgPink)))
                    .alpha(if (locked) 0.5f else if (dim) 0.65f else 1f),
                contentAlignment = Alignment.Center
            ) { EmojiGlyph(raw = emoji, size = 22.dp) }
            Text(name, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 5.dp))
            if (priceTag) {
                Spacer(Modifier.height(2.dp))
                PriceChip(sub.toIntOrNull() ?: 0)
            } else {
                Text(
                    sub, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                    color = when { active -> PinkDark; premiumLock -> Color(0xFFB388FF); else -> SoftText }
                )
            }
        }
        if (locked) LockBadge(Modifier.align(Alignment.TopEnd), premium = premiumLock)
    }
}
