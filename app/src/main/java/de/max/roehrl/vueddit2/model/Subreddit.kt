package de.max.roehrl.vueddit2.model

import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.service.Reddit
import org.json.JSONObject

class Subreddit(json: JSONObject, var isMultiReddit: Boolean = false) : NamedItem("subreddit") {
    companion object {
        val frontPage = fromName(Reddit.frontpage)
        val all = fromName("all")
        val popular = fromName("popular")
        val random = fromName("random")
        val defaultSubreddits = listOf(frontPage, all, popular, random)

        fun fromName(name: String) : Subreddit {
            return Subreddit(JSONObject("{\"display_name\": \"$name\"}"))
        }

        fun fromUser(name: String) : Subreddit {
            return fromName(name).apply {
                user = name
                isSubscribedTo = false
            }
        }
    }

    val name = json.optString("display_name")
    val subreddits = getSubreddits(json)
    var user: String? = null
    var isSubscribedTo = true
    var isStarred = false
    var isVisited = true

    override fun toString(): String {
        return "$name (${if (isMultiReddit) subreddits else "isStarred: $isStarred, isVisited: $isStarred"})"
    }

    private fun getSubreddits(json: JSONObject) : String {
        val subreddits = json.optJSONArray("subreddits")
        val list = mutableListOf<String>()
        if (subreddits != null) {
            for (i in 0 until subreddits.length()) {
                list.add(subreddits.getJSONObject(i).getString("name"))
            }
        }
        return list.joinToString("+")
    }

    fun getIconId(): Int {
        return when {
            user != null -> R.drawable.ic_person
            isMultiReddit -> R.drawable.ic_folder_multiple
            defaultSubreddits.contains(this) ->  R.drawable.ic_home_outline
            isStarred -> R.drawable.ic_star
            isSubscribedTo -> R.drawable.ic_star_outline
            else -> R.drawable.ic_plus_thick
        }
    }

    override fun equals(other: Any?): Boolean {
        return (other is Subreddit) && name.equals(other.name, true)
                && isMultiReddit == other.isMultiReddit
    }

    override fun hashCode(): Int {
        var result = isMultiReddit.hashCode()
        result = 31 * result + name.lowercase().hashCode()
        return result
    }
}