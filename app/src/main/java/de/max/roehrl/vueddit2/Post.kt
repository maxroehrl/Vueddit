package de.max.roehrl.vueddit2

import org.json.JSONObject

data class Post(val json : JSONObject) {
    val name = json.getString("name")
    val title = json.getString("title")
    val link_flair_text = json.getString("link_flair_text ")
    val link_flair_background_color = json.getString("link_flair_background_color ")
    val stickied = json.getBoolean("stickied")
    val domain = json.getString("domain")
    val over18 = json.getBoolean("over_18")
    val spoiler = json.getBoolean("spoiler")
    val num_comments = json.getInt("num_comments")
    val subreddit = json.getString("subreddit")
    val author = json.getString("author")
    val author_flair_text = json.getString("author_flair_text")
    val author_flair_background_color = json.getString("author_flair_background_color")
    val gid_1 = json.optJSONObject("gildings")?.optInt("gid_1", 0)
    val gid_2 = json.optJSONObject("gildings")?.optInt("gid_2", 0)
    val gid_3 = json.optJSONObject("gildings")?.optInt("gid_3", 0)
    val edited = json.getBoolean("edited")
    val created_utc = json.getInt("created_utc")
    val selftext = json.getString("selftext")
    val shown_comments = 0
}