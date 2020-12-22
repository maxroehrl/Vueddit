package de.max.roehrl.vueddit2.service

import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.NoCache
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.JsonObjectRequest
import de.max.roehrl.vueddit2.Post
import org.json.JSONObject
import kotlin.math.absoluteValue

object Reddit {
    const val frontpage = "reddit front page"
    private val requestQueue: RequestQueue = getInstance()

    private fun getInstance(): RequestQueue {
        return RequestQueue(NoCache(), BasicNetwork(HurlStack())).apply {
            start()
        }
    }

    fun getSubredditPosts(
        subreddit: String = frontpage,
        after: String = "",
        sorting: String = "best",
        cb: (MutableList<Post>) -> Unit
    ) {
        val url = "https://reddit.com/$sorting.json?raw_json=1"
        requestQueue.add(JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                val g = response.getJSONObject("data").getJSONArray("children")
                val list = mutableListOf<Post>()
                for (i in 0 until g.length()) {
                    list.add(Post(g.getJSONObject(i).getJSONObject("data")))
                }
                cb.invoke(if (after == "") {
                    list
                } else {
                    list
                })
            },
            { error ->
            }
        ))
    }

    fun getPreview(post: JSONObject, preferredWidth: Int = 300): JSONObject? {
        val images = post.optJSONObject("preview")?.optJSONArray("images")
        if (images != null && images.length() > 0) {
            val resolutions = images.getJSONObject(0).getJSONArray("resolutions")
            val dists = mutableListOf<Int>()
            for (i in 0 until resolutions.length()) {
                dists.add(resolutions.getJSONObject(i).getInt("width") - preferredWidth)
            }
            val min = dists.minByOrNull { it.absoluteValue }
            val index = dists.indexOf(min)
            return resolutions.getJSONObject(index)
        }
        return null
    }
}