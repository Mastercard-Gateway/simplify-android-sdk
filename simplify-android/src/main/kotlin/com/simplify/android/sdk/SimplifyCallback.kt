package com.simplify.android.sdk

interface SimplifyCallback {

    /**
     * Callback on a successful call to the Simplify API
     *
     * @param response A response map
     */
    fun onSuccess(response: SimplifyMap)

    /**
     * Callback executed when error thrown during call to Simplify API
     *
     * @param throwable The exception thrown
     */
    fun onError(throwable: Throwable)
}