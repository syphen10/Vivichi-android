package com.vivichi.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.graphics.Color
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import android.content.Intent
import com.vivichi.app.domain.GameLogic
import com.vivichi.app.monetize.PremiumBilling
import com.vivichi.app.monetize.RewardedAds
import com.vivichi.app.notify.ReminderScheduler
import com.vivichi.app.showcase.ShowcaseLoader
import com.vivichi.app.ui.StoreHooks
import com.vivichi.app.ui.VivichiApp
import com.vivichi.app.ui.ViewModelFactory
import com.vivichi.app.ui.VivichiViewModel
import com.vivichi.app.ui.theme.VivichiTheme
import com.vivichi.app.util.SoundFx
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: VivichiViewModel by viewModels {
        val app = application as VivichiApplication
        ViewModelFactory(app.repository, ReminderScheduler(this))
    }

    private lateinit var billing: PremiumBilling

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        // targetSdk 35+ forces edge-to-edge on Android 15+ anyway; enabling it explicitly makes
        // older versions behave the same. Light style = dark status/nav icons over our pastel UI.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { !viewModel.isReady.value }
        SoundFx.init(this)
        ShowcaseLoader.maybeLoad(intent, viewModel)

        billing = PremiumBilling(this) { owned -> viewModel.setPremium(owned) }
        lifecycleScope.launch {
            billing.messages.collect { Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show() }
        }

        // Premium users never see ads, so don't even start the ad SDK (or its consent form) for them.
        // Onboarding is finished first so the consent form doesn't land on top of the welcome flow.
        lifecycleScope.launch {
            viewModel.isReady.first { it }
            val s = viewModel.state.first { it.onboarded }
            if (!s.premium) RewardedAds.start(this@MainActivity)
        }

        val notifLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

        setContent {
            VivichiTheme {
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                val price by billing.price.collectAsState()
                val adReady by RewardedAds.ready.collectAsState()
                val privacyRequired by RewardedAds.privacyOptionsRequired.collectAsState()
                VivichiApp(
                    viewModel = viewModel,
                    store = StoreHooks(
                        premiumPrice = price,
                        adReady = adReady,
                        onBuyPremium = { billing.buy(this) },
                        onRestorePremium = { billing.restore() },
                        onWatchAd = ::watchAd,
                        onAdPrivacy = if (privacyRequired) ({ RewardedAds.showPrivacyOptions(this) }) else null
                    )
                )
            }
        }
    }

    private fun watchAd() {
        if (GameLogic.adsLeftToday(viewModel.state.value) <= 0) return
        RewardedAds.show(
            this,
            onReward = { viewModel.rewardAdWatched() },
            onUnavailable = {
                Toast.makeText(this, "No ad available right now — try again in a minute.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        ShowcaseLoader.maybeLoad(intent, viewModel)
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkMissedYou()
        // Picks up purchases completed elsewhere (e.g. a pending payment that just cleared).
        billing.refresh()
    }

    override fun onDestroy() {
        billing.endConnection()
        super.onDestroy()
    }
}
