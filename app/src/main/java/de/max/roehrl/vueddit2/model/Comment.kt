package de.max.roehrl.vueddit2.model

import android.content.Context
import android.text.Spanned
import de.max.roehrl.vueddit2.service.Markdown
import de.max.roehrl.vueddit2.service.Reddit
import org.json.JSONObject

class Comment(json: JSONObject) : NamedItem(json.optString("name")) {
    val name = id
    val author = json.optString("author")
    val body = json.optString("body", "")
    var spannedBody: Spanned? = null
    val subreddit = json.optString("subreddit")
    val likes = json.optInt("likes", 0)
    val count = json.optInt("count", 0)
    val ups = json.optInt("ups", 0)
    val depth = json.optInt("depth", 0)
    val children: List<String>
    val created_utc = json.optInt("created_utc")
    val permalink = json.optString("permalink")
    val edited = json.optBoolean("edited", false)
    val is_submitter = json.optBoolean("is_submitter", false)
    val distinguished = json.optString("distinguished", "")
    val parent_id = json.optString("parent_id", "")
    val author_flair_text = json.optString("author_flair_text")
    val author_flair_background_color = json.optString("author_flair_background_color")
    val gid_1 = json.optJSONObject("gildings")?.optInt("gid_1", 0)
    val gid_2 = json.optJSONObject("gildings")?.optInt("gid_2", 0)
    val gid_3 = json.optJSONObject("gildings")?.optInt("gid_3", 0)
    var isLoading = false

    init {
        val mutableChildren = mutableListOf<String>()
        val childrenData = json.optJSONArray("children")
        if (childrenData != null) {
            for (i in 0 until childrenData.length()) {
                mutableChildren.add(childrenData.getString(i))
            }
        }
        children = mutableChildren.toList()
    }

    fun getSpannedBody(context: Context): Spanned? {
        if (spannedBody == null) {
            spannedBody = Markdown.getInstance(context).toMarkDown(body)
        }
        return spannedBody
    }

    fun getScore(): String {
        return Reddit.getFormattedScore(likes)
    }
}