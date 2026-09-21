package com.vivichi.app.ui.components

import android.app.ActivityManager
import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

/**
 * One clock for all the app's ambient motion (sparkles, bobbing pets, shimmer, wobbles).
 *
 * Before, every decoration ran its own infinite animation at full frame rate forever, so the
 * app never stopped drawing and scrolling had to fight the decorations for the phone's time.
 * Now they all read this shared clock, which:
 *  - pauses while the user is scrolling (eyes are on the list, and the scroll gets every frame),
 *    then carries on from where it stopped, so nothing jumps;
 *  - ticks at 30fps on modest phones, which still looks smooth for slow ambient loops but halves
 *    the constant work. Capable phones keep the full rate.
 *
 * Read [State.value] only inside draw / graphicsLayer lambdas, so a tick repaints just that
 * layer and never recomposes anything.
 */
val LocalAmbientTime = staticCompositionLocalOf<State<Long>> { mutableLongStateOf(0L) }

/** Pause after the last scroll movement before ambient motion resumes. */
private const val RESUME_AFTER_SCROLL_MS = 350L

@Composable
fun ProvideAmbientMotion(modifier: Modifier = Modifier, content: @Composable (Modifier) -> Unit) {
    val context = LocalContext.current
    val modest = remember { isModestDevice(context) }
    val time = remember { mutableLongStateOf(0L) }
    var scrolling by remember { mutableStateOf(false) }
    val lastScroll = remember { longArrayOf(0L) }

    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                lastScroll[0] = android.os.SystemClock.uptimeMillis()
                if (!scrolling) scrolling = true
                return Offset.Zero
            }
        }
    }

    // Resume shortly after the scroll (including a fling's tail) has gone quiet.
    LaunchedEffect(scrolling) {
        if (scrolling) {
            while (android.os.SystemClock.uptimeMillis() - lastScroll[0] < RESUME_AFTER_SCROLL_MS) delay(90)
            scrolling = false
        }
    }

    LaunchedEffect(scrolling) {
        if (scrolling) return@LaunchedEffect
        var last = -1L
        var elapsed = time.longValue
        var frame = 0
        while (true) {
            withFrameMillis { now ->
                // Cap the step so a stall (or the pause) never makes things leap ahead.
                if (last >= 0) elapsed += (now - last).coerceIn(0L, 50L)
                last = now
                frame++
                if (!modest || frame % 2 == 0) time.longValue = elapsed
            }
        }
    }

    CompositionLocalProvider(LocalAmbientTime provides time) {
        content(modifier.nestedScroll(connection))
    }
}

/** Under ~4 GB of RAM, or flagged low-RAM by the OS: budget hardware, go easy on constant work. */
private fun isModestDevice(context: Context): Boolean {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
    if (am.isLowRamDevice) return true
    val info = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
    return info.totalMem < 4L * 1024 * 1024 * 1024
}

/** 0→1 over [periodMs], repeating. */
fun loopPhase(timeMs: Long, periodMs: Int): Float = (timeMs % periodMs).toFloat() / periodMs

/**
 * Smooth there-and-back 0→1→0 over 2×[periodMs], eased like the old
 * `infiniteRepeatable(tween(periodMs, FastOutSlowInEasing), RepeatMode.Reverse)`.
 */
fun pingPong(timeMs: Long, periodMs: Int): Float {
    val cycle = (timeMs % (2L * periodMs)).toFloat() / periodMs
    val linear = if (cycle <= 1f) cycle else 2f - cycle
    return FastOutSlowInEasing.transform(linear)
}

/** Linear there-and-back, for motions that used LinearEasing with RepeatMode.Reverse. */
fun pingPongLinear(timeMs: Long, periodMs: Int): Float {
    val cycle = (timeMs % (2L * periodMs)).toFloat() / periodMs
    return if (cycle <= 1f) cycle else 2f - cycle
}

/** Linear interpolation helper. */
fun lerpF(a: Float, b: Float, t: Float) = a + (b - a) * t
