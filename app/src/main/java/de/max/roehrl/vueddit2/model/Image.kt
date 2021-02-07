package de.max.roehrl.vueddit2.model

import android.util.Log
import de.max.roehrl.vueddit2.service.Util
import org.json.JSONObject
import kotlin.math.absoluteValue

class Image(post: JSONObject, preferredWidth: Int = Util.getScreenWidth()) {
    companion object {
        private val noThumbnails = listOf("default", "self", "")

        private fun getPreferredImage(
            resolutions: List<JSONObject>,
            preferredWidth: Int,
            widthProp: String = "width"
        ): JSONObject? {
            val distances = resolutions.map { it.getInt(widthProp) - preferredWidth }
            val min = distances.minByOrNull { it.absoluteValue }
            val index = distances.indexOf(min)
            return resolutions.getOrNull(index)
        }
    }

    var url: String? = null
    var height: Int? = null
    var width: Int? = null

    init {
        val images = post.optJSONObject("preview")?.optJSONArray("images")
        val mediaEmbedded = post.optJSONObject("media_metadata")
        val thumbnail = post.optString("thumbnail")
        if (images != null && images.length() > 0) {
            val resolutionsArray = images.getJSONObject(0).getJSONArray("resolutions")
            val resolutions = mutableListOf<JSONObject>()
            for (i in 0 until resolutionsArray.length()) {
                resolutions.add(resolutionsArray.getJSONObject(i))
            }
            val preferredImage = getPreferredImage(resolutions, preferredWidth)
            if (preferredImage != null) {
                url = preferredImage.optString("url")
                height = preferredImage.optInt("height")
                width = preferredImage.optInt("width")
            } else {
                Log.d("Image", "Failed to load image for post: '${post.optString("title")}'")
            }
        } else if (mediaEmbedded != null) {
            for (key in mediaEmbedded.keys()) {
                val galleryImage = mediaEmbedded.getJSONObject(key)
                val resolutions = mutableListOf(galleryImage.getJSONObject("s"))
                val previews = galleryImage.getJSONArray("p")
                for (i in 0 until previews.length()) {
                    resolutions.add(previews.getJSONObject(i))
                }
                val preferredImage = getPreferredImage(resolutions, preferredWidth, "x")
                if (preferredImage != null) {
                    url = preferredImage.optString("u")
                    height = preferredImage.optInt("y")
                    width = preferredImage.optInt("x")
                    break
                }
            }
        } else if (!noThumbnails.contains(thumbnail) && post.has("thumbnail_height")) {
            url = thumbnail
            height = post.optInt("thumbnail_height")
            width = post.optInt("thumbnail_width")
        }
    }
}