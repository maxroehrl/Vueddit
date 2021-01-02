package de.max.roehrl.vueddit2.service

import android.content.res.Resources
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import com.google.android.material.appbar.MaterialToolbar
import de.max.roehrl.vueddit2.R

object Util {
    fun getTimeFromNow() : String {
        return "2 minutes ago"
    }

    fun getUnixTime() : Long {
        return System.currentTimeMillis()
    }

    fun getAspectFixHeight(width: Int, height: Int) : Int {
        return if (height != 0 && width != 0) {
            height * getScreenWidth() / width
        } else {
            0
        }
    }

    fun getScreenWidth() : Int = Resources.getSystem().displayMetrics.widthPixels

    fun MaterialToolbar.setActionBarUpIndicator(showAsDrawerIndicator: Boolean) {
        val arrowDrawable = DrawerArrowDrawable(context)
        navigationIcon = arrowDrawable
        setNavigationContentDescription(if (showAsDrawerIndicator) R.string.nav_app_bar_open_drawer_description else R.string.nav_app_bar_navigate_up_description)
        arrowDrawable.progress = if (showAsDrawerIndicator) 0f else 1f
    }
}