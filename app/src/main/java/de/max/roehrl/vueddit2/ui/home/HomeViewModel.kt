package de.max.roehrl.vueddit2.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.max.roehrl.vueddit2.service.Reddit
import org.json.JSONObject

class HomeViewModel : ViewModel() {

    val posts = MutableLiveData<List<JSONObject>>().apply {
        val after = ""
        Reddit.getSubredditPosts(Reddit.frontpage, after, "best") {
            val g = it.getJSONObject("data").getJSONArray("children")
            val list = mutableListOf<JSONObject>()
            for (i in 0 until g.length()) {
                list.add(g.getJSONObject(i).getJSONObject("data"))
            }
            value = if (after == "") {
                list
            } else {
                list
            }
        }
    }
}