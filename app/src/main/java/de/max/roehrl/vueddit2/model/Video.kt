package de.max.roehrl.vueddit2.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

@Parcelize
data class Video(
    var url: String? = null,
    var type: VideoType = VideoType.NONE,
    var width: Int = 0,
    var height: Int = 0,
) : Parcelable {
    companion object {
        fun fromJson(json: JSONObject): Video {
            return when {
                json.optJSONObject("secure_media")?.optJSONObject("reddit_video") != null ->
                    setVideoObject(
                        json.optJSONObject("secure_media")?.optJSONObject("reddit_video"),
                        "dash_url"
                    )
                json.optJSONObject("preview")?.optJSONObject("reddit_video_preview") != null ->
                    setVideoObject(
                        json.optJSONObject("preview")?.optJSONObject("reddit_video_preview"),
                        "fallback_url"
                    )
                json.optJSONObject("preview")?.optJSONArray("images")?.optJSONObject(0)
                    ?.optJSONObject("variants")?.optJSONObject("mp4")
                    ?.optJSONObject("source") != null ->
                    setVideoObject(
                        json.optJSONObject("preview")?.optJSONArray("images")?.optJSONObject(0)
                            ?.optJSONObject("variants")?.optJSONObject("mp4")
                            ?.optJSONObject("source"),
                        "url"
                    )
                json.optJSONObject("secure_media_embed")?.optString("media_domain_url") != null ->
                    setVideoObject(json.optJSONObject("secure_media_embed"), "media_domain_url")
                else -> Video()
            }
        }

        private fun setVideoObject(
            source: JSONObject?,
            srcProp: String = "src",
            heightProp: String = "height",
            widthProp: String = "width"
        ): Video {
            if (source != null) {
                val url = source.optString(srcProp)
                if (url != "") {
                    val type = when (srcProp) {
                        "dash_url" -> VideoType.DASH
                        "url" -> VideoType.MP4
                        "fallback_url" -> VideoType.MP4
                        "media_domain_url" -> VideoType.EMBEDDED
                        else -> VideoType.NONE
                    }
                    return Video(
                        url = url,
                        type = type,
                        height = source.optInt(heightProp, 0),
                        width = source.optInt(widthProp, 0),
                    )
                }
            }
            return Video()
        }
    }
}