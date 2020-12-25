package de.max.roehrl.vueddit2.service

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.*
import de.max.roehrl.vueddit2.BuildConfig
import de.max.roehrl.vueddit2.model.Comment
import de.max.roehrl.vueddit2.model.Post
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.random.Random


object Reddit {
    const val api = "https://www.reddit.com"
    const val oauthApi = "https://oauth.reddit.com"
    var userAgent: String
    private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    private val randomState = (1..10)
        .map { Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")
    private const val clientId = "m_gI8cFDcqC7uA"
    const val redirectUri = "http://localhost:8080"
    private val redirectUriEncoded = URLEncoder.encode(redirectUri, "utf-8")
    private val scope = URLEncoder.encode(
        "mysubreddits read vote save subscribe wikiread identity history flair",
        "utf-8"
    )
    const val frontpage = "reddit front page"
    private val requestQueue = RequestQueue(NoCache(), BasicNetwork(HurlStack())).apply {
        start()
    }
    val oAuthLoginUrl =
        "${api}/api/v1/authorize.compact?client_id=${clientId}&response_type=code&state=${randomState}&redirect_uri=${redirectUriEncoded}&scope=${scope}&duration=permanent"

    private class BasicRequest(
        url: String?,
        val bodyParameters: MutableMap<String, String>,
        listener: Response.Listener<String>?,
        errorListener: Response.ErrorListener?
    ) : StringRequest(Method.POST, url, listener, errorListener) {
        override fun getHeaders(): MutableMap<String, String> {
            return mutableMapOf(
                Pair(
                    "Authorization",
                    "Basic ${Base64.encodeToString("$clientId:".toByteArray(), Base64.DEFAULT)}"
                )
            )
        }

        override fun getParams(): MutableMap<String, String> {
            return bodyParameters
        }
    }

    init {
        val packageName = BuildConfig.APPLICATION_ID
        val version = BuildConfig.VERSION_NAME
        userAgent = "$packageName:$version (by /u/MaxRoehrl)"
    }

    fun getUser(cb: (String) -> Unit) {
        get("$oauthApi/api/v1/me?raw_json=1") {
            cb(it.getString("name"))
        }
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

    fun getPostAndComments(
        permalink: String,
        sort: String = "top",
        cb: (Post, MutableList<Comment>) -> Unit
    ) {
        getArray("$api$permalink.json?raw_json=1&sort=$sort") {
            val post = Post(
                it
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

    fun getFormattedScore(score: Int): String {
        return if (score >= 10000) (score / 1000).toString() + 'k' else score.toString()
    }

    private fun post(
        url: String,
        content: JSONObject,
        responseListener: (JSONObject) -> Unit
    ) {
        request(url, Request.Method.POST, content, responseListener)
    }

    private fun getArray(
        url: String,
        responseListener: (JSONArray) -> Unit
    ) {
        request(url, Request.Method.GET, null, responseListener)
    }

    private fun get(
        url: String,
        responseListener: (JSONObject) -> Unit
    ) {
        request(url, Request.Method.GET, null, responseListener)
    }

    private fun request(
        url: String,
        method: Int,
        content: JSONObject?,
        responseListener: (JSONObject) -> Unit
    ) {
        val request = JsonObjectRequest(
            method, url, content,
            { response -> responseListener.invoke(response) },
            { error -> Log.e("Reddit", "Request failed", error) }
        )
        requestQueue.add(request)
    }

    private fun request(
        url: String,
        method: Int,
        content: JSONArray?,
        responseListener: (JSONArray) -> Unit
    ) {
        val request = JsonArrayRequest(
            method, url, content,
            { response -> responseListener.invoke(response) },
            { error -> Log.e("Reddit", "Request failed", error) }
        )
        requestQueue.add(request)
    }

    fun onAuthorizationSuccessful(context: Context, responseUrl: String) {
        val uri = Uri.parse(responseUrl)
        try {
            if (uri.getQueryParameter("state") == randomState) {
                val code = uri.getQueryParameter("code")
                requestQueue.add(BasicRequest(
                    "${api}/api/v1/access_token",
                    mutableMapOf(
                        Pair("grant_type", "authorization_code"),
                        Pair("code", code!!),
                        Pair("redirect_uri", redirectUriEncoded)
                    ),
                    { response ->
                        updateToken(context, JSONObject(response))
                        (context as Activity).onBackPressed()
                    },
                    { error -> Log.e("Reddit", "Request failed", error) }
                ))
            }
        } catch (e: NullPointerException) {
            try {
                val error = uri.getQueryParameter("error")
                if (error == "access_denied") {
                    (context as Activity).finish()
                } else {
                    Log.e("Reddit", "Login error: $error")
                }
            } catch (e: NullPointerException) {
                (context as Activity).finish()
            }
        }
    }

    private fun updateToken(context: Context, response: JSONObject) {
        runBlocking {
            try {
                Store.getInstance(context).updateTokens(
                    response.getString("access_token"),
                    response.getInt("expires_in") + Util.getUnixTime(),
                    response.getString("refresh_token"),
                )
            } catch (e: JSONException) {
                Log.e("Reddit", "Failed to update tokens", e)
            }
        }
    }

    private fun refreshAuthToken(context: Context) {
        var refreshToken: String? = null
        runBlocking {
            Store.getInstance(context).refreshToken.collect { token -> refreshToken = token }
        }
        if (refreshToken != null) {
            requestQueue.add(BasicRequest(
                "${api}/api/v1/access_token",
                mutableMapOf(
                    Pair("grant_type", "refresh_token"),
                    Pair("refresh_token", refreshToken!!)
                ),
                { response -> updateToken(context, JSONObject(response)) },
                { error -> Log.e("Reddit", "Request failed", error) }
            ))
        } else {
            Log.e("Reddit", "No refresh token")
        }
    }

    private fun refreshTokenIfNecessary(context: Context) {
        var validUntil: Int? = null
        runBlocking {
            Store.getInstance(context).validUntil.collect { valid -> validUntil = valid }
        }
        if (validUntil != null && validUntil!! <= Util.getUnixTime())
            refreshAuthToken(context)
    }
}