package com.vivichi.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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

/** Hooks into the Activity-bound store pieces (Play Billing, rewarded ads). */
data class StoreHooks(
    val premiumPrice: String? = null,
    val adReady: Boolean = false,
    val onBuyPremium: () -> Unit = {},
    val onRestorePremium: () -> Unit = {},
    val onWatchAd: () -> Unit = {},
    /** Non-null only where the user must be able to revisit ad consent (EEA/UK). */
    val onAdPrivacy: (() -> Unit)? = null
)

enum class Tab(val label: String) {
    HOME("Home"), HABITS("Habits"), STATS("Stats"), STYLE("Style"),
    PLAY("Play"), CEMETERY("R.I.P"), SETTINGS("More")
}

@Composable
fun VivichiApp(viewModel: VivichiViewModel, store: StoreHooks = StoreHooks()) {
    val state by viewModel.state.collectAsState()
    val event by viewModel.event.collectAsState()
    var tab by remember { mutableStateOf(Tab.HOME) }
    var showPremium by remember { mutableStateOf(false) }
    var showCoins by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val bgColor = seasonalBackground(state.pet.outfit)
    // Premium means no ads anywhere, so the offer simply doesn't exist for them.
    val adOffer = if (state.premium) null else com.vivichi.app.ui.components.AdOffer(
        adsLeft = com.vivichi.app.domain.GameLogic.adsLeftToday(state),
        ready = store.adReady,
        onWatch = store.onWatchAd
    )

    Box(Modifier.fillMaxSize().background(bgColor)) {
        if (!state.onboarded) {
            OnboardingScreen(viewModel = viewModel)
        } else {
            Scaffold(
                containerColor = bgColor,
                // Coins + Premium are visible on every tab. The bar handles the status bar inset
                // itself, so screens underneath no longer need to paint behind the status bar.
                topBar = {
                    com.vivichi.app.ui.components.EconomyBar(
                        coins = state.coins,
                        isPremium = state.premium,
                        onCoins = { showCoins = true },
                        onPremium = { showPremium = true }
                    )
                },
                bottomBar = { VivichiBottomBar(tab) { tab = it } }
            ) { padding ->
                Box(Modifier.padding(padding).fillMaxSize()) {
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            (fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 24 }) togetherWith fadeOut(tween(120))
                        },
                        label = "tab"
                    ) { current ->
                        when (current) {
                            Tab.HOME -> HomeScreen(viewModel, onNavigateHabits = { tab = Tab.HABITS })
                            Tab.HABITS -> HabitsScreen(viewModel)
                            Tab.STATS -> StatsScreen(viewModel)
                            Tab.STYLE -> StyleScreen(viewModel, adOffer = adOffer, onOpenPremium = { showPremium = true })
                            Tab.PLAY -> PlaygroundScreen(viewModel)
                            Tab.CEMETERY -> CemeteryScreen(viewModel)
                            Tab.SETTINGS -> SettingsScreen(
                                viewModel,
                                onOpenPremium = { showPremium = true },
                                onRestorePremium = store.onRestorePremium,
                                onAdPrivacy = if (state.premium) null else store.onAdPrivacy
                            )
                        }
                    }
                }
            }
            if (showPremium) {
                com.vivichi.app.ui.components.PremiumDialog(
                    isPremium = state.premium,
                    priceLabel = store.premiumPrice,
                    onBuy = store.onBuyPremium,
                    onRestore = store.onRestorePremium,
                    onDismiss = { showPremium = false }
                )
            }
            if (showCoins) {
                com.vivichi.app.ui.components.CoinsDialog(
                    coins = state.coins,
                    isPremium = state.premium,
                    ad = adOffer,
                    onGetPremium = { showCoins = false; showPremium = true },
                    onDismiss = { showCoins = false }
                )
            }
            if (event.mustPickPet && event.diedEntry == null) {
                com.vivichi.app.ui.components.ForcePickPetDialog(
                    petName = state.pet.name,
                    owned = com.vivichi.app.data.PETS.filter { com.vivichi.app.domain.GameLogic.ownsSpecies(state, it.id) },
                    onPick = { viewModel.pickSpecies(it) }
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
            event.xpToast?.let { reward ->
                XpToast(xp = reward.xp, intensity = reward.intensity, coins = reward.coins)
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
