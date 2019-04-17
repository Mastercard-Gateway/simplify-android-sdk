package com.simplify.android.sdk

internal class SimplifyRequest(val url: String, val method: Method, val payload: SimplifyMap, val headers: MutableMap<String, String> = mutableMapOf()) {

    // internally supported request methods
    internal enum class Method {
        POST
    }
}