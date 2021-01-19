package de.max.roehrl.vueddit2.model

import android.app.Application
import android.util.Log
import android.webkit.CookieManager
import androidx.lifecycle.*
import de.max.roehrl.vueddit2.service.Reddit
import de.max.roehrl.vueddit2.service.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.NoSuchElementException
import kotlin.math.min

// https://developer.android.com/topic/libraries/architecture/coroutines
class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "AppViewModel"

    val isLoggedIn: LiveData<Boolean> = liveData(Dispatchers.IO) {
        emit(Reddit.login(application))
    }

    val username: LiveData<String?> = liveData(Dispatchers.IO) {
        emit(Store.getInstance(application).getUsername())
    }

    val subreddits: LiveData<List<Subreddit>> = liveData {
        emit(Subreddit.defaultSubreddits)
    }

    val subreddit: LiveData<Subreddit> = liveData {
        emit(Subreddit.frontPage)
    }

    fun isPostListLoading(): Boolean {
        return try {
            posts.value?.last() == NamedItem.Loading
        } catch (error: NoSuchElementException) {
            false
        }
    }

    fun isUserPostListLoading(): Boolean {
        return try {
            userPostsAndComments.value?.last() == NamedItem.Loading
        } catch (error: NoSuchElementException) {
            false
        }
    }

    val posts: LiveData<List<NamedItem>> = liveData {
        emit(listOf(NamedItem.Loading))
    }

    val selectedPost = MutableLiveData<Post>()

    val userPostsAndComments: LiveData<List<NamedItem>> = liveData(Dispatchers.IO) {
        emit(listOf(NamedItem.Loading))
    }

    val selectedUser: LiveData<String> = liveData {
    }

    val userSorting: LiveData<String> = liveData {
        emit("new")
    }

    val selectedType: LiveData<String> = liveData {
        emit("all")
    }

    val topPostsTime: LiveData<String> = liveData {
        emit("all")
    }

    val isBigTemplatePreferred: LiveData<Boolean?> = liveData {
        emit(null)
    }

    val comments: LiveData<MutableList<NamedItem>> = liveData {
        emit(mutableListOf<NamedItem>(NamedItem.Loading))
    }

    val subtreeCommentName: LiveData<String> = liveData {
        emit("")
    }

    val postSorting: LiveData<String> = liveData {
        emit("best")
    }

    val commentSorting: LiveData<String> = liveData {
        emit("top")
    }

    val userPostGroup: LiveData<String> = liveData {
        emit("overview")
    }

    fun loadComments(cb: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val sorting = commentSorting.value ?: "top"
            val permalink = selectedPost.value!!.permalink + subtreeCommentName.value
            val pair = Reddit.getPostAndComments(permalink, sorting)
            selectedPost.postValue(pair.first)
            (comments as MutableLiveData).postValue(pair.second.toMutableList())
            cb?.invoke()
        }
    }

    fun loadMoreUserPosts(showLoadingIndicator: Boolean = true, cb: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            var lastPost: NamedItem? = null
            try {
                lastPost = userPostsAndComments.value?.last { post -> post != NamedItem.Loading }
            } catch (e: NoSuchElementException) {

            }
            val oldPosts = userPostsAndComments.value?.toMutableList() ?: mutableListOf()
            if (showLoadingIndicator) {
                if (!isPostListLoading()) {
                    (userPostsAndComments as MutableLiveData).postValue(oldPosts + NamedItem)
                } else {
                    oldPosts.removeLast()
                }
            }
            val userName = selectedUser.value!!
            val after = lastPost?.id ?: ""
            val sorting = userSorting.value ?: "new"
            val group = userPostGroup.value ?: "overview"
            val time = topPostsTime.value ?: "all"
            val type = selectedType.value ?: "all"
            val p = Reddit.getUserPosts(userName, after, sorting, group, time, type)
            (userPostsAndComments as MutableLiveData).postValue(oldPosts + p)

            cb?.invoke()
        }
    }

    fun loadMorePosts(showLoadingIndicator: Boolean = true, cb: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            var lastPost: NamedItem? = null
            try {
                lastPost = posts.value?.last { post -> post != NamedItem.Loading }
            } catch (e: NoSuchElementException) {

            }
            val oldPosts = posts.value?.toMutableList() ?: mutableListOf()
            if (showLoadingIndicator) {
                if (!isPostListLoading()) {
                    (posts as MutableLiveData).postValue(oldPosts + NamedItem)
                } else {
                    oldPosts.removeLast()
                }
            }
            val subredditName = if (subreddit.value?.isMultiReddit == true) {
                subreddit.value!!.subreddits
            } else {
                subreddit.value?.name ?: Subreddit.frontPage.name
            }
            val after = lastPost?.id ?: ""
            val sorting = postSorting.value ?: "best"
            val time = topPostsTime.value ?: "all"
            val p = Reddit.getSubredditPosts(subredditName, after, sorting, time)
            (posts as MutableLiveData).postValue(oldPosts + p)

            cb?.invoke()
        }
    }

    fun loadSubscriptions() {
        viewModelScope.launch(Dispatchers.IO) {
            val subscriptions = Reddit.getSubscriptions()
                .sortedWith(compareBy { sub: Subreddit -> sub.isStarred }
                    .thenBy { it.isVisited }
                    .thenBy { it.name.toLowerCase(Locale.getDefault()) })
            val multiReddits = Reddit.getMultis()
            val all = listOf(Subreddit.defaultSubreddits, subscriptions, multiReddits).flatten().toMutableList()
            (subreddits as MutableLiveData).postValue(all)
        }
    }

    fun setPostSorting(sorting: String) {
        (postSorting as MutableLiveData).value = sorting
    }

    fun refreshPosts(showLoadingIndicator: Boolean = true, cb: (() -> Unit)? = null) {
        (posts as MutableLiveData).value = emptyList()
        loadMorePosts(showLoadingIndicator, cb)
    }

    fun setUserPostSorting(sorting: String) {
        (userSorting as MutableLiveData).value = sorting
    }

    fun setSavedPostsType(type: String) {
        (selectedType as MutableLiveData).value = type
    }

    fun setTopPostsTime(time : String) {
        (topPostsTime as MutableLiveData).value = time
    }

    fun refreshUserPosts(showLoadingIndicator: Boolean = true, cb: (() -> Unit)? = null) {
        (userPostsAndComments as MutableLiveData).value = emptyList()
        loadMoreUserPosts(showLoadingIndicator, cb)
    }

    val searchResults: LiveData<List<Subreddit>?> = liveData {
        emit(null)
    }

    fun updateSearchText(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            (searchResults as MutableLiveData).postValue(if (text.isEmpty()) null else Reddit.searchForSubreddit(text))
        }
    }

    fun selectSubreddit(name: String, isMultiReddit: Boolean) {
        var sub = subreddits.value?.find { subreddit ->
            subreddit.name == name && subreddit.isMultiReddit == isMultiReddit
        }
        if (sub == null) {
            sub = Subreddit.fromName(name)
        }
        (subreddit as MutableLiveData).value = sub
    }

    fun setSelectedUser(username: String) {
        (selectedUser as MutableLiveData).value = username
    }

    fun logoutUser() {
        CookieManager.getInstance().removeAllCookies(null)
        viewModelScope.launch(Dispatchers.IO) {
            Store.getInstance(getApplication()).logoutUser()
            (username as MutableLiveData).postValue(null)
            Reddit.login(getApplication())
        }
    }

    fun refreshComments(cb: (() -> Unit)? = null) {
        resetComments()
        loadComments(cb)
    }

    fun resetComments() {
        (comments as MutableLiveData).value = mutableListOf(NamedItem.Loading)
    }

    fun loadMoreComments(comment: Comment) {
        if (comment.count == 0) {
            (subtreeCommentName as MutableLiveData).value = comment.parent_id.split("_")[1]
            loadComments()
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                val newComments = Reddit.getMoreComments(selectedPost.value!!.name, comment.children)
                val index = comments.value?.indexOf(comment)
                if (index != null && index >= 0) {
                    val oldComments = comments.value!!
                    oldComments.remove(comment)
                    oldComments.addAll(index, newComments)
                    (comments as MutableLiveData).postValue(oldComments)
                } else {
                    Log.e(TAG, "Error loading more comments")
                }
            }
        }
    }

    fun setUserPostGroup(group: String) {
        (userPostGroup as MutableLiveData).value = group
    }

    fun saveOrUnsave(post: Post) {
        viewModelScope.launch {
            val response = Reddit.saveOrUnsave(post.saved, post.name)
            Log.d(TAG, "Post saved $response")
            post.saved = !post.saved
        }
    }

    fun toggleBigPreview(postList: List<NamedItem>) {
        (isBigTemplatePreferred as MutableLiveData).value = if (isBigTemplatePreferred.value != null) {
            !isBigTemplatePreferred.value!!
        } else {
            !shouldShowBigTemplate(postList, null)
        }
    }

    fun shouldShowBigTemplate(postList: List<NamedItem>, currentSubreddit: Subreddit?): Boolean {
        return if (isBigTemplatePreferred.value != null) {
            isBigTemplatePreferred.value!!
        } else {
            val subHasMoreThanHalfPictures = postList
                    .subList(min(2, postList.size), min(10, postList.size))
                    .map { item -> if (item is Post && item.image.url != null) 1 else 0 }
                    .fold(0) { acc, it -> acc + it } > 4
            val isNotFrontPage = currentSubreddit != null && currentSubreddit != Subreddit.frontPage
            subHasMoreThanHalfPictures && isNotFrontPage
        }
    }
}