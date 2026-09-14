package com.vivichi.app.domain

import com.vivichi.app.data.*
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt
import kotlin.random.Random

enum class HabitStatus { AVAILABLE, DONE, EXPIRED, DISABLED }
enum class Mood { HAPPY, NEUTRAL, SAD, FRAIL }

data class DayCheckOutcome(val state: AppState, val died: Boolean, val cemeteryEntry: CemeteryEntry?)

data class HabitAttemptResult(
    val state: AppState,
    val applied: Boolean,
    val needsEarlyConfirm: Boolean,
    val minutesAhead: Int,
    val xpGained: Int,
    val leveledUpTo: Int?,
    val streakCompletedToday: Boolean,
    val coinsGained: Int = 0
)

object GameLogic {

    fun today(): String = LocalDate.now().toString()

    // ---------- Economy ----------

    const val COINS_ALL_DONE_BONUS = 20
    const val COINS_PER_AD = 15
    const val MAX_ADS_PER_DAY = 5
    const val PREMIUM_DAILY_BONUS = 25

    fun coinsForHabit(habit: Habit): Int = when (habit.intensity) {
        "high" -> 15
        "medium" -> 8
        else -> 5
    }

    fun ownsSpecies(state: AppState, speciesId: String): Boolean {
        val species = PETS.find { it.id == speciesId } ?: return false
        return species.price == 0 || state.premium || speciesId in state.ownedSpecies
    }

    /** Whether an outfit/theme is usable right now (level, coin purchase, or Premium). */
    fun canWear(state: AppState, item: OutfitInfo): Boolean = when {
        item.premium -> state.premium
        item.price > 0 -> state.premium || item.id in state.ownedWearables
        item.seasonal -> true
        else -> state.pet.level >= item.level
    }

    fun adsLeftToday(state: AppState): Int =
        if (state.adsDate != today()) MAX_ADS_PER_DAY else (MAX_ADS_PER_DAY - state.adsWatchedToday).coerceAtLeast(0)

    private fun toMins(time: String): Int {
        val (h, m) = time.split(":").map { it.toInt() }
        return h * 60 + m
    }

    private fun nowMins(): Int {
        val t = LocalTime.now()
        return t.hour * 60 + t.minute
    }

    fun habitStatus(state: AppState, habit: Habit): HabitStatus {
        if (!habit.enabled) return HabitStatus.DISABLED
        if (state.todayLog.completed[habit.id] == true) return HabitStatus.DONE
        if (state.todayLog.expired[habit.id] == true) return HabitStatus.EXPIRED
        val nm = nowMins()
        return if (nm > toMins(habit.time) + 150) HabitStatus.EXPIRED else HabitStatus.AVAILABLE
    }

    fun isEarly(habit: Habit): Boolean = nowMins() < toMins(habit.time) - 30

    fun minutesUntil(habit: Habit): Int = toMins(habit.time) - nowMins()

    /** "Xh Ym ahead" / "Ym ahead" */
    fun fmtMinutesAhead(minutes: Int): String {
        val m = kotlin.math.abs(minutes)
        val hrs = m / 60
        val mins = m % 60
        return if (hrs > 0) "${hrs}h ${mins}m ahead" else "${mins}m ahead"
    }

    /** Countdown shown on a locked/upcoming habit card. */
    fun fmtUntilUnlock(time: String): String {
        val diff = toMins(time) - 30 - nowMins()
        if (diff <= 0) return "Now!"
        val hrs = diff / 60
        val mins = diff % 60
        return if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
    }

    fun completionPct(state: AppState): Int {
        val enabled = state.habits.filter { it.enabled }
        if (enabled.isEmpty()) return 0
        val done = enabled.count { state.todayLog.completed[it.id] == true }
        return ((done.toDouble() / enabled.size) * 100).roundToInt()
    }

    private fun habitMood(state: AppState): Mood {
        val enabled = state.habits.filter { it.enabled }
        if (enabled.isEmpty()) return Mood.HAPPY
        val done = enabled.count { state.todayLog.completed[it.id] == true }
        val p = done.toDouble() / enabled.size
        return when {
            p >= 0.8 -> Mood.HAPPY
            p >= 0.4 -> Mood.NEUTRAL
            else -> Mood.SAD
        }
    }

    private fun healthMood(health: Int, base: Mood): Mood = when {
        health <= 0 -> Mood.SAD
        health < 25 -> Mood.FRAIL
        health < 50 -> Mood.SAD
        else -> base
    }

    fun mood(state: AppState): Mood = healthMood(state.pet.health, habitMood(state))

