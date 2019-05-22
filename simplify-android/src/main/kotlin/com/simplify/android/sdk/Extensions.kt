package com.simplify.android.sdk

import java.io.InputStream
import java.net.URLEncoder
import java.nio.charset.Charset

fun <T> lazyAndroid(initializer: () -> T): Lazy<T> = lazy(LazyThreadSafetyMode.NONE, initializer)

fun String.urlEncode(): String {
    return URLEncoder.encode(this, Charsets.UTF_8.name())
}

fun InputStream.readTextAndClose(charset: Charset = Charsets.UTF_8): String {
    return this.bufferedReader(charset).use { it.readText() }
}