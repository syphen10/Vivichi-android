package com.vivichi.app.monetize

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Plain-language record of what the ads SDK last did, for the hidden diagnostics sheet in
 * More → "Ad status" (tap the Premium & Ads heading 7 times).
 *
 * Ads can silently show nothing for reasons that live entirely on Google's side — a brand new
 * ad unit with no fill yet, an AdMob app still awaiting review, or consent never granted — and
 * all of them look identical from the outside: no banner. This turns that into a readable
 * answer without needing a USB cable and logcat.
 */
object AdDiagnostics {
    var consent: String by mutableStateOf("not asked yet")
    var canRequestAds: Boolean? by mutableStateOf(null)
    var sdkStarted: Boolean by mutableStateOf(false)
    var sdkReady: Boolean by mutableStateOf(false)
    var adapters: String by mutableStateOf("—")

    var bannerRequested: Int by mutableStateOf(0)
    var bannerLoaded: Boolean by mutableStateOf(false)
    var bannerError: String? by mutableStateOf(null)

    var rewardedRequested: Int by mutableStateOf(0)
    var rewardedLoaded: Boolean by mutableStateOf(false)
    var rewardedError: String? by mutableStateOf(null)

    /** Google's numeric codes mean little on their own; say what each one actually implies. */
    fun describe(code: Int, message: String): String {
        val plain = when (code) {
            0 -> "Internal error at Google's end"
            1 -> "Invalid request — the ad unit ID or app ID doesn't match this app in AdMob"
            2 -> "Network error — no connection"
            3 -> "No ad available to show (no fill). Usual for a new ad unit, or one whose AdMob app isn't approved/ready yet"
            8 -> "App ID missing from the manifest"
            9 -> "Mediation returned nothing"
            else -> "Code $code"
        }
        return "$plain\n[$code] $message"
    }

    fun summary(): String = buildString {
        appendLine("Consent: $consent")
        appendLine("Ads allowed: ${canRequestAds ?: "unknown"}")
        appendLine("SDK started: $sdkStarted    ready: $sdkReady")
        appendLine("Adapters: $adapters")
        appendLine()
        appendLine("BANNER  requests: $bannerRequested   loaded: $bannerLoaded")
        appendLine(bannerError?.let { "Last error:\n$it" } ?: "No error reported")
        appendLine()
        appendLine("REWARDED  requests: $rewardedRequested   loaded: $rewardedLoaded")
        appendLine(rewardedError?.let { "Last error:\n$it" } ?: "No error reported")
    }
}
