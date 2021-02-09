package de.max.roehrl.vueddit2.ui.viewmodel

import android.app.Application
import android.webkit.CookieManager
import androidx.lifecycle.*
import de.max.roehrl.vueddit2.model.NamedItem
import de.max.roehrl.vueddit2.model.Post
import de.max.roehrl.vueddit2.model.Subreddit
import de.max.roehrl.vueddit2.service.Reddit
import de.max.roehrl.vueddit2.service.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.min

class AppViewModel(application: Application) : AndroidViewModel(application) {
    val isLoggedIn: LiveData<Boolean> = liveData(Dispatchers.IO) {
        emit(Reddit.login(application))
    }

    val username: LiveData<String?> = liveData(Dispatchers.IO) {
        emit(Store.getInstance(application).getUsername())
    }

    val subreddit: LiveData<Subreddit> = liveData {
        emit(Subreddit.frontPage)
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

    val isBigTemplatePreferred: LiveData<Boolean?> = liveData {
        emit(null)
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
                subreddit.name.equals(name, true) && subreddit.isMultiReddit == isMultiReddit
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

    fun logoutUser() {
        CookieManager.getInstance().removeAllCookies(null)
        viewModelScope.launch(Dispatchers.IO) {
            Store.getInstance(getApplication()).logoutUser()
            (username as MutableLiveData).postValue(null)
            Reddit.login(getApplication())
        }
    }

    fun toggleBigPreview(postList: List<NamedItem>) {
        (isBigTemplatePreferred as MutableLiveData).value = !if (isBigTemplatePreferred.value != null) {
            isBigTemplatePreferred.value!!
        } else {
            shouldShowBigTemplate(postList, null)
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