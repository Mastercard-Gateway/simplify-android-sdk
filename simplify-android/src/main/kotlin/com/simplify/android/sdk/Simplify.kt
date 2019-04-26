package com.simplify.android.sdk

import android.app.Activity
import android.content.Intent
import android.util.Base64
import android.util.Log
import androidx.annotation.VisibleForTesting
import io.reactivex.Single
import java.nio.charset.Charset
import java.util.*


/**
 *
 * The public interface to the Simplify SDK
 *
 * Example set up:
 * <pre>
 * Simplify simplify = new Simplify("YOUR_SIMPLIFY_PUBLIC_API_KEY");
 * </pre>
 */
@Suppress("unused")
class Simplify(apiKey: String) {

    /**
     * The Simplify public API key
     */
    var apiKey: String = ""
        set(value) {
            if (!validateApiKey(value)) {
                throw IllegalArgumentException("Invalid api key: $value")
            }
            field = value
        }

    init {
        // keep this in init block, forces setter validation
        this.apiKey = apiKey
    }

    @VisibleForTesting
    internal var comms = SimplifyComms()

    private val url: String
        get() = if (isLive) API_BASE_LIVE_URL else API_BASE_SANDBOX_URL

    private val isLive: Boolean
        get() = apiKey.startsWith(LIVE_KEY_PREFIX)

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

    private fun validateApiKey(apiKey: String): Boolean {
        return PATTERN_API_KEY.toRegex().find(apiKey)?.groupValues?.get(1)?.let { group ->
            try {
                // parse UUID

                UUID.fromString(Base64.decode(group, Base64.DEFAULT).toString(Charset.defaultCharset()))
                true
            } catch (e: Exception) {
                Log.d("Simplify", "Error parsing API key: $group", e)
                false
            }
        } ?: false
    }

    companion object {

        @VisibleForTesting
        internal const val REQUEST_CODE_3DS = 1000

        @VisibleForTesting
        internal const val API_BASE_LIVE_URL = "https://api.simplify.com/v1/api"

        @VisibleForTesting
        internal const val API_BASE_SANDBOX_URL = "https://sandbox.simplify.com/v1/api"

        @VisibleForTesting
        internal const val API_PATH_CARDTOKEN = "/payment/cardToken"

        private const val PATTERN_API_KEY = "(?:lv|sb)pb_(.+)"
        private const val LIVE_KEY_PREFIX = "lvpb_"


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

