package de.max.roehrl.vueddit2.service

import android.content.Context
import android.util.Log
import android.util.Patterns
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.navOptions
import de.max.roehrl.vueddit2.R

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
            && (Regex("^/r/[^\\s;./]+/comments/[^\\s;./]+/[^\\s;./]+$").matches(path)
                    || Regex("^/r/[^\\s;./]+$").matches(path)
                    || Regex("^/u(ser)?/[^\\s;./]+$").matches(path))
        ) {
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

    fun openDeepLink(permalink: String, navController: NavController, popUpToId: Int = -1) {
        val request = NavDeepLinkRequest.Builder.fromUri((Reddit.api + permalink).toUri()).build()
        val options = navOptions {
            anim {
                enter = R.anim.slide_in_right
                exit = R.anim.slide_out_left
                popEnter = R.anim.slide_in_left
                popExit = R.anim.slide_out_right
            }
            popUpTo = popUpToId
        }
        navController.navigate(request, options)
    }
}