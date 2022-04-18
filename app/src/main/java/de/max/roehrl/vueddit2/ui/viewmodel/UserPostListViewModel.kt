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
        private const val defaultGroup = "overview"
        private const val defaultType = "all"
        private const val SELECTED_USER = "selectedUser"
        private const val USER_POST_GROUP = "userPostGroup"
        private const val SELECTED_TYPE = "selectedType"
    }
    override val sortingList = listOf("new", "top", "hot", "controversial")
    override val defaultSorting = "new"

    val selectedUser: LiveData<String> = liveData {
        val saved: String? = savedStateHandle.get(SELECTED_USER)
        if (saved != null) {
            emit(saved)
        }
    }

    val userPostGroup: LiveData<String> = liveData {
        val saved: String? = savedStateHandle.get(USER_POST_GROUP)
        emit(saved ?: defaultGroup)
    }

    private val selectedType: LiveData<String> = liveData {
        val saved: String? = savedStateHandle.get(SELECTED_TYPE)
        emit(saved ?: defaultType)
    }

    fun setSelectedUser(username: String) {
        (selectedUser as MutableLiveData).value = username
    }

    fun setSavedPostsType(type: String) {
        (selectedType as MutableLiveData).value = type
    }

    fun setUserPostGroup(group: String) {
        (userPostGroup as MutableLiveData).value = group
    }

    override suspend fun getMorePosts(
            after: String,
            sorting: String,
            time: String,
            count: Int,
    ): List<NamedItem> {
        val userName = selectedUser.value!!
        val group = userPostGroup.value ?: defaultGroup
        val type = selectedType.value ?: defaultType
        return Reddit.getInstance(getApplication()).getUserPosts(userName, after, sorting, group, time, type, count)
    }

    override fun saveBundle() {
        super.saveBundle()
        savedStateHandle.set(SELECTED_USER, selectedUser.value)
        savedStateHandle.set(USER_POST_GROUP, userPostGroup.value)
        savedStateHandle.set(SELECTED_TYPE, selectedType.value)
    }
}