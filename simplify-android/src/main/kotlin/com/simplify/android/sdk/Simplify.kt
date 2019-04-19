package com.simplify.android.sdk

import android.app.Activity
import android.content.Intent
import android.util.Base64
import androidx.annotation.VisibleForTesting
import com.google.android.gms.wallet.FullWallet
import com.google.android.gms.wallet.MaskedWallet
import com.google.android.gms.wallet.WalletConstants
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.reactivex.Single
import org.json.JSONObject
import java.util.*
import java.util.regex.Pattern


/**
 *
 * The public interface to the Simplify SDK
 *
 * Example set up:
 * <br></br>
 * `Simplify simplify = new Simplify();
 * simplify.setApiKey("YOUR_SIMPLIFY_PUBLIC_API_KEY");
 * simplify.setAndroidPayPublicKey("YOUR_ANDROID_PAY_PUBLIC_KEY");`
 *
 */
/**
 * Creates an instance of the Simplify object
 */
class Simplify {

    /**
     * Initializes the SDK with an Simplify public API key
     */
    var apiKey: String? = null
        set(value) {
            if (!validateApiKey(value)) {
                throw IllegalArgumentException("Invalid api key")
            }
            field = value
        }

    /**
     * Initializes the SDK with the Android Pay public key provided by Simplify.
     * <br/>
     * This value is optional and is used internally to encrypt card information during an Android Pay transaction.
     * It can be retrieved from your Simplify account settings and is different from your API key.
     */
    var androidPayPublicKey: String? = null

    @VisibleForTesting
    internal var comms = SimplifyComms()

    private val url: String
        get() = if (isLive) API_BASE_LIVE_URL else API_BASE_SANDBOX_URL

    private val isLive: Boolean
        get() = apiKey != null && apiKey!!.startsWith(LIVE_KEY_PREFIX)

    /**
     *
     * Performs an asynchronous request to the Simplify server to retrieve a card token
     * that can then be used to process a payment.
     * <br></br>Includes optional 3DS request data required to initiate a 3DS authentication process.
     *
     * @param card                A valid card object
     * @param secure3DRequestData Data required to initiate 3DS authentication. may be null
     * @param callback            The callback to invoke after the request is complete
     */
    @JvmOverloads
    fun createCardToken(card: SimplifyMap, secure3DRequestData: SimplifyMap? = null, callback: SimplifyCallback) {
        val request = buildCreateCardTokenRequest(card, secure3DRequestData)
        comms.runSimplifyRequest(request, callback)
    }

    /**
     *
     * Builds a Single to retrieve a card token that can then be used to process a payment
     * <br></br>Includes 3DS request data required to initiate a 3DS authentication process.
     *
     *
     * Does not operate on any particular scheduler
     *
     * @param card                A valid card object
     * @param secure3DRequestData Data requires to initiate 3DS authentication
     * @return A Single of a SimplifyMap containing card token information
     */
    @JvmOverloads
    fun createCardToken(card: SimplifyMap, secure3DRequestData: SimplifyMap? = null): Single<SimplifyMap> {
        val request = buildCreateCardTokenRequest(card, secure3DRequestData)
        return comms.runSimplifyRequest(request)
    }

    /**
     * Performs an asynchronous request to the Simplify server to retrieve a card token
     * that can then be used to process a payment
     *
     * @param fullWallet A full wallet response from Android Pay
     * @param callback   The callback to invoke after the request is complete
     */
    fun createAndroidPayCardToken(fullWallet: FullWallet, callback: SimplifyCallback) {
        createCardToken(buildAndroidPayCard(fullWallet), callback = callback)
    }

    /**
     *
     * Builds a Single to retrieve a card token that can then be used to process a payment
     *
     * Does not operate on any particular scheduler
     *
     * @param fullWallet A full wallet response from Android Pay
     * @return A Single of a SimplifyMap containing card token information
     */
    fun createAndroidPayCardToken(fullWallet: FullWallet): Single<SimplifyMap> {
        return createCardToken(buildAndroidPayCard(fullWallet))
    }

    private fun validateApiKey(apiKey: String?): Boolean {
        val m = Pattern.compile(PATTERN_API_KEY).matcher(apiKey)
        return if (m.matches()) {
            try {
                // parse UUID
                val uuidStr = String(Base64.decode(m.group(1), Base64.NO_PADDING))
                UUID.fromString(uuidStr)
                true
            } catch (e: Exception) {
                false
            }
        } else false
    }

    private fun buildCreateCardTokenRequest(card: SimplifyMap, secure3DRequestData: SimplifyMap?): SimplifyRequest {
        val payload = SimplifyMap()
                .set("key", apiKey)
                .set("card", card)

        secure3DRequestData?.let {
            payload.set("secure3DRequestData", it)
        }

        return SimplifyRequest(
                url = url + API_PATH_CARDTOKEN,
                method = SimplifyRequest.Method.POST,
                payload = payload
        )
    }

    private fun buildAndroidPayCard(fullWallet: FullWallet): SimplifyMap {
        return fullWallet.buyerBillingAddress.let { address ->
            SimplifyMap()
                    .set("cardEntryMode", "ANDROID_PAY_IN_APP")
                    .set("androidPayData", SimplifyMap()
                            .set("publicKey", androidPayPublicKey)
                            .set("paymentToken", SimplifyMap(fullWallet.paymentMethodToken.token)))
                    .set("addressLine1", address.address1)
                    .set("addressLine2", address.address2)
                    .set("addressCity", address.locality)
                    .set("addressState", address.administrativeArea)
                    .set("addressZip", address.postalCode)
                    .set("addressCountry", address.countryCode)
                    .set("customer.name", address.name)
                    .set("customer.email", fullWallet.email)
        }
    }

