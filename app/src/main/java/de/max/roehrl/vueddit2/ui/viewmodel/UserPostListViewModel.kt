package de.max.roehrl.vueddit2.ui.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import de.max.roehrl.vueddit2.model.NamedItem
import de.max.roehrl.vueddit2.service.Reddit

class UserPostListViewModel(application: Application) : PostListViewModel(application) {
    companion object {
        private const val defaultGroup = "overview"
        private const val defaultType = "all"
    }
    override val sortingList = listOf("new", "top", "hot", "controversial")
    override val defaultSorting = "new"

    val selectedUser: LiveData<String> = liveData {}

    val userPostGroup: LiveData<String> = liveData {
        emit(defaultGroup)
    }

    private val selectedType: LiveData<String> = liveData {
        emit(defaultType)
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
}