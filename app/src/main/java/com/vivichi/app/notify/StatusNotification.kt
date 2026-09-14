package com.vivichi.app.notify

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.caverock.androidsvg.SVG
import com.vivichi.app.MainActivity
import com.vivichi.app.R
import com.vivichi.app.VivichiApplication
import com.vivichi.app.data.AppState
import com.vivichi.app.data.Habit
import com.vivichi.app.data.petEmoji
import com.vivichi.app.domain.GameLogic
import com.vivichi.app.domain.HabitStatus
import com.vivichi.app.ui.components.EMOJI_MAP
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

private const val STATUS_NOTIFICATION_ID = 424242

/** What the panel counts down to: either a habit opening, or the window to finish one closing. */
private data class NextUp(val habit: Habit, val targetEpochMs: Long, val caption: String)

object StatusNotification {

    fun show(context: Context, state: AppState) {
        if (!state.onboarded) return
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        val next = findNextUp(state)
        val backdrop = backdrop(state)
        val face = petFaceBitmap(context, state)
        val bar = healthBarBitmap(state.pet.health)

        val big = RemoteViews(context.packageName, R.layout.notification_status)
        populate(big, state, next, backdrop, face, bar, compact = false)
        val small = RemoteViews(context.packageName, R.layout.notification_status_small)
        populate(small, state, next, backdrop, face, bar, compact = true)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, 1, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, VivichiApplication.STATUS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFE8607E.toInt())
            .setCustomContentView(small)
            .setCustomBigContentView(big)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(STATUS_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    fun hide(context: Context) {
        NotificationManagerCompat.from(context).cancel(STATUS_NOTIFICATION_ID)
    }

    /**
     * Fills either layout. The compact one has no @id/timer_caption — setting a value on an id
     * that isn't in the inflated layout throws when the shade renders it, so guard on [compact].
     */
    private fun populate(
        views: RemoteViews,
        state: AppState,
        next: NextUp?,
        backdrop: Bitmap,
        face: Bitmap?,
        bar: Bitmap,
        compact: Boolean
    ) {
        views.setImageViewBitmap(R.id.bg, backdrop)
        face?.let { views.setImageViewBitmap(R.id.pet_face, it) }
        views.setImageViewBitmap(R.id.health_bar, bar)

        views.setTextViewText(R.id.pet_name, state.pet.name)
        views.setTextViewText(
            R.id.health_label,
            if (compact) "${state.pet.health}%" else "${healthWord(state.pet.health)} · ${state.pet.health}%"
        )

        if (next != null) {
            views.setTextViewText(R.id.next_label, "${next.habit.icon} ${next.habit.name}")
            if (!compact) views.setTextViewText(R.id.timer_caption, next.caption)
            // Chronometer counts down on its own, driven by the system — the panel stays live
            // without the app running, and without us re-posting every second.
            views.setChronometer(
                R.id.timer,
                SystemClock.elapsedRealtime() + (next.targetEpochMs - System.currentTimeMillis()),
                null,
                true
            )
            views.setChronometerCountDown(R.id.timer, true)
        } else {
            views.setTextViewText(R.id.next_label, "All done for today!")
            if (!compact) views.setTextViewText(R.id.timer_caption, "")
            views.setChronometer(R.id.timer, SystemClock.elapsedRealtime(), null, false)
            views.setTextViewText(R.id.timer, "")
        }
    }

    /** Rounded track + fill, sized for fitXY into the 5–7dp tall bar views. */
    private fun healthBarBitmap(health: Int): Bitmap {
        val w = 400
        val h = 28
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val r = h / 2f
        c.drawRoundRect(
            RectF(0f, 0f, w.toFloat(), h.toFloat()), r, r,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; alpha = 170 }
        )
        val pct = health.coerceIn(0, 100) / 100f
        if (pct > 0f) {
            // keep at least a full rounded cap visible so a near-empty bar still reads as a bar
            val fillW = (w * pct).coerceAtLeast(h.toFloat())
            c.drawRoundRect(
                RectF(0f, 0f, fillW, h.toFloat()), r, r,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = healthColor(health) }
            )
        }
        return bmp
    }

    private fun healthWord(health: Int): String = when {
        health >= 75 -> "Healthy"
        health >= 50 -> "Tired"
        health >= 25 -> "Weak"
        else -> "Frail"
    }

    private fun healthColor(health: Int): Int = when {
        health >= 75 -> 0xFF4DD9AC.toInt()
        health >= 50 -> 0xFFFF9A3C.toInt()
        health >= 25 -> 0xFFFF6B6B.toInt()
        else -> 0xFFCC0000.toInt()
    }

    /**
     * The habit the panel should count down to: if one is open right now, count down to its
     * window closing (2.5h after unlock); otherwise count down to the next one unlocking —
     * rolling over to tomorrow's earliest habit once the day is done.
     */
    private fun findNextUp(state: AppState): NextUp? {
        val live = state.habits.filter { it.enabled }
        if (live.isEmpty()) return null

        val openNow = live
            .filter { GameLogic.habitStatus(state, it) == HabitStatus.AVAILABLE }
            .minByOrNull { mins(it.time) }
        if (openNow != null) {
            val expiresAt = atTodayPlusMinutes(openNow.time, 150)
            if (expiresAt > System.currentTimeMillis()) {
                return NextUp(openNow, expiresAt, "left to do")
            }
        }

        val nowM = nowMins()
        val laterToday = live
            .filter { GameLogic.habitStatus(state, it) != HabitStatus.DONE }
            .filter { mins(it.time) > nowM }
            .minByOrNull { mins(it.time) }
        if (laterToday != null) {
            return NextUp(laterToday, atTodayPlusMinutes(laterToday.time, 0), "until due")
        }

        val tomorrow = live.minByOrNull { mins(it.time) } ?: return null
        return NextUp(tomorrow, atTodayPlusMinutes(tomorrow.time, 0) + 86_400_000L, "until due")
    }

    private fun mins(time: String): Int {
        val p = time.split(":")
        val h = p.getOrNull(0)?.toIntOrNull() ?: 0
        val m = p.getOrNull(1)?.toIntOrNull() ?: 0
        return h * 60 + m
    }

    private fun nowMins(): Int {
        val t = LocalDateTime.now()
        return t.hour * 60 + t.minute
    }

    private fun atTodayPlusMinutes(time: String, extra: Int): Long {
        val total = mins(time) + extra
        return LocalDate.now()
            .atStartOfDay()
            .plusMinutes(total.toLong())
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private fun seasonalColors(outfit: String): Pair<Int, Int> = when (outfit) {
        "spring" -> 0xFFDFF5E6.toInt() to 0xFFBFE8CD.toInt()
        "summer" -> 0xFFFFF3D1.toInt() to 0xFFFFE0A8.toInt()
        "autumn" -> 0xFFFFE6D2.toInt() to 0xFFFFCBA8.toInt()
        "winter" -> 0xFFE2F0FB.toInt() to 0xFFC5E2F5.toInt()
        else -> 0xFFFFE6F0.toInt() to 0xFFEADEFF.toInt()
    }

    /**
     * Seasonal blurred backdrop. The blur is done by drawing at 1/12 scale and upscaling with
     * bilinear filtering — cheap, and avoids RenderScript (removed) or RenderEffect (API 31+
     * only, and View-bound rather than Bitmap-bound). Nothing positional is drawn here: the
     * bitmap is scaled to the shade's size, so fixed-coordinate content would collide with text.
     */
    private fun backdrop(state: AppState): Bitmap {
        val w = 720
        val h = 220
        val sw = w / 12
        val sh = h / 12

        val small = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
        val c = Canvas(small)
        val (top, bottom) = seasonalColors(state.pet.outfit)

        c.drawPaint(Paint().apply {
            shader = LinearGradient(0f, 0f, sw.toFloat(), sh.toFloat(), top, bottom, Shader.TileMode.CLAMP)
        })
        // soft blobs so the upscale reads as an out-of-focus backdrop rather than a flat ramp
        val blob = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; alpha = 90 }
        c.drawCircle(sw * 0.22f, sh * 0.30f, sh * 0.42f, blob)
        blob.alpha = 60
        c.drawCircle(sw * 0.78f, sh * 0.72f, sh * 0.50f, blob)

        val out = Bitmap.createScaledBitmap(small, w, h, true)
        small.recycle()
        return out
    }

    private fun petFaceBitmap(context: Context, state: AppState): Bitmap? {
        val resId = EMOJI_MAP[petEmoji(state.pet.species)]?.resId ?: return null
        return try {
            val size = 156
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            SVG.getFromResource(context, resId)
                .renderToCanvas(canvas, RectF(0f, 0f, size.toFloat(), size.toFloat()))
            bmp
        } catch (_: Exception) {
            null
        }
    }
}
