package com.simplify.android.sdk

import android.app.Activity
import android.content.Intent
import android.util.Base64
import androidx.annotation.VisibleForTesting
import com.google.android.gms.common.api.Status
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import io.reactivex.Single
import java.nio.charset.Charset
import java.util.*


/**
 *
 * The public interface to the Simplify SDK
 *
 * Example set up:
 * <br>
 * <pre>
 * Simplify simplify = new Simplify();
 * simplify.setApiKey("YOUR_SIMPLIFY_PUBLIC_API_KEY");
 * simplify.setGooglePayPublicKey("YOUR_ANDROID_PAY_PUBLIC_KEY");
 * </pre>
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
     * Initializes the SDK with the Google Pay public key provided by Simplify.
     * <br>This value is optional and is used internally to encrypt card information during an Android Pay transaction.
     * It can be retrieved from your Simplify account settings and is different from your API key.
     */
    var googlePayPublicKey: String? = null

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
     * <br>Includes optional 3DS request data required to initiate a 3DS authentication process.
     *
     * @param card                A valid card object
     * @param secure3DRequestData Data required to initiate 3DS authentication. may be null
     * @param callback            The callback to invoke after the request is complete
     */
    @JvmOverloads
    fun createCardToken(card: SimplifyMap, secure3DRequestData: SimplifyMap? = null, callback: SimplifyCallback) =
            buildCreateCardTokenRequest(card, secure3DRequestData).run { comms.runSimplifyRequest(this, callback) }

    /**
     *
     * Builds a Single to retrieve a card token that can then be used to process a payment
     * <br>Includes optional 3DS request data required to initiate a 3DS authentication process.
     * <br>Does not operate on any particular scheduler
     *
     * @param card                A valid card object
     * @param secure3DRequestData Data requires to initiate 3DS authentication
     * @return A Single of a SimplifyMap containing card token information
     */
    @JvmOverloads
    fun createCardToken(card: SimplifyMap, secure3DRequestData: SimplifyMap? = null): Single<SimplifyMap> =
            buildCreateCardTokenRequest(card, secure3DRequestData).run(comms::runSimplifyRequest)

    /**
     * Performs an asynchronous request to the Simplify server to retrieve a card token
     * that can then be used to process a payment
     *
     * @param paymentData A [PaymentData] object retrieved from the Google Pay API
     * @param callback   The callback to invoke after the request is complete
     */
    internal fun createGooglePayCardToken(paymentData: PaymentData, callback: SimplifyCallback) =
            createCardToken(buildGooglePayCard(paymentData), callback = callback)

    /**
     * Builds a Single to retrieve a card token that can then be used to process a payment
     * <br>Does not operate on any particular scheduler
     *
     * @param paymentData A [PaymentData] object retrieved from the Google Pay API
     * @return A Single of a SimplifyMap containing card token information
     */
    internal fun createGooglePayCardToken(paymentData: PaymentData): Single<SimplifyMap> =
            createCardToken(buildGooglePayCard(paymentData))


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

    private fun buildGooglePayCard(paymentData: PaymentData): SimplifyMap {
        val paymentDataJson = paymentData.toJson()
        val paymentDataMap = SimplifyMap(paymentDataJson)

        // nested json string
        val token = SimplifyMap(paymentDataMap["paymentMethodData.tokenizationData.token"] as String)

        // another nested json string
        val signedMessage = SimplifyMap(token["signedMessage"] as String)

        return SimplifyMap()
                .set("cardEntryMode", "ANDROID_PAY_IN_APP")
                .set("androidPayData", SimplifyMap()
                        .set("publicKey", googlePayPublicKey)
                        .set("paymentToken", signedMessage))
    }

    private fun validateApiKey(apiKey: String?): Boolean {
        return apiKey?.let {
            PATTERN_API_KEY.toRegex().find(it)?.groupValues?.get(1)?.let { group ->
                try {
                    // parse UUID
                    UUID.fromString(Base64.decode(group, Base64.NO_PADDING).toString(Charset.defaultCharset()))
                    true
                } catch (e: Exception) {
                    false
                }
            }
        } ?: false
    }

    companion object {

        @VisibleForTesting
        internal const val REQUEST_CODE_3DS = 1000

        @VisibleForTesting
        internal const val REQUEST_CODE_GOOGLE_PAY_LOAD_PAYMENT_DATA = 1001

        @VisibleForTesting
        internal const val API_BASE_LIVE_URL = "https://api.simplify.com/v1/api"

        @VisibleForTesting
        internal const val API_BASE_SANDBOX_URL = "https://sandbox.simplify.com/v1/api"

        //        @VisibleForTesting internal const val API_BASE_LIVE_URL = "https://api.uat.simplify.com/v1/api";
//        @VisibleForTesting internal const val API_BASE_SANDBOX_URL = "https://sandbox.uat.simplify.com/v1/api";

        @VisibleForTesting
        internal const val API_PATH_CARDTOKEN = "/payment/cardToken"

        private const val PATTERN_API_KEY = "(?:lv|sb)pb_(.+)"
        private const val LIVE_KEY_PREFIX = "lvpb_"


        /**
         * A convenience method for initializing the request to get Google Pay card info
         *
         * @param paymentsClient An instance of the PaymentClient
         * @param request A properly formatted PaymentDataRequest
         * @param activity The calling activity
         * @see [Payments Client](https://developers.google.com/pay/api/android/guides/tutorial.paymentsclient)
         * @see [Payment Data Request](https://developers.google.com/pay/api/android/guides/tutorial.paymentdatarequest)
         */
        @JvmStatic
        internal fun requestGooglePayData(paymentsClient: PaymentsClient, request: PaymentDataRequest, activity: Activity) {
            AutoResolveHelper.resolveTask(paymentsClient.loadPaymentData(request), activity, REQUEST_CODE_GOOGLE_PAY_LOAD_PAYMENT_DATA)
        }

        /**
         * A convenience method for handling activity result messages returned from Google Pay.
         * This method should be called withing the calling Activity's onActivityResult() lifecycle method.
         * This helper only works if the Google Pay dialog was launched using the
         * [Simplify.requestGooglePayData] method.
         *
         * @param requestCode The request code returning from the activity result
         * @param resultCode The result code returning from the activity result
         * @param data The intent data returning from the activity result
         * @param callback An implementation of [SimplifyGooglePayCallback]
         * @return True if handled, False otherwise
         * @see Simplify.requestGooglePayData
         */
        @JvmStatic
        internal fun handleGooglePayResult(requestCode: Int, resultCode: Int, data: Intent?, callback: SimplifyGooglePayCallback): Boolean {
            return when (requestCode) {
                REQUEST_CODE_GOOGLE_PAY_LOAD_PAYMENT_DATA -> {
                    when (resultCode) {
                        Activity.RESULT_OK -> data?.run(PaymentData::getFromIntent)?.run(callback::onReceivedPaymentData)
                                ?: callback.onGooglePayError(Status.RESULT_INTERNAL_ERROR)
                        Activity.RESULT_CANCELED -> callback.onGooglePayCancelled()
                        AutoResolveHelper.RESULT_ERROR -> AutoResolveHelper.getStatusFromIntent(data)?.run(callback::onGooglePayError)
                    }

                    true
                }
                else -> false
            }
        }

        /**
         * Starts the [SimplifySecure3DActivity] for result, and initializes it with the required 3DS info
         *
         * @param activity  The calling activity
         * @param cardToken A map of card token data with a valid card, containing 3DS data
         * @param title     The title to display in the activity's toolbar
         * @throws IllegalArgumentException If cardToken missing 3DS data
         */
        @JvmOverloads
        @JvmStatic
        fun start3DSActivity(activity: Activity, cardToken: SimplifyMap, title: String? = null) {
            cardToken["card.secure3DData"]?.run {
                SimplifySecure3DActivity.buildIntent(activity, cardToken, title).run {
                    activity.startActivityForResult(this, REQUEST_CODE_3DS)
                }
            } ?: throw IllegalArgumentException("The provided card token must contain 3DS data.")
        }

        /**
         *
         * Convenience method to handle the 3DS authentication lifecycle. You may use this method within
         * [Activity.onActivityResult] to delegate the transaction to a [SimplifySecure3DCallback].
         * <pre>
         * protected void onActivityResult(int requestCode, int resultCode, Intent data) {
         *
         * if (Simplify.handle3DSResult(requestCode, resultCode, data, this)) {
         * return;
         * }
         *
         * // ...
         * }</pre>
         *
         * @param requestCode The activity request code
         * @param resultCode  The activity result code
         * @param data        Data returned by the activity
         * @param callback    The [SimplifySecure3DCallback]
         * @return True if handled, False otherwise
         * @throws IllegalArgumentException If callback is null
         */
        @JvmStatic
        fun handle3DSResult(requestCode: Int, resultCode: Int, data: Intent?, callback: SimplifySecure3DCallback): Boolean {
            return when (requestCode) {
                REQUEST_CODE_3DS -> {
                    when (resultCode) {
                        Activity.RESULT_OK -> {
                            try {
                                SimplifyMap(data!!.getStringExtra(SimplifySecure3DActivity.EXTRA_RESULT)).run {
                                    when {
                                        containsKey("secure3d.authenticated") -> callback.onSecure3DComplete(this["secure3d.authenticated"] as Boolean)
                                        containsKey("secure3d.error") -> callback.onSecure3DError(this["secure3d.error.message"] as String)
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

