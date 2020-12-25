package de.max.roehrl.vueddit2.model

import org.json.JSONObject

class Subreddit(json: String) : NamedItem("subreddit") {
    val name = "todo"
    val isMultiReddit = false
    val isStarred = false
    val isVisited = false
}