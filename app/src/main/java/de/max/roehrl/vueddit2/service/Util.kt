package de.max.roehrl.vueddit2.service

import android.content.res.Resources

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
}