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
    val statusPanel: Boolean = true
)

fun xpForLevel(level: Int): Int = level * 50
