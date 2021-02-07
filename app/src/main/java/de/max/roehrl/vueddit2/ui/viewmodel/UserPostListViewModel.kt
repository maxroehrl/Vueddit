package de.max.roehrl.vueddit2.ui.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import de.max.roehrl.vueddit2.model.Comment
import de.max.roehrl.vueddit2.model.NamedItem
import de.max.roehrl.vueddit2.service.Reddit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserPostListViewModel(application: Application) : PostListViewModel(application) {
    override val defaultSorting = "new"
    private val defaultGroup = "overview"
    private val defaultType = "all"

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

    override suspend fun getMorePosts(after: String, sorting: String, time: String): List<NamedItem> {
        val userName = selectedUser.value!!
        val group = userPostGroup.value ?: defaultGroup
        val type = selectedType.value ?: defaultType
        return Reddit.getUserPosts(userName, after, sorting, group, time, type)
    }

    fun saveOrUnsave(comment: Comment) {
        viewModelScope.launch(Dispatchers.IO) {
            Reddit.saveOrUnsave(comment.saved, comment.name)
            comment.saved = !comment.saved
        }
    }
}