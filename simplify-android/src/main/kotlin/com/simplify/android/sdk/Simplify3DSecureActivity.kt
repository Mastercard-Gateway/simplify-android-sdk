package com.simplify.android.sdk

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

class Simplify3DSecureActivity : AppCompatActivity() {

    internal lateinit var toolbar: Toolbar
    internal lateinit var webView: WebView

    internal val defaultTitle: String
        get() = getString(R.string.simplify_3d_secure_authentication)

    internal val extraTitle: String?
        get() = intent.extras?.getString(EXTRA_TITLE)

    internal val extraHtml: String?
        get() = intent.extras?.getString(EXTRA_HTML)


    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.simplify_3dsecure)

        // init toolbar
        toolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { view -> onBackPressed() }

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
        val extraHtml = extraHtml
        if (extraHtml == null) {
            onBackPressed()
            return
        }
        else setWebViewHtml(extraHtml)

        // init title
        setToolbarTitle(extraTitle ?: defaultTitle)
    }

    internal fun setToolbarTitle(title: String) {
        toolbar.title = title
    }

    internal fun setWebViewHtml(html: String) {
        webView.loadData(html, "text/html", "utf-8")
    }

    internal fun webViewUrlChanges(uri: Uri) {
        when {
            REDIRECT_SCHEME.equals(uri.scheme, ignoreCase = true) -> complete(getACSResultFromUri(uri))
            MAILTO_SCHEME.equals(uri.scheme, ignoreCase = true) -> intentToEmail(uri)
            else -> loadWebViewUrl(uri)
        }
    }

    internal fun complete(acsResult: String?) {
        complete(acsResult, Intent())
    }

    // separate for testability
    internal fun complete(acsResult: String?, intent: Intent) {
        intent.putExtra(EXTRA_ACS_RESULT, acsResult)
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

    internal fun getACSResultFromUri(uri: Uri): String? {
        var result: String? = null

        uri.queryParameterNames.forEach { param ->
            if ("acsResult".equals(param, ignoreCase = true)) {
                result = uri.getQueryParameter(param)
            }
        }

        return result
    }

    internal fun buildWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                webViewUrlChanges(Uri.parse(url))
                return true
            }
        }
    }

    companion object {

        /**
         * The HTML used to initialize the WebView. Should be the HTML content returned from the Gateway
         * during the Check 3DS Enrollment call
         */
        const val EXTRA_HTML = "com.simplify.android.sdk.HTML"

        /**
         * An OPTIONAL title to display in the toolbar for this activity
         */
        const val EXTRA_TITLE = "com.simplify.android.sdk.TITLE"

        /**
         * The ACS Result data after performing 3DS
         */
        const val EXTRA_ACS_RESULT = "com.simplify.android.sdk.ACS_RESULT"


        internal const val REDIRECT_SCHEME = "simplifysdk"
        internal const val MAILTO_SCHEME = "mailto"
    }


}