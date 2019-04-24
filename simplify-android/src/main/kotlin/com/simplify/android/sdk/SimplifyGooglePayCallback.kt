package com.simplify.android.sdk


import com.google.android.gms.common.api.Status
import com.google.android.gms.wallet.PaymentData

import org.json.JSONObject

interface SimplifyGooglePayCallback {

    /**
     * Called when payment data is returned from GooglePay
     *
     * @param paymentData An object containing details about the payment
     * @see [PaymentData](https://developers.google.com/pay/api/android/reference/object.PaymentData)
     */
    fun onReceivedPaymentData(paymentData: PaymentData)

    /**
     * Called when a user cancels a GooglePay transaction
     */
    fun onGooglePayCancelled()

    /**
     * Called when an error occurs during a GooglePay transaction
     *
     * @param status The corresponding status object of the request
     */
    fun onGooglePayError(status: Status)
}
