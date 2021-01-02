package de.max.roehrl.vueddit2.ui.postdetail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.max.roehrl.vueddit2.model.Comment
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.model.NamedItem
import de.max.roehrl.vueddit2.model.Post

class CommentsAdapter(private val post: Post) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private inner class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    private var comments : MutableList<NamedItem> = mutableListOf()

    companion object {
        private const val VIEW_TYPE_PROGRESS_BIG = 0
        private const val VIEW_TYPE_PROGRESS = 1
        private const val VIEW_TYPE_COMMENT = 2
        private const val VIEW_TYPE_HEADER = 3
        private const val VIEW_TYPE_HEADER_BIG = 4
        private const val VIEW_TYPE_MORE = 5
    }

    fun setComments(comments: MutableList<NamedItem>) {
        this.comments = comments
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_PROGRESS_BIG -> ProgressViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                            R.layout.loading_item,
                            parent,
                            false
                    )
            )
            VIEW_TYPE_PROGRESS -> ProgressViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                            R.layout.loading_item_small,
                            parent,
                            false
                    )
            )
            VIEW_TYPE_COMMENT -> CommentViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                            R.layout.comment_item,
                            parent,
                            false
                    )
            )
            VIEW_TYPE_HEADER -> PostHeaderViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                            R.layout.post_detail_header,
                            parent,
                            false
                    )
            )
            VIEW_TYPE_HEADER_BIG -> PostBigHeaderViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                            R.layout.post_detail_header_big,
                            parent,
                            false
                    )
            )
            VIEW_TYPE_MORE -> MoreCommentsViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                            R.layout.more_comments_item,
                            parent,
                            false
                    )
            )
            else -> throw IllegalArgumentException("viewType not found")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position == 0 && post.preview.url != null && post.video.src == "" -> VIEW_TYPE_HEADER_BIG
            position == 0                                                     -> VIEW_TYPE_HEADER
            position == 1 && comments[0] == NamedItem.Loading                 -> VIEW_TYPE_PROGRESS_BIG
            comments[position - 1] == NamedItem.Loading                       -> VIEW_TYPE_PROGRESS
            (comments[position - 1] as Comment).body == ""                    -> VIEW_TYPE_MORE
            else                                                              -> VIEW_TYPE_COMMENT
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is PostHeaderViewHolder   -> holder.bind(post)
            is MoreCommentsViewHolder -> holder.bind(comments[position - 1] as Comment)
            is CommentViewHolder      -> holder.bind(comments[position - 1] as Comment)
        }
    }

    override fun getItemCount(): Int {
        return comments.size + 1
    }
}