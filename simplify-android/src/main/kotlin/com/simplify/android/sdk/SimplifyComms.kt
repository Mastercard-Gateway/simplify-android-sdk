package com.simplify.android.sdk

import android.os.Build
import android.os.Handler
import com.google.gson.GsonBuilder
import io.reactivex.Single
import java.io.ByteArrayInputStream
import java.net.URL
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

internal class SimplifyComms {

    var logger: Logger = BaseLogger()
    var gson = GsonBuilder().disableHtmlEscaping().create()

    fun runSimplifyRequest(request: SimplifyRequest, callback: SimplifyCallback) {
        // create handler on current thread
        val handler = Handler { msg -> handleCallbackMessage(callback, msg.obj) }

        Thread {
            val m = handler.obtainMessage()
            try {
                m.obj = executeSimplifyRequest(request)
            } catch (e: Exception) {
                m.obj = e
            }

            handler.sendMessage(m)
        }.start()
    }

    fun runSimplifyRequest(request: SimplifyRequest): Single<SimplifyMap> {
        return Single.fromCallable { executeSimplifyRequest(request) }
    }

    // handler callback method when executing a request on a new thread
    fun handleCallbackMessage(callback: SimplifyCallback?, arg: Any): Boolean {
        if (callback != null) {
            if (arg is Throwable) {
                callback.onError(arg)
            } else {
                callback.onSuccess(arg as SimplifyMap)
            }
        }
        return true
    }

    fun executeSimplifyRequest(request: SimplifyRequest): SimplifyMap {
        // init connection
        val c = createHttpsUrlConnection(request)

        // encode request data to json
        val requestData = gson.toJson(request.payload)

        // log request data
        logger.logRequest(c, requestData)

        // write request data
        if (requestData != null) {
            val os = c.outputStream
            os.write(requestData.toByteArray(charset("UTF-8")))
            os.close()
        }

        // initiate the connection
        c.connect()

        var responseData: String? = null
        val statusCode = c.responseCode
        val isStatusOk = isStatusCodeOk(statusCode)

        // if connection has output stream, get the data
        // socket time-out exceptions will be thrown here
        if (c.doInput) {
            val inputStream = if (isStatusOk) c.inputStream else c.errorStream
            responseData = inputStream.readTextAndClose()
        }

        c.disconnect()

        // log response
        logger.logResponse(c, responseData)

        // parse the response body
        val response = SimplifyMap(responseData)

        // if response static is good, return response
        if (isStatusOk) {
            return response
        }

        // otherwise, create a gateway exception and throw it
        val message = response["error.message"] as String? ?: "An error occurred"

        throw SimplifyException(message, statusCode, response)
    }

    fun createSslContext(): SSLContext {
        // create and initialize a KeyStore
        val keyStore = createSslKeyStore()

        // create a TrustManager that trusts the INTERMEDIATE_CA in our KeyStore
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(keyStore)

        val trustManagers = tmf.trustManagers

        val context = SSLContext.getInstance("TLSv1.2")
        context.init(null, trustManagers, null)

        return context
    }

    fun createSslKeyStore(): KeyStore {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)

        // add our trusted cert to the keystore
        keyStore.setCertificateEntry(KEYSTORE_CA_ALIAS, readCertificate(INTERMEDIATE_CA))

