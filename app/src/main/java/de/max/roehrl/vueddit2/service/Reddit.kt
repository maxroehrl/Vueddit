package de.max.roehrl.vueddit2.service

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.NoCache
import com.android.volley.toolbox.StringRequest
import de.max.roehrl.vueddit2.BuildConfig
import de.max.roehrl.vueddit2.model.Comment
import de.max.roehrl.vueddit2.model.Post
import de.max.roehrl.vueddit2.model.Subreddit
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random


object Reddit {
    private const val TAG = "Reddit"
    const val api = "https://www.reddit.com"
    private const val oauthApi = "https://oauth.reddit.com"
    private const val userAgent = "${BuildConfig.APPLICATION_ID}:${BuildConfig.VERSION_NAME} (by /u/MaxRoehrl)"
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
    val oAuthLoginUrl = "${api}/api/v1/authorize.compact?client_id=${clientId}&response_type=code&state=${randomState}&redirect_uri=${redirectUriEncoded}&scope=${scope}&duration=permanent"
    @Volatile private lateinit var store: Store
    @Volatile private var authToken: String = ""
    @Volatile private var refreshToken: String? = ""
    @Volatile private var validUntil: Long? = null

    suspend fun login(context: Context) : Boolean {
        store = Store.getInstance(context)
        val token = store.getAuthToken()
        if (token != null) {
            authToken = token
            refreshToken = store.getRefreshToken()
            validUntil = store.getValidUntil()
            try {
                val username = getUser()
                store.updateUserName(username)
                Log.i(TAG, "User $username is logged in")
                return true
            } catch (error: VolleyError) {
                Log.i(TAG, "User is logged out")
            }
        }
        return false
    }

    private class TokenRequest(
            url: String?,
            val bodyText: String,
            listener: Response.Listener<String>?,
            errorListener: Response.ErrorListener?
    ) : StringRequest(Method.POST, url, listener, errorListener) {
        override fun getHeaders(): MutableMap<String, String> {
            return mutableMapOf(Pair("Authorization", "Basic ${Base64.encodeToString("$clientId:".toByteArray(), Base64.DEFAULT)}"))
        }

        override fun getBody(): ByteArray {
            return bodyText.toByteArray()
        }
    }

    private class OAuthRequest(
            method: Int,
            url: String?,
            val bodyText: String?,
            listener: Response.Listener<String>,
            errorListener: Response.ErrorListener?
    ) : StringRequest(method, url, listener, errorListener) {
        override fun getHeaders(): MutableMap<String, String> {
            return mutableMapOf(
                    Pair("User-Agent", userAgent),
                    Pair("Authorization", "Bearer $authToken")
            )
        }

        override fun getBody(): ByteArray? {
            return bodyText?.toByteArray()
        }
    }

    suspend fun getUser() : String {
        return JSONObject(get("/api/v1/me?raw_json=1")).getString("name")
    }

    suspend fun getSubredditPosts(
            subreddit: String = frontpage,
            after: String = "",
            sorting: String = "best",
            group: String? = null,
            time: String? = null,
    ): MutableList<Post> {
        val url = "${if (subreddit == frontpage) "" else "/r/$subreddit"}/$sorting.json?raw_json=1"
        return getPosts(url, after, sorting, time)
    }

    suspend fun getUserPosts(
            user: String,
            after: String? = "",
            sorting: String = "new",
            group: String = "submitted",
            time: String = "all",
            type: String = "all",
    ): MutableList<Post> {
        var url = "/user/$user/$group.json?raw_json=1&sort=$sorting"
        url += if (type != "all") "&type=$type" else ""
        return getPosts(url, after, sorting, time);
    }

    private suspend fun getPosts(
            url2: String,
            after: String?,
            sorting: String,
            time: String?,
            limit: Int = 25,
    ): MutableList<Post> {
        var url = url2 + "&limit=$limit" + (if (after != null && after != "") "&after=$after" else "")
        if (time != null && setOf("top", "rising").contains(sorting)) {
            url += "&t=$time"
        }
        val response = get(url)
        val postsData = JSONObject(response)
                .optJSONObject("data")
                ?.optJSONArray("children")
        val posts = mutableListOf<Post>()
        if (postsData != null) {
            for (i in 0 until postsData.length()) {
                posts.add(Post(postsData.getJSONObject(i).getJSONObject("data")))
            }
        }
        return posts
    }

    suspend fun getPostAndComments(permalink: String, sort: String = "top"): Pair<Post, MutableList<Comment>> {
        val response = get("$permalink.json?raw_json=1&sort=$sort")
        val responseArray = JSONArray(response)
        val post = Post(
                responseArray
                        .optJSONObject(0)
                        ?.optJSONObject("data")
                        ?.optJSONArray("children")
                        ?.optJSONObject(0)
                        ?.optJSONObject("data") ?: JSONObject()
        )
        val commentsData = responseArray
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
        return Pair(post, comments)
    }

    suspend fun getMoreComments(link: String, children: List<String>): List<Comment> {
        val response = post("/api/morechildren", "api_type=json&link_id=$link&children=${children.joinToString(",")}")
        val commentsData = JSONObject(response).getJSONObject("json").getJSONObject("data").getJSONArray("things")
        val comments = mutableListOf<Comment>()
        for (i in 0 until commentsData.length()) {
            comments.add(Comment(commentsData.getJSONObject(i).getJSONObject("data")))
        }
        return comments
    }