    /** Mood used specifically for the animated character (matches original renderHome behavior). */
    fun characterMood(state: AppState): Mood {
        val health = state.pet.health
        return when {
            health < 25 -> Mood.FRAIL
            health < 50 -> Mood.SAD
            else -> mood(state)
        }
    }

    fun greeting(): String {
        val h = LocalTime.now().hour
        return when {
            h < 12 -> "Good morning! ☀️"
            h < 17 -> "Good afternoon! 🌤️"
            else -> "Good evening! 🌙"
        }
    }

    fun buddyMessage(state: AppState): String {
        val enabled = state.habits.filter { it.enabled }
        val allDone = enabled.isNotEmpty() && enabled.all { state.todayLog.completed[it.id] == true }
        val m = mood(state)
        val h = LocalTime.now().hour
        return when {
            allDone -> pick(
                "WE DID IT!! All habits complete! You are my hero!! 🎉",
                "PERFECT DAY! I am so proud of us both! 🌟",
                "Full completion!! We are unstoppable! ✨"
            )
            m == Mood.FRAIL -> pick(
                "I do not feel so good... please take care of me 🤍",
                "My energy is so low... I need you 🥺",
                "Please... I am getting weaker... 💔"
            )
            state.streak >= 7 -> "${state.streak} days strong! We are LEGENDARY! 🔥"
            h < 9 -> pick("Good morning! Lets make today amazing! ☀️", "Rise and shine! Your habits are waiting!")
            h >= 20 -> pick("Evening! Do not miss your night habits! 🌙", "Almost bedtime, lets wrap up right!")
            m == Mood.SAD -> pick(
                "Please, just one habit? For me? 🥹",
                "I have been waiting for you all day 💭",
                "I miss us completing everything 😢"
            )
            else -> pick(
                "Hey, we still have some habits left 💪",
                "You have got this! Push through 🚀",
                "Almost there. A little more effort!"
            )
        }
    }

    private fun pick(vararg opts: String): String = opts[Random.nextInt(opts.size)]

    /**
     * Attempt to complete a habit. If it is being done more than 30 minutes early and
     * [forceEarly] is not set, no mutation happens and [needsEarlyConfirm] is true —
     * the caller should confirm with the user and re-invoke with forceEarly = true.
     */
    fun completeHabit(state: AppState, habitId: String, forceEarly: Boolean = false): HabitAttemptResult {
        val habit = state.habits.find { it.id == habitId }
            ?: return HabitAttemptResult(state, false, false, 0, 0, null, false)
        if (state.todayLog.completed[habitId] == true) {
            return HabitAttemptResult(state, false, false, 0, 0, null, false)
        }
        val status = habitStatus(state, habit)
        if (status == HabitStatus.EXPIRED || status == HabitStatus.DISABLED) {
            return HabitAttemptResult(state, false, false, 0, 0, null, false)
        }
        if (!forceEarly && isEarly(habit)) {
            return HabitAttemptResult(state, false, true, minutesUntil(habit), 0, null, false)
        }

        val completed = state.todayLog.completed.toMutableMap().apply { put(habitId, true) }
        val hour = LocalTime.now().hour
        var achievements = state.achievements
        if (hour < 8) achievements = achievements.copy(earlyBird = true)
        if (hour >= 23) achievements = achievements.copy(nightOwl = true)

        var level = state.pet.level
        var xp = state.pet.xp + habit.xp
        while (xp >= xpForLevel(level)) {
            xp -= xpForLevel(level)
            level++
        }
        val leveledUp = if (level > state.pet.level) level else null

        val enabled = state.habits.filter { it.enabled }
        val allDoneNow = enabled.isNotEmpty() && enabled.all { completed[it.id] == true }
        var streak = state.streak
        var bestStreak = state.bestStreak
        var health = state.pet.health
        if (allDoneNow) {
            streak++
            if (streak > bestStreak) bestStreak = streak
            health = minOf(100, health + 5)
        }

        val coinsGained = coinsForHabit(habit) + if (allDoneNow) COINS_ALL_DONE_BONUS else 0

        val newState = state.copy(
            pet = state.pet.copy(level = level, xp = xp, health = health),
            totalXP = state.totalXP + habit.xp,
            totalDone = state.totalDone + 1,
            streak = streak,
            bestStreak = bestStreak,
            achievements = achievements,
            todayLog = state.todayLog.copy(completed = completed),
            coins = state.coins + coinsGained
        )
        return HabitAttemptResult(newState, true, false, 0, habit.xp, leveledUp, allDoneNow, coinsGained)
    }

