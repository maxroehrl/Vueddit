package de.max.roehrl.vueddit2.service

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent.Builder
import androidx.browser.customtabs.CustomTabsIntent.SHARE_STATE_ON
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import de.max.roehrl.vueddit2.R

object CustomTabs {
    private const val TAG = "CustomTabs"

    fun openInCustomTabs(context: Context, url: String) {
        val backArrow = ContextCompat.getDrawable(context, R.drawable.ic_arror_left)!!.toBitmap()
        val customTabsIntent = Builder()
            .setShareState(SHARE_STATE_ON)
            .setShowTitle(true)
            .setCloseButtonIcon(backArrow)
            .setUrlBarHidingEnabled(true)
            .build()
        val uri = Uri.parse(url)
        try {
            customTabsIntent.launchUrl(context, uri)
        } catch (error: ActivityNotFoundException) {
            Log.e(TAG, "Failed to open custom tab", error)
            Toast.makeText(context, "You don't have any browser to open the web page", Toast.LENGTH_LONG).show()
        }
    }
}