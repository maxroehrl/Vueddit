package de.max.roehrl.vueddit2.model

import android.content.Context
import android.text.Spanned
import de.max.roehrl.vueddit2.service.Markdown
import de.max.roehrl.vueddit2.service.Reddit
import org.json.JSONException
import org.json.JSONObject

class Comment(json: JSONObject) : NamedItem(json.optString("name")) {
    val name = id
    val author = json.optString("author")
    val body = json.optString("body", "")
    var spannedBody: Spanned? = null
    val subreddit = json.optString("subreddit")
    var likes: Boolean? = null
    val count = json.optInt("count", 0)
    var ups = json.optInt("ups", 0)
    var saved = json.optBoolean("saved", false)
    val depth = json.optInt("depth", 0)
    var children: List<NamedItem>? = null
    var moreChildren: List<String>
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
        moreChildren = mutableChildren.toList()
        likes = try {
            json.getBoolean("likes")
        } catch (e: JSONException) {
            null
        }
    }

    fun getSpannedBody(context: Context): Spanned? {
        if (spannedBody == null) {
            spannedBody = Markdown.getInstance(context).toMarkDown(body)
        }
        return spannedBody
    }

    fun getScore(): String {
        return Reddit.getFormattedScore(ups)
    }

    override fun equals(other: Any?): Boolean {
        return other is Comment && name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    fun isCollapsed(): Boolean {
        return children != null
    }
}