package com.simplify.android.sdk

class SimplifyException(message: String?, val statusCode: Int, val errorResponse: SimplifyMap) : Exception(message)