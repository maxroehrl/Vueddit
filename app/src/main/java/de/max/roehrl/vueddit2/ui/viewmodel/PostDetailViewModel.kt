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

class PostDetailViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "PostDetailViewModel"
        private const val SELECTED_POST = "selectedPost"
        private const val SELECTED_COMMENT = "selectedComment"
        private const val COMMENTS = "comments"
        private const val COMMENT_SORTING = "commentSorting"
    }

    var commentSorting: String = savedStateHandle[COMMENT_SORTING] ?: "top"
    val selectedPost: LiveData<Post> = savedStateHandle.getLiveData(SELECTED_POST)
    val selectedComment: LiveData<Comment?> = savedStateHandle.getLiveData(SELECTED_COMMENT)
    val comments: LiveData<MutableList<NamedItem>> = savedStateHandle.getLiveData(COMMENTS, mutableListOf(NamedItem.Loading))

    private fun loadComments(commentName: String?, cb: (() -> Unit)? = null) {
        val post = selectedPost.value!!
        loadComments(post.subreddit, post.name, commentName, cb)
    }

    fun loadComments(
        subredditName: String,
        postName: String,
        commentName: String?,
        cb: (() -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            var post = postName
            var comment = commentName
            if (post.contains("/comments/")) {
                val split = post.split("/")
                post = split[0]
                comment = split.getOrElse(2) { comment }
            }
            post = if (post.startsWith("t3_")) post.substring(3) else post
            var permalink = "/r/$subredditName/comments/$post"
            if (comment != null) {
                permalink += "/$comment"
            }
            try {
                val pair = Reddit.getInstance(getApplication()).getPostAndComments(
                    permalink,
                    commentSorting
                )
                (selectedPost as MutableLiveData).postValue(pair.first)
                (comments as MutableLiveData).postValue(pair.second.toMutableList())
            } catch (e: Exception) {
                Log.e(TAG, "Error getting comments and post", e)
            }
            cb?.invoke()
        }
    }

    fun selectComment(comment: Comment?) {
        savedStateHandle[SELECTED_COMMENT] = comment
    }

    fun refreshComments(cb: (() -> Unit)? = null) {
        savedStateHandle[COMMENTS] = mutableListOf(NamedItem.Loading)
        loadComments(null, cb)
    }

    fun setSelectedPost(post: Post) {
        savedStateHandle[SELECTED_POST] = post
    }

    fun loadMoreComments(comment: Comment) {
        if (comment.count == 0) {
            val subtreeCommentName = comment.parent_id.split("_")[1]
            loadComments(subtreeCommentName)
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                val newComments = Reddit.getInstance(getApplication())
                    .getMoreComments(selectedPost.value!!.name, comment.moreChildren)
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
        commentCandidates =
            commentCandidates.filter { it is Comment && it.depth == depth }.toMutableList()
        if (commentCandidates.isNotEmpty()) {
            val newlySelectedComment =
                commentCandidates[if (next) 0 else commentCandidates.size - 1]
            selectComment(newlySelectedComment as Comment)
        }
    }
}