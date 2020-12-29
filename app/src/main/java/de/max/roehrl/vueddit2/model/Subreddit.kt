package de.max.roehrl.vueddit2.model

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
    val isStarred = false
    var isVisited = true
}