    /** Marks any active habits whose 2.5h completion window has passed as expired. */
    fun refreshExpiry(state: AppState): AppState {
        if (!state.onboarded) return state
        var changed = false
        val expired = state.todayLog.expired.toMutableMap()
        state.habits.filter { it.enabled }.forEach { h ->
            if (state.todayLog.completed[h.id] != true && expired[h.id] != true) {
                if (nowMins() > toMins(h.time) + 150) {
                    expired[h.id] = true
                    changed = true
                }
            }
        }
        return if (changed) state.copy(todayLog = state.todayLog.copy(expired = expired)) else state
    }

    /** Call on app open / resume. Handles the daily rollover: streak/health/history update, possible death. */
    fun checkDayRollover(state: AppState): DayCheckOutcome {
        val td = today()
        if (state.todayLog.date == td) return DayCheckOutcome(state, false, null)

        if (state.todayLog.date == null) {
            return DayCheckOutcome(state.copy(todayLog = DayLog(td, mutableMapOf(), mutableMapOf())), false, null)
        }

        val yesterday = LocalDate.now().minusDays(1).toString()
        val enabled = state.habits.filter { it.enabled }
        val done = enabled.count { state.todayLog.completed[it.id] == true }
        val pct = if (enabled.isNotEmpty()) done.toDouble() / enabled.size else 1.0

        var streak = state.streak
        var bestStreak = state.bestStreak
        if (state.todayLog.date == yesterday && pct >= 0.8) {
            streak++
            if (streak > bestStreak) bestStreak = streak
        } else if (state.todayLog.date != yesterday) {
            streak = 0
        }

        var health = state.pet.health
        health = when {
            pct >= 0.8 -> minOf(100, health + 8)
            pct >= 0.5 -> maxOf(0, health - 10)
            pct >= 0.25 -> maxOf(0, health - 22)
            else -> maxOf(0, health - 38)
        }

        val newHistory = (listOf(HistoryEntry(state.todayLog.date, (pct * 100).roundToInt())) + state.history).take(7)

        val updated = state.copy(
            streak = streak,
            bestStreak = bestStreak,
            pet = state.pet.copy(health = health),
            history = newHistory
        )

        if (health <= 0 && state.onboarded) {
            val bornAt = state.pet.bornAt ?: td
            val entry = CemeteryEntry(
                id = System.currentTimeMillis(),
                name = state.pet.name,
                species = state.pet.species,
                outfit = state.pet.outfit,
                level = state.pet.level,
                streak = bestStreak,
                totalDone = state.totalDone,
                totalXP = state.totalXP,
                bornAt = bornAt,
                diedAt = td,
                daysAlive = maxOf(1, java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(bornAt), LocalDate.parse(td)).toInt())
            )
            // Reset todayLog now (even though the pet stays "dead" until the user
            // acknowledges via afterDeath()) so repeated rollover checks — e.g. the
            // 60s ticker firing again before the death screen is dismissed — don't
            // re-run this branch and append duplicate cemetery entries.
            val withCemetery = updated.copy(
                cemetery = listOf(entry) + updated.cemetery,
                todayLog = DayLog(td, mutableMapOf(), mutableMapOf())
            )
            return DayCheckOutcome(withCemetery, true, entry)
        }

        return DayCheckOutcome(updated.copy(todayLog = DayLog(td, mutableMapOf(), mutableMapOf())), false, null)
    }

    /** Resets pet/progress after a death, keeping cemetery + notification settings. */
    fun afterDeath(state: AppState): AppState = state.copy(
        onboarded = false,
        pet = Pet(bornAt = today()),
        streak = 0,
        totalXP = 0,
        totalDone = 0,
        activeTitle = null,
        todayLog = DayLog(today(), mutableMapOf(), mutableMapOf()),
        history = emptyList()
    )

    fun activeTitle(state: AppState): TitleInfo? {
        val id = state.activeTitle ?: return null
        val t = TITLES.find { it.id == id } ?: return null
        return if (t.check(state.streak, state.bestStreak, state.totalXP, state.totalDone, state.pet.level, state.achievements, state.cemetery.size)) t else null
    }

    fun earnedTitles(state: AppState): List<Pair<TitleInfo, Boolean>> = TITLES.map { t ->
        t to t.check(state.streak, state.bestStreak, state.totalXP, state.totalDone, state.pet.level, state.achievements, state.cemetery.size)
    }

    fun daysAwaySince(lastSeen: String?): Int? {
        if (lastSeen == null) return null
        val td = today()
        if (lastSeen == td) return null
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(lastSeen), LocalDate.parse(td)).toInt()
    }
}