        return keyStore
    }

    fun readCertificate(cert: String): X509Certificate {
        val bytes = cert.toByteArray()
        val inputStream = ByteArrayInputStream(bytes)

        return CertificateFactory.getInstance("X.509").generateCertificate(inputStream) as X509Certificate
    }

    fun createHttpsUrlConnection(request: SimplifyRequest): HttpsURLConnection {
        // parse url
        val url = URL(request.url)

        // init ssl context with limiting trust managers
        val context = createSslContext()

        val c = url.openConnection() as HttpsURLConnection
        c.sslSocketFactory = context.socketFactory
        c.connectTimeout = CONNECTION_TIMEOUT
        c.readTimeout = READ_TIMEOUT
        c.requestMethod = request.method.name
        c.doOutput = true

        c.setRequestProperty("User-Agent", buildUserAgent())
        c.setRequestProperty("Content-Type", "application/json")

        // add extra headers
        for (key in request.headers.keys) {
            c.setRequestProperty(key, request.headers[key])
        }

        return c
    }

    fun isStatusCodeOk(statusCode: Int): Boolean {
        return statusCode in 200..299
    }

    fun buildUserAgent(): String {
        return USER_AGENT + " (API " + Build.VERSION.SDK_INT + "; Device:" + Build.DEVICE + ")"
    }

    companion object {
        const val CONNECTION_TIMEOUT = 15000
        const val READ_TIMEOUT = 60000
        const val USER_AGENT = "Android-SDK/" + BuildConfig.VERSION_NAME
        const val KEYSTORE_CA_ALIAS = "simplify-ca"
        const val INTERMEDIATE_CA = "-----BEGIN CERTIFICATE-----\n" +
                "MIIExDCCA6ygAwIBAgIEUdNgzzANBgkqhkiG9w0BAQsFADCBvjELMAkGA1UEBhMC\n" +
                "VVMxFjAUBgNVBAoTDUVudHJ1c3QsIEluYy4xKDAmBgNVBAsTH1NlZSB3d3cuZW50\n" +
                "cnVzdC5uZXQvbGVnYWwtdGVybXMxOTA3BgNVBAsTMChjKSAyMDA5IEVudHJ1c3Qs\n" +
                "IEluYy4gLSBmb3IgYXV0aG9yaXplZCB1c2Ugb25seTEyMDAGA1UEAxMpRW50cnVz\n" +
                "dCBSb290IENlcnRpZmljYXRpb24gQXV0aG9yaXR5IC0gRzIwHhcNMTQwODI2MTcx\n" +
                "NDQ5WhcNMjQwODI3MDgzNDQ3WjCBujELMAkGA1UEBhMCVVMxFjAUBgNVBAoTDUVu\n" +
                "dHJ1c3QsIEluYy4xKDAmBgNVBAsTH1NlZSB3d3cuZW50cnVzdC5uZXQvbGVnYWwt\n" +
                "dGVybXMxOTA3BgNVBAsTMChjKSAyMDEyIEVudHJ1c3QsIEluYy4gLSBmb3IgYXV0\n" +
                "aG9yaXplZCB1c2Ugb25seTEuMCwGA1UEAxMlRW50cnVzdCBDZXJ0aWZpY2F0aW9u\n" +
                "IEF1dGhvcml0eSAtIEwxSzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB\n" +
                "ANo/ltBNuS9E59s5XptQ7lylYdpBZ1MJqgCajld/KWvbx+EhJKo60I1HI9Ltchbw\n" +
                "kSHSXbe4S6iDj7eRMmjPziWTLLJ9l8j+wbQXugmeA5CTe3xJgyJoipveR8MxmHou\n" +
                "fUAL0u8+07KMqo9Iqf8A6ClYBve2k1qUcyYmrVgO5UK41epzeWRoUyW4hM+Ueq4G\n" +
                "RQyja03Qxr7qGKQ28JKyuhyIjzpSf/debYMcnfAf5cPW3aV4kj2wbSzqyc+UQRlx\n" +
                "RGi6RzwE6V26PvA19xW2nvIuFR4/R8jIOKdzRV1NsDuxjhcpN+rdBQEiu5Q2Ko1b\n" +
                "Nf5TGS8IRsEqsxpiHU4r2RsCAwEAAaOByzCByDAOBgNVHQ8BAf8EBAMCAQYwDwYD\n" +
                "VR0TBAgwBgEB/wIBADAzBggrBgEFBQcBAQQnMCUwIwYIKwYBBQUHMAGGF2h0dHA6\n" +
                "Ly9vY3NwLmVudHJ1c3QubmV0MDAGA1UdHwQpMCcwJaAjoCGGH2h0dHA6Ly9jcmwu\n" +
                "ZW50cnVzdC5uZXQvZzJjYS5jcmwwHQYDVR0OBBYEFIKicHTdvFM/z3vU981/p2DG\n" +
                "Cky/MB8GA1UdIwQYMBaAFGpyJnrQHu995ztpUdRsjZ+QEmarMA0GCSqGSIb3DQEB\n" +
                "CwUAA4IBAQABGUAYTLooPBQ3tGo723EtMXSENfDq9VTIRtcpFXOeX+Ud6Dc7W70n\n" +
                "/UeoFnFuNwCU8itlX4dhC6BEUhtfvrZNMkqsFJSTbCM288cEL+kJETObWkxFi/9E\n" +
                "lZ2HHpaO3GjILlYfled/IoRl+1FNdsuCbAP2re+5kqO9o9GEADps6xQjdQBShe2p\n" +
                "gPtJLgy/ctGI0/w7ychJun5DVxgNcwHE2SopMw50ATIFcrCMVh4vacT9x6Aqn07I\n" +
                "V4pf1qLDNe/mHIBMNQOucuqMf1q7PMIkCM4LHGexG6ApbwBQYwJp6GiaZR0dwYvi\n" +
                "fuc46qX1D2lnGyC1EktHnL3lazAZFuFC\n" +
                "-----END CERTIFICATE-----\n"
    }
}