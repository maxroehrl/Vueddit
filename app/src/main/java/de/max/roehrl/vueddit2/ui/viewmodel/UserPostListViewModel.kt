package de.max.roehrl.vueddit2.ui.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.liveData
import de.max.roehrl.vueddit2.model.NamedItem
import de.max.roehrl.vueddit2.service.Reddit

class UserPostListViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : PostListViewModel(application, savedStateHandle) {
    companion object {
        private const val SELECTED_USER = "selectedUser"
        private const val USER_POST_GROUP = "userPostGroup"
        private const val SELECTED_TYPE = "selectedType"
    }

    override val sortingList = listOf("new", "top", "hot", "controversial")
    override val defaultSorting = "new"
    var selectedType: String = savedStateHandle.get(SELECTED_TYPE) ?: "all"
    var userPostGroup: String = savedStateHandle.get(USER_POST_GROUP) ?: "submitted"

    val selectedUser: LiveData<String> = liveData {
        val saved: String? = savedStateHandle.get(SELECTED_USER)
        if (saved != null) {
            emit(saved)
        }
    }

    fun setSelectedUser(username: String) {
        val old = selectedUser.value
        (selectedUser as MutableLiveData).value = username
        if (old != username) {
            refreshPosts()
        }
    }

    override suspend fun getMorePosts(
            after: String,
            sorting: String,
            time: String,
            count: Int,
    ): List<NamedItem> {
        return Reddit.getInstance(getApplication()).getUserPosts(
            selectedUser.value!!,
            after,
            sorting,
            userPostGroup,
            time,
            selectedType,
            count
        )
    }

    override fun saveBundle() {
        super.saveBundle()
        savedStateHandle.set(SELECTED_USER, selectedUser.value)
        savedStateHandle.set(USER_POST_GROUP, userPostGroup)
        savedStateHandle.set(SELECTED_TYPE, selectedType)
    }
}