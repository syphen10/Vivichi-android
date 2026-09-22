package com.vivichi.app.monetize

import android.os.Handler
import android.os.Looper
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.vivichi.app.BuildConfig

/**
 * Anchored adaptive banner shown above the tab bar for non-Premium users. It takes no space
 * until an ad has actually loaded, so a failed or slow load never leaves an empty grey strip.
 * The AdView refreshes itself on the interval set in AdMob; after a failed load we retry.
 */
private const val FIRST_LOAD_DELAY_MS = 5_000L

@Composable
fun BottomBannerAd(modifier: Modifier = Modifier) {
    // Consent + SDK init happen in RewardedAds.start(); never request a banner before that.
    val sdkReady by RewardedAds.sdkReady.collectAsState()
    if (!sdkReady) return

    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    val adSize = remember(widthDp) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp) }
    var loaded by remember { mutableStateOf(false) }

    val adView = remember(adSize) {
        val handler = Handler(Looper.getMainLooper())
        AdView(context).apply {
            adUnitId = BuildConfig.BANNER_UNIT_ID
            setAdSize(adSize)
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    loaded = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    // Only the first load needs a manual retry; once shown, the SDK refreshes.
                    if (!loaded) handler.postDelayed({ loadAd(AdRequest.Builder().build()) }, 60_000L)
                }
            }
            // Let the app finish opening first: decoding the ad response runs on the main thread
            // inside Google's SDK, and landing mid-startup made launch stutter on slow phones.
            handler.postDelayed({ loadAd(AdRequest.Builder().build()) }, FIRST_LOAD_DELAY_MS)
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, adView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> adView.resume()
                Lifecycle.Event.ON_PAUSE -> adView.pause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }

    Column(modifier.fillMaxWidth().animateContentSize()) {
        if (loaded) {
            // Hairline divider + a little breathing room so the ad isn't flush against the
            // tab buttons (AdMob policy discourages placements that invite accidental taps).
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x1A3D2E4E)))
        }
        AndroidView(
            factory = { adView },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .height(if (loaded) adSize.height.dp + 4.dp else 0.dp)
        )
    }
}
