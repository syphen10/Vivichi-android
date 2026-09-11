package com.vivichi.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.vivichi.app.R

/**
 * Soft, non-electronic UI sound effects (synthesized short WAVs, not telephony tones — those
 * read as sci-fi/computer beeps and glitch on rapid taps). Distinct sounds per interaction type,
 * including a distinct synthesized sound per Playground action.
 */
object SoundFx {
    private var soundPool: SoundPool? = null
    private var clickId = 0
    private var navId = 0
    private var completeId = 0
    private val playgroundIds = mutableMapOf<String, Int>()

    fun init(context: Context) {
        if (soundPool != null) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val pool = SoundPool.Builder().setMaxStreams(6).setAudioAttributes(attrs).build()
        soundPool = pool
        val app = context.applicationContext
        clickId = pool.load(app, R.raw.snd_click, 1)
        navId = pool.load(app, R.raw.snd_nav, 1)
        completeId = pool.load(app, R.raw.snd_complete, 1)
        playgroundIds["feed"] = pool.load(app, R.raw.pg_feed, 1)
        playgroundIds["pet"] = pool.load(app, R.raw.pg_pet, 1)
        playgroundIds["play"] = pool.load(app, R.raw.pg_play, 1)
        playgroundIds["hug"] = pool.load(app, R.raw.pg_hug, 1)
        playgroundIds["sing"] = pool.load(app, R.raw.pg_sing, 1)
        playgroundIds["tickle"] = pool.load(app, R.raw.pg_tickle, 1)
    }

    /** General buttons, toggles, dialog actions. */
    fun click() = play(clickId)

    /** Switching between main tabs/screens. */
    fun nav() = play(navId)

    /** Finishing a habit — the rewarding one. */
    fun complete() = play(completeId)

    /** Playground action (feed/pet/play/hug/sing/tickle) — each has its own distinct sound. */
    fun playgroundAction(key: String) {
        playgroundIds[key]?.let { play(it) }
    }

    private fun play(id: Int) {
        soundPool?.play(id, 0.85f, 0.85f, 0, 0, 1f)
    }
}
