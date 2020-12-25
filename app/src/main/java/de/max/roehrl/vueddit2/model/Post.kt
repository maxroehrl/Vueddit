package de.max.roehrl.vueddit2.model

import android.content.Context
import android.text.Spanned
import de.max.roehrl.vueddit2.service.Markdown
import de.max.roehrl.vueddit2.service.Reddit
import org.json.JSONObject

class Post(json: JSONObject) : NamedItem(json.optString("name")) {
    val name = id
    val title = json.optString("title")
    val permalink = json.optString("permalink")
    val link_flair_text = json.optString("link_flair_text")
    val link_flair_background_color = json.optString("link_flair_background_color")
    val stickied = json.optBoolean("stickied", false)
    val domain = json.optString("domain")
    val url = json.optString("url")
    val over18 = json.optBoolean("over_18", false)
    val spoiler = json.optBoolean("spoiler", false)
    val num_comments = json.optInt("num_comments")
    val subreddit = json.optString("subreddit")
    val author = json.optString("author")
    val author_flair_text = json.optString("author_flair_text")
    val author_flair_background_color = json.optString("author_flair_background_color")
    val gid_1 = json.optJSONObject("gildings")?.optInt("gid_1", 0)
    val gid_2 = json.optJSONObject("gildings")?.optInt("gid_2", 0)
    val gid_3 = json.optJSONObject("gildings")?.optInt("gid_3", 0)
    val edited = json.optBoolean("edited")
    val created_utc = json.optInt("created_utc")
    val selftext = json.optString("selftext")
    var spannedSelftext: Spanned? = null
    val score = json.optInt("score", 0)
    val likes = json.optBoolean("likes", false)
    val shown_comments = 0
    val comments = listOf<JSONObject>()
    val previewUrl: String? = Reddit.getPreview(json)?.optString("url")
    val bigPreview: String? = null

    fun getSpannedSelftext(context: Context): Spanned? {
        if (spannedSelftext == null) {
            spannedSelftext = Markdown.getInstance(context).toMarkDown(selftext)
        }
        return spannedSelftext
    }

    fun getScore(): String {
        return Reddit.getFormattedScore(score)
    }

    override fun toString(): String {
        return "Post $name ('$title') by $author"
    }
}