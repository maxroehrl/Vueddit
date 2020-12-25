package de.max.roehrl.vueddit2.ui.login

import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.service.Reddit

class LoginWebView : AppCompatActivity() {
    private inner class Client : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            return request?.url.toString().startsWith(Reddit.redirectUri)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            if (url != null && (url.contains("code=") || url.contains("error="))) {
                Reddit.onAuthorizationSuccessful(url)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val webView: WebView = findViewById(R.id.web_view)
        webView.webViewClient = Client()
        webView.settings.displayZoomControls = false
        webView.settings.loadWithOverviewMode = true
        // TODO Prevent back action
    }
}