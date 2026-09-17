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
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
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
import com.vivichi.app.data.xpForLevel
import com.vivichi.app.domain.GameLogic
import com.vivichi.app.domain.HabitStatus
import com.vivichi.app.ui.components.EMOJI_MAP
import com.vivichi.app.ui.components.hudGradient
import com.vivichi.app.util.formatHabitTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val STATUS_NOTIFICATION_ID = 424242

/** What the panel counts down to: either a habit opening, or the window to finish one closing. */
private data class NextUp(val habit: Habit, val targetEpochMs: Long, val caption: String)

/**
 * The ongoing "pet status" notification: a miniature of the in-app game HUD (themed gradient,
 * pet portrait with level badge, health and XP bars) plus the next habit with a live countdown.
 */
object StatusNotification {

    fun show(context: Context, state: AppState) {
        if (!state.onboarded) return
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        val next = findNextUp(state)
        val colors = hudGradient(state.pet.outfit).map { it.toArgb() }
        val portrait = portraitBitmap(context, state)
        val healthBar = barBitmap(state.pet.health / 100f, intArrayOf(healthColor(state.pet.health), healthColor(state.pet.health)))
        val need = xpForLevel(state.pet.level)
        val xpBar = barBitmap((state.pet.xp.toFloat() / need).coerceIn(0f, 1f), intArrayOf(0xFFFFE08A.toInt(), 0xFFFFB020.toInt()))

        val big = RemoteViews(context.packageName, R.layout.notification_status)
        populate(big, state, next, backdrop(colors, 720, 256, state.pet.species.hashCode()), portrait, healthBar, xpBar, need, compact = false)
        val small = RemoteViews(context.packageName, R.layout.notification_status_small)
        populate(small, state, next, backdrop(colors, 720, 96, state.pet.species.hashCode()), portrait, healthBar, xpBar, need, compact = true)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, 1, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, VivichiApplication.STATUS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(colors.first())
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
     * Fills either layout. The compact one lacks the XP row, mood chip and timer caption — setting a
     * value on an id that isn't in the inflated layout throws when the shade renders, so guard on [compact].
     */
    private fun populate(
        views: RemoteViews,
        state: AppState,
        next: NextUp?,
        backdrop: Bitmap,
        portrait: Bitmap?,
        healthBar: Bitmap,
        xpBar: Bitmap,
        xpNeed: Int,
        compact: Boolean
    ) {
        views.setImageViewBitmap(R.id.bg, backdrop)
        portrait?.let { views.setImageViewBitmap(R.id.pet_face, it) }
        views.setImageViewBitmap(R.id.health_bar, healthBar)
        views.setTextViewText(R.id.pet_name, state.pet.name.ifBlank { "Buddy" })
        views.setTextViewText(R.id.health_label, if (compact) "${state.pet.health}%" else "${state.pet.health}/100")
        if (!compact) {
            views.setImageViewBitmap(R.id.xp_bar, xpBar)
            views.setTextViewText(R.id.xp_label, "${state.pet.xp}/$xpNeed")
            views.setTextViewText(R.id.mood_chip, "${healthWord(state.pet.health)} · 🔥 ${state.streak}")
        }

        if (next != null) {
            val time = formatHabitTime(next.habit.time, state.use24h)
            views.setTextViewText(R.id.next_label, "${next.habit.icon}  ${next.habit.name}")
            if (!compact) {
                views.setTextViewText(
                    R.id.timer_caption,
                    if (next.caption == "left to do") "OPEN NOW · CLOSES IN" else "NEXT UP · $time"
                )
            }
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
            views.setTextViewText(R.id.next_label, "🎉  All done for today!")
            if (!compact) views.setTextViewText(R.id.timer_caption, "GREAT JOB")
            views.setChronometer(R.id.timer, SystemClock.elapsedRealtime(), null, false)
            views.setTextViewText(R.id.timer, "✓")
        }
    }

    // ---------- Bitmaps ----------

    /** Themed gradient with soft light blobs and a sprinkle of sparkles — the HUD look, in the shade. */
    private fun backdrop(colors: List<Int>, w: Int, h: Int, seed: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        // Rounded card corners (transparent outside), so the panel reads as a card, not a slab.
        val radius = if (h > 150) 40f else 30f
        val clip = Path().apply { addRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), radius, radius, Path.Direction.CW) }
        c.clipPath(clip)
        c.drawPaint(Paint().apply {
            shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), colors.first(), colors.last(), Shader.TileMode.CLAMP)
        })
        val blob = Paint(Paint.ANTI_ALIAS_FLAG)
        blob.shader = RadialGradient(w * 0.9f, 0f, h * 1.1f, 0x2EFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        c.drawCircle(w * 0.9f, 0f, h * 1.1f, blob)
        blob.shader = RadialGradient(w * 0.05f, h.toFloat(), h * 0.8f, 0x1FFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        c.drawCircle(w * 0.05f, h.toFloat(), h * 0.8f, blob)

        val rnd = Random(seed)
        val star = Paint(Paint.ANTI_ALIAS_FLAG)
        repeat(if (h > 150) 14 else 7) {
            star.color = Color.argb(60 + rnd.nextInt(90), 255, 255, 255)
            sparkle(c, w * rnd.nextFloat(), h * rnd.nextFloat(), 4f + rnd.nextFloat() * 7f, star)
        }
        return bmp
    }

    private fun sparkle(c: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
        val path = Path()
        for (i in 0 until 8) {
            val rr = if (i % 2 == 0) r else r * 0.3f
            val a = i * Math.PI / 4 - Math.PI / 2
            val x = cx + (cos(a) * rr).toFloat()
            val y = cy + (sin(a) * rr).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        c.drawPath(path, paint)
    }

    /** White circular portrait with a soft ring and a gold level badge. */
    private fun portraitBitmap(context: Context, state: AppState): Bitmap? {
        val size = 174
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val r = size * 0.44f
        val cx = size * 0.46f
        val cy = size * 0.46f
        p.color = 0x55FFFFFF
        c.drawCircle(cx, cy, r, p)
        p.color = Color.WHITE
        c.drawCircle(cx, cy, r - 8f, p)

        EMOJI_MAP[petEmoji(state.pet.species)]?.resId?.let { resId ->
            try {
                val e = r * 1.15f
                SVG.getFromResource(context, resId).renderToCanvas(c, RectF(cx - e / 2, cy - e / 2 + 2f, cx + e / 2, cy + e / 2 + 2f))
            } catch (_: Exception) {
            }
        }

        // level badge
        val br = size * 0.17f
        val bx = size - br - 2f
        val by = size - br - 2f
        p.color = Color.WHITE
        c.drawCircle(bx, by, br + 5f, p)
        p.shader = LinearGradient(bx - br, by - br, bx + br, by + br, 0xFFFFD66B.toInt(), 0xFFFF9F2E.toInt(), Shader.TileMode.CLAMP)
        c.drawCircle(bx, by, br, p)
        p.shader = null
        p.color = Color.WHITE
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        p.textAlign = Paint.Align.CENTER
        val label = "${state.pet.level}"
        p.textSize = if (label.length > 2) br * 0.9f else br * 1.15f
        c.drawText(label, bx, by - (p.descent() + p.ascent()) / 2, p)
        return bmp
    }

    /** Rounded translucent track with a gradient fill, sized for fitXY into the 6–7dp bar views. */
    private fun barBitmap(progress: Float, fill: IntArray): Bitmap {
        val w = 400
        val h = 28
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val rad = h / 2f
        c.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), rad, rad, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x40FFFFFF })
        val pct = progress.coerceIn(0f, 1f)
        if (pct > 0f) {
            // keep at least a full rounded cap visible so a near-empty bar still reads as a bar
            val fillW = (w * pct).coerceAtLeast(h.toFloat())
            c.drawRoundRect(
                RectF(0f, 0f, fillW, h.toFloat()), rad, rad,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(0f, 0f, fillW, 0f, fill[0], fill[1], Shader.TileMode.CLAMP)
                }
            )
            // top shine
            c.drawRoundRect(
                RectF(4f, 3f, fillW - 4f, h * 0.45f), rad, rad,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33FFFFFF }
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
        health >= 75 -> 0xFF5BE3B4.toInt()
        health >= 50 -> 0xFFFFB35C.toInt()
        health >= 25 -> 0xFFFF7A7A.toInt()
        else -> 0xFFFF4D4D.toInt()
    }

    // ---------- Next habit ----------

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
}
