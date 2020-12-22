package de.max.roehrl.vueddit2.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.max.roehrl.vueddit2.Post
import de.max.roehrl.vueddit2.service.Reddit

class HomeViewModel : ViewModel() {

    val posts = MutableLiveData<MutableList<Post>>().apply {
        val after = ""
        Reddit.getSubredditPosts(Reddit.frontpage, after, "best") {
            value = it
        }
    }
}