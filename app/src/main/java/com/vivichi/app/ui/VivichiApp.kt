package com.vivichi.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vivichi.app.ui.components.CharacterView
import com.vivichi.app.ui.dialogs.*
import com.vivichi.app.ui.screens.*
import com.vivichi.app.ui.theme.*
import com.vivichi.app.util.SoundFx

enum class Tab(val label: String) {
    HOME("Home"), HABITS("Habits"), STATS("Stats"), STYLE("Style"),
    PLAY("Play"), CEMETERY("R.I.P"), SETTINGS("More")
}

@Composable
fun VivichiApp(viewModel: VivichiViewModel) {
    val state by viewModel.state.collectAsState()
    val event by viewModel.event.collectAsState()
    var tab by remember { mutableStateOf(Tab.HOME) }

    val bgColor = seasonalBackground(state.pet.outfit)

    Box(Modifier.fillMaxSize().background(bgColor)) {
        if (!state.onboarded) {
            OnboardingScreen(viewModel = viewModel)
        } else {
            Scaffold(
                containerColor = bgColor,
                bottomBar = { VivichiBottomBar(tab) { tab = it } }
            ) { padding ->
                Box(Modifier.padding(padding).fillMaxSize()) {
                    when (tab) {
                        Tab.HOME -> HomeScreen(viewModel, onNavigateHabits = { tab = Tab.HABITS })
                        Tab.HABITS -> HabitsScreen(viewModel)
                        Tab.STATS -> StatsScreen(viewModel)
                        Tab.STYLE -> StyleScreen(viewModel)
                        Tab.PLAY -> PlaygroundScreen(viewModel)
                        Tab.CEMETERY -> CemeteryScreen(viewModel)
                        Tab.SETTINGS -> SettingsScreen(viewModel)
                    }
                }
            }
            // Edge-to-edge: the app draws behind the status bar, and window.statusBarColor is
            // ignored from Android 15. The Playground header is a gradient, so paint the same
            // gradient behind the status bar to avoid a seam. Drawn after the Scaffold, whose
            // container background would otherwise cover it.
            if (tab == Tab.PLAY) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                        .background(androidx.compose.ui.graphics.Brush.horizontalGradient(seasonalGradient(state.pet.outfit)))
                )
            }
            if (!state.tutorialSeen) {
                TutorialOverlay(onDone = { viewModel.finishTutorial() })
            }
        }

        // ---- Overlays ----
        event.diedEntry?.let { entry ->
            DeathScreen(entry = entry) {
                viewModel.afterDeath()
                viewModel.clearDied()
                tab = Tab.HOME
            }
        }
        if (event.diedEntry == null) {
            event.missedDays?.let { days ->
                MissedYouOverlay(days = days, petName = state.pet.name, species = state.pet.species, streak = state.streak) {
                    viewModel.clearMissedDays()
                }
            }
            event.leveledUpTo?.let { lvl ->
                LevelUpDialog(level = lvl) { viewModel.clearLeveledUp() }
            }
            event.earlyConfirm?.let { (habitId, minutesAhead) ->
                val habit = state.habits.find { it.id == habitId }
                EarlyConfirmDialog(
                    habitName = habit?.name ?: "This habit",
                    minutesAhead = minutesAhead,
                    onConfirm = {
                        viewModel.clearEarlyConfirm()
                        viewModel.completeHabit(habitId, forceEarly = true)
                    },
                    onCancel = { viewModel.clearEarlyConfirm() }
                )
            }
            event.xpToast?.let { (xp, intensity) ->
                XpToast(xp = xp, intensity = intensity)
                LaunchedEffect(event.xpToast) {
                    kotlinx.coroutines.delay(1400)
                    if (event.leveledUpTo == null) viewModel.clearXpToast()
                }
            }
        }
    }
}

@Composable
private fun VivichiBottomBar(current: Tab, onSelect: (Tab) -> Unit) {
    NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
        val icons = mapOf(
            Tab.HOME to Icons.Filled.Home,
            Tab.HABITS to Icons.Filled.CheckCircle,
            Tab.STATS to Icons.Filled.BarChart,
            Tab.STYLE to Icons.Filled.Palette,
            Tab.PLAY to Icons.Filled.Pets,
            Tab.CEMETERY to Icons.Filled.Park,
            Tab.SETTINGS to Icons.Filled.Settings
        )
        Tab.entries.forEach { t ->
            NavigationBarItem(
                selected = current == t,
                onClick = { SoundFx.nav(); onSelect(t) },
                icon = { Icon(icons[t]!!, contentDescription = t.label) },
                label = { Text(t.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PinkDark,
                    selectedTextColor = PinkDark,
                    unselectedIconColor = MutedText,
                    unselectedTextColor = MutedText,
                    indicatorColor = BgPink
                )
            )
        }
    }
}
