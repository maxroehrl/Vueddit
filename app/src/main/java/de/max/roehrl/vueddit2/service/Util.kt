package de.max.roehrl.vueddit2.service

import android.content.res.Resources
import android.text.format.DateUtils
import kotlin.math.floor

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

    fun getFormattedScore(score: Int): String {
        return if (score >= 10000) String.format("%.1fk", score.toFloat() / 1000f) else score.toString()
    }

    fun dpToPx(dp: Int): Int {
        return floor((dp * Resources.getSystem().displayMetrics.density).toDouble()).toInt()
    }
}