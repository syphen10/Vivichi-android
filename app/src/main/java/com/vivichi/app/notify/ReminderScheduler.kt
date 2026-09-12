package com.vivichi.app.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.vivichi.app.R
import com.vivichi.app.VivichiApplication
import com.vivichi.app.data.AppState
import com.vivichi.app.data.Habit
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Schedules a per-habit reminder alarm at the habit's unlock time. AlarmManager alarms are
 * inherently one-shot, so each fired alarm is responsible for re-arming itself for the next
 * day — see [ReminderReceiver], which calls [scheduleOne] again after showing the notification.
 * Each habit gets a stable request code (from its id) so re-scheduling naturally replaces any
 * existing alarm for that habit rather than stacking duplicates.
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager: AlarmManager? = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun rescheduleAll(state: AppState) {
        cancelAll(state)
        if (!state.notif) return
        state.habits.filter { it.enabled }.forEach { scheduleOne(it) }
    }

    fun scheduleOne(habit: Habit) {
        val am = alarmManager ?: return
        val parts = habit.time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return

        var target = LocalDateTime.now().withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!target.isAfter(LocalDateTime.now())) target = target.plusDays(1)
        val triggerAt = target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_HABIT_ID, habit.id)
            putExtra(ReminderReceiver.EXTRA_HABIT_NAME, habit.name)
            putExtra(ReminderReceiver.EXTRA_HABIT_ICON, habit.icon)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            habit.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } catch (e: SecurityException) {
            // Exact-alarm permission not granted (Android 12+) — falls back to an inexact
            // alarm, which the OS may delay under Doze. See canScheduleExact()/exactAlarmSettingsIntent().
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun cancelOne(habitId: String) {
        val am = alarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            habitId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pending)
    }

    fun cancelAll(state: AppState) {
        state.habits.forEach { cancelOne(it.id) }
    }

    /** False on Android 12+ when the user hasn't granted the "Alarms & reminders" special permission. */
    fun canScheduleExact(): Boolean {
        val am = alarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) am.canScheduleExactAlarms() else true
    }

    /** Intent to the system settings screen where the user can grant exact-alarm scheduling. */
    fun exactAlarmSettingsIntent(): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
    }

    /** True once the app is exempted from battery optimization (or the check doesn't apply). */
    fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Intent asking the OS to exempt this app from battery optimization (so Doze doesn't kill reminders). */
    fun batteryOptimizationIntent(): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
    }

    /**
     * The single most common reason reminders never appear at all: either the OS-level
     * notification permission is off for the whole app, or — the sneakier one — the specific
     * "Habit reminders" channel itself is blocked. Channel importance is locked by Android the
     * moment it's first created; once it's set (even to something blocked, e.g. from an
     * accidental swipe-to-disable during earlier testing), no amount of app code can change it
     * back — only the user, manually, in system settings. Both are completely separate from the
     * in-app "Reminders" toggle, which only controls whether we schedule alarms in the first
     * place. ReminderReceiver silently no-ops if either of these is off.
     */
    fun areNotificationsEnabled(): Boolean {
        val nmCompat = NotificationManagerCompat.from(context)
        if (!nmCompat.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = nmCompat.getNotificationChannel(VivichiApplication.CHANNEL_ID)
            if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) return false
        }
        return true
    }

    /**
     * Intent to this app's system "App info" screen, where the user can turn notifications on.
     * Deliberately not ACTION_APP_NOTIFICATION_SETTINGS — several OEM skins (MIUI, ColorOS,
     * FuntouchOS, etc.) reroute that action into their own bundled screen that mixes in
     * lock-screen/always-on-display toggles, which reads as "wrong settings" to the user. App
     * info is a stock Android screen every manufacturer renders the same way, with a plain
     * "Notifications" row that goes to the real per-app notification controls.
     */
    fun notificationSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
    }

    /** Posts a notification right now, bypassing AlarmManager entirely — the fastest way to
     * check whether notifications actually work on this device, independent of any scheduling. */
    fun sendTestNotification() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val notification = NotificationCompat.Builder(context, VivichiApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFE8607E.toInt())
            .setContentTitle("🔔 Test notification")
            .setContentText("If you can see this, notifications are working on this phone!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(999999, notification)
        } catch (_: SecurityException) {
        }
    }
}