    suspend fun getSubscriptions() : List<Subreddit> {
        val response = get("/subreddits/mine/subscriber?raw_json=1&limit=100")
        val subscriptions = mutableListOf<Subreddit>()
        try {
            // TODO This is called to early
            val subs = JSONObject(response)
                    .getJSONObject("data")
                    .getJSONArray("children")
            for (i in 0 until subs.length()) {
                val sub = subs.getJSONObject(i).getJSONObject("data")
                subscriptions.add(Subreddit(sub))
            }
        } catch (error: JSONException) {
            Log.e(TAG, "Failed to parse subscriptions", error)
        }
        return subscriptions
    }

    suspend fun subscribe(subreddit: String, unsub:Boolean=false) : String {
        return post("/api/subscribe?action=${if (unsub) "un" else ""}sub&sr_name=$subreddit")
    }

    suspend fun getMultis() : List<Subreddit> {
        val response = get("/api/multi/mine?raw_json=1")
        val list = mutableListOf<Subreddit>()
        if (response != "[]") {
            val data = JSONObject(response).getJSONArray("data")
            for (i in 0 until data.length()) {
                list.add(Subreddit(data.getJSONObject(i)))
            }
        }
        return list
    }

    suspend fun searchForSubreddit(query: String) : List<Subreddit> {
        val response = get("/api/search_reddit_names?raw_json=1&include_over_18=true&include_unadvertisable=true&query=$query")
        val list = mutableListOf<Subreddit>()
        if (response != "[]") {
            val data = JSONObject(response).getJSONArray("data")
            for (i in 0 until data.length()) {
                list.add(Subreddit(data.getJSONObject(i)))
            }
        }
        return list
    }

    suspend fun getSidebar(subreddit: String) : String {
        val response = get("/r/$subreddit/about.json?raw_json=1")
        return JSONObject(response).getJSONObject("data").getString("description")
    }

    suspend fun vote(id: String, dir: String) : String {
        return post("/api/vote?id=$id&dir=$dir")
    }

    suspend fun saveOrUnsave(saved: Boolean, name: String) : String {
        return post("/api/${if (saved) "un" else ""}save?id=$name")
        // saveable.saved = !saveable.saved
    }

    fun getFormattedScore(score: Int): String {
        return if (score >= 10000) (score / 1000).toString() + 'k' else score.toString()
    }

    private suspend fun post(url: String, bodyText: String? = null) : String {
        refreshAuthToken()
        return suspendCoroutine { continuation ->
            makeOAuthRequest("$oauthApi$url", Request.Method.POST, bodyText, { response ->
                continuation.resume(response)
            }, { error ->
                continuation.resumeWithException(error)
            })
        }
    }

    private suspend fun get(url: String): String {
        refreshAuthToken()
        return suspendCoroutine { continuation ->
            makeOAuthRequest("$oauthApi$url", Request.Method.GET, null, { response ->
                continuation.resume(response)
            }, { error ->
                continuation.resumeWithException(error)
            })
        }
    }

    private fun makeOAuthRequest(
            url: String,
            method: Int,
            content: String?,
            successListener: Response.Listener<String>,
            errorListener: Response.ErrorListener?
    ) {
        requestQueue.add(OAuthRequest(method, url, content, successListener, errorListener))
    }

    private suspend fun makeTokenRequest(url: String, bodyText: String) : String {
        return suspendCoroutine { continuation ->
            requestQueue.add(TokenRequest(url, bodyText,
                    { response -> continuation.resume(response) },
                    { error -> continuation.resumeWithException(error) }
            ))
        }
    }

    suspend fun onAuthorizationSuccessful(uri: Uri) : Boolean {
        if (uri.getQueryParameter("state") == randomState) {
            val code = uri.getQueryParameter("code")
            val url = "$api/api/v1/access_token"
            val bodyText = "grant_type=authorization_code&code=$code&redirect_uri=$redirectUriEncoded"
            try {
                val response = makeTokenRequest(url, bodyText)
                updateToken(JSONObject(response))
                Log.i(TAG, "User was successfully logged in")
                return true
            } catch (error: VolleyError) {
                Log.e(TAG, "Refreshing auth token failed", error)
            }
        } else {
            Log.e(TAG, "Random state mismatch")
        }
        return false
    }

    private suspend fun updateToken(response: JSONObject) {
        try {
            authToken = response.getString("access_token")
            validUntil = response.getLong("expires_in") + Util.getUnixTime()
            val optRefreshToken = response.optString("refresh_token")
            refreshToken = optRefreshToken ?: refreshToken
            store.updateTokens(authToken, validUntil!!, refreshToken)
        } catch (error: JSONException) {
            Log.e(TAG, "Failed to get auth token", error)
        }
    }

    private suspend fun refreshAuthToken() {
        val unixTime = Util.getUnixTime()
        if (validUntil != null && validUntil!! <= unixTime) {
            if (refreshToken != null && refreshToken != "") {
                val url = "${api}/api/v1/access_token"
                val bodyText = "grant_type=refresh_token&refresh_token=$refreshToken"
                try {
                    val response = makeTokenRequest(url, bodyText)
                    updateToken(JSONObject(response))
                } catch (error: VolleyError) {
                    Log.e(TAG, "Refreshing auth token failed", error)
                }
            } else {
                Log.w(TAG, "No refresh token")
            }
        }
    }
}