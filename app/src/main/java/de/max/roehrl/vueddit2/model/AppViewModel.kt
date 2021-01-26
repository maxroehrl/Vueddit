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

    val subreddits: LiveData<List<List<Subreddit>>> = liveData {
        emit(listOfNotNull(Subreddit.defaultSubreddits, subscribedSubreddits.value, multiReddits.value))
    }

    private val subscribedSubreddits: LiveData<List<Subreddit>> = liveData {
        emit(Store.getInstance(getApplication()).getCachedSubscribedSubreddits().map {
            Subreddit.fromName(it)
        }.toList())
    }

    private val multiReddits: LiveData<List<Subreddit>> = liveData {
        emit(Store.getInstance(getApplication()).getCachedMultiSubreddits().map {
            Subreddit.fromName(it).apply {
                isMultiReddit = true
            }
        }.toList())
    }

    private val visitedSubreddits: LiveData<List<Subreddit>> = liveData {
        emit(Store.getInstance(getApplication()).getVisitedSubreddits().map {
            Subreddit.fromName(it)
        }.toList())
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

    val userPostsAndComments: LiveData<List<NamedItem>> = liveData {
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

    val selectedComment: LiveData<Comment?> = liveData { }

    val postSorting: LiveData<String> = liveData {
        emit("best")
    }

    val commentSorting: LiveData<String> = liveData {
        emit("top")
    }

    val userPostGroup: LiveData<String> = liveData {
        emit("overview")
    }

    private fun loadComments(commentName: String?, cb: (() -> Unit)? = null) {
        val post = selectedPost.value!!
        loadComments(post.subreddit, post.name, commentName, cb)
    }

    fun loadComments(subredditName: String,
                     postName: String,
                     commentName: String?,
                     cb: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val sorting = commentSorting.value ?: "top"
            val post = if (postName.startsWith("t3_")) postName.substring(3) else postName
            var permalink = "/r/$subredditName/comments/$post"
            if (commentName != null) {
                permalink += "/$commentName"
            }
            val pair = Reddit.getPostAndComments(permalink, sorting)
            selectedPost.postValue(pair.first)
            (comments as MutableLiveData).postValue(pair.second.toMutableList())
            cb?.invoke()
        }
    }

    fun selectComment(comment: Comment?) {
        (selectedComment as MutableLiveData).value = comment
    }

    fun collapse(comment: Comment) {
        val comments = comments.value!!
        val index = comments.indexOf(comment)
        if (comment.children != null) {
            // Restore current and child comments
            if (comment.children!!.isNotEmpty() && comments.addAll(index + 1, comment.children!!)) {
                (this.comments as MutableLiveData).postValue(comments)
            }
            comment.children = null
            selectComment(comment)
        } else {
            // Collapse current and child comments
            val numToCollapse = comments.subList(index + 1, comments.size).indexOfFirst { item ->
                item is Comment && item.depth <= comment.depth
            }
            if (numToCollapse >= 1) {
                val subList = comments.subList(index + 1, numToCollapse + index + 1)
                comment.children = subList.toList()
                subList.clear()
                (this.comments as MutableLiveData).postValue(comments)
            } else {
                comment.children = emptyList()
            }
            selectComment(null)
        }
    }

    fun selectNeighboringComment(comment: Comment, depth: Int, next: Boolean) {
        val comments = comments.value!!
        val index = comments.indexOf(comment)
        var commentCandidates = if (next) {
            comments.subList(index + 1, comments.size)
        } else {
            comments.subList(0, index)
        }
        commentCandidates = commentCandidates.filter { it is Comment && it.depth == depth }.toMutableList()
        if (commentCandidates.isNotEmpty()) {
            val newlySelectedComment = commentCandidates[if (next) 0 else commentCandidates.size - 1]
            selectComment(newlySelectedComment as Comment)
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
                } else if (oldPosts.isNotEmpty()) {
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
                } else if (oldPosts.isNotEmpty()) {
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

    fun updateSearchText(text: CharSequence?) {
        viewModelScope.launch(Dispatchers.IO) {
            val results = if (text.isNullOrEmpty()) null else Reddit.searchForSubreddit(text.toString())
            results?.forEach { subreddit ->
                subreddit.isSubscribedTo = subscribedSubreddits.value?.contains(subreddit) ?: false
            }
            (searchResults as MutableLiveData).postValue(results)
        }
    }

    fun selectSubreddit(name: String, isMultiReddit: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            var sub = subreddits.value?.flatten()?.find { subreddit ->
                subreddit.name == name && subreddit.isMultiReddit == isMultiReddit
            }
            if (sub == null && !subreddits.value.isNullOrEmpty()) {
                sub = Subreddit.fromName(name)
                sub.isSubscribedTo = false
                addToVisitedSubreddits(sub)
            }
            (subreddit as MutableLiveData).postValue(sub)
        }
    }

    private suspend fun addToVisitedSubreddits(sub: Subreddit) {
        Store.getInstance(getApplication()).addToVisitedSubreddits(sub.name)
        (visitedSubreddits as MutableLiveData).postValue(sortByName((visitedSubreddits.value!! + listOf(sub))))
        updateAllSubredditsList()
    }

    private fun sortByName(list: List<Subreddit>): List<Subreddit> {
        return list.sortedWith(
                compareBy { sub: Subreddit -> !sub.isStarred }
                        .thenBy { it.name.toLowerCase(Locale.getDefault()) }
        )
    }

    fun loadSubscriptions() {
        viewModelScope.launch(Dispatchers.IO) {
            val store = Store.getInstance(getApplication())
            val starred = store.getStarredSubreddits()
            val subscriptions = Reddit.getSubscriptions()
            subscriptions.forEach { subreddit ->
                if (starred.contains(subreddit.name))
                    subreddit.isStarred = true
            }
            (subscribedSubreddits as MutableLiveData).postValue(sortByName(subscriptions))
            store.updateCachedSubscribedSubreddits(subscribedSubreddits.value ?: emptyList())
            (visitedSubreddits as MutableLiveData).postValue(
                    sortByName(store.getVisitedSubreddits().map { name ->
                        Subreddit.fromName(name).apply {
                            isSubscribedTo = false
                        }
                    }))
            (multiReddits as MutableLiveData).postValue(sortByName(Reddit.getMultis()))
            store.updateCachedMultiSubreddits(multiReddits.value ?: emptyList())
            updateAllSubredditsList()
        }
    }

    private fun updateAllSubredditsList() {
        viewModelScope.launch(Dispatchers.Main) {
            (subreddits as MutableLiveData).value = listOfNotNull(
                    Subreddit.defaultSubreddits,
                    subscribedSubreddits.value,
                    visitedSubreddits.value,
                    multiReddits.value,
            )
        }
    }

    fun subscribeToSubreddit(subreddit: Subreddit) {
        viewModelScope.launch(Dispatchers.IO) {
            Reddit.subscribe(subreddit.name, subreddit.isSubscribedTo)
            subreddit.isSubscribedTo = !subreddit.isSubscribedTo
            updateSubscribedSubreddit(subreddit, null)
            removeSubredditFromVisited(subreddit.name)
        }
    }

    fun removeSubredditFromVisited(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            Store.getInstance(getApplication()).removeFromVisitedSubreddits(name)
            (visitedSubreddits as MutableLiveData).postValue(visitedSubreddits.value?.filter {
                it.name != name
            })
            updateAllSubredditsList()
        }
    }

    fun removeSubredditFromStarred(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            Store.getInstance(getApplication()).removeFromStarredSubreddits(name)
            updateSubscribedSubreddit(Subreddit.fromName(name), false)
        }
    }

    fun addSubredditsToStarred(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            Store.getInstance(getApplication()).addToStarredSubreddits(name)
            updateSubscribedSubreddit(Subreddit.fromName(name), true)
        }
    }

    private fun updateSubscribedSubreddit(subreddit: Subreddit, isStarred: Boolean?) {
        val subscriptions = subscribedSubreddits.value!!.toMutableList()
        if (isStarred != null) {
            subscriptions.find { it == subreddit }?.isStarred = isStarred
        } else {
            if (subscriptions.contains(subreddit)) {
                subscriptions.remove(subreddit)
            } else {
                subscriptions.add(subreddit)
            }
        }
        (subscribedSubreddits as MutableLiveData).postValue(sortByName(subscriptions))
        updateAllSubredditsList()
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
        loadComments(null, cb)
    }

    fun resetComments() {
        (comments as MutableLiveData).value = mutableListOf(NamedItem.Loading)
    }

    fun loadMoreComments(comment: Comment) {
        if (comment.count == 0) {
            val subtreeCommentName = comment.parent_id.split("_")[1]
            loadComments(subtreeCommentName)
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                val newComments = Reddit.getMoreComments(selectedPost.value!!.name, comment.moreChildren)
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
        viewModelScope.launch(Dispatchers.IO) {
            Reddit.saveOrUnsave(post.saved, post.name)
            post.saved = !post.saved
        }
    }

    fun saveOrUnsave(comment: Comment) {
        viewModelScope.launch(Dispatchers.IO) {
            Reddit.saveOrUnsave(comment.saved, comment.name)
            comment.saved = !comment.saved
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