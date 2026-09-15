package com.vivichi.app.monetize

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.vivichi.app.BuildConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The one-time "vivichi_premium" Google Play purchase. Google Play is the source of truth:
 * every connect re-reads owned purchases, so Premium restores itself on reinstall / new phone
 * and is withdrawn if the purchase is refunded.
 *
 * [onOwnershipKnown] is only called with a definite answer from Play — never on a network or
 * connection failure, so being offline can't take Premium away.
 */
class PremiumBilling(
    context: Context,
    private val onOwnershipKnown: (Boolean) -> Unit
) : PurchasesUpdatedListener {

    private val client = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private var details: ProductDetails? = null

    private val _price = MutableStateFlow<String?>(null)
    /** Localised price from Play (e.g. "$0.99", "Rs 280"), or null until it's known. */
    val price: StateFlow<String?> = _price.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun connect(afterConnect: (() -> Unit)? = null) {
        if (client.isReady) {
            afterConnect?.invoke()
            return
        }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingResponseCode.OK) {
                    queryDetails()
                    refreshOwnership()
                    afterConnect?.invoke()
                } else if (afterConnect != null) {
                    // Only a user-initiated action (buy/restore) is worth telling them about.
                    _messages.tryEmit("Google Play isn't available on this device right now. Please try again later.")
                }
            }

            override fun onBillingServiceDisconnected() {
                // Reconnected lazily on the next buy/restore/resume.
            }
        })
    }

    fun endConnection() = client.endConnection()

    private fun queryDetails() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(BuildConfig.PREMIUM_PRODUCT_ID)
            .setProductType(ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
        client.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode == BillingResponseCode.OK) {
                details = queryResult.productDetailsList.firstOrNull()
                _price.value = details?.oneTimePurchaseOfferDetails?.formattedPrice
            }
        }
    }

    private fun refreshOwnership(announce: Boolean = false) {
        val params = QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingResponseCode.OK) {
                if (announce) _messages.tryEmit("Couldn't reach Google Play. Check your connection and try again.")
                return@queryPurchasesAsync
            }
            val premium = purchases.filter { BuildConfig.PREMIUM_PRODUCT_ID in it.products }
            premium.forEach { handle(it) }
            val owned = premium.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            onOwnershipKnown(owned)
            if (announce) {
                _messages.tryEmit(if (owned) "Premium restored — welcome back! 💎" else "No Premium purchase found on this Google account.")
            }
        }
    }

    /** Silent re-check, e.g. on resume. */
    fun refresh() {
        if (client.isReady) refreshOwnership() else connect()
    }

    fun restore() = connect { refreshOwnership(announce = true) }

    fun buy(activity: Activity) {
        connect {
            val d = details
            if (d == null) {
                // Product not set up in Play Console yet, or this build isn't installed from Play.
                queryDetails()
                _messages.tryEmit("Premium can only be bought when Vivichi is installed from Google Play. If you got it from Google Play, please try again in a moment.")
                return@connect
            }
            val params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).build())
                )
                .build()
            activity.runOnUiThread { client.launchBillingFlow(activity, params) }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> purchases.orEmpty().forEach { handle(it, fresh = true) }
            BillingResponseCode.ITEM_ALREADY_OWNED -> refreshOwnership(announce = true)
            BillingResponseCode.USER_CANCELED -> Unit
            else -> _messages.tryEmit("The purchase didn't go through. You haven't been charged.")
        }
    }

    private fun handle(purchase: Purchase, fresh: Boolean = false) {
        if (BuildConfig.PREMIUM_PRODUCT_ID !in purchase.products) return
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                onOwnershipKnown(true)
                if (fresh) _messages.tryEmit("Welcome to Premium! Everything is unlocked 💎")
                // Unacknowledged purchases are automatically refunded by Play after 3 days.
                if (!purchase.isAcknowledged) {
                    val ack = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                    client.acknowledgePurchase(ack) { }
                }
            }
            Purchase.PurchaseState.PENDING -> {
                if (fresh) _messages.tryEmit("Payment pending — Premium unlocks as soon as it completes.")
            }
            else -> Unit
        }
    }
}
