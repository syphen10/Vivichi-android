package com.vivichi.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.vivichi.app.notify.ReminderScheduler
import com.vivichi.app.ui.VivichiApp
import com.vivichi.app.ui.ViewModelFactory
import com.vivichi.app.ui.VivichiViewModel
import com.vivichi.app.ui.theme.VivichiTheme
import com.vivichi.app.util.SoundFx

class MainActivity : ComponentActivity() {

    private val viewModel: VivichiViewModel by viewModels {
        val app = application as VivichiApplication
        ViewModelFactory(app.repository, ReminderScheduler(this))
    }

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

        val notifLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

        setContent {
            VivichiTheme {
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                VivichiApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkMissedYou()
    }
}
