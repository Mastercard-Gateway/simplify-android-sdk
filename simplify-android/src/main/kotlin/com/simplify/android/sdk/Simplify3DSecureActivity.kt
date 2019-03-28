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

    internal lateinit var toolbar: Toolbar
    internal lateinit var webView: WebView

    internal val defaultTitle: String
        get() = getString(R.string.simplify_3d_secure_authentication)

    internal val extraTitle: String?
        get() = intent.extras?.getString(EXTRA_TITLE)

    internal val extraAcsUrl: String?
        get() = intent.extras?.getString(EXTRA_ACS_URL)

    internal val extraPaReq: String?
        get() = intent.extras?.getString(EXTRA_PA_REQ)

    internal val extraMerchantData: String?
        get() = intent.extras?.getString(EXTRA_MERCHANT_DATA)

    internal val extraTermUrl: String?
        get() = intent.extras?.getString(EXTRA_TERM_URL)


    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.simplify_3dsecure)

        // init toolbar
        toolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        // init web view
        webView = findViewById(R.id.webview)
        webView.webChromeClient = WebChromeClient()
        webView.settings.domStorageEnabled = true
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = buildWebViewClient()

        init()
    }

    fun init() {
        // init html
        if (extraAcsUrl == null || extraPaReq == null || extraTermUrl == null || extraMerchantData == null) {
            onBackPressed()
            return
        }
        else {
            val url = MOBILE_3DS1_URL + "?acsUrl=${encode(extraAcsUrl!!)}&paReq=${encode(extraPaReq!!)}&md=${encode(extraMerchantData!!)}&termUrl=${encode(extraTermUrl!!)}"
            loadWebViewUrl(Uri.parse(url))
        }

        // init title
        setToolbarTitle(extraTitle ?: defaultTitle)
    }

    internal fun setToolbarTitle(title: String) {
        toolbar.title = title
    }

    internal fun webViewUrlChanges(uri: Uri) {
        when {
            REDIRECT_SCHEME.equals(uri.scheme, ignoreCase = true) -> complete(getResultFromUri(uri))
            MAILTO_SCHEME.equals(uri.scheme, ignoreCase = true) -> intentToEmail(uri)
            else -> loadWebViewUrl(uri)
        }
    }

    internal fun complete(acsResult: String?) {
        complete(acsResult, Intent())
    }

    // separate for testability
    internal fun complete(result: String?, intent: Intent) {
        intent.putExtra(EXTRA_RESULT, result)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    internal fun loadWebViewUrl(uri: Uri) {
        webView.loadUrl(uri.toString())
    }

    internal fun intentToEmail(uri: Uri) {
        intentToEmail(uri, Intent(Intent.ACTION_SENDTO))
    }

    // separate for testability
    internal fun intentToEmail(uri: Uri, intent: Intent) {
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.data = uri

        startActivity(intent)
    }

    internal fun getResultFromUri(uri: Uri): String? {
        var result: String? = null

        uri.queryParameterNames.forEach { param ->
            if ("result".equals(param, ignoreCase = true)) {
                result = uri.getQueryParameter(param)
            }
        }

        return result
    }

    internal fun buildWebViewClient(): WebViewClientCompat {
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

    internal fun encode(value: String): String {
        return URLEncoder.encode(value, "UTF-8")
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


        internal const val MOBILE_3DS1_URL = "https://young-chamber-23463.herokuapp.com/mobile3ds1.html" // TODO update this to static deploy url
        internal const val REDIRECT_SCHEME = "simplifysdk"
        internal const val MAILTO_SCHEME = "mailto"

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
            intent.putExtra(Simplify3DSecureActivity.EXTRA_MERCHANT_DATA, secure3DData.getMerchantData())
            intent.putExtra(Simplify3DSecureActivity.EXTRA_TERM_URL, secure3DData.getTermUrl())

            if (title != null) {
                intent.putExtra(Simplify3DSecureActivity.EXTRA_TITLE, title)
            }

            return intent
        }
    }
}