package de.max.roehrl.vueddit2.model

import android.util.Log
import de.max.roehrl.vueddit2.service.Util
import org.json.JSONObject
import kotlin.math.absoluteValue

class Image(post: JSONObject, preferredWidth: Int = Util.getScreenWidth()) {
    var url: String? = null
    var height: Int? = null
    var width: Int? = null

    init {
        val images = post.optJSONObject("preview")?.optJSONArray("images")
        if (images != null && images.length() > 0) {
            val resolutions = images.getJSONObject(0).getJSONArray("resolutions")
            val dists = mutableListOf<Int>()
            for (i in 0 until resolutions.length()) {
                dists.add(resolutions.getJSONObject(i).getInt("width") - preferredWidth)
            }
            val min = dists.minByOrNull { it.absoluteValue }
            val index = dists.indexOf(min)
            if (index >= 0) {
                val jsonObject = resolutions.getJSONObject(index)
                url = jsonObject.optString("url")
                height = jsonObject.optInt("height")
                width = jsonObject.optInt("width")
            } else {
                Log.d("Image", "Failed to load image for post: '${post.optString("title")}'")
            }
        }
    }
}