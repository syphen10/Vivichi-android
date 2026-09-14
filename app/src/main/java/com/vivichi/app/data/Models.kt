package com.vivichi.app.data

import kotlinx.serialization.Serializable

@Serializable
data class Habit(
    val id: String,
    val name: String,
    val icon: String,
    val xp: Int,
    val intensity: String, // low | medium | high
    val time: String, // "HH:mm"
    val enabled: Boolean = true,
    val custom: Boolean = false
)

@Serializable
data class Pet(
    val name: String = "",
    val species: String = "cat",
    val level: Int = 1,
    val xp: Int = 0,
    val outfit: String = "default",
    val health: Int = 100,
    val bornAt: String? = null
)

@Serializable
data class DayLog(
    val date: String? = null,
    val completed: MutableMap<String, Boolean> = mutableMapOf(),
    val expired: MutableMap<String, Boolean> = mutableMapOf()
)

@Serializable
data class HistoryEntry(val date: String, val pct: Int)

@Serializable
data class CemeteryEntry(
    val id: Long,
    val name: String,
    val species: String,
    val outfit: String,
    val level: Int,
    val streak: Int,
    val totalDone: Int,
    val totalXP: Int,
    val bornAt: String,
    val diedAt: String,
    val daysAlive: Int
)

@Serializable
data class Achievements(
    val earlyBird: Boolean = false,
    val nightOwl: Boolean = false
)

@Serializable
data class AppState(
    val onboarded: Boolean = false,
    val notif: Boolean = false,
    val activeTitle: String? = null,
    val pet: Pet = Pet(),
    val habits: List<Habit> = DefaultContent.defaultHabits,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val totalXP: Int = 0,
    val totalDone: Int = 0,
    val todayLog: DayLog = DayLog(),
    val history: List<HistoryEntry> = emptyList(),
    val cemetery: List<CemeteryEntry> = emptyList(),
    val lastSeen: String? = null,
    val achievements: Achievements = Achievements(),
    val tutorialSeen: Boolean = false,
    val statusPanel: Boolean = true,
    val coins: Int = 0,
    /** Species bought with coins. Free species and Premium aren't recorded here. */
    val ownedSpecies: List<String> = emptyList(),
    /** Outfits/themes bought with coins. */
    val ownedWearables: List<String> = emptyList(),
    val premium: Boolean = false,
    val lastPremiumBonus: String? = null,
    val adsDate: String? = null,
    val adsWatchedToday: Int = 0
)

/**
 * XP needed to clear [level]. Levels 1–5 are deliberately cheap so new players level up within
 * their first days (default habits earn ~30 XP/day); from level 6 the original level × 50 curve
 * applies.
 */
fun xpForLevel(level: Int): Int = when (level) {
    1 -> 20
    2 -> 35
    3 -> 55
    4 -> 75
    5 -> 100
    else -> level * 50
}
