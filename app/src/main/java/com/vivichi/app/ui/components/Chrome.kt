package com.vivichi.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vivichi.app.data.AppState
import com.vivichi.app.data.petEmoji
import com.vivichi.app.data.xpForLevel
import com.vivichi.app.domain.GameLogic
import com.vivichi.app.ui.theme.*
import com.vivichi.app.util.SoundFx

/*
 * Vivichi's "chrome": the shared surfaces every screen is built from, so the app reads as one
 * designed product instead of a stack of one-off boxes.
 *
 *  - GameHud      persistent top bar: pet portrait, level, health + XP, coins, Premium
 *  - PageHeader   per-screen title with an icon tile and summary stat tiles
 *  - SectionTitle small labelled divider inside a screen
 *  - cardShadow / cardSurface   the one card style (soft plum shadow, hairline border)
 *  - AppBackdrop  themed page colour with a faint paw-and-sparkle texture
 */

private val Plum = Color(0xFF3D2E4E)
val CardBorder = Color(0x143D2E4E)

/** Deeper, richer take on the theme gradient for the HUD, so white text always reads well. */
fun hudGradient(outfit: String): List<Color> {
    val base = seasonalGradient(outfit)
    return listOf(lerp(base.first(), Plum, 0.42f), lerp(base.last(), Plum, 0.18f))
}

/** Soft plum-tinted drop shadow. Put before bounceClick/clip so it isn't clipped away. */
fun Modifier.cardShadow(radius: Dp = 20.dp, elevation: Dp = 4.dp): Modifier =
    shadow(elevation, RoundedCornerShape(radius), clip = false, ambientColor = Color(0x553D2E4E), spotColor = Color(0x443D2E4E))

/** White card body with a hairline border. Use after clip/bounceClick. */
fun Modifier.cardSurface(radius: Dp = 20.dp, color: Color = Color.White): Modifier =
    background(color, RoundedCornerShape(radius)).border(1.dp, CardBorder, RoundedCornerShape(radius))

/** Shadow + clip + surface in one, for non-clickable cards. */
fun Modifier.card(radius: Dp = 20.dp, color: Color = Color.White, elevation: Dp = 4.dp): Modifier =
    cardShadow(radius, elevation).clip(RoundedCornerShape(radius)).cardSurface(radius, color)

/** Dashed rounded outline, for "add something" affordances. */
fun Modifier.dashedBorder(color: Color, radius: Dp, width: Dp = 2.dp): Modifier = drawBehind {
    val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
        width = width.toPx(),
        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 6.dp.toPx()))
    )
    val inset = width.toPx() / 2
    drawRoundRect(
        color = color,
        topLeft = Offset(inset, inset),
        size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius.toPx()),
        style = stroke
    )
}

fun intensityColor(intensity: String): Color = when (intensity) {
    "high" -> PinkDark
    "medium" -> Orange
    else -> GreenDark
}

/** Page background: theme colour plus a faint, static paw-print and sparkle texture. */
@Composable
fun AppBackdrop(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize().background(color)) {
        val cell = 86.dp.toPx()
        val ink = Plum.copy(alpha = 0.045f)
        var row = 0
        var y = cell * 0.5f
        while (y < size.height + cell) {
            var x = if (row % 2 == 0) cell * 0.5f else cell
            var col = 0
            while (x < size.width + cell) {
                if ((row + col) % 2 == 0) {
                    // paw: pad + four toes
                    val r = 5.dp.toPx()
                    drawCircle(ink, r * 1.25f, Offset(x, y + r * 0.6f))
                    drawCircle(ink, r * 0.55f, Offset(x - r * 1.35f, y - r * 0.55f))
                    drawCircle(ink, r * 0.55f, Offset(x - r * 0.48f, y - r * 1.35f))
                    drawCircle(ink, r * 0.55f, Offset(x + r * 0.48f, y - r * 1.35f))
                    drawCircle(ink, r * 0.55f, Offset(x + r * 1.35f, y - r * 0.55f))
                } else {
                    drawSparkle(Offset(x, y), 6.dp.toPx(), ink)
                }
                x += cell
                col++
            }
            y += cell * 0.8f
            row++
        }
    }
}

/** Persistent game HUD shown above every tab. */
@Composable
fun GameHud(state: AppState, onCoins: () -> Unit, onPremium: () -> Unit) {
    val grad = hudGradient(state.pet.outfit)
    val shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(12.dp, shape, clip = false, ambientColor = grad.first(), spotColor = grad.first())
            .clip(shape)
            .background(Brush.linearGradient(grad))
    ) {
        // Decorative light blobs + drifting sparkles give the bar depth without noise.
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(alpha = 0.08f), size.height * 0.9f, Offset(size.width * 0.92f, size.height * 0.05f))
            drawCircle(Color.White.copy(alpha = 0.06f), size.height * 0.55f, Offset(size.width * 0.05f, size.height * 1.05f))
        }
        StaticSparkles(Modifier.matchParentSize(), color = Color.White.copy(alpha = 0.55f), count = 9, seed = 42)

        Column(
            Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CoinChip(state.coins, onClick = onCoins)
                Spacer(Modifier.weight(1f))
                PremiumButton(state.premium, modifier = Modifier.pulsing(1.04f, 1400), onClick = onPremium)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PetPortrait(state)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            state.pet.name.ifBlank { "Buddy" },
                            color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                        )
                        GameLogic.activeTitle(state)?.let { t ->
                            Spacer(Modifier.width(6.dp))
                            Row(
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.18f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                EmojiGlyph(raw = t.emoji, size = 10.dp)
                                Text(" ${t.name}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, maxLines = 1)
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    val health = state.pet.health
                    HudBar(
                        emoji = "❤️",
                        progress = health / 100f,
                        fill = SolidColor(
                            when { health >= 75 -> Color(0xFF5BE3B4); health >= 50 -> Color(0xFFFFB35C); health >= 25 -> Color(0xFFFF7A7A); else -> Color(0xFFFF4D4D) }
                        ),
                        label = "$health/100"
                    )
                    Spacer(Modifier.height(5.dp))
                    val need = xpForLevel(state.pet.level)
                    HudBar(
                        emoji = "⭐",
                        progress = (state.pet.xp.toFloat() / need).coerceIn(0f, 1f),
                        fill = Brush.horizontalGradient(listOf(Color(0xFFFFE08A), Color(0xFFFFB020))),
                        label = "${state.pet.xp}/$need XP"
                    )
                }
            }
        }
    }
}

