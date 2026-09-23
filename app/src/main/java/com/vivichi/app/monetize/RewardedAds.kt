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
 * Rewarded ads only — nothing ever interrupts the user uninvited. An ad is fetched when a
 * "watch for coins" button comes on screen, so it's usually ready by the time it's tapped.
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
                    note(consent)
                    if (consent.canRequestAds()) startSdk(activity.applicationContext)
                }
            },
            { error ->
                // Offline or misconfigured: fall back to whatever consent was stored last time.
                AdDiagnostics.consent = "lookup failed: ${error.message}"
                AdDiagnostics.canRequestAds = consent.canRequestAds()
                if (consent.canRequestAds()) startSdk(activity.applicationContext)
            }
        )
        note(consent)
        // A previous session's consent is already valid — don't wait for the network round trip.
        if (consent.canRequestAds()) startSdk(activity.applicationContext)
    }

    private fun note(consent: ConsentInformation) {
        AdDiagnostics.consent = when (consent.consentStatus) {
            ConsentInformation.ConsentStatus.NOT_REQUIRED -> "not required here"
            ConsentInformation.ConsentStatus.OBTAINED -> "obtained"
            ConsentInformation.ConsentStatus.REQUIRED -> "required, not given yet"
            else -> "unknown"
        }
        AdDiagnostics.canRequestAds = consent.canRequestAds()
    }

    private fun updatePrivacyFlag(consent: ConsentInformation) {
        _privacyOptionsRequired.value =
            consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ -> }
    }

    private val _sdkReady = MutableStateFlow(false)
    /** True once consent allows ads and the SDK has initialised — banners wait for this too. */
    val sdkReady: StateFlow<Boolean> = _sdkReady.asStateFlow()

    private fun startSdk(context: Context) {
        if (!sdkStarted.compareAndSet(false, true)) return
        // Google recommends initializing off the main thread; the completion callback still
        // arrives on the main thread, where loading must start.
        AdDiagnostics.sdkStarted = true
        Thread {
            MobileAds.initialize(context) { status ->
                AdDiagnostics.sdkReady = true
                AdDiagnostics.adapters = status.adapterStatusMap.entries.joinToString {
                    "${it.key.substringAfterLast('.')}=${it.value.initializationState}"
                }
                _sdkReady.value = true
                // No load here. The ads SDK decodes each ad response on the main thread, which
                // froze budget phones for seconds at a time; a rewarded ad is only fetched once
                // a "Watch ad" button is actually on screen (see [offerShown]).
                if (visibleOffers > 0) load(context)
            }
        }.start()
    }

    /** How many "Watch ad" buttons are currently on screen. */
    private var visibleOffers = 0

    /** A "Watch ad" button appeared: fetch an ad now so it's ready by the time it's tapped. */
    fun offerShown(context: Context) {
        visibleOffers++
        retriesLeft = MAX_RETRIES
        load(context.applicationContext)
    }

    /** That button left the screen. Pending retries stop once nothing is asking for an ad. */
    fun offerHidden() {
        visibleOffers = (visibleOffers - 1).coerceAtLeast(0)
        if (visibleOffers == 0) mainHandler.removeCallbacksAndMessages(null)
    }

    private fun load(context: Context) {
        if (loading || rewarded != null || !sdkStarted.get()) return
        loading = true
        AdDiagnostics.rewardedRequested++
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
                    AdDiagnostics.rewardedLoaded = true
                    AdDiagnostics.rewardedError = null
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewarded = null
                    loading = false
                    _ready.value = false
                    AdDiagnostics.rewardedError = AdDiagnostics.describe(error.code, error.message)
                    // No fill or no network: retry a couple of times with backoff, but only
                    // while a "Watch ad" button is still showing. Endless background retries
                    // meant endless main-thread ad work on slow phones.
                    if (visibleOffers > 0 && retriesLeft > 0) {
                        retriesLeft--
                        val delay = retryDelayMs
                        retryDelayMs = (retryDelayMs * 2).coerceAtMost(MAX_RETRY_MS)
                        mainHandler.postDelayed({ load(context) }, delay)
                    }
                }
            }
        )
    }

    private const val FIRST_RETRY_MS = 15_000L
    private const val MAX_RETRY_MS = 60_000L
    private const val MAX_RETRIES = 2
    private var retryDelayMs = FIRST_RETRY_MS
    private var retriesLeft = MAX_RETRIES
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
        // Line up the next one only if a "Watch ad" button is still showing (e.g. the coins dialog).
        if (visibleOffers > 0) {
            retriesLeft = MAX_RETRIES
            load(activity.applicationContext)
        }
    }
}
