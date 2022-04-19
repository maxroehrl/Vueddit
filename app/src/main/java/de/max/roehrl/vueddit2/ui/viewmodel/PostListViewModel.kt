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
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    companion object {
        protected const val TAG = "PostListViewModel"
        private const val defaultTopPostTime = "all"
        private const val SUBREDDIT = "subreddit"
        private const val STARTING_SUBREDDIT = "startingSubreddit"
        private const val POST_SORTING = "postSorting"
        private const val POSTS = "posts"
        private const val TOP_POSTS_TIME = "topPostsTime"
    }

    protected open val defaultSorting = "hot"
    open val sortingList = listOf("hot", "top", "new", "best", "controversial", "rising")

    @Suppress("LeakingThis")
    var postSorting: String = savedStateHandle.get(POST_SORTING) ?: defaultSorting
    var topPostsTime: String = savedStateHandle.get(TOP_POSTS_TIME) ?: defaultTopPostTime
    var startingSubreddit: String? = savedStateHandle.get(STARTING_SUBREDDIT)

    val subreddit: LiveData<Subreddit> = liveData {
        val saved: Subreddit? = savedStateHandle.get(SUBREDDIT) ?: startingSubreddit?.let {
            Subreddit.fromName(it)
        }
        if (saved != null) {
            emit(saved)
        }
    }

    val posts: LiveData<List<NamedItem>> = liveData {
        val saved: List<NamedItem>? = savedStateHandle.get(POSTS)
        emit(saved ?: listOf(NamedItem.Loading))
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
                getMorePosts(
                    after,
                    postSorting,
                    topPostsTime,
                    25 * ((posts.value?.size ?: 0) / 25 + 1)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error getting posts", e)
                emptyList()
            }
            val names = oldPosts.map { it.id }
            // Filter duplicate posts
            (posts as MutableLiveData).postValue(oldPosts + newPosts.filter { !names.contains(it.id) })
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
        return Reddit.getInstance(getApplication())
            .getSubredditPosts(subredditName, after, sorting, time, count)
    }

    fun refreshPosts(showLoadingIndicator: Boolean = true, cb: (() -> Unit)? = null) {
        resetPosts()
        loadMorePosts(showLoadingIndicator, cb)
    }

    fun resetPosts() {
        (posts as MutableLiveData).value = emptyList()
    }

    open fun saveBundle() {
        savedStateHandle.set(POST_SORTING, postSorting)
        savedStateHandle.set(TOP_POSTS_TIME, topPostsTime)
        savedStateHandle.set(STARTING_SUBREDDIT, startingSubreddit)
        savedStateHandle.set(POSTS, posts.value)
        savedStateHandle.set(SUBREDDIT, subreddit.value)
    }
}