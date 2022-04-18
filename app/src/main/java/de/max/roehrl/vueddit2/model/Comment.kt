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
data class Comment(
    val name: String,
    val author: String,
    val body: String,
    val subreddit: String,
    var likes: Boolean?,
    val count: Int,
    var ups: Int,
    var saved: Boolean,
    val depth: Int,
    var children: List<NamedItem>? = null,
    var moreChildren: List<String>,
    val created_utc: Int,
    val permalink: String,
    val edited: Boolean,
    val is_submitter: Boolean,
    val distinguished: String,
    val parent_id: String,
    val author_flair_text: String,
    val author_flair_background_color: String,
    val gid_1: Int?,
    val gid_2: Int?,
    val gid_3: Int?,
    var isLoading: Boolean = false,
) : NamedItem(name) {
    companion object {
        private const val TAG = "Comment"

        fun fromJson(json: JSONObject): Comment? {
            return try {
                Comment(json)
            } catch (error: JSONException) {
                Log.e(TAG, "Failed to parse json string to comment: '$json'", error)
                null
            }
        }

        fun getMoreChildren(json: JSONObject) : List<String> {
            val mutableChildren = mutableListOf<String>()
            val childrenData = json.optJSONArray("children")
            if (childrenData != null) {
                for (i in 0 until childrenData.length()) {
                    mutableChildren.add(childrenData.getString(i))
                }
            }
            return mutableChildren.toList()
        }
    }

    @IgnoredOnParcel
    private var spannedBody: Spanned? = null

    private constructor(json: JSONObject) : this(
        name = json.optString("name"),
        author = json.optString("author"),
        body = json.optString("body", ""),
        subreddit = json.optString("subreddit"),
        likes = if (json.isNull("likes")) null else json.optBoolean("likes"),
        count = json.optInt("count", 0),
        ups = json.optInt("ups", 0),
        saved = json.optBoolean("saved", false),
        depth = json.optInt("depth", 0),
        moreChildren = getMoreChildren(json),
        created_utc = json.optInt("created_utc"),
        permalink = json.optString("permalink"),
        edited = json.optBoolean("edited", false),
        is_submitter = json.optBoolean("is_submitter", false),
        distinguished = json.optString("distinguished", ""),
        parent_id = json.optString("parent_id", ""),
        author_flair_text = json.optString("author_flair_text"),
        author_flair_background_color = json.optString("author_flair_background_color"),
        gid_1 = json.optJSONObject("gildings")?.optInt("gid_1", 0),
        gid_2 = json.optJSONObject("gildings")?.optInt("gid_2", 0),
        gid_3 = json.optJSONObject("gildings")?.optInt("gid_3", 0),
        isLoading = false,
    )

    private constructor(parcel: Parcel) : this(
        JSONObject(parcel.readString()!!)
    )

    fun getSpannedBody(context: Context): Spanned? {
        if (spannedBody == null) {
            spannedBody = Markdown.getInstance(context).toMarkDown(body)
        }
        return spannedBody
    }

    fun getScore(): String {
        return Util.getFormattedScore(ups)
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

    fun saveOrUnsave(context: Context, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            Reddit.getInstance(context).saveOrUnsave(saved, name)
            saved = !saved
        }
    }
}