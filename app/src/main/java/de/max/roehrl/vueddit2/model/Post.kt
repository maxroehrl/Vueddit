package de.max.roehrl.vueddit2.model

import android.content.Context
import android.os.Parcel
import android.text.Spanned
import android.util.Log
import de.max.roehrl.vueddit2.service.Markdown
import de.max.roehrl.vueddit2.service.Reddit
import de.max.roehrl.vueddit2.service.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject

@Parcelize
data class Post(
    val name: String,
    val title: String,
    // val permalink: String,
    val link_flair_text: String,
    val link_flair_background_color: String,
    val stickied: Boolean = false,
    val url: String,
    val over18: Boolean = false,
    val spoiler: Boolean = false,
    val num_comments: Int,
    val subreddit: String,
    val author: String,
    val author_flair_text: String?,
    val author_flair_background_color: String?,
    val gid_1: Int?,
    val gid_2: Int?,
    val gid_3: Int?,
    val edited: Boolean,
    val created_utc: Int,
    val selftext: String,
    var score: Int = 0,
    var likes: Boolean? = null,
    var saved: Boolean = false,
    val is_gallery: Boolean = false,
    val domain: String,
    val preview: Image,
    val image: Image,
    val video: Video,
) : NamedItem(name) {
    companion object {
        private const val TAG = "Post"

        fun fromJson(json: JSONObject): Post? {
            return try {
                Post(json)
            } catch (error: JSONException) {
                Log.e(TAG, "Failed to parse json string to post: '$json'", error)
                null
            }
        }
    }

    private constructor(json: JSONObject) : this(
        name = json.optString("name"),
        title = json.optString("title"),
        // permalink = json.optString("permalink"),
        link_flair_text = json.optString("link_flair_text"),
        link_flair_background_color = json.optString("link_flair_background_color"),
        stickied = json.optBoolean("stickied", false),
        url = json.optString("url"),
        over18 = json.optBoolean("over_18", false),
        spoiler = json.optBoolean("spoiler", false),
        num_comments = json.optInt("num_comments"),
        subreddit = json.optString("subreddit"),
        author = json.optString("author"),
        author_flair_text = json.optString("author_flair_text"),
        author_flair_background_color = json.optString("author_flair_background_color"),
        gid_1 = json.optJSONObject("gildings")?.optInt("gid_1", 0),
        gid_2 = json.optJSONObject("gildings")?.optInt("gid_2", 0),
        gid_3 = json.optJSONObject("gildings")?.optInt("gid_3", 0),
        edited = json.optBoolean("edited"),
        created_utc = json.optInt("created_utc"),
        selftext = json.optString("selftext"),
        score = json.optInt("score", 0),
        likes = if (json.isNull("likes")) null else json.optBoolean("likes"),
        saved = json.optBoolean("saved", false),
        is_gallery = json.optBoolean("is_gallery", false),
        domain = if (json.optBoolean(
                "is_gallery",
                false
            )
        ) "reddit/gallery" else json.optString("domain"),
        preview = Image.fromJson(json, 300),
        image = Image.fromJson(json),
        video = Video.fromJson(json),
    )

    private constructor(parcel: Parcel) : this(
        JSONObject(parcel.readString()!!)
    )

    @IgnoredOnParcel
    private var spannedSelftext: Spanned? = null

    fun getSpannedSelftext(context: Context): Spanned? {
        if (spannedSelftext == null) {
            spannedSelftext = Markdown.getInstance(context).toMarkDown(selftext)
        }
        return spannedSelftext
    }

    fun getScore(): String {
        return Util.getFormattedScore(score)
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

    fun saveOrUnsave(context: Context, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            Reddit.getInstance(context).saveOrUnsave(saved, name)
            saved = !saved
        }
    }
}