package com.vivichi.app.domain

import com.vivichi.app.data.AppState
import com.vivichi.app.data.DayLog
import com.vivichi.app.data.Habit
import com.vivichi.app.util.formatHabitTime
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class CoinRulesTest {

    private val today = LocalDate.now().toString()
    private val yesterday = LocalDate.now().minusDays(1).toString()

    /** Scheduled for "now", so the habit is open (not early, not expired) whenever the test runs. */
    private val nowTime: String = LocalTime.now().let {
        "${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}"
    }

    private fun habit(id: String, intensity: String = "high", createdOn: String? = null) =
        Habit(id, id, "x", if (intensity == "high") 10 else 3, intensity, nowTime, enabled = true, custom = createdOn != null, createdOn = createdOn)

    private fun state(habits: List<Habit>) =
        AppState(onboarded = true, habits = habits, todayLog = DayLog(today, mutableMapOf(), mutableMapOf()))

    private fun completeAll(start: AppState): AppState {
        var s = start
        s.habits.forEach { s = GameLogic.completeHabit(s, it.id, forceEarly = true).state }
        return s
    }

    @Test
    fun habitAddedTodayGivesXpButNoCoins() {
        val s = state(listOf(habit("a", createdOn = today), habit("b"), habit("c")))
        val r = GameLogic.completeHabit(s, "a", forceEarly = true)
        assertEquals(10, r.xpGained)
        assertEquals(0, r.coinsGained)
    }

    @Test
    fun habitAddedYesterdayEarnsCoins() {
        val s = state(listOf(habit("a", createdOn = yesterday), habit("b"), habit("c")))
        assertEquals(15, GameLogic.completeHabit(s, "a", forceEarly = true).coinsGained)
    }

    @Test
    fun farmingManyHabitsStopsAtDailyCap() {
        // 20 high habits would be 300 coins + bonus without the cap.
        val s = completeAll(state((1..20).map { habit("h$it", createdOn = yesterday) }))
        assertEquals(GameLogic.MAX_HABIT_COINS_PER_DAY, s.coins)
        assertEquals(GameLogic.MAX_HABIT_COINS_PER_DAY, s.habitCoinsToday)
    }

    @Test
    fun capResetsOnANewDay() {
        val s = state(listOf(habit("a"), habit("b"), habit("c"))).copy(habitCoinsDate = yesterday, habitCoinsToday = GameLogic.MAX_HABIT_COINS_PER_DAY)
        assertEquals(15, GameLogic.completeHabit(s, "a", forceEarly = true).coinsGained)
    }

    @Test
    fun allDoneBonusNeedsAtLeastThreeHabits() {
        val two = completeAll(state(listOf(habit("a", "low"), habit("b", "low"))))
        assertEquals(10, two.coins) // 5 + 5, no bonus
        val three = completeAll(state(listOf(habit("a", "low"), habit("b", "low"), habit("c", "low"))))
        assertEquals(15 + GameLogic.COINS_ALL_DONE_BONUS, three.coins)
    }

    @Test
    fun defaultDayStaysUnderCap() {
        // Typical enabled defaults: four low, one high, one low, one medium.
        val habits = listOf(
            habit("h1", "low"), habit("h2", "low"), habit("h3", "low"), habit("h4", "low"),
            habit("h5", "high"), habit("h8", "low"),
            Habit("h9", "h9", "x", 5, "medium", nowTime)
        )
        val s = completeAll(state(habits))
        assertEquals(5 * 5 + 15 + 8 + GameLogic.COINS_ALL_DONE_BONUS, s.coins)
    }

    @Test
    fun twelveHourFormat() {
        assertEquals("12:00 AM", formatHabitTime("00:00", false))
        assertEquals("11:30 PM", formatHabitTime("23:30", false))
        assertEquals("12:15 PM", formatHabitTime("12:15", false))
        assertEquals("7:05 AM", formatHabitTime("07:05", false))
        assertEquals("1:00 PM", formatHabitTime("13:00", false))
        assertEquals("07:05", formatHabitTime("07:05", true))
    }
}
