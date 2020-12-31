package de.max.roehrl.vueddit2.model

import de.max.roehrl.vueddit2.R
import org.json.JSONObject

class Subreddit(json: JSONObject) : NamedItem("subreddit") {
    companion object {
        val frontPage = fromName("reddit front page")
        val all = fromName("all")
        val popular = fromName("popular")
        val random = fromName("random")
        val defaultSubreddits = listOf(frontPage, all, popular, random)

        private fun fromName(name: String) : Subreddit {
            return Subreddit(JSONObject("{\"display_name\": \"$name\"}"))
        }
    }

    val name = json.optString("display_name")
    val subreddits = json.optJSONArray("subreddits")?.join("")
    val isMultiReddit = !subreddits.isNullOrEmpty()
    val isSubscribedTo = false
    val isStarred = false
    var isVisited = true

    override fun toString(): String {
        return "$name (${if (isMultiReddit) subreddits else "isStarred: $isStarred"})"
    }

    fun getIconId(): Int {
        return when {
            isMultiReddit -> R.drawable.ic_folder_multiple
            defaultSubreddits.contains(this) ->  R.drawable.ic_home_outline
            isStarred -> R.drawable.ic_star
            isSubscribedTo -> R.drawable.ic_star_outline
            else -> R.drawable.ic_plus_thick
        }
    }
}