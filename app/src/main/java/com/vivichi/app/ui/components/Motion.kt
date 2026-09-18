package com.vivichi.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Motion toolkit shared by every screen, so the whole app moves with one consistent feel:
 * springy presses, staggered entrances, celebratory particles and live progress bars.
 */

val ConfettiColors = listOf(
    Color(0xFFFF85A2), Color(0xFFFFD166), Color(0xFF4DD9AC),
    Color(0xFF9B85E8), Color(0xFF6FB1FF), Color(0xFFFF9A3C)
)
val CoinColors = listOf(Color(0xFFFFD166), Color(0xFFFFC23D), Color(0xFFFFE8A3), Color(0xFFF5A623))

/**
 * Tap target that squishes while pressed and springs back. Put it *first* in the chain (after
 * any outer padding): it clips to [shape] itself so the ripple and background stay rounded.
 */
fun Modifier.bounceClick(
    shape: Shape = RoundedCornerShape(18.dp),
    enabled: Boolean = true,
    pressedScale: Float = 0.95f,
    onClick: () -> Unit
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && enabled) pressedScale else 1f,
        spring(dampingRatio = 0.45f, stiffness = 700f),
        label = "press"
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clip(shape)
        .clickable(interactionSource = interaction, indication = LocalIndication.current, enabled = enabled, onClick = onClick)
}

/** Same squish, for elements that aren't clickable themselves but share an interaction source. */
fun Modifier.pressScale(pressed: Boolean, pressedScale: Float = 0.95f): Modifier = composed {
    val scale by animateFloatAsState(if (pressed) pressedScale else 1f, spring(dampingRatio = 0.45f, stiffness = 700f), label = "press")
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/**
 * Fades and floats the element up into place, delayed by [index] for a staggered cascade.
 * Plays once per item — lazy lists don't replay it when an item scrolls back into view.
 */
fun Modifier.enterFromBelow(index: Int = 0, distance: Dp = 28.dp): Modifier = composed {
    var played by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (played) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!played) {
            delay(index.coerceIn(0, 12) * 45L)
            progress.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 260f))
            played = true
        }
    }
    graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * distance.toPx()
        val s = 0.96f + 0.04f * p
        scaleX = s; scaleY = s
    }
}

/** Gentle endless bob, for mascots and icons that should feel alive. */
fun Modifier.floating(amplitude: Dp = 6.dp, periodMs: Int = 1800): Modifier = composed {
    val t = rememberInfiniteTransition(label = "float")
    val y by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(periodMs, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "y")
    graphicsLayer { translationY = y * amplitude.toPx() }
}

/** Endless soft pulse (scale), e.g. for urgent banners or a flame. */
fun Modifier.pulsing(max: Float = 1.06f, periodMs: Int = 900): Modifier = composed {
    val t = rememberInfiniteTransition(label = "pulse")
    val s by t.animateFloat(1f, max, infiniteRepeatable(tween(periodMs, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "s")
    graphicsLayer { scaleX = s; scaleY = s }
}

/** Endless little wiggle (rotation). */
fun Modifier.wiggling(degrees: Float = 8f, periodMs: Int = 700): Modifier = composed {
    val t = rememberInfiniteTransition(label = "wiggle")
    val r by t.animateFloat(-degrees, degrees, infiniteRepeatable(tween(periodMs, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "r")
    graphicsLayer { rotationZ = r }
}

/**
 * Rounded progress bar whose fill eases to its new value and carries a slow shine sweep,
 * so progress always reads as something happening rather than a static number.
 */
@Composable
fun AnimatedBar(
    progress: Float,
    brush: Brush,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
    track: Color = Color(0xFFEDE4FF),
    shine: Boolean = true
) {
    var target by remember { mutableStateOf(0f) }
    LaunchedEffect(progress) { target = progress.coerceIn(0f, 1f) }
    val fill by animateFloatAsState(target, tween(900, easing = FastOutSlowInEasing), label = "fill")
    // The shine sweeps twice whenever the value changes, then rests. A forever-looping sweep kept
    // every bar on screen redrawing 60 times a second, which added up across the HUD and screens.
    val sweepAnim = remember { Animatable(-0.4f) }
    LaunchedEffect(progress) {
        if (!shine) return@LaunchedEffect
        delay(400)
        repeat(2) {
            sweepAnim.snapTo(-0.4f)
            sweepAnim.animateTo(1.4f, tween(1400, easing = LinearEasing))
        }
    }
    // (sweepAnim.value is read inside the draw lambda below, so the sweep never recomposes.)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height))
            .background(track)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fill)
                .clip(RoundedCornerShape(height))
                .graphicsLayer()
                .background(brush)
                .drawWithContent {
                    drawContent()
                    if (shine && fill > 0.02f) {
                        val x = size.width * sweepAnim.value
                        drawRect(
                            Brush.linearGradient(
                                listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0f)),
                                start = Offset(x - size.height * 3, 0f),
                                end = Offset(x + size.height * 3, size.height)
                            )
                        )
                    }
                }
        )
    }
}

