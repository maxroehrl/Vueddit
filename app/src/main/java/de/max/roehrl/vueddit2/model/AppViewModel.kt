package de.max.roehrl.vueddit2.model

import android.app.Application
import androidx.lifecycle.*
import de.max.roehrl.vueddit2.service.Reddit
import de.max.roehrl.vueddit2.service.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// https://developer.android.com/topic/libraries/architecture/coroutines
class AppViewModel(application: Application) : AndroidViewModel(application) {
    val isLoggedIn: LiveData<Boolean> = liveData {
        emit(Reddit.login(application))
    }

    val username: LiveData<String?> = liveData {
        emit(Store.getInstance(application).getUsername())
    }

    val subreddits: MutableLiveData<MutableList<Subreddit>> by lazy {
        MutableLiveData<MutableList<Subreddit>>().also {
            it.value = Subreddit.defaultSubreddits.toMutableList()
        }
    }

    val subreddit: LiveData<Subreddit> = liveData {
        emit(Subreddit.frontPage)
    }

    fun isPostListLoading() : Boolean {
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
            val subredditName = subreddit.value?.name ?: Subreddit.frontPage.name
            val p = Reddit.getSubredditPosts(subredditName, after, "best")
            posts.value?.remove(NamedItem.Loading)
            posts.value?.addAll(p)
            posts.postValue(posts.value)
            cb()
        }
    }

    fun loadSubscriptions() {
        viewModelScope.launch(Dispatchers.IO) {
            val subscriptions = Reddit.getSubscriptions()
            val multis = Reddit.getMultis()
            //.sortedWith(compareBy { sub: Subreddit -> !sub.isMultiReddit }.thenBy { it.isVisited })
            subreddits.postValue(listOf(Subreddit.defaultSubreddits, subscriptions).flatten().toMutableList())
        }
    }

    fun refreshPosts(cb: () -> Unit) {
        posts.value?.clear()
        this.loadMorePosts(cb)
    }

    fun updateSearchText(text: String) {
        viewModelScope.launch {
            val searchResults = Reddit.searchForSubreddit(text)
            subreddits.postValue(searchResults.toMutableList())
        }
    }
}