    companion object {

        /**
         * Request code to use with WalletFragment when requesting a [MaskedWallet]
         * <br/>
         * **required if using [.handleAndroidPayResult]**
         */
        const val REQUEST_CODE_MASKED_WALLET = 1000

        /**
         * Request code to use with WalletFragment when requesting a [FullWallet]
         * <br/>
         * **required if using [.handleAndroidPayResult]**
         */
        const val REQUEST_CODE_FULL_WALLET = 1001

        /**
         * A request code to use when launching the 3DS activity for result.
         * <br>
         * Will be used when calling the convenience methods [.start3DSActivity]
         * and [.handle3DSResult]
         */
        internal const val REQUEST_3DS = 1002


        //        internal const val API_BASE_LIVE_URL = "https://api.simplify.com/v1/api"
//        internal const val API_BASE_SANDBOX_URL = "https://sandbox.simplify.com/v1/api"
        internal const val API_BASE_LIVE_URL = "https://api.uat.simplify.com/v1/api";
        internal const val API_BASE_SANDBOX_URL = "https://sandbox.uat.simplify.com/v1/api";
        internal const val API_PATH_CARDTOKEN = "/payment/cardToken"
        internal const val PATTERN_API_KEY = "(?:lv|sb)pb_(.+)"
        internal const val LIVE_KEY_PREFIX = "lvpb_"


        /**
         *
         * Convenience method to handle the Android Pay transaction lifecycle. You may use this method within
         * onActivityResult() to delegate the transaction to an [SimplifyAndroidPayCallback].
         * <pre>`
         * protected void onActivityResult(int requestCode, int resultCode, Intent data) {
         *
         * if (Simplify.handleAndroidPayResult(requestCode, resultCode, data, this)) {
         * return;
         * }
         *
         * // ...
         * }`</pre>
         *
         * @param requestCode The activity request code
         * @param resultCode  The activity result code
         * @param data        Data returned by activity
         * @param callback    The [SimplifyAndroidPayCallback]
         * @return True if handled, False otherwise
         * @throws IllegalArgumentException If callback is null
         */
        @JvmStatic
        fun handleAndroidPayResult(requestCode: Int, resultCode: Int, data: Intent, callback: SimplifyAndroidPayCallback): Boolean {
            return when(requestCode) {
                REQUEST_CODE_MASKED_WALLET, REQUEST_CODE_FULL_WALLET -> {
                    when (resultCode) {
                        Activity.RESULT_OK -> when (requestCode) {
                            REQUEST_CODE_MASKED_WALLET -> callback.onReceivedMaskedWallet(data.getParcelableExtra(WalletConstants.EXTRA_MASKED_WALLET))
                            REQUEST_CODE_FULL_WALLET -> callback.onReceivedFullWallet(data.getParcelableExtra(WalletConstants.EXTRA_FULL_WALLET))
                        }
                        Activity.RESULT_CANCELED -> callback.onAndroidPayCancelled()
                        else -> callback.onAndroidPayError(data.getIntExtra(WalletConstants.EXTRA_ERROR_CODE, -1))
                    }

                    true
                }
                else -> false
            }
        }

        /**
         * Starts the [Simplify3DSecureActivity] for result, and initializes it with the required 3DS info
         *
         * @param activity  The calling activity
         * @param cardToken A map of card token data with a valid card, containing 3DS data
         * @param title     The title to display in the activity's toolbar
         * @throws IllegalArgumentException If cardToken missing 3DS data
         */
        @JvmOverloads
        @JvmStatic
        fun start3DSActivity(activity: Activity, cardToken: SimplifyMap, title: String? = null) {
            if (!cardToken.containsKey("card.secure3DData")) {
                throw IllegalArgumentException("The provided card token must contain 3DS data.")
            }

            Simplify3DSecureActivity.buildIntent(activity, cardToken, title).run {
                activity.startActivityForResult(this, REQUEST_3DS)
            }
        }

        /**
         *
         * Convenience method to handle the 3DS authentication lifecycle. You may use this method within
         * onActivityResult() to delegate the transaction to a [SimplifySecure3DCallback].
         * <pre>`
         * protected void onActivityResult(int requestCode, int resultCode, Intent data) {
         *
         * if (Simplify.handle3DSResult(requestCode, resultCode, data, this)) {
         * return;
         * }
         *
         * // ...
         * }`</pre>
         *
         * @param requestCode The activity request code
         * @param resultCode  The activity result code
         * @param data        Data returned by the activity
         * @param callback    The [SimplifySecure3DCallback]
         * @return True if handled, False otherwise
         * @throws IllegalArgumentException If callback is null
         */
        @JvmStatic
        fun handle3DSResult(requestCode: Int, resultCode: Int, data: Intent, callback: SimplifySecure3DCallback): Boolean {
            return when(requestCode) {
                REQUEST_3DS  -> {
                    when (resultCode) {
                        Activity.RESULT_OK -> {
                            try {
                                SimplifyMap(data.getStringExtra(Simplify3DSecureActivity.EXTRA_RESULT)).run {
                                    when {
                                        containsKey("secure3d.authenticated") -> callback.onSecure3DComplete(this["secure3D.authenticated"] as Boolean)
                                        containsKey("secure3D.error") -> callback.onSecure3DError(this["secure3D.error.message"] as String)
                                        else -> callback.onSecure3DError("Unknown error occurred during authentication")
                                    }
                                }
                            } catch (e: Exception) {
                                callback.onSecure3DError("Unable to read 3DS result")
                            }
                        }
                        else -> callback.onSecure3DCancel()
                    }

                    true
                }
                else -> false
            }
        }
    }
}

