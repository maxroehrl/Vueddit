package de.max.roehrl.vueddit2.service

import android.util.DisplayMetrics
import de.max.roehrl.vueddit2.model.Video

object Util {
    fun getTimeFromNow() : String {
        return "2 minutes ago"
    }

    fun getUnixTime() : Int {
        return System.currentTimeMillis().toInt()
    }

    fun getAspectFixHeight(video: Video) : Int {
        return if (video.height != 0 && video.width != 0) {
            video.height * getScreenWidth() / video.width
        } else {
            0
        }
    }

    fun getScreenWidth() : Int = DisplayMetrics().widthPixels
}