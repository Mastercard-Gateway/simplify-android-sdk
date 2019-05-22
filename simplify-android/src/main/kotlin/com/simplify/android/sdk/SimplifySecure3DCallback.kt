package com.simplify.android.sdk

/**
 * A callback interface for handling the 3DS transaction lifecycle
 */
interface SimplifySecure3DCallback {

    /**
     * Called when the 3DS authentication process is complete.
     *
     * @param success True or False indicating successful authentication
     */
    fun onSecure3DComplete(success: Boolean)

    /**
     * Called when encountering an error during the 3DS process.
     *
     * @param message A description of the error
     */
    fun onSecure3DError(message: String)

    /**
     * Called when a user cancels the 3DS authentication process
     */
    fun onSecure3DCancel()
}