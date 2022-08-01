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
        private const val SUBREDDIT = "subreddit"
        private const val POST_SORTING = "postSorting"
        private const val POST_SORTINGS = "postSortings"
        private const val POSTS = "posts"
        private const val TOP_POSTS_TIME = "topPostsTime"
    }

    val sortingList: LiveData<List<String>> = savedStateHandle.getLiveData(POST_SORTINGS, getPostSortingList(true))
    var postSorting: String = savedStateHandle[POST_SORTING] ?: "best"
    var topPostsTime: String = savedStateHandle[TOP_POSTS_TIME] ?: "all"
    val subreddit: LiveData<Subreddit> = savedStateHandle.getLiveData(SUBREDDIT, Subreddit.frontPage)
    val posts: LiveData<List<NamedItem>> = savedStateHandle.getLiveData(POSTS, listOf(NamedItem.Loading))

    open fun getPostSortingList(isFrontpage: Boolean): List<String> {
        return if (isFrontpage) {
            listOf("best", "hot", "top", "new", "rising", "controversial")
        } else {
            listOf("hot", "top", "new", "rising", "controversial")
        }
    }

    fun selectSubreddit(sub: Subreddit) {
        val old = subreddit.value
        savedStateHandle[SUBREDDIT] = sub
        if (old != sub) {
            savedStateHandle[POST_SORTINGS] = getPostSortingList(sub == Subreddit.frontPage)
            refreshPosts()
        }
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
                    25 * ((posts.value?.size ?: 0) / 25 + 1),
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
        savedStateHandle[POSTS] = emptyList<NamedItem>()
    }
}