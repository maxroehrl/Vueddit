package de.max.roehrl.vueddit2.ui.viewmodel

import android.app.Application
import android.util.Log
import android.webkit.CookieManager
import androidx.lifecycle.*
import de.max.roehrl.vueddit2.model.NamedItem
import de.max.roehrl.vueddit2.model.Post
import de.max.roehrl.vueddit2.model.Subreddit
import de.max.roehrl.vueddit2.service.Reddit
import de.max.roehrl.vueddit2.service.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppViewModel(application: Application, private val savedStateHandle: SavedStateHandle) :
    AndroidViewModel(application) {
    companion object {
        private const val TAG = "AppViewModel"
        const val WAS_LOGGED_IN = "wasLoggedIn"
        const val IS_LOGGED_IN = "isLoggedIn"
        const val USERNAME = "username"
        const val SUBREDDIT = "subreddit"
        const val SUBREDDITS = "subreddits"
        const val SUBSCRIBED_SUBREDDITS = "subscribedSubreddits"
        const val VISITED_SUBREDDITS = "visitedSubreddits"
        const val VISITED_USERS = "visitedUsers"
        const val MULTI_REDDITS = "multiReddits"
        const val IS_BIG_TEMPLATE_PREFERRED = "isBigTemplatePreferred"
        const val SEARCH_RESULTS = "searchResults"
    }

    val isLoggedIn: LiveData<Boolean> = liveData(Dispatchers.IO) {
        val saved: Boolean? = savedStateHandle.get(IS_LOGGED_IN)
        emit(saved ?: Reddit.getInstance(getApplication()).login())
    }

    val username: LiveData<String?> = liveData(Dispatchers.IO) {
        val saved: String? = savedStateHandle.get(USERNAME)
        emit(saved ?: Store.getInstance(getApplication()).getUsername())
    }

    val subreddit: LiveData<Subreddit> = liveData {
        val saved: Subreddit? = savedStateHandle.get(SUBREDDIT)
        emit(saved ?: Subreddit.frontPage)
    }

    val subreddits: LiveData<List<List<Subreddit>>> = liveData {
        emit(loadCachedSubreddits())
    }

    private val subscribedSubreddits: LiveData<List<Subreddit>> = liveData {
        val saved: List<Subreddit>? = savedStateHandle.get(SUBSCRIBED_SUBREDDITS)
        if (saved != null) {
            emit(saved)
        }
    }

    private val visitedSubreddits: LiveData<List<Subreddit>> = liveData {
        val saved: List<Subreddit>? = savedStateHandle.get(VISITED_SUBREDDITS)
        if (saved != null) {
            emit(saved)
        }
    }

    private val visitedUsers: LiveData<List<Subreddit>> = liveData {
        val saved: List<Subreddit>? = savedStateHandle.get(VISITED_USERS)
        if (saved != null) {
            emit(saved)
        }
    }

    private val multiReddits: LiveData<List<Subreddit>> = liveData {
        val saved: List<Subreddit>? = savedStateHandle.get(MULTI_REDDITS)
        if (saved != null) {
            emit(saved)
        }
    }

    val isBigTemplatePreferred: LiveData<Boolean?> = liveData {
        val saved: Boolean? = savedStateHandle.get(IS_BIG_TEMPLATE_PREFERRED)
        emit(saved)
    }

    val searchResults: LiveData<List<Subreddit>?> = liveData {
        val saved: List<Subreddit>? = savedStateHandle.get(SEARCH_RESULTS)
        emit(saved)
    }

    fun saveBundle() {
        savedStateHandle.set(IS_LOGGED_IN, isLoggedIn.value)
        savedStateHandle.set(USERNAME, username.value)
        savedStateHandle.set(SUBREDDIT, subreddit.value)
        savedStateHandle.set(SUBREDDITS, subreddits.value)
        savedStateHandle.set(SUBSCRIBED_SUBREDDITS, subscribedSubreddits.value)
        savedStateHandle.set(VISITED_SUBREDDITS, visitedSubreddits.value)
        savedStateHandle.set(VISITED_USERS, visitedUsers.value)
        savedStateHandle.set(MULTI_REDDITS, multiReddits.value)
        savedStateHandle.set(IS_BIG_TEMPLATE_PREFERRED, isBigTemplatePreferred.value)
        savedStateHandle.set(SEARCH_RESULTS, searchResults.value)
    }

    fun updateSearchText(text: CharSequence?) {
        viewModelScope.launch(Dispatchers.IO) {
            val results = if (text.isNullOrEmpty()) null else Reddit.getInstance(getApplication()).searchForSubreddit(text.toString())
            results?.forEach { subreddit ->
                subreddit.isSubscribedTo = subscribedSubreddits.value?.contains(subreddit) ?: false
            }
            (searchResults as MutableLiveData).postValue(results)
        }
    }

    fun selectSubreddit(name: String, isMultiReddit: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            var sub = subreddits.value?.flatten()?.find { subreddit ->
                subreddit.name.equals(name, true) && subreddit.isMultiReddit == isMultiReddit
            }
            if (sub == null && !subreddits.value.isNullOrEmpty()) {
                sub = Subreddit.fromName(name)
                sub.isSubscribedTo = false
                addToVisitedSubreddits(sub)
            }
            if (sub != null) {
                (subreddit as MutableLiveData).postValue(sub)
            }
        }
    }

    fun selectUser(user: String) {
        if (visitedUsers.value?.find { sub ->sub.user == user} == null) {
            viewModelScope.launch(Dispatchers.IO) {
                addToVisitedUsers(Subreddit.fromUser(user))
            }
        }
    }

    private suspend fun addToVisitedSubreddits(sub: Subreddit) {
        Store.getInstance(getApplication()).addToVisitedSubreddits(sub.name)
        (visitedSubreddits as MutableLiveData).postValue(sortByName((visitedSubreddits.value!! + listOf(sub))))
        updateAllSubredditsList()
    }

    private suspend fun addToVisitedUsers(sub: Subreddit) {
        Store.getInstance(getApplication()).addToVisitedUsers(sub.user!!)
        (visitedUsers as MutableLiveData).postValue(sortByName((visitedUsers.value!! + listOf(sub))))
        updateAllSubredditsList()
    }

    private fun sortByName(list: List<Subreddit>): List<Subreddit> {
        return list.sortedWith(
                compareBy { sub: Subreddit -> !sub.isStarred }
                        .thenBy { it.name.lowercase() }
        )
    }

    private suspend fun loadCachedSubreddits(): List<List<Subreddit>> {
        val store = Store.getInstance(getApplication())
        val starred = store.getStarredSubreddits()
        val subscriptions = sortByName(store.getCachedSubscribedSubreddits().map {
            Subreddit.fromName(it).apply {
                if (starred.contains(name))
                    isStarred = true
            }
        })
        Log.d(TAG, "Loaded ${subscriptions.size} cached subscribed subreddits")
        (subscribedSubreddits as MutableLiveData).value = subscriptions

        val visited = sortByName(store.getVisitedSubreddits().map {
            Subreddit.fromName(it).apply {
                isSubscribedTo = false
            }
        })
        Log.d(TAG, "Loaded ${visited.size} visited subreddits")
        (visitedSubreddits as MutableLiveData).value = visited

        val users = sortByName(store.getVisitedUsers().map {
            Subreddit.fromUser(it)
        })
        Log.d(TAG, "Loaded ${users.size} visited users")
        (visitedUsers as MutableLiveData).value = users

        val multis = sortByName(store.getCachedMultiSubreddits().map {
            Subreddit.fromName(it).apply {
                isMultiReddit = true
            }
        })
        Log.d(TAG, "Loaded ${multis.size} cached multi subreddits")
        (multiReddits as MutableLiveData).value = multis

        return listOfNotNull(Subreddit.defaultSubreddits, subscriptions, visited, multis)
    }

    fun loadSubscriptions() {
        viewModelScope.launch(Dispatchers.IO) {
            val store = Store.getInstance(getApplication())
            val starred = store.getStarredSubreddits()
            val subscriptions = sortByName(Reddit.getInstance(getApplication()).getSubscriptions().map { subreddit ->
                subreddit.apply {
                    if (starred.contains(name))
                        isStarred = true
                }
            })
            (subscribedSubreddits as MutableLiveData).postValue(subscriptions)
            store.updateCachedSubscribedSubreddits(subscriptions)
            (visitedSubreddits as MutableLiveData).postValue(
                    sortByName(store.getVisitedSubreddits().map { name ->
                        Subreddit.fromName(name).apply {
                            isSubscribedTo = false
                        }
                    }))
            (visitedUsers as MutableLiveData).postValue(
                    sortByName(store.getVisitedUsers().map { name ->
                        Subreddit.fromUser(name)
                    }))
            val multis = sortByName(Reddit.getInstance(getApplication()).getMultis())
            (multiReddits as MutableLiveData).postValue(multis)
            store.updateCachedMultiSubreddits(multis)
            updateAllSubredditsList()
        }
    }

    private fun updateAllSubredditsList() {
        viewModelScope.launch(Dispatchers.Main) {
            (subreddits as MutableLiveData).value = listOfNotNull(
                Subreddit.defaultSubreddits,
                    subscribedSubreddits.value,
                    visitedSubreddits.value,
                    visitedUsers.value,
                    multiReddits.value,
            )
        }
    }

    fun subscribeToSubreddit(subreddit: Subreddit) {
        viewModelScope.launch(Dispatchers.IO) {
            Reddit.getInstance(getApplication()).subscribe(subreddit.name, subreddit.isSubscribedTo)
            subreddit.isSubscribedTo = !subreddit.isSubscribedTo
            updateSubscribedSubreddit(subreddit, null)
            if (subreddit.isSubscribedTo) {
                removeSubredditFromVisited(subreddit.name)
            } else {
                subreddit.isStarred = false
                addToVisitedSubreddits(subreddit)
            }
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

    fun removeUserFromVisited(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            Store.getInstance(getApplication()).removeFromVisitedUsers(name)
            (visitedUsers as MutableLiveData).postValue(visitedUsers.value?.filter {
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
        if (!searchResults.value.isNullOrEmpty()) {
            val results = searchResults.value!!.map { sub ->
                if (sub == subreddit) {
                    sub.isSubscribedTo = subreddit.isSubscribedTo
                    sub.isStarred = isStarred ?: sub.isStarred
                }
                sub
            }
            (searchResults as MutableLiveData).postValue(results)
        }
        (subscribedSubreddits as MutableLiveData).postValue(sortByName(subscriptions))
        updateAllSubredditsList()
    }

    fun logoutUser() {
        CookieManager.getInstance().removeAllCookies(null)
        viewModelScope.launch(Dispatchers.IO) {
            Store.getInstance(getApplication()).logoutUser()
            (username as MutableLiveData).postValue(null)
            (subreddits as MutableLiveData).postValue(emptyList())
            (isLoggedIn as MutableLiveData).postValue(Reddit.getInstance(getApplication()).login())
        }
    }

    fun toggleBigPreview(postList: List<NamedItem>) {
        (isBigTemplatePreferred as MutableLiveData).value = !if (isBigTemplatePreferred.value != null) {
            isBigTemplatePreferred.value!!
        } else {
            shouldShowBigTemplate(postList)
        }
    }

    fun shouldShowBigTemplate(postList: List<NamedItem>): Boolean {
        return if (isBigTemplatePreferred.value != null) {
            isBigTemplatePreferred.value!!
        } else {
            val numberOfPreviewPictures = postList
                .filterIsInstance<Post>()
                .take(10)
                .map { item -> if (item.image.url != null) 1 else 0 }
                .fold(0) { acc, it -> acc + it }
            Log.d(TAG, "$numberOfPreviewPictures of the first 10 posts have a preview")
            numberOfPreviewPictures > 4
        }
    }
}