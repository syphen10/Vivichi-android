package com.vivichi.app.ui

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivichi.app.data.*
import com.vivichi.app.domain.GameLogic
import com.vivichi.app.domain.HabitAttemptResult
import com.vivichi.app.notify.ReminderScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class Reward(val xp: Int, val intensity: String, val coins: Int)

data class UiEvent(
    val xpToast: Reward? = null,
    val leveledUpTo: Int? = null,
    val diedEntry: CemeteryEntry? = null,
    val missedDays: Int? = null,
    val earlyConfirm: Pair<String, Int>? = null, // habitId, minutesAhead
    /** Current pet is a species the user no longer owns (e.g. after animals became locked). */
    val mustPickPet: Boolean = false
)

class VivichiViewModel(
    private val repository: PetRepository,
    private val scheduler: ReminderScheduler?
) : ViewModel() {

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val _event = MutableStateFlow(UiEvent())
    val event: StateFlow<UiEvent> = _event.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    val obStep = mutableStateOf(0)
    val obSpeciesChoice = mutableStateOf("cat")

    init {
        viewModelScope.launch {
            val loaded = repository.current()
            _state.value = loaded
            runDayCheck()
            applyPremiumBonus()
            checkOwnedPet()
            _isReady.value = true
            // Re-arm every enabled habit's alarm on each launch. AlarmManager alarms don't
            // survive reboots or app force-stops, and previously they were only ever set when
            // the user explicitly edited a habit — so reminders could silently stop forever.
            scheduler?.rescheduleAll(_state.value)
            startTicker()
        }
    }

    private suspend fun startTicker() {
        while (true) {
            delay(60_000)
            runDayCheck()
            _state.value = GameLogic.refreshExpiry(_state.value)
            persist()
        }
    }

    private fun persist() {
        viewModelScope.launch { repository.save(_state.value) }
        // Every state mutation funnels through here, so this is the one hook that keeps the
        // shade panel (health, next habit, countdown) in step with the app.
        scheduler?.updateStatusPanel(_state.value)
    }

    /** Swaps in a whole state once the disk load has finished (used by the debug showcase profile). */
    fun replaceState(newState: AppState) {
        viewModelScope.launch {
            isReady.first { it }
            _state.value = newState
            _event.value = UiEvent()
            checkOwnedPet()
            persist()
            scheduler?.rescheduleAll(_state.value)
        }
    }

    private fun checkOwnedPet() {
        val s = _state.value
        if (s.onboarded && !GameLogic.ownsSpecies(s, s.pet.species)) {
            _event.value = _event.value.copy(mustPickPet = true)
        }
    }

    private fun applyPremiumBonus() {
        val s = _state.value
        val today = GameLogic.today()
        if (s.premium && s.lastPremiumBonus != today) {
            _state.value = s.copy(coins = s.coins + GameLogic.PREMIUM_DAILY_BONUS, lastPremiumBonus = today)
            persist()
        }
    }

    fun setStatusPanel(enabled: Boolean) {
        _state.value = _state.value.copy(statusPanel = enabled)
        persist()
    }

    private fun runDayCheck() {
        val outcome = GameLogic.checkDayRollover(_state.value)
        _state.value = outcome.state
        if (outcome.died && outcome.cemeteryEntry != null) {
            _event.value = _event.value.copy(diedEntry = outcome.cemeteryEntry)
        }
        persist()
    }

    /**
     * Called from Activity.onResume, which fires *before* the initial disk load finishes.
     * Reading/writing state here without waiting used to clobber the real saved data with the
     * empty default AppState (wiping `notif`, habits, streak...) and persist that to disk —
     * which is why reminders silently stopped and the missed-you overlay appeared late or not
     * at all. Suspending until the load completes fixes both.
     */
    fun checkMissedYou() {
        viewModelScope.launch {
            isReady.first { it }
            val days = GameLogic.daysAwaySince(_state.value.lastSeen)
            if (days != null && days >= 1 && _state.value.onboarded) {
                _event.value = _event.value.copy(missedDays = days)
            }
            _state.value = _state.value.copy(lastSeen = GameLogic.today())
            persist()
        }
    }

    fun clearEvent() {
        _event.value = UiEvent()
    }

    // Targeted clears — each overlay only nulls its own field, since multiple overlay
    // states (e.g. an XP toast and a later early-confirm dialog) can be in flight at once
    // and a blanket clearEvent() would wipe out an unrelated overlay still on screen.
    fun clearXpToast() {
        _event.value = _event.value.copy(xpToast = null)
    }

    fun clearLeveledUp() {
        _event.value = _event.value.copy(leveledUpTo = null)
    }

    fun clearMissedDays() {
        _event.value = _event.value.copy(missedDays = null)
    }

    fun clearEarlyConfirm() {
        _event.value = _event.value.copy(earlyConfirm = null)
    }

    fun clearDied() {
        _event.value = _event.value.copy(diedEntry = null)
    }

    fun finishTutorial() {
        _state.value = _state.value.copy(tutorialSeen = true)
        persist()
    }

    // ---------- Onboarding ----------

    fun finishOnboarding(name: String, species: String, habitTimes: Map<String, String>, habitEnabled: Map<String, Boolean>, notifOptIn: Boolean, statusPanelOptIn: Boolean) {
        val habits = DefaultContent.defaultHabits.map { h ->
            h.copy(time = habitTimes[h.id] ?: h.time, enabled = habitEnabled[h.id] ?: h.enabled)
        }
        _state.value = _state.value.copy(
            onboarded = true,
            notif = notifOptIn,
            statusPanel = statusPanelOptIn,
            pet = Pet(name = name, species = species, bornAt = GameLogic.today()),
            habits = habits,
            lastSeen = GameLogic.today(),
            todayLog = DayLog(GameLogic.today(), mutableMapOf(), mutableMapOf())
        )
        persist()
        scheduler?.rescheduleAll(_state.value)
    }

    // ---------- Habits ----------

    fun completeHabit(habitId: String, forceEarly: Boolean = false) {
        val result: HabitAttemptResult = GameLogic.completeHabit(_state.value, habitId, forceEarly)
        if (result.needsEarlyConfirm) {
            _event.value = _event.value.copy(earlyConfirm = habitId to result.minutesAhead)
            return
        }
        if (!result.applied) return
        _state.value = result.state
        val habit = _state.value.habits.find { it.id == habitId }
        _event.value = _event.value.copy(
            xpToast = Reward(result.xpGained, habit?.intensity ?: "low", result.coinsGained),
            leveledUpTo = result.leveledUpTo
        )
        persist()
    }

    fun addOrUpdateHabit(id: String?, name: String, icon: String, xp: Int, time: String): Boolean {
        val hm = time.split(":").map { it.toInt() }
        val minsOfDay = hm[0] * 60 + hm[1]
        val nowH = java.time.LocalTime.now()
        val nowMins = nowH.hour * 60 + nowH.minute
        if (id == null && nowMins > minsOfDay + 150) return false // caller should show "expired" confirm
        val intensity = when (xp) { 3 -> "low"; 5 -> "medium"; else -> "high" }
        val habits = _state.value.habits.toMutableList()
        if (id != null) {
            val idx = habits.indexOfFirst { it.id == id }
            if (idx >= 0) habits[idx] = habits[idx].copy(name = name, icon = icon, xp = xp, intensity = intensity, time = time, enabled = true)
        } else {
            habits.add(Habit("c${System.currentTimeMillis()}", name, icon, xp, intensity, time, true, true))
        }
        _state.value = _state.value.copy(habits = habits)
        persist()
        scheduler?.rescheduleAll(_state.value)
        return true
    }

    fun forceAddHabit(name: String, icon: String, xp: Int, time: String) {
        val intensity = when (xp) { 3 -> "low"; 5 -> "medium"; else -> "high" }
        val habits = _state.value.habits + Habit("c${System.currentTimeMillis()}", name, icon, xp, intensity, time, true, true)
        _state.value = _state.value.copy(habits = habits)
        persist()
        scheduler?.rescheduleAll(_state.value)
    }

    fun toggleHabit(id: String, enabled: Boolean) {
        _state.value = _state.value.copy(habits = _state.value.habits.map { if (it.id == id) it.copy(enabled = enabled) else it })
        persist()
        scheduler?.rescheduleAll(_state.value)
    }

    fun deleteHabit(id: String) {
        _state.value = _state.value.copy(habits = _state.value.habits.filterNot { it.id == id })
        persist()
        scheduler?.rescheduleAll(_state.value)
    }

    fun saveHabitTimes(times: Map<String, String>) {
        _state.value = _state.value.copy(habits = _state.value.habits.map { h -> times[h.id]?.let { h.copy(time = it) } ?: h })
        persist()
        scheduler?.rescheduleAll(_state.value)
    }

    // ---------- Style ----------

    fun pickSpecies(species: String) {
        if (!GameLogic.ownsSpecies(_state.value, species)) return
        _state.value = _state.value.copy(pet = _state.value.pet.copy(species = species))
        _event.value = _event.value.copy(mustPickPet = false)
        persist()
    }

    /** Spends coins on a species and switches to it. Returns false if it can't be afforded. */
    fun buySpecies(species: String): Boolean {
        val s = _state.value
        val info = PETS.find { it.id == species } ?: return false
        if (GameLogic.ownsSpecies(s, species)) { pickSpecies(species); return true }
        if (s.coins < info.price) return false
        _state.value = s.copy(
            coins = s.coins - info.price,
            ownedSpecies = s.ownedSpecies + species,
            pet = s.pet.copy(species = species)
        )
        persist()
        return true
    }

    fun pickOutfit(outfit: String) {
        val item = ALL_WEARABLES.find { it.id == outfit } ?: return
        if (!GameLogic.canWear(_state.value, item)) return
        _state.value = _state.value.copy(pet = _state.value.pet.copy(outfit = outfit))
        persist()
    }

    /** Spends coins on an outfit/theme and wears it. Returns false if it can't be afforded. */
    fun buyWearable(id: String): Boolean {
        val s = _state.value
        val item = ALL_WEARABLES.find { it.id == id } ?: return false
        if (GameLogic.canWear(s, item)) { pickOutfit(id); return true }
        if (item.premium || item.price <= 0 || s.coins < item.price) return false
        _state.value = s.copy(
            coins = s.coins - item.price,
            ownedWearables = s.ownedWearables + id,
            pet = s.pet.copy(outfit = id)
        )
        persist()
        return true
    }

    // ---------- Stats ----------

    fun equipTitle(id: String) {
        _state.value = _state.value.copy(activeTitle = id)
        persist()
    }

    // ---------- Settings ----------

    fun setNotif(enabled: Boolean) {
        _state.value = _state.value.copy(notif = enabled)
        persist()
        if (enabled) scheduler?.rescheduleAll(_state.value) else scheduler?.cancelAll(_state.value)
    }

    fun resetAll() {
        viewModelScope.launch {
            repository.reset()
            _state.value = AppState()
            scheduler?.cancelAll(_state.value)
        }
    }

    // ---------- Cemetery / Death ----------

    fun afterDeath() {
        _state.value = GameLogic.afterDeath(_state.value)
        persist()
        obStep.value = 0
    }
}
