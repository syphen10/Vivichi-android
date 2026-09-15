package com.vivichi.app.monetize

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.vivichi.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Rewarded ads only — nothing ever interrupts the user uninvited. One ad is kept preloaded so
 * the "watch for coins" button responds instantly.
 *
 * Consent comes first: Google's UMP form is shown to users in regions that require it
 * (EEA/UK/Switzerland) and the SDK isn't initialised until ads may be requested.
 */
object RewardedAds {

    private var rewarded: RewardedAd? = null
    private var loading = false
    private val sdkStarted = AtomicBoolean(false)

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    /** True where the user must be able to revisit their ad-consent choice (shown in Settings). */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    fun start(activity: Activity) {
        val consent = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()
        consent.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                    updatePrivacyFlag(consent)
                    if (consent.canRequestAds()) startSdk(activity.applicationContext)
                }
            },
            { _ ->
                // Offline or misconfigured: fall back to whatever consent was stored last time.
                if (consent.canRequestAds()) startSdk(activity.applicationContext)
            }
        )
        // A previous session's consent is already valid — don't wait for the network round trip.
        if (consent.canRequestAds()) startSdk(activity.applicationContext)
    }

    private fun updatePrivacyFlag(consent: ConsentInformation) {
        _privacyOptionsRequired.value =
            consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ -> }
    }

    private fun startSdk(context: Context) {
        if (!sdkStarted.compareAndSet(false, true)) return
        MobileAds.initialize(context) { load(context) }
    }

    private fun load(context: Context) {
        if (loading || rewarded != null || !sdkStarted.get()) return
        loading = true
        RewardedAd.load(
            context,
            BuildConfig.REWARDED_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewarded = ad
                    loading = false
                    retryDelayMs = FIRST_RETRY_MS
                    _ready.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewarded = null
                    loading = false
                    _ready.value = false
                    // No fill or no network: try again later with backoff, otherwise the button
                    // would sit on "Loading ad…" until the app is restarted.
                    val delay = retryDelayMs
                    retryDelayMs = (retryDelayMs * 2).coerceAtMost(MAX_RETRY_MS)
                    mainHandler.postDelayed({ load(context) }, delay)
                }
            }
        )
    }

    /** Call when the user is likely to want an ad soon (e.g. app resumed); no-op if one is ready. */
    fun preload(context: Context) {
        if (rewarded == null && !loading && sdkStarted.get()) {
            mainHandler.removeCallbacksAndMessages(null)
            retryDelayMs = FIRST_RETRY_MS
            load(context.applicationContext)
        }
    }

    private const val FIRST_RETRY_MS = 15_000L
    private const val MAX_RETRY_MS = 120_000L
    private var retryDelayMs = FIRST_RETRY_MS
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    /**
     * Shows the preloaded ad. [onReward] runs after the ad closes, and only if it was watched
     * to the point Google counts as earned; [onUnavailable] runs if there was nothing to show.
     */
    fun show(activity: Activity, onReward: () -> Unit, onUnavailable: () -> Unit) {
        val ad = rewarded
        if (ad == null) {
            onUnavailable()
            load(activity.applicationContext)
            return
        }
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                consumed(activity)
                if (earned) onReward()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                consumed(activity)
                onUnavailable()
            }
        }
        ad.show(activity) { earned = true }
    }

    private fun consumed(activity: Activity) {
        rewarded = null
        _ready.value = false
        load(activity.applicationContext)
    }
}
