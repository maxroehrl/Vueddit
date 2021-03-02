package de.max.roehrl.vueddit2.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import de.max.roehrl.vueddit2.model.NamedItem
import de.max.roehrl.vueddit2.model.Subreddit
import de.max.roehrl.vueddit2.service.Reddit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class PostListViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        protected const val TAG = "PostListViewModel"
        private const val defaultTopPostTime = "all"
    }
    protected open val defaultSorting = "hot"
    open val sortingList = listOf("hot", "top", "new", "best", "controversial", "rising")

    val subreddit: LiveData<Subreddit> = liveData { }

    open val postSorting: LiveData<String> = liveData {
        emit(defaultSorting)
    }

    val posts: LiveData<List<NamedItem>> = liveData {
        emit(listOf(NamedItem.Loading))
    }

    private val topPostsTime: LiveData<String> = liveData {
        emit(defaultTopPostTime)
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
                    (posts as MutableLiveData).postValue(oldPosts + NamedItem)
                } else {
                    oldPosts.removeLastOrNull()
                }
            }
            val after = lastPost?.id ?: ""
            val sorting = postSorting.value ?: defaultSorting
            val time = topPostsTime.value ?: defaultTopPostTime
            val p = getMorePosts(after, sorting, time, getPostCount())
            (posts as MutableLiveData).postValue(oldPosts + p)
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

    fun setPostSorting(sorting: String) {
        (postSorting as MutableLiveData).value = sorting
    }

    fun setTopPostsTime(time : String) {
        (topPostsTime as MutableLiveData).value = time
    }

    fun resetPosts() {
        (posts as MutableLiveData).value = emptyList()
    }

    private fun getPostCount(): Int {
        val size = posts.value?.size ?: 0
        return 25 * (size / 25 + 1)
    }
}