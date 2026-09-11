package com.vivichi.app.notify

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.vivichi.app.MainActivity
import com.vivichi.app.VivichiApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID)
        val name = intent.getStringExtra(EXTRA_HABIT_NAME) ?: "A habit"
        val icon = intent.getStringExtra(EXTRA_HABIT_ICON) ?: "✨"

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, VivichiApplication.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$icon $name is ready!")
            .setContentText("Tap to complete it and keep your streak alive.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompatSafe.notify(context, name.hashCode(), notification)
        }

        // AlarmManager alarms are one-shot — re-arm this habit's reminder for its next
        // occurrence (tomorrow), using live app state in case the habit was edited or
        // disabled since this alarm was originally set.
        if (habitId != null) {
            val app = context.applicationContext as? VivichiApplication
            if (app != null) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        val state = app.repository.current()
                        val habit = state.habits.find { it.id == habitId }
                        if (state.notif && habit != null && habit.enabled) {
                            ReminderScheduler(context).scheduleOne(habit)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_HABIT_NAME = "habit_name"
        const val EXTRA_HABIT_ICON = "habit_icon"
    }
}

private object NotificationManagerCompatSafe {
    fun notify(context: Context, id: Int, notification: android.app.Notification) {
        val nm = context.getSystemService(NotificationManager::class.java)
        try {
            nm?.notify(id, notification)
        } catch (_: SecurityException) {
        }
    }
}
