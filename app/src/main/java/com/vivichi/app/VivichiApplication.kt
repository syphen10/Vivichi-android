package com.vivichi.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import coil.Coil
import coil.ImageLoader
import coil.decode.SvgDecoder
import com.vivichi.app.data.PetRepository

class VivichiApplication : Application() {

    lateinit var repository: PetRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = PetRepository(this)
        createNotificationChannel()
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .components { add(SvgDecoder.Factory()) }
                .build()
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Habit reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders when your habits unlock and streak warnings"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        // Bumped from "vivichi_reminders": Android permanently locks a channel's importance the
        // moment it's first created, even across app updates — if that original channel ever got
        // set to blocked (e.g. an accidental swipe-to-disable during earlier testing), no amount
        // of app code could ever unblock it again. A fresh channel ID forces a clean slate.
        const val CHANNEL_ID = "vivichi_reminders_v2"
    }
}
