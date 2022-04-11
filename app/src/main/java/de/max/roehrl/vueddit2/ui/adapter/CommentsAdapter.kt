package de.max.roehrl.vueddit2.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.model.Comment
import de.max.roehrl.vueddit2.model.NamedItem
import de.max.roehrl.vueddit2.model.Post
import de.max.roehrl.vueddit2.ui.viewholder.CommentViewHolder
import de.max.roehrl.vueddit2.ui.viewholder.MoreCommentsViewHolder
import de.max.roehrl.vueddit2.ui.viewholder.PostBigHeaderViewHolder
import de.max.roehrl.vueddit2.ui.viewholder.PostHeaderViewHolder
import de.max.roehrl.vueddit2.ui.viewmodel.PostDetailViewModel

class CommentsAdapter(private val viewModel: PostDetailViewModel) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private inner class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(comment: NamedItem?) {
            if (comment is Comment) {
                itemView.findViewById<CircularProgressIndicator>(R.id.progress)
                    .setPadding((40f * comment.depth + 20).toInt(), 10, 10, 10)
            }
        }
    }

    var post: Post? = null
    var comments: MutableList<NamedItem> = mutableListOf(NamedItem.Loading)
    var selectedComment: Comment? = null
        set(value) {
            if (field != null) {
                refreshComment(field!!)
            }
            field = value
            if (value != null) {
                refreshComment(value)
            }
        }

    companion object {
        private const val VIEW_TYPE_PROGRESS_BIG = 0
        private const val VIEW_TYPE_PROGRESS = 1
        private const val VIEW_TYPE_COMMENT = 2
        private const val VIEW_TYPE_HEADER = 3
        private const val VIEW_TYPE_HEADER_BIG = 4
        private const val VIEW_TYPE_MORE = 5
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_PROGRESS_BIG -> ProgressViewHolder(inflate(parent, R.layout.loading_item))
            VIEW_TYPE_PROGRESS     -> ProgressViewHolder(inflate(parent, R.layout.loading_item_small))
            VIEW_TYPE_COMMENT      -> CommentViewHolder(inflate(parent, R.layout.comment_item), viewModel, viewModel.viewModelScope)
            VIEW_TYPE_HEADER       -> PostHeaderViewHolder(inflate(parent, R.layout.post_detail_header), viewModel)
            VIEW_TYPE_HEADER_BIG   -> PostBigHeaderViewHolder(inflate(parent, R.layout.post_detail_header_big), viewModel)
            VIEW_TYPE_MORE         -> MoreCommentsViewHolder(inflate(parent, R.layout.more_comments_item), this, viewModel)
            else                   -> throw IllegalArgumentException("viewType not found")
        }
    }

    fun refreshComment(comment: Comment) {
        val index = comments.indexOf(comment)
        if (index >= 0) {
            notifyItemChanged(index + 1)
        }
    }

    private fun inflate(parent: ViewGroup, viewId: Int): View {
        return LayoutInflater.from(parent.context).inflate(viewId, parent, false)
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position == 0 && post?.preview?.url != null && post?.video?.height == 0 -> VIEW_TYPE_HEADER_BIG
            position == 0 && post != null                                           -> VIEW_TYPE_HEADER
            post == null || (position == 1 && comments[0] == NamedItem.Loading)     -> VIEW_TYPE_PROGRESS_BIG
            (comments[position - 1] as Comment).isLoading                           -> VIEW_TYPE_PROGRESS
            (comments[position - 1] as Comment).body == ""                          -> VIEW_TYPE_MORE
            else                                                                    -> VIEW_TYPE_COMMENT
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is PostHeaderViewHolder -> holder.bind(post!!)
            is MoreCommentsViewHolder -> holder.bind(comments[position - 1] as Comment)
            is CommentViewHolder -> holder.bind(comments[position - 1] as Comment, selectedComment == comments[position - 1])
            is ProgressViewHolder -> holder.bind(if (position > 0) comments[position - 1] else null)
        }
    }

    override fun getItemCount(): Int {
        return if (post != null) comments.size + 1 else comments.size
    }
}