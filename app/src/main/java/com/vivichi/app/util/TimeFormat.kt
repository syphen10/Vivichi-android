package com.vivichi.app.util

/** True when a habit at [hhmm] has already closed for today (2.5h completion window). */
fun habitWindowPassed(hhmm: String): Boolean {
    val p = hhmm.split(":")
    val mins = (p.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (p.getOrNull(1)?.toIntOrNull() ?: 0)
    val now = java.time.LocalTime.now()
    return now.hour * 60 + now.minute > mins + 150
}

/** Next full hour from now as "HH:mm" — a sensible default that's open today. */
fun nextFullHour(): String = "%02d:00".format((java.time.LocalTime.now().hour + 1) % 24)

/**
 * Habit times are always *stored* as 24-hour "HH:mm" (alarms and expiry maths rely on it);
 * this only changes how they're shown. 12-hour is the default: 00:30 → 12:30 AM, 13:00 → 1:00 PM.
 */
fun formatHabitTime(hhmm: String, use24h: Boolean): String {
    val parts = hhmm.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: return hhmm
    val minute = (parts.getOrNull(1) ?: "00").padStart(2, '0')
    if (use24h) return "${hour.toString().padStart(2, '0')}:$minute"
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    return "$h12:$minute ${if (hour < 12) "AM" else "PM"}"
}
