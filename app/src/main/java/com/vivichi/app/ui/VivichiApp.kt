package com.vivichi.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import com.vivichi.app.ui.components.ConfettiBurst
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
    /** Latest purchase/restore outcome from Google Play, waiting to be shown. */
    val storeMessage: String? = null,
    val onStoreMessageSeen: () -> Unit = {},
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

    // Theme changes wash across the app instead of snapping.
    val bgColor by animateColorAsState(seasonalBackground(state.pet.outfit), tween(700), label = "bg")
    // Premium means no ads anywhere, so the offer simply doesn't exist for them.
    val adOffer = if (state.premium) null else com.vivichi.app.ui.components.AdOffer(
        adsLeft = com.vivichi.app.domain.GameLogic.adsLeftToday(state),
        ready = store.adReady,
        onWatch = store.onWatchAd
    )

    // Shared clock for all ambient motion; it pauses while anything scrolls (see Ambient.kt).
    // No background here: AppBackdrop already paints the page colour, and filling the whole
    // screen twice per frame is wasted work on budget GPUs.
    com.vivichi.app.ui.components.ProvideAmbientMotion { ambientScroll ->
    Box(Modifier.fillMaxSize().then(ambientScroll)) {
        com.vivichi.app.ui.components.AppBackdrop(bgColor)
        if (!state.onboarded) {
            OnboardingScreen(viewModel = viewModel)
        } else {
            Scaffold(
                containerColor = Color.Transparent,
                // The game HUD (pet portrait, level, health, XP, coins, Premium) sits above every tab
                // and handles the status bar inset itself.
                topBar = {
                    com.vivichi.app.ui.components.GameHud(
                        state = state,
                        onCoins = { showCoins = true },
                        onPremium = { showPremium = true }
                    )
                },
                bottomBar = {
                    Column {
                        // Banner lives outside the tab content, so it survives tab switches
                        // (one ad view, not a reload per tab). Premium removes it entirely.
                        if (!state.premium && !com.vivichi.app.showcase.ShowcaseLoader.active) com.vivichi.app.monetize.BottomBannerAd()
                        VivichiBottomBar(tab) { tab = it }
                    }
                }
            ) { padding ->
                Box(Modifier.padding(padding).fillMaxSize()) {
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            (fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 24 }) togetherWith fadeOut(tween(120))
                        },
                        label = "tab"
                    ) { current ->
                        // Each tab gets a fresh "opened at" stamp, so its cards animate in once.
                        val openedAt = remember(current) { android.os.SystemClock.uptimeMillis() }
                        androidx.compose.runtime.CompositionLocalProvider(
                            com.vivichi.app.ui.components.LocalScreenOpenedAt provides openedAt
                        ) {
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
            }
            if (showPremium) {
                com.vivichi.app.ui.components.PremiumDialog(
                    isPremium = state.premium,
                    priceLabel = store.premiumPrice,
                    message = store.storeMessage,
                    onBuy = { store.onStoreMessageSeen(); store.onBuyPremium() },
                    onRestore = { store.onStoreMessageSeen(); store.onRestorePremium() },
                    onDismiss = { store.onStoreMessageSeen(); showPremium = false }
                )
            } else if (store.storeMessage != null) {
                // e.g. "Restore purchase" tapped from the More screen, with no Premium dialog open.
                AlertDialog(
                    onDismissRequest = store.onStoreMessageSeen,
                    icon = { com.vivichi.app.ui.components.EmojiGlyph(raw = "💎", size = 34.dp) },
                    title = { Text("Vivichi Premium", fontWeight = androidx.compose.ui.text.font.FontWeight.Black) },
                    text = { Text(store.storeMessage) },
                    confirmButton = {
                        TextButton(onClick = store.onStoreMessageSeen) {
                            Text("OK", color = PinkDark, fontWeight = androidx.compose.ui.text.font.FontWeight.Black)
                        }
                    }
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
            // Keep the last reward around so the toast can animate *out* after the event clears.
            var shownReward by remember { mutableStateOf<Reward?>(null) }
            LaunchedEffect(event.xpToast) {
                val reward = event.xpToast ?: return@LaunchedEffect
                shownReward = reward
                kotlinx.coroutines.delay(1500)
                if (viewModel.event.value.leveledUpTo == null) viewModel.clearXpToast()
            }
            // Confetti for habits, a shower of coins for coin-only rewards (ads).
            ConfettiBurst(
                trigger = event.xpToast,
                colors = if ((event.xpToast?.xp ?: 1) > 0) com.vivichi.app.ui.components.ConfettiColors else com.vivichi.app.ui.components.CoinColors,
                coins = (event.xpToast?.xp ?: 1) == 0,
                count = if ((event.xpToast?.xp ?: 1) > 0) 60 else 36,
                origin = androidx.compose.ui.geometry.Offset(0.5f, 0.5f)
            )
            AnimatedVisibility(
                visible = event.xpToast != null,
                enter = scaleIn(spring(dampingRatio = 0.45f, stiffness = 400f), initialScale = 0.4f) + fadeIn(tween(150)),
                exit = fadeOut(tween(250)) + slideOutVertically(tween(300)) { -it * 2 },
                modifier = Modifier.fillMaxSize()
            ) {
                Box(Modifier.fillMaxSize()) {
                    shownReward?.let { XpToast(xp = it.xp, intensity = it.intensity, coins = it.coins) }
                }
            }
        }
    }
    }
}

@Composable
private fun VivichiBottomBar(current: Tab, onSelect: (Tab) -> Unit) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp,
        modifier = Modifier
            .shadow(16.dp, androidx.compose.foundation.shape.RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), clip = false, ambientColor = Color(0x553D2E4E), spotColor = Color(0x553D2E4E))
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
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
                icon = {
                    // Selected icon hops up and settles with a little overshoot.
                    val selected = current == t
                    val lift by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = 0.4f, stiffness = 500f), label = "navLift")
                    Icon(
                        icons[t]!!,
                        contentDescription = t.label,
                        modifier = Modifier.graphicsLayer {
                            val s = 1f + 0.18f * lift
                            scaleX = s; scaleY = s
                            translationY = -3.dp.toPx() * lift
                        }
                    )
                },
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
