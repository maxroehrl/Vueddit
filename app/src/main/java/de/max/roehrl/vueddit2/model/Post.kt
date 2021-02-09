package de.max.roehrl.vueddit2.model

import android.content.Context
import android.text.Spanned
import android.util.Log
import de.max.roehrl.vueddit2.service.Markdown
import de.max.roehrl.vueddit2.service.Reddit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject

class Post(private val json: JSONObject) : NamedItem(json.optString("name")) {
    companion object {
        private const val TAG = "Post"

        fun fromJSONString(json: String): Post? {
            return try {
                Post(JSONObject(json))
            } catch (error: JSONException) {
                Log.e(TAG, "Failed to parse json string to post: '$json'", error)
                null
            }
        }
    }
    val name = id
    val title = json.optString("title")
    val permalink = json.optString("permalink")
    val link_flair_text = json.optString("link_flair_text")
    val link_flair_background_color = json.optString("link_flair_background_color")
    val stickied = json.optBoolean("stickied", false)
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
    var score = json.optInt("score", 0)
    var likes: Boolean? = null
    var saved = json.optBoolean("saved", false)
    val is_gallery = json.optBoolean("is_gallery", false)
    val domain = if (is_gallery) "reddit/gallery" else json.optString("domain")
    val preview: Image = Image(json, 300)
    val image: Image = Image(json)
    val video: Video = Video(json)

    init {
        likes = try {
            json.getBoolean("likes")
        } catch (e: JSONException) {
            null
        }
    }

    fun getSpannedSelftext(context: Context): Spanned? {
        if (spannedSelftext == null) {
            spannedSelftext = Markdown.getInstance(context).toMarkDown(selftext)
        }
        return spannedSelftext
    }

    fun getScore(): String {
        return Reddit.getFormattedScore(score)
    }

    fun getJSONString(): String {
        return json.toString()
    }

    override fun toString(): String {
        return "Post $name ('$title') by $author"
    }

    override fun equals(other: Any?): Boolean {
        return other is Post && other.name == name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    fun saveOrUnsave() {
        GlobalScope.launch(Dispatchers.IO) {
            Reddit.saveOrUnsave(saved, name)
            saved = !saved
        }
    }
}