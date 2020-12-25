package de.max.roehrl.vueddit2.model

import de.max.roehrl.vueddit2.service.Reddit
import org.json.JSONObject

data class Comment(val json: JSONObject) : NamedItem(json.optString("name")) {
    val name = id
    val author = json.optString("author")
    val body = json.optString("body")
    val subreddit = json.optString("subreddit")
    val likes = json.optInt("likes")
    val ups = json.optInt("ups")
    val depth = json.optInt("depth")
    val children = mutableListOf<Comment>()
    val created_utc = json.optInt("created_utc")
    val edited = json.optBoolean("edited")
    val is_submitter = json.optBoolean("is_submitter", false)
    val distinguished = json.optString("distinguished", "")
    val author_flair_text = json.optString("author_flair_text")
    val author_flair_background_color = json.optString("author_flair_background_color")
    val gid_1 = json.optJSONObject("gildings")?.optInt("gid_1", 0) ?: 0
    val gid_2 = json.optJSONObject("gildings")?.optInt("gid_2", 0) ?: 0
    val gid_3 = json.optJSONObject("gildings")?.optInt("gid_3", 0) ?: 0

    fun getScore() : String {
        return Reddit.getFormattedScore(likes)
    }
}