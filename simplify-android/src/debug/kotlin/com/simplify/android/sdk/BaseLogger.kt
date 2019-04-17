package com.simplify.android.sdk


import android.util.Log

import java.net.HttpURLConnection

internal class BaseLogger : Logger {

    override fun logRequest(c: HttpURLConnection, data: String?) {
        var log = "REQUEST: ${c.requestMethod} ${c.url}"

        if (data != null) {
            log += "\n-- Data: $data"
        }

        // log request headers
        c.requestProperties.keys.forEach { key ->
            c.requestProperties[key]!!.forEach { value ->
                log += "\n-- $key: $value"
            }
        }

        logMultiline(log)
    }

    override fun logResponse(c: HttpURLConnection, data: String?) {
        var log = "RESPONSE: "

        // log response headers
        var i = 0
        c.headerFields.keys.forEach { key ->
            c.headerFields[key]!!.forEach { value ->
                if (i == 0 && key == null) {
                    log += value

                    if (data != null && data.isNotEmpty()) {
                        log += "\n-- Data: $data"
                    }
                } else {
                    log += "\n-- " + (if (key == null) "" else "$key: ") + value
                }
                i++
            }
        }

        logMultiline(log)
    }

    private fun logMultiline(message: String) {
        message.split("\n".toRegex()).forEach(this::logDebug)
    }

    override fun logDebug(message: String) {
        if (message.isNotEmpty()) {
            Log.d(SimplifyKotlin::class.java.simpleName, message)
        }
    }
}
