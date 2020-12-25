package de.max.roehrl.vueddit2.service

import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.*
import de.max.roehrl.vueddit2.BuildConfig
import de.max.roehrl.vueddit2.model.Comment
import de.max.roehrl.vueddit2.model.Post
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.math.absoluteValue
import kotlin.random.Random

object Reddit {
    const val api = "https://www.reddit.com"
    const val oauthApi = "https://oauth.reddit.com"
    var userAgent: String
    private val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    val randomState = (1..10)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    const val clientId = "m_gI8cFDcqC7uA"
    const val redirectUri = "http://localhost:8080"
    val redirectUriEncoded = URLEncoder.encode(redirectUri, "utf-8")
    val scope = URLEncoder.encode("mysubreddits read vote save subscribe wikiread identity history flair", "utf-8")
    const val frontpage = "reddit front page"
    private val requestQueue: RequestQueue = getInstance()

    private fun getInstance(): RequestQueue {
        return RequestQueue(NoCache(), BasicNetwork(HurlStack())).apply {
            start()
        }
    }

    init {
        val packageName = BuildConfig.APPLICATION_ID
        val version = BuildConfig.VERSION_NAME
        userAgent = "$packageName:$version (by /u/MaxRoehrl)"
    }

    fun getSubredditPosts(
        subreddit: String = frontpage,
        after: String = "",
        sorting: String = "best",
        cb: (MutableList<Post>) -> Unit
    ) {
        var url = "$api/$sorting.json?raw_json=1"
        if (after != "") {
            url += "&after=$after"
        }
        get(url) {
            val postsData = it.optJSONObject("data")?.optJSONArray("children")
            val posts = mutableListOf<Post>()
            if (postsData != null) {
                for (i in 0 until postsData.length()) {
                    posts.add(Post(postsData.getJSONObject(i).getJSONObject("data")))
                }
            }
            cb.invoke(posts)
        }
    }

    fun getPostAndComments(permalink: String,
                           sort: String = "top",
                           cb: (Post, MutableList<Comment>) -> Unit) {
        getArray("$api$permalink.json?raw_json=1&sort=$sort") {
            val post = Post(it
                .optJSONObject(0)
                ?.optJSONObject("data")
                ?.optJSONArray("children")
                ?.optJSONObject(0)
                ?.optJSONObject("data") ?: JSONObject()
            )
            val commentsData = it
                .optJSONObject(1)
                ?.optJSONObject("data")
                ?.optJSONArray("children")
            val comments = mutableListOf<Comment>()

            fun addAllChildren(item: JSONObject) {
                comments.add(Comment(item))
                val children = item
                        .optJSONObject("replies")
                        ?.optJSONObject("data")
                        ?.optJSONArray("children")
                if (children != null) {
                    for (i in 0 until children.length()) {
                        addAllChildren(children.getJSONObject(i).getJSONObject("data"))
                    }
                }
            }
            if (commentsData != null) {
                for (i in 0 until commentsData.length()) {
                    addAllChildren(commentsData.getJSONObject(i).getJSONObject("data"))
                }
            }
            cb.invoke(post, comments)
        }
    }

    fun getFormattedScore(score : Int) : String {
        return if (score >= 10000) (score / 1000).toString() + 'k' else score.toString()
    }

    private fun post(url: String,
                     content: JSONObject,
                     responseListener: (JSONObject) -> Unit) {
        request(url, Request.Method.POST, content, responseListener)
    }

    private fun getArray(url: String,
                         responseListener: (JSONArray) -> Unit) {
        request(url, Request.Method.GET, null, responseListener)
    }

    private fun get(url: String,
                    responseListener: (JSONObject) -> Unit) {
        request(url, Request.Method.GET, null, responseListener)
    }

    private fun request(url: String,
                        method: Int,
                        content: JSONObject?,
                        responseListener: (JSONObject) -> Unit) {
        val request = JsonObjectRequest(
            method, url, content,
            { response -> responseListener.invoke(response) },
            { error -> Log.e("Reddit", "Request failed", error) }
        )
        requestQueue.add(request)
    }

    private fun request(url: String,
                        method: Int,
                        content: JSONArray?,
                        responseListener: (JSONArray) -> Unit) {
        val request = JsonArrayRequest(
            method, url, content,
            { response -> responseListener.invoke(response) },
            { error -> Log.e("Reddit", "Request failed", error) }
        )
        requestQueue.add(request)
    }

    fun onAuthorizationSuccessful(url: String) {
        TODO("Not yet implemented")
    }
}