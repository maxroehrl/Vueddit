package de.max.roehrl.vueddit2.service

import android.content.Context
import android.util.Log
import android.util.Patterns
import androidx.annotation.IdRes
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.navOptions

object Url {
    private const val TAG = "UrlService"

    fun openUrl(context: Context, navController: NavController?, url: String) {
        val redditUrlPrefix = "^https?://(www\\.)?(old\\.|new\\.|m\\.)?reddit\\.com"
        var path = url.trim()
        if (Regex("${redditUrlPrefix}.*").matches(path)
            && !Regex("$redditUrlPrefix/gallery/.*").matches(path)
        ) {
            path = path.replace(Regex(redditUrlPrefix), "")
        }
        if (navController != null
            && (Regex("^/r/[^\\s;./]+/comments/[^\\s;./]+/[^\\s;./]+/?.*$").matches(path)
                    || Regex("^/r/[^\\s;./]+$").matches(path)
                    || Regex("^/u(ser)?/[^\\s;./]+$").matches(path))
        ) {
            if (Regex("^/r/[^\\s;./]+/comments/[^\\s;./]+/[^\\s;./]+/.*$").matches(path)) {
                path = path.substringBeforeLast("/")
            }
            try {
                openDeepLink(path, navController)
            } catch (error: IllegalStateException) {
                Log.e(TAG, "Error finding nav controller'", error)
            }
        } else if (Patterns.WEB_URL.matcher(url.trim()).matches()) {
            CustomTabs.openInCustomTabs(context, url.trim())
        } else {
            Log.e(TAG, "Invalid url: '$url'")
        }
    }

    fun openDeepLink(permalink: String, navController: NavController, @IdRes popUpToId: Int = -1) {
        val request = NavDeepLinkRequest.Builder.fromUri((Reddit.api + permalink).toUri()).build()
        val options = navOptions {
            popUpTo(popUpToId)
        }
        navController.navigate(request, options)
    }
}