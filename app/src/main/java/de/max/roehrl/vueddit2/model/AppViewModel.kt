package de.max.roehrl.vueddit2.model

import android.app.Application
import android.webkit.CookieManager
import androidx.lifecycle.*
import de.max.roehrl.vueddit2.service.Reddit
import de.max.roehrl.vueddit2.service.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// https://developer.android.com/topic/libraries/architecture/coroutines
class AppViewModel(application: Application) : AndroidViewModel(application) {
    val isLoggedIn: LiveData<Boolean> = liveData(Dispatchers.IO) {
        emit(Reddit.login(application))
    }

    val username: LiveData<String?> = liveData(Dispatchers.IO) {
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

    val posts: MutableLiveData<List<NamedItem>> by lazy {
        MutableLiveData<List<NamedItem>>().also {
            it.value = listOf(NamedItem.Loading)
        }
    }

    val selectedPost = MutableLiveData<Post>()

    val userPostsAndComments: LiveData<List<NamedItem>> = liveData(Dispatchers.IO) {
        emit(listOf(NamedItem.Loading))
    }

    val selectedUser: LiveData<String> = liveData {
        emit("")
    }

    val selectedGroup: LiveData<String> = liveData {
        emit("overview")
    }

    val userSorting: LiveData<String> = liveData {
        emit("new")
    }

    val selectedType: LiveData<String> = liveData {
        emit("all")
    }

    val isBigTemplatePreferred: LiveData<Boolean?> = liveData {
        emit(null)
    }

    val comments: MutableLiveData<MutableList<NamedItem>> by lazy {
        MutableLiveData<MutableList<NamedItem>>().also {
            it.value = mutableListOf(NamedItem.Loading)
        }
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

    fun loadComments(cb: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val sorting = commentSorting.value ?: "top"
            val permalink = selectedPost.value!!.permalink + subtreeCommentName.value
            val pair = Reddit.getPostAndComments(permalink, sorting)
            selectedPost.postValue(pair.first)
            comments.postValue(pair.second.toMutableList())
            cb()
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
            val after = lastPost?.id ?: ""
            val userName = selectedUser.value!!
            val sorting = userSorting.value ?: "new"
            val group = userPostGroup.value ?: "overview"
            val p = Reddit.getUserPosts(userName, after, sorting, group)
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
                    posts.postValue(oldPosts + NamedItem)
                } else {
                    oldPosts.removeLast()
                }
            }
            val after = lastPost?.id ?: ""
            val subredditName = if (subreddit.value?.isMultiReddit == true) {
                subreddit.value!!.subreddits
            } else {
                subreddit.value?.name ?: Subreddit.frontPage.name
            }
            val sorting = postSorting.value ?: "best"
            val p = Reddit.getSubredditPosts(subredditName, after, sorting)
            posts.postValue(oldPosts + p)

            cb?.invoke()
        }
    }

    fun loadSubscriptions() {
        viewModelScope.launch(Dispatchers.IO) {
            val subscriptions = Reddit.getSubscriptions()
                    .sortedWith(compareBy { sub: Subreddit -> sub.isStarred }.thenBy { it.isVisited })
            val multiReddits = Reddit.getMultis()
            val all = listOf(Subreddit.defaultSubreddits, subscriptions, multiReddits).flatten().toMutableList()
            subreddits.postValue(all)
        }
    }

    fun setPostSorting(sorting: String) {
        (postSorting as MutableLiveData).value = sorting
    }

    fun refreshPosts(showLoadingIndicator: Boolean = true, cb: (() -> Unit)? = null) {
        posts.value = emptyList()
        loadMorePosts(showLoadingIndicator, cb)
    }

    fun setUserPostSorting(sorting: String) {
        (userSorting as MutableLiveData).value = sorting
    }

    fun refreshUserPosts(showLoadingIndicator: Boolean = true, cb: (() -> Unit)? = null) {
        (userPostsAndComments as MutableLiveData).value = emptyList()
        loadMoreUserPosts(showLoadingIndicator, cb)
    }

    fun updateSearchText(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (text.isEmpty()) {
                loadSubscriptions()
            } else {
                val searchResults = Reddit.searchForSubreddit(text)
                subreddits.postValue(searchResults.toMutableList())
            }
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

    fun resetComments() {
        comments.value = mutableListOf(NamedItem.Loading)
    }

    fun loadMoreComments(comment: Comment) {
        if (comment.count == 0) {
            (subtreeCommentName as MutableLiveData).value = comment.parent_id.split("_")[1]
            loadComments {}
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                val newComments = Reddit.getMoreComments(selectedPost.value!!.name, comment.children)
                val index = comments.value?.indexOf(comment)
                val oldComments = comments.value!!
                oldComments.remove(comment)
                oldComments.addAll(index!!, newComments)
                comments.postValue(oldComments)
            }
        }
    }

    fun setUserPostGroup(group: String) {
        (userPostGroup as MutableLiveData).value = group
    }
}