package de.max.roehrl.vueddit2.model

import org.json.JSONObject

data class Subreddit(val json: String) : NamedItem("subreddit") {
    val name = "todo"
    val isMultiReddit = false
    val isStarred = false
    val isVisited = false
}