package com.vivichi.app.showcase

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.vivichi.app.data.Achievements
import com.vivichi.app.data.AppState
import com.vivichi.app.data.DayLog
import com.vivichi.app.data.DefaultContent
import com.vivichi.app.data.HistoryEntry
import com.vivichi.app.data.Pet
import com.vivichi.app.ui.VivichiViewModel
import java.time.LocalDate

/**
 * DEBUG BUILDS ONLY — the release source set has a no-op with the same signature.
 *
 * Loads a realistic, flattering profile for capturing Play Store screenshots:
 *   adb shell am start -n com.vivichi.app/.MainActivity --ez showcase true
 *
 * Habit completion is time-dependent, so set the device clock to early afternoon (~13:20)
 * before launching: morning habits read as done, Lunch is ready now, evening ones upcoming.
 */
object ShowcaseLoader {

    /** True once the showcase profile is loaded: hides the test-ad banner so it isn't in the shots. */
    var active: Boolean by androidx.compose.runtime.mutableStateOf(false)
        private set

    fun maybeLoad(intent: Intent?, viewModel: VivichiViewModel) {
        if (intent?.getBooleanExtra("showcase", false) == true) {
            active = true
            viewModel.replaceState(build())
        }
    }

    private fun build(): AppState {
        val today = LocalDate.now()
        val habits = DefaultContent.defaultHabits.map {
            when (it.id) {
                "h6", "h7" -> it.copy(enabled = true)
                else -> it
            }
        }
        val history = listOf(100, 88, 100, 75, 100, 88, 100).mapIndexed { i, pct ->
            HistoryEntry(today.minusDays(i + 1L).toString(), pct)
        }
        return AppState(
            onboarded = true,
            notif = true,
            activeTitle = "t3",
            pet = Pet(
                name = "Mochi",
                species = "cat",
                level = 4,
                xp = 42,
                outfit = "default",
                health = 92,
                bornAt = today.minusDays(18).toString()
            ),
            habits = habits,
            streak = 12,
            bestStreak = 12,
            totalXP = 198,
            totalDone = 61,
            todayLog = DayLog(
                date = today.toString(),
                completed = mutableMapOf("h1" to true, "h2" to true, "h3" to true, "h5" to true),
                expired = mutableMapOf()
            ),
            history = history,
            lastSeen = today.toString(),
            achievements = Achievements(earlyBird = true),
            tutorialSeen = true,
            statusPanel = true,
            coins = 240,
            ownedSpecies = listOf("fox"),
            ownedWearables = listOf("ocean")
        )
    }
}
