package de.max.roehrl.vueddit2.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import de.max.roehrl.vueddit2.model.Comment
import de.max.roehrl.vueddit2.model.NamedItem
import de.max.roehrl.vueddit2.model.Post
import de.max.roehrl.vueddit2.service.Reddit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PostDetailViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "PostDetailViewModel"
        private const val defaultSorting = "top"
    }

    val selectedPost = MutableLiveData<Post>()

    val selectedComment: LiveData<Comment?> = liveData { }

    val comments: LiveData<MutableList<NamedItem>> = liveData {
        emit(mutableListOf<NamedItem>(NamedItem))
    }

    val commentSorting: LiveData<String> = liveData {
        emit(defaultSorting)
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
            val sorting = commentSorting.value ?: defaultSorting
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

    fun saveOrUnsave(comment: Comment) {
        viewModelScope.launch(Dispatchers.IO) {
            Reddit.saveOrUnsave(comment.saved, comment.name)
            comment.saved = !comment.saved
        }
    }
}