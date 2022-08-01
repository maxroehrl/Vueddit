package de.max.roehrl.vueddit2.ui.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
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

    var selectedType: String = savedStateHandle[SELECTED_TYPE] ?: "all"
    var userPostGroup: String = savedStateHandle[USER_POST_GROUP] ?: "submitted"
    val selectedUser: LiveData<String> = savedStateHandle.getLiveData(SELECTED_USER)

    override fun getPostSortingList(isFrontpage: Boolean): List<String> {
        return listOf("new", "top", "hot", "controversial")
    }

    fun setSelectedUser(username: String) {
        val old = selectedUser.value
        savedStateHandle[SELECTED_USER] = username
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
}