/** Number that rolls to its new value instead of jumping. */
@Composable
fun CountUpText(value: Int, style: TextStyle, modifier: Modifier = Modifier, prefix: String = "", suffix: String = "") {
    var target by remember { mutableStateOf(0) }
    LaunchedEffect(value) { target = value }
    val shown by animateIntAsState(target, tween(800, easing = FastOutSlowInEasing), label = "count")
    Text("$prefix$shown$suffix", style = style, modifier = modifier)
}

private class Particle(
    val angle: Float,
    val speed: Float,
    val spin: Float,
    val size: Float,
    val color: Color,
    val shape: Int,
    val drift: Float
)

/**
 * One-shot particle explosion, replayed whenever [trigger] changes to a non-null value.
 * Pure Canvas — cheap enough to overlay full screen, and it never blocks touches.
 */
@Composable
fun ConfettiBurst(
    trigger: Any?,
    modifier: Modifier = Modifier,
    colors: List<Color> = ConfettiColors,
    count: Int = 70,
    origin: Offset = Offset(0.5f, 0.42f),
    durationMs: Int = 1700,
    coins: Boolean = false
) {
    if (trigger == null) return
    val particles = remember(trigger) {
        List(count) {
            // Mostly upward fan so pieces rise, arc and rain back down.
            val a = (-PI / 2 + (Random.nextFloat() - 0.5f) * PI * 1.25).toFloat()
            Particle(
                angle = a,
                speed = 0.55f + Random.nextFloat() * 0.9f,
                spin = (Random.nextFloat() - 0.5f) * 900f,
                size = if (coins) 9f + Random.nextFloat() * 7f else 5f + Random.nextFloat() * 7f,
                color = colors[Random.nextInt(colors.size)],
                shape = if (coins) 2 else Random.nextInt(3),
                drift = (Random.nextFloat() - 0.5f) * 0.3f
            )
        }
    }
    val time = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) { time.animateTo(1f, tween(durationMs, easing = LinearEasing)) }
    if (time.value >= 1f) return

    Canvas(modifier.fillMaxSize().graphicsLayer()) {
        val t = time.value
        val ox = size.width * origin.x
        val oy = size.height * origin.y
        val scale = size.minDimension
        val fade = if (t < 0.7f) 1f else 1f - (t - 0.7f) / 0.3f
        particles.forEach { p ->
            val v = p.speed * scale * 1.1f
            val x = ox + cos(p.angle) * v * t + p.drift * scale * t
            val y = oy + sin(p.angle) * v * t + 1.5f * scale * t * t
            val px = p.size.dp.toPx()
            rotate(p.spin * t, Offset(x, y)) {
                drawParticle(p.shape, Offset(x, y), px, p.color.copy(alpha = fade.coerceIn(0f, 1f)))
            }
        }
    }
}

private fun DrawScope.drawParticle(shape: Int, c: Offset, s: Float, color: Color) {
    when (shape) {
        0 -> drawRect(color, topLeft = Offset(c.x - s / 2, c.y - s / 4), size = Size(s, s / 2))
        1 -> drawCircle(color, radius = s / 2.4f, center = c)
        else -> {
            // Coin: flat disc with a darker rim, squashed by rotation for a spinning look.
            drawCircle(Color(0xFFE09B12).copy(alpha = color.alpha), radius = s / 2, center = c)
            drawCircle(color, radius = s / 2.7f, center = c)
        }
    }
}

/**
 * Ambient drifting sparkles for hero cards. Positions are derived from one looping clock, so
 * a dozen sparkles cost a single animation.
 */
