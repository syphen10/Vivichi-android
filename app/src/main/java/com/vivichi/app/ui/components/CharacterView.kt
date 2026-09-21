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
    modifier: Modifier = Modifier,
    /** Show the closed-eyes "satisfied" face (Playground reactions). */
    happy: Boolean = false
) {
    // Driven by the shared ambient clock (see Ambient.kt); only the motion this mood uses is computed.
    val clock = LocalAmbientTime.current

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
                    val t = clock.value
                    translationY = if (useFloat) lerpF(0f, -10f, pingPong(t, 1200)) else 0f
                    rotationZ = if (useRotation) lerpF(-4f, 4f, pingPongLinear(t, 220)) else 0f
                    val s = if (mood == Mood.NEUTRAL) lerpF(1f, 1.05f, pingPong(t, 1400)) else 1f
                    scaleX = s
                    scaleY = s
                    this.alpha = alphaVal
                },
            contentAlignment = Alignment.Center
        ) {
            // Both faces stay composed (and so decoded) the whole time; only their opacity swaps.
            // Loading the happy SVG on demand made it flash in late, or not at all on slow phones.
            val happyRes = happyPetRes(species)
            val happyAlpha by animateFloatAsState(if (happy && happyRes != null) 1f else 0f, tween(140), label = "happy")
            Box(contentAlignment = Alignment.Center) {
                EmojiGlyph(raw = petEmoji(species), size = size * 0.6f, modifier = Modifier.graphicsLayer { this.alpha = 1f - happyAlpha })
                if (happyRes != null) {
                    SvgIcon(happyRes, size * 0.6f, Modifier.graphicsLayer { this.alpha = happyAlpha })
                }
            }
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
