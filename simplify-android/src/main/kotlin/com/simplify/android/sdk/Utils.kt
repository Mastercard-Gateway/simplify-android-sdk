package com.simplify.android.sdk

import java.net.URLEncoder

fun <T> lazyAndroid(initializer: () -> T): Lazy<T> = lazy(LazyThreadSafetyMode.NONE, initializer)

fun String.urlEncode(): String {
    return URLEncoder.encode(this, Charsets.UTF_8.name())
}