@Composable
fun FloatingSparkles(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFFFD166),
    count: Int = 12,
    seed: Int = 7
) {
    val seeds = remember(seed, count) {
        val r = Random(seed)
        List(count) { floatArrayOf(r.nextFloat(), r.nextFloat(), 0.4f + r.nextFloat() * 0.8f, 3f + r.nextFloat() * 5f, r.nextFloat()) }
    }
    val clock = rememberInfiniteTransition(label = "sparkles")
    val t by clock.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "t")
    // Own layer: the per-frame redraw stays inside this canvas instead of re-recording the
    // whole card or screen around it (the main source of scroll jank).
    Canvas(modifier.fillMaxSize().graphicsLayer()) {
        seeds.forEach { (x0, y0, speed, sz, phase) ->
            val y = ((y0 - t * speed) % 1f + 1f) % 1f
            val x = x0 + sin((t * 2 * PI * speed + phase * 6).toFloat()) * 0.03f
            val twinkle = (sin((t * 2 * PI * 3 + phase * 10).toFloat()) + 1f) / 2f
            val alpha = (0.25f + 0.6f * twinkle) * edgeFade(y)
            drawSparkle(Offset(x * size.width, y * size.height), sz.dp.toPx() * (0.7f + 0.3f * twinkle), color.copy(alpha = alpha))
        }
    }
}

/**
 * Same sparkle look as [FloatingSparkles] but frozen: for surfaces that are always on screen
 * (the HUD), where a constant animation would keep the whole top of the app repainting.
 */
@Composable
fun StaticSparkles(modifier: Modifier = Modifier, color: Color = Color.White, count: Int = 10, seed: Int = 7) {
    val points = remember(seed, count) {
        val r = Random(seed)
        List(count) { floatArrayOf(r.nextFloat(), 0.1f + r.nextFloat() * 0.8f, 3f + r.nextFloat() * 5f, 0.35f + r.nextFloat() * 0.5f) }
    }
    Canvas(modifier.fillMaxSize().graphicsLayer()) {
        points.forEach { (x, y, sz, a) ->
            drawSparkle(Offset(x * size.width, y * size.height), sz.dp.toPx(), color.copy(alpha = color.alpha * a))
        }
    }
}

private fun edgeFade(y: Float): Float = when {
    y < 0.12f -> y / 0.12f
    y > 0.88f -> (1f - y) / 0.12f
    else -> 1f
}

/** Four-point twinkle star. */
fun DrawScope.drawSparkle(center: Offset, radius: Float, color: Color) {
    // One shared unit-size star, moved and scaled into place. Building a fresh Path per sparkle
    // per frame (dozens of sparkles, 60fps, several cards) churned memory and pegged the CPU.
    withTransform({
        translate(center.x, center.y)
        scale(radius, radius, Offset.Zero)
    }) {
        drawPath(UnitSparkle, color)
    }
}

private val UnitSparkle: Path by lazy {
    Path().apply {
        for (i in 0 until 8) {
            val r = if (i % 2 == 0) 1f else 0.28f
            val a = (i * PI / 4 - PI / 2).toFloat()
            val px = cos(a) * r
            val py = sin(a) * r
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }
}

/**
 * A handful of emoji that pop out of a point and float up while fading — used for playground
 * reactions (hearts, food, notes) so every tap visibly lands.
 */
@Composable
fun EmojiBurst(trigger: Any?, emoji: String, modifier: Modifier = Modifier, count: Int = 7) {
    if (trigger == null) return
    val specs = remember(trigger) {
        List(count) { floatArrayOf((Random.nextFloat() - 0.5f) * 220f, 140f + Random.nextFloat() * 160f, (Random.nextFloat() - 0.5f) * 50f, 22f + Random.nextFloat() * 14f, Random.nextFloat() * 180f) }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        specs.forEach { (dx, rise, rot, sz, delayMs) ->
            val p = remember(trigger) { Animatable(0f) }
            LaunchedEffect(trigger) {
                delay(delayMs.toLong())
                launch { p.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
            }
            val v = p.value
            if (v > 0f && v < 1f) {
                EmojiGlyph(
                    raw = emoji,
                    size = sz.dp,
                    modifier = Modifier
                        .offset { IntOffset((dx * v).dp.roundToPx(), (-rise * v).dp.roundToPx()) }
                        .graphicsLayer {
                            alpha = if (v < 0.6f) 1f else 1f - (v - 0.6f) / 0.4f
                            val s = if (v < 0.2f) v / 0.2f else 1f
                            scaleX = s; scaleY = s
                            rotationZ = rot * v
                        }
                )
            }
        }
    }
}
