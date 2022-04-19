package de.max.roehrl.vueddit2.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import de.max.roehrl.vueddit2.model.NamedItem
import de.max.roehrl.vueddit2.model.Subreddit
import de.max.roehrl.vueddit2.service.Reddit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class PostListViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    companion object {
        protected const val TAG = "PostListViewModel"
        private const val defaultTopPostTime = "all"
        private const val SUBREDDIT = "subreddit"
        private const val POST_SORTING = "postSorting"
        private const val POSTS = "posts"
        private const val TOP_POSTS_TIME = "topPostsTime"
    }
    protected open val defaultSorting = "hot"
    open val sortingList = listOf("hot", "top", "new", "best", "controversial", "rising")
    var postSorting: String = savedStateHandle.get(POST_SORTING) ?: defaultSorting
    var topPostsTime: String = savedStateHandle.get(TOP_POSTS_TIME) ?: defaultTopPostTime

    val subreddit: LiveData<Subreddit> = liveData {
        val saved: Subreddit? = savedStateHandle.get(SUBREDDIT)
        if (saved != null) {
            emit(saved)
        }
    }

    val posts: LiveData<List<NamedItem>> = liveData {
        val saved: List<NamedItem>? = savedStateHandle.get(POSTS)
        emit(saved ?: listOf(NamedItem.Loading))
    }

    fun selectSubreddit(subredditName: String) {
        selectSubreddit(Subreddit.fromName(subredditName))
    }

    fun selectSubreddit(sub: Subreddit) {
        (subreddit as MutableLiveData).value = sub
    }

    fun isPostListLoading(): Boolean {
        return try {
            posts.value?.last() == NamedItem.Loading
        } catch (error: NoSuchElementException) {
            false
        }
    }

    fun loadMorePosts(showLoadingIndicator: Boolean = true, cb: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val lastPost = posts.value?.lastOrNull { post -> post != NamedItem.Loading }
            val oldPosts = posts.value?.toMutableList() ?: mutableListOf()
            if (showLoadingIndicator) {
                if (!isPostListLoading()) {
                    (posts as MutableLiveData).postValue(oldPosts + NamedItem.Loading)
                } else {
                    oldPosts.removeLastOrNull()
                }
            }
            val after = lastPost?.id ?: ""
            val newPosts = try {
                getMorePosts(after, postSorting, topPostsTime, getPostCount())
            } catch (e: Exception) {
                Log.e(TAG, "Error getting posts", e)
                emptyList()
            }
            (posts as MutableLiveData).postValue(oldPosts + newPosts)
            cb?.invoke()
        }
    }

    protected open suspend fun getMorePosts(
            after: String,
            sorting: String,
            time: String,
            count: Int,
    ): List<NamedItem> {
        val subredditName = if (subreddit.value?.isMultiReddit == true) {
            subreddit.value!!.subreddits
        } else {
            subreddit.value?.name ?: Subreddit.frontPage.name
        }
        return Reddit.getInstance(getApplication()).getSubredditPosts(subredditName, after, sorting, time, count)
    }

    fun refreshPosts(showLoadingIndicator: Boolean = true, cb: (() -> Unit)? = null) {
        resetPosts()
        loadMorePosts(showLoadingIndicator, cb)
    }

    fun resetPosts() {
        (posts as MutableLiveData).value = emptyList()
    }

    private fun getPostCount(): Int {
        val size = posts.value?.size ?: 0
        return 25 * (size / 25 + 1)
    }

    open fun saveBundle() {
        savedStateHandle.set(POST_SORTING, postSorting)
        savedStateHandle.set(TOP_POSTS_TIME, topPostsTime)
        savedStateHandle.set(POSTS, posts.value)
        savedStateHandle.set(SUBREDDIT, subreddit.value)
    }
}