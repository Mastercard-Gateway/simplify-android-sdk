package com.simplify.android.sdk

import com.google.android.gms.wallet.FullWallet
import com.google.android.gms.wallet.MaskedWallet

/**
 * A callback interface for handling the Android Pay transaction lifecycle.
 *
 *
 * Used in conjunction with [.handleAndroidPayResult]
 */
interface SimplifyAndroidPayCallback {

    /**
     * Called when a masked wallet is returned from AndroidPay
     *
     * @param maskedWallet A masked wallet object
     */
    fun onReceivedMaskedWallet(maskedWallet: MaskedWallet)

    /**
     * Called when a full wallet is returned from AndroidPay
     *
     * @param fullWallet A full wallet object
     */
    fun onReceivedFullWallet(fullWallet: FullWallet)

    /**
     * Called when a user cancels an AndroidPay transaction
     */
    fun onAndroidPayCancelled()

    /**
     * Called when an error occurs during an AndroidPay transaction
     *
     * @param errorCode The corresponding error code (see [com.google.android.gms.wallet.WalletConstants] for a list of supported errors)
     */
    fun onAndroidPayError(errorCode: Int)
}