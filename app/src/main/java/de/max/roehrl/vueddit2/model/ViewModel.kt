package de.max.roehrl.vueddit2.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.max.roehrl.vueddit2.service.Reddit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppViewModel: ViewModel() {
    val subreddit: MutableLiveData<Subreddit> by lazy {
        MutableLiveData<Subreddit>().also {
            it.value = Subreddit(Reddit.frontpage)
        }
    }

    fun isLoading() : Boolean {
        return posts.value?.last() == NamedItem.Loading
    }

    val posts: MutableLiveData<MutableList<NamedItem>> by lazy {
        MutableLiveData<MutableList<NamedItem>>().also {
            it.value = mutableListOf()
        }
    }

    val selectedPost = MutableLiveData<Post>()

    val comments: MutableLiveData<MutableList<NamedItem>> by lazy {
        MutableLiveData<MutableList<NamedItem>>().also {
            it.value = mutableListOf(NamedItem.Loading)
        }
    }

    fun loadComments(cb: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val pair = Reddit.getPostAndComments(selectedPost.value!!.permalink, "top")
            selectedPost.postValue(pair.first)
            comments.postValue(pair.second.toMutableList())
            cb()
        }
    }

    fun loadMorePosts(cb: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var lastPost: NamedItem? = null
            try {
                lastPost = posts.value?.last { post -> post != NamedItem.Loading }
            } catch (e: NoSuchElementException) {

            }
            posts.value?.add(NamedItem.Loading)
            posts.postValue(posts.value)
            val after = lastPost?.id ?: ""
            val p = Reddit.getSubredditPosts(Reddit.frontpage, after, "best")
            posts.value?.remove(NamedItem.Loading)
            posts.value?.addAll(p)
            posts.postValue(posts.value)
            cb()
        }
    }

    fun refreshPosts(cb: () -> Unit) {
        posts.value?.clear()
        this.loadMorePosts(cb)
    }
}