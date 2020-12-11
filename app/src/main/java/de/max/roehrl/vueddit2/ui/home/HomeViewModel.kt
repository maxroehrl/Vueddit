package de.max.roehrl.vueddit2.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.max.roehrl.vueddit2.Post
import de.max.roehrl.vueddit2.service.Reddit

class HomeViewModel : ViewModel() {

    val posts = MutableLiveData<MutableList<Post>>().apply {
        val after = ""
        Reddit.getSubredditPosts(Reddit.frontpage, after, "best") {
            val g = it.getJSONObject("data").getJSONArray("children")
            val list = mutableListOf<Post>()
            for (i in 0 until g.length()) {
                list.add(Post(g.getJSONObject(i).getJSONObject("data")))
            }
            value = if (after == "") {
                list
            } else {
                list
            }
        }
    }
}