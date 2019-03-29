package com.simplify.android.sdk

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.webkit.WebViewClientCompat
import java.lang.IllegalArgumentException
import java.net.URLEncoder

class Simplify3DSecureActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var webView: WebView

    private val defaultTitle by lazyAndroid { getString(R.string.simplify_3d_secure_authentication) }
    private val extraTitle by lazyAndroid { intent.extras?.getString(EXTRA_TITLE) }
    private val extraAcsUrl by lazyAndroid { intent.extras?.getString(EXTRA_ACS_URL) }
    private val extraPaReq by lazyAndroid { intent.extras?.getString(EXTRA_PA_REQ) }
    private val extraMerchantData by lazyAndroid { intent.extras?.getString(EXTRA_MERCHANT_DATA) }
    private val extraTermUrl by lazyAndroid { intent.extras?.getString(EXTRA_TERM_URL) }


    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.simplify_3dsecure)

        if (extraAcsUrl == null || extraPaReq == null || extraTermUrl == null || extraMerchantData == null) {
            onBackPressed()
            return
        }

        // init toolbar
        toolbar = findViewById<Toolbar>(R.id.toolbar).apply {
            setNavigationOnClickListener { onBackPressed() }
            title = extraTitle ?: defaultTitle
        }

        // init web view
        webView = findViewById<WebView>(R.id.webview).apply {
            webChromeClient = WebChromeClient()
            settings.domStorageEnabled = true
            settings.javaScriptEnabled = true
            webViewClient = buildWebViewClient()
        }

        loadWebViewHtml(buildStartingHtml())
    }

    internal fun webViewUrlChanges(uri: Uri) {
        when {
            REDIRECT_SCHEME.equals(uri.scheme, ignoreCase = true) -> complete(getResultFromUri(uri))
            MAILTO_SCHEME.equals(uri.scheme, ignoreCase = true) -> intentToEmail(uri)
            else -> loadWebViewUrl(uri)
        }
    }

    private fun complete(acsResult: String?) {
        complete(acsResult, Intent())
    }

    // separate for testability
    private fun complete(result: String?, intent: Intent) {
        intent.putExtra(EXTRA_RESULT, result)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun loadWebViewUrl(uri: Uri) {
        webView.loadUrl(uri.toString())
    }

    private fun loadWebViewHtml(html: String) {
        webView.loadData(html, "text/html", Charsets.UTF_8.name())
    }

    private fun buildStartingHtml(): String {
        return resources.openRawResource(R.raw.secure3d1).readBytes().toString(Charsets.UTF_8)
                .replace(PLACEHOLDER_ACS_URL, encode(extraAcsUrl!!))
                .replace(PLACEHOLDER_PA_REQ, encode(extraPaReq!!))
                .replace(PLACEHOLDER_MERCHANT_DATA, encode(extraMerchantData!!))
                .replace(PLACEHOLDER_TERM_URL, encode(extraTermUrl!!))
    }

    private fun intentToEmail(uri: Uri) {
        intentToEmail(uri, Intent(Intent.ACTION_SENDTO))
    }

    // separate for testability
    private fun intentToEmail(uri: Uri, intent: Intent) {
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.data = uri

        startActivity(intent)
    }

    private fun getResultFromUri(uri: Uri): String? {
        var result: String? = null

        uri.queryParameterNames.forEach { param ->
            if (REDIRECT_QUERY_PARAM.equals(param, ignoreCase = true)) {
                result = uri.getQueryParameter(param)
            }
        }

        return result
    }

    private fun buildWebViewClient(): WebViewClientCompat {
        return object : WebViewClientCompat() {

            @Suppress("OverridingDeprecatedMember")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                webViewUrlChanges(Uri.parse(url))
                return true
            }

            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                webViewUrlChanges(request.url)
                return true
            }
        }
    }

    private fun encode(value: String): String {
        return URLEncoder.encode(value, Charsets.UTF_8.name())
    }

    companion object {

        /**
         * The ACS URL returned from CardToken create
         */
        const val EXTRA_ACS_URL = "com.simplify.android.sdk.ASC_URL"

        /**
         * The PaReq returned from CardToken create
         */
        const val EXTRA_PA_REQ = "com.simplify.android.sdk.PA_REQ"

        /**
         * The Merchant Data (md) returned from CardToken create
         */
        const val EXTRA_MERCHANT_DATA = "com.simplify.android.sdk.MERCHANT_DATA"

        /**
         * The Termination URL (termUrl) returned from CardToken create
         */
        const val EXTRA_TERM_URL = "com.simplify.android.sdk.TERM_URL"

        /**
         * An OPTIONAL title to display in the toolbar for this activity
         */
        const val EXTRA_TITLE = "com.simplify.android.sdk.TITLE"

        /**
         * The result data after performing 3DS
         */
        const val EXTRA_RESULT = "com.simplify.android.sdk.RESULT"


        private const val REDIRECT_SCHEME = "simplifysdk"
        private const val REDIRECT_QUERY_PARAM = "result"
        private const val MAILTO_SCHEME = "mailto"
        private const val PLACEHOLDER_ACS_URL = "{{{acsUrl}}}"
        private const val PLACEHOLDER_PA_REQ = "{{{paReq}}}"
        private const val PLACEHOLDER_MERCHANT_DATA = "{{{md}}}"
        private const val PLACEHOLDER_TERM_URL = "{{{termUrl}}}"

        /**
         * Construct an intent to the [Simplify3DSecureActivity] activity, adding the relevant 3DS data from the card token as intent extras
         *
         * @param context The calling context
         * @param cardToken The card token used for a 3DS transaction
         * @param title An OPTIONAL title to display in the toolbar
         * @throws IllegalArgumentException If the card token does not contain valid [Secure3DData]
         */
        @JvmOverloads
        @JvmStatic
        fun buildIntent(context: Context, cardToken: CardToken, title: String? = null): Intent {
            val secure3DData = cardToken.getCard().getSecure3DData() ?: throw IllegalArgumentException("The provided card token must contain 3DS data. See: Simplify.createCardToken(Card, Secure3DRequestData, Callback);")

            val intent = Intent(context, Simplify3DSecureActivity::class.java)
            intent.putExtra(Simplify3DSecureActivity.EXTRA_ACS_URL, secure3DData.getAcsUrl())
            intent.putExtra(Simplify3DSecureActivity.EXTRA_PA_REQ, secure3DData.getPaReq())
            intent.putExtra(Simplify3DSecureActivity.EXTRA_MERCHANT_DATA, secure3DData.getMd())
            intent.putExtra(Simplify3DSecureActivity.EXTRA_TERM_URL, secure3DData.getTermUrl())

            if (title != null) {
                intent.putExtra(Simplify3DSecureActivity.EXTRA_TITLE, title)
            }

            return intent
        }
    }
}