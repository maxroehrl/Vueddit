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
        private const val IS_LOGGED_IN = "isLoggedIn"
        private const val USERNAME = "username"
        private const val SUBREDDIT = "subreddit"
        private const val SUBSCRIBED_SUBREDDITS = "subscribedSubreddits"
        private const val VISITED_SUBREDDITS = "visitedSubreddits"
        private const val VISITED_USERS = "visitedUsers"
        private const val MULTI_REDDITS = "multiReddits"
        private const val IS_BIG_TEMPLATE_PREFERRED = "isBigTemplatePreferred"
        private const val SEARCH_RESULTS = "searchResults"
    }

    private var subscribedSubreddits: List<Subreddit> = savedStateHandle[SUBSCRIBED_SUBREDDITS] ?: emptyList()
    private var visitedSubreddits: List<Subreddit> = savedStateHandle[VISITED_SUBREDDITS] ?: emptyList()
    private var visitedUsers: List<Subreddit> = savedStateHandle[VISITED_USERS] ?: emptyList()
    private var multiReddits: List<Subreddit> = savedStateHandle[MULTI_REDDITS] ?: emptyList()

    val isLoggedIn: LiveData<Boolean> = liveData(Dispatchers.IO) {
        val saved: Boolean? = savedStateHandle[IS_LOGGED_IN]
        emit(saved ?: Reddit.getInstance(getApplication()).login())
    }

    private val username: LiveData<String?> = liveData(Dispatchers.IO) {
        val saved: String? = savedStateHandle[USERNAME]
        emit(saved ?: Store.getInstance(getApplication()).getUsername())
    }

    val subreddit: LiveData<Subreddit> = savedStateHandle.getLiveData(SUBREDDIT, Subreddit.frontPage)

    val subreddits: LiveData<List<List<Subreddit>>> = liveData {
        emit(loadCachedSubreddits())
    }

    val isBigTemplatePreferred: LiveData<Boolean?> = savedStateHandle.getLiveData(IS_BIG_TEMPLATE_PREFERRED)

    val searchResults: LiveData<List<Subreddit>?> = savedStateHandle.getLiveData(SEARCH_RESULTS)

    fun updateSearchText(text: CharSequence?) {
        viewModelScope.launch(Dispatchers.IO) {
            val results = if (text.isNullOrEmpty()) null else Reddit.getInstance(getApplication())
                .searchForSubreddit(text.toString())
            results?.forEach { subreddit ->
                subreddit.isSubscribedTo = subscribedSubreddits.contains(subreddit)
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
        if (visitedUsers.find { sub -> sub.user == user } == null) {
            viewModelScope.launch(Dispatchers.IO) {
                addToVisitedUsers(Subreddit.fromUser(user))
            }
        }
    }

    private suspend fun addToVisitedSubreddits(sub: Subreddit) {
        Store.getInstance(getApplication()).addToVisitedSubreddits(sub.name)
        visitedSubreddits = sortByName((visitedSubreddits + listOf(sub)))
        updateAllSubredditsList()
    }

    private suspend fun addToVisitedUsers(sub: Subreddit) {
        Store.getInstance(getApplication()).addToVisitedUsers(sub.user!!)
        visitedUsers = sortByName((visitedUsers + listOf(sub)))
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

        subscribedSubreddits = sortByName(store.getCachedSubscribedSubreddits().map {
            Subreddit.fromName(it).apply {
                if (starred.contains(name))
                    isStarred = true
            }
        })
        visitedSubreddits = sortByName(store.getVisitedSubreddits().map {
            Subreddit.fromName(it).apply {
                isSubscribedTo = false
            }
        })
        visitedUsers = sortByName(store.getVisitedUsers().map {
            Subreddit.fromUser(it)
        })
        multiReddits = sortByName(store.getCachedMultiSubreddits().map {
            Subreddit.fromName(it).apply {
                isMultiReddit = true
            }
        })
        return listOfNotNull(
            Subreddit.defaultSubreddits,
            subscribedSubreddits,
            visitedSubreddits,
            multiReddits,
        )
    }

    fun loadSubscriptions() {
        viewModelScope.launch(Dispatchers.IO) {
            val store = Store.getInstance(getApplication())
            val starred = store.getStarredSubreddits()

            subscribedSubreddits = sortByName(
                Reddit.getInstance(getApplication()).getSubscriptions().map { subreddit ->
                    subreddit.apply {
                        if (starred.contains(name))
                            isStarred = true
                    }
                })
            store.updateCachedSubscribedSubreddits(subscribedSubreddits)
            visitedSubreddits = sortByName(store.getVisitedSubreddits().map { name ->
                Subreddit.fromName(name).apply {
                    isSubscribedTo = false
                }
            })
            visitedUsers = sortByName(store.getVisitedUsers().map { name ->
                Subreddit.fromUser(name)
            })
            multiReddits = sortByName(Reddit.getInstance(getApplication()).getMultis())
            store.updateCachedMultiSubreddits(multiReddits)
            updateAllSubredditsList()
        }
    }

    private fun updateAllSubredditsList() {
        viewModelScope.launch(Dispatchers.Main) {
            (subreddits as MutableLiveData).value = listOfNotNull(
                Subreddit.defaultSubreddits,
                subscribedSubreddits,
                visitedSubreddits,
                visitedUsers,
                multiReddits,
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
            visitedSubreddits = visitedSubreddits.filter {
                it.name != name
            }
            updateAllSubredditsList()
        }
    }

    fun removeUserFromVisited(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            Store.getInstance(getApplication()).removeFromVisitedUsers(name)
            visitedUsers = visitedUsers.filter {
                it.name != name
            }
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
        val subscriptions = subscribedSubreddits.toMutableList()
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
        subscribedSubreddits = sortByName(subscriptions)
        updateAllSubredditsList()
    }

    fun logoutUser() {
        CookieManager.getInstance().removeAllCookies(null)
        viewModelScope.launch(Dispatchers.IO) {
            Store.getInstance(getApplication()).logoutUser()
            (username as MutableLiveData).postValue(null)
            savedStateHandle[USERNAME] = null
            (subreddits as MutableLiveData).postValue(emptyList())
            setIsLoggedIn(Reddit.getInstance(getApplication()).login())
        }
    }

    fun setIsLoggedIn(value: Boolean) {
        (isLoggedIn as MutableLiveData).postValue(value)
        savedStateHandle[IS_LOGGED_IN] = value
    }

    fun toggleBigPreview(postList: List<NamedItem>) {
        (isBigTemplatePreferred as MutableLiveData).value =
            !if (isBigTemplatePreferred.value != null) {
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