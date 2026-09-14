package com.vivichi.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vivichi.app.data.ALL_WEARABLES
import com.vivichi.app.data.petEmoji
import com.vivichi.app.domain.Mood
import com.vivichi.app.ui.theme.*

/**
 * Native Compose stand-in for the original's embedded Lottie/video pet animations:
 * an emoji rendering of the current species, breathing/bouncing/shaking based on [mood],
 * desaturating as [health] drops, with a small outfit badge overlay.
 */
@Composable
fun CharacterView(
    species: String,
    mood: Mood,
    outfit: String,
    health: Int,
    size: Dp = 160.dp,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "char")

    val floatY by infinite.animateFloat(
        initialValue = 0f,
        targetValue = if (mood == Mood.HAPPY) -10f else 0f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "floatY"
    )
    val rotation by infinite.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            tween(if (mood == Mood.SAD || mood == Mood.FRAIL) 220 else 900, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "rotate"
    )
    val pulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = if (mood == Mood.NEUTRAL) 1.05f else 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    val alphaVal = when {
        health <= 0 -> 0.4f
        health < 25 -> 0.6f
        health < 50 -> 0.8f
        else -> 1f
    }
    val useRotation = mood == Mood.SAD || mood == Mood.FRAIL
    val useFloat = mood == Mood.HAPPY

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    translationY = if (useFloat) floatY else 0f
                    rotationZ = if (useRotation) rotation else 0f
                    scaleX = if (mood == Mood.NEUTRAL) pulse else 1f
                    scaleY = if (mood == Mood.NEUTRAL) pulse else 1f
                    this.alpha = alphaVal
                },
            contentAlignment = Alignment.Center
        ) {
            EmojiGlyph(raw = petEmoji(species), size = size * 0.6f)
        }

        val outfitInfo = ALL_WEARABLES.find { it.id == outfit }
        if (outfitInfo != null && outfit != "default") {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(size * 0.28f)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                EmojiGlyph(raw = outfitInfo.emoji, size = size * 0.16f)
            }
        }
    }
}
