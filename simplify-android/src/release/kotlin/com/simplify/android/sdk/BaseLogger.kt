package com.simplify.android.sdk


import java.net.HttpURLConnection


internal class BaseLogger : Logger {

    override fun logRequest(c: HttpURLConnection, data: String?) {
        // no-op
    }

    override fun logResponse(c: HttpURLConnection, data: String?) {
        // no-op
    }

    override fun logDebug(message: String) {
        // no-op
    }
}
