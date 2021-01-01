package de.max.roehrl.vueddit2.service

import android.content.ActivityNotFoundException
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent.Builder
import androidx.browser.customtabs.CustomTabsIntent.SHARE_STATE_ON
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import de.max.roehrl.vueddit2.R
import saschpe.android.customtabs.CustomTabsHelper
import saschpe.android.customtabs.WebViewFallback

object CustomTabs {
    fun openUrl(context: Context, url: String) {
        val backArrow : Bitmap = ContextCompat.getDrawable(context, R.drawable.ic_arror_left)!!.toBitmap()
        val customTabsIntent = Builder()
                .setShareState(SHARE_STATE_ON)
                .setShowTitle(true)
                .setStartAnimations(context, R.anim.slide_in_right, R.anim.slide_out_left)
                .setExitAnimations(context, R.anim.slide_in_left, R.anim.slide_out_right)
                .setCloseButtonIcon(backArrow)
                .setUrlBarHidingEnabled(true)
                .build()
        CustomTabsHelper.addKeepAliveExtra(context, customTabsIntent.intent)
        val uri = Uri.parse(url)
        try {
            CustomTabsHelper.openCustomTab(context, customTabsIntent, uri, WebViewFallback())
        } catch (error: ActivityNotFoundException) {
            Log.e("CustomTabs", "Failed to open custom tab", error)
        }
    }
}