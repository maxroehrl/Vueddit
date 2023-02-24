package de.max.roehrl.vueddit2.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.service.Reddit
import de.max.roehrl.vueddit2.ui.viewmodel.AppViewModel
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "LoginActivity"
    }

    private inner class Client : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            return request?.url.toString().startsWith(Reddit.redirectUri)
        }

        override fun onPageFinished(webView: WebView?, url: String?) {
            val uri = Uri.parse(url)
            if (url != null && url.contains("code=") && url.contains("state=")) {
                lifecycleScope.launch {
                    val success = Reddit.getInstance(applicationContext).onAuthorizationSuccessful(uri)
                    if (success) {
                        val intent = Intent(applicationContext, MainActivity::class.java)
                        intent.putExtra(AppViewModel.WAS_LOGGED_IN, true)
                        startActivity(intent)
                        finish()
                    } else {
                        webView?.goBack()
                    }
                }
            } else if (url != null && url.contains("error=")) {
                val error = uri.getQueryParameter("error")
                if (error == "access_denied") {
                    webView?.goBack()
                } else {
                    Log.e(TAG, "Login error: $error")
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        onBackPressedDispatcher.addCallback(this) {
            finishAffinity()
        }

        val webView: WebView = findViewById(R.id.web_view)
        webView.webViewClient = Client()
        webView.settings.displayZoomControls = false
        webView.settings.loadWithOverviewMode = true
        webView.settings.javaScriptEnabled = true
        webView.loadUrl(Reddit.oAuthLoginUrl)
    }
}