@Composable
private fun HudBar(emoji: String, progress: Float, fill: Brush, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        EmojiGlyph(raw = emoji, size = 12.dp)
        Spacer(Modifier.width(6.dp))
        AnimatedBar(progress, fill, Modifier.weight(1f), height = 8.dp, track = Color.White.copy(alpha = 0.22f))
        Text(
            label, color = Color.White.copy(alpha = 0.95f), fontSize = 10.sp, fontWeight = FontWeight.Black,
            maxLines = 1, modifier = Modifier.padding(start = 8.dp).widthIn(min = 54.dp)
        )
    }
}

/** Circular pet portrait with a level badge — the HUD's anchor. */
@Composable
fun PetPortrait(state: AppState, size: Dp = 62.dp) {
    Box(Modifier.size(size + 6.dp)) {
        Box(
            Modifier
                .size(size)
                .align(Alignment.TopStart)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.15f))))
                .padding(3.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            EmojiGlyph(raw = petEmoji(state.pet.species), size = size * 0.62f, modifier = Modifier.floating(2.dp, 1800))
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(24.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFFFFD66B), Color(0xFFFF9F2E))))
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("${state.pet.level}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
    }
}

data class HeaderStat(val value: String, val label: String, val accent: Color, val emoji: String? = null)

/** Screen title with an icon tile, plus optional summary tiles so the top of every screen says something. */
@Composable
fun PageHeader(
    title: String,
    subtitle: String,
    emoji: String,
    accent: List<Color> = listOf(Pink, PurpleDark),
    stats: List<HeaderStat> = emptyList(),
    trailing: (@Composable () -> Unit)? = null,
    horizontalPadding: Dp = 16.dp
) {
    Column(Modifier.fillMaxWidth().padding(start = horizontalPadding, end = horizontalPadding, top = 16.dp, bottom = 4.dp).enterFromBelow(0)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(46.dp)
                    .shadow(6.dp, RoundedCornerShape(15.dp), clip = false, spotColor = accent.last(), ambientColor = accent.last())
                    .clip(RoundedCornerShape(15.dp))
                    .background(Brush.linearGradient(accent)),
                contentAlignment = Alignment.Center
            ) { EmojiGlyph(raw = emoji, size = 24.dp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 22.sp, fontWeight = FontWeight.Black, color = TextDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SoftText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            trailing?.invoke()
        }
        if (stats.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                stats.forEach { StatTile(it, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun StatTile(stat: HeaderStat, modifier: Modifier = Modifier) {
    Row(
        modifier
            .card(radius = 16.dp, elevation = 2.dp)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(4.dp).height(28.dp).clip(RoundedCornerShape(2.dp)).background(stat.accent))
        Spacer(Modifier.width(8.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stat.value, fontSize = 17.sp, fontWeight = FontWeight.Black, color = TextDark, maxLines = 1)
                stat.emoji?.let { Spacer(Modifier.width(3.dp)); EmojiGlyph(raw = it, size = 13.dp) }
            }
            Text(stat.label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SoftText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** In-screen section label: icon, title, optional count or action on the right. */
@Composable
fun SectionTitle(
    title: String,
    emoji: String? = null,
    trailing: String? = null,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
    onTrailing: (() -> Unit)? = null
) {
    Row(modifier.fillMaxWidth().padding(start = horizontalPadding, end = horizontalPadding, top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        emoji?.let { EmojiGlyph(raw = it, size = 16.dp); Spacer(Modifier.width(7.dp)) }
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Black, color = TextDark)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f).height(1.dp).background(CardBorder))
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            val m = if (onTrailing != null) Modifier.bounceClick(RoundedCornerShape(10.dp)) { SoundFx.click(); onTrailing() } else Modifier
            Text(
                trailing, fontSize = 11.sp, fontWeight = FontWeight.Black,
                color = if (onTrailing != null) PinkDark else SoftText,
                modifier = m.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

/** Small rounded info pill (time, XP, coins…). */
@Composable
fun InfoPill(text: String, emoji: String? = null, tint: Color = SoftText, bg: Color = Color(0xFFF6F1FB)) {
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(bg).padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        emoji?.let { EmojiGlyph(raw = it, size = 10.dp); Spacer(Modifier.width(3.dp)) }
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Black, color = tint, maxLines = 1)
    }
}

@Composable
fun rememberTodayCounts(state: AppState): Triple<Int, Int, Int> = remember(state.habits, state.todayLog) {
    val enabled = state.habits.filter { it.enabled }
    val done = enabled.count { GameLogic.habitStatus(state, it) == com.vivichi.app.domain.HabitStatus.DONE }
    val missed = enabled.count { GameLogic.habitStatus(state, it) == com.vivichi.app.domain.HabitStatus.EXPIRED }
    Triple(done, enabled.size - done - missed, missed)
}
