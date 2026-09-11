package com.vivichi.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.caverock.androidsvg.SVG
import com.vivichi.app.ui.components.EMOJI_MAP
import java.io.File
import java.io.FileOutputStream

data class StreakFrame(val id: String, val name: String, val emoji: String, val bgTop: String, val bgBottom: String, val border: String, val txt: String, val nameIsDark: Boolean)

val STREAK_FRAMES = listOf(
    StreakFrame("vivichi", "Vivichi", "🌸", "#FFE2EE", "#F0DDFF", "#FF85A2", "#E8607E", nameIsDark = true),
    StreakFrame("flame", "Flame", "🔥", "#2D1B00", "#4A2800", "#FF6B35", "#FFD166", nameIsDark = false),
    StreakFrame("cosmic", "Cosmic", "🌌", "#0A0A2E", "#1A0A3E", "#C4B0FF", "#C4B0FF", nameIsDark = false),
    StreakFrame("nature", "Nature", "🌿", "#1A3A1A", "#0A2A0A", "#A8E6CF", "#A8E6CF", nameIsDark = false)
)

private fun textPaint(colorHex: String, size: Float, bold: Boolean, alpha: Float = 1f): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.parseColor(colorHex)
    textSize = size
    typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
    this.alpha = (alpha * 255).toInt()
}

fun buildStreakBitmap(
    context: Context,
    frame: StreakFrame,
    petName: String,
    titleEmoji: String?,
    titleName: String?,
    streak: Int,
    level: Int,
    totalXP: Int,
    totalDone: Int,
    petEmoji: String
): Bitmap {
    val w = 800
    val h = 500
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), Color.parseColor(frame.bgTop), Color.parseColor(frame.bgBottom), Shader.TileMode.CLAMP)
    }
    canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), 28f, 28f, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 12f
        color = Color.parseColor(frame.border)
        alpha = (0.55f * 255).toInt()
    }
    canvas.drawRoundRect(RectF(6f, 6f, w - 6f, h - 6f), 24f, 24f, borderPaint)

    canvas.drawText("Vivichi", 32f, 46f, textPaint(frame.txt, 22f, bold = true, alpha = 0.7f))

    val nameColor = if (frame.nameIsDark) "#3D2E4E" else frame.border
    canvas.drawText(petName, 36f, 100f, textPaint(nameColor, 32f, bold = true))

    if (titleEmoji != null && titleName != null) {
        canvas.drawText("$titleEmoji $titleName", 38f, 128f, textPaint(frame.txt, 17f, bold = false, alpha = 0.75f))
    }

    val streakPaint = textPaint(frame.border, 130f, bold = true)
    val streakStr = streak.toString()
    val sw = streakPaint.measureText(streakStr)
    canvas.drawText(streakStr, w / 2f - sw / 2f, h / 2f + 60f, streakPaint)

    val labelPaint = textPaint(frame.txt, 22f, bold = true, alpha = 0.8f).apply { textAlign = Paint.Align.CENTER }
    canvas.drawText("Day Streak", w / 2f, h / 2f + 100f, labelPaint)

    val chips = listOf("Level $level", "$totalXP XP", "$totalDone habits")
    chips.forEachIndexed { i, ch ->
        val bx = 32f + i * 180f
        val by = h - 80f
        val chipBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; alpha = (0.1f * 255).toInt() }
        canvas.drawRoundRect(RectF(bx, by, bx + 164f, by + 44f), 12f, 12f, chipBg)
        canvas.drawText(ch, bx + 14f, by + 28f, textPaint(frame.border, 14f, bold = true))
    }

    val emojiSize = 150f
    val emojiRect = RectF(w - 150f - emojiSize / 2f, h - 80f - emojiSize * 0.72f, w - 150f + emojiSize / 2f, h - 80f + emojiSize * 0.28f)
    val resId = EMOJI_MAP[petEmoji]?.resId
    if (resId != null) {
        try {
            val svg = SVG.getFromResource(context, resId)
            svg.renderToCanvas(canvas, emojiRect)
        } catch (_: Exception) {
        }
    } else {
        val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 160f; textAlign = Paint.Align.CENTER }
        canvas.drawText(petEmoji, w - 150f, h - 80f, emojiPaint)
    }

    return bmp
}

fun shareStreakBitmap(context: Context, bitmap: Bitmap, fileName: String) {
    val cacheDir = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File(cacheDir, fileName)
    FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share your streak"))
}
