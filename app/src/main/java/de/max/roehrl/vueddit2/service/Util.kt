package de.max.roehrl.vueddit2.service

import android.content.res.Resources
import android.text.format.DateUtils

object Util {
    fun getTimeFromNow(time: Long) : String {
        return DateUtils.getRelativeTimeSpanString(time * 1000).toString()
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