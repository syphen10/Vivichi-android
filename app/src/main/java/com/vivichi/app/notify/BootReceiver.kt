package com.vivichi.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vivichi.app.VivichiApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Re-arms daily habit alarms after a reboot (AlarmManager alarms don't survive reboot). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? VivichiApplication ?: return
        val scheduler = ReminderScheduler(context)
        CoroutineScope(Dispatchers.Default).launch {
            val state = app.repository.current()
            scheduler.rescheduleAll(state)
        }
    }
}
