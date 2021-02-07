package de.max.roehrl.vueddit2.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.model.Comment
import de.max.roehrl.vueddit2.model.NamedItem
import de.max.roehrl.vueddit2.model.Post
import de.max.roehrl.vueddit2.ui.viewholder.CommentViewHolder
import de.max.roehrl.vueddit2.ui.viewholder.PostBigViewHolder
import de.max.roehrl.vueddit2.ui.viewholder.PostViewHolder
import de.max.roehrl.vueddit2.ui.viewholder.UserCommentViewHolder

class PostsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private inner class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    var posts: List<NamedItem> = emptyList()
    var showBigPreview: Boolean? = null
    var highlightStickied = true

    companion object {
        private const val VIEW_TYPE_PROGRESS = 0
        private const val VIEW_TYPE_POST_SMALL = 1
        private const val VIEW_TYPE_POST_BIG = 2
        private const val VIEW_TYPE_COMMENT = 3
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_PROGRESS   -> ProgressViewHolder(inflate(parent, R.layout.loading_item))
            VIEW_TYPE_POST_SMALL -> PostViewHolder(inflate(parent, R.layout.post_item))
            VIEW_TYPE_POST_BIG   -> PostBigViewHolder(inflate(parent, R.layout.post_item_big))
            VIEW_TYPE_COMMENT    -> UserCommentViewHolder(inflate(parent, R.layout.comment_item))
            else                 -> throw IllegalArgumentException("viewType not found")
        }
    }

    private fun inflate(parent: ViewGroup, viewId: Int): View {
        return LayoutInflater.from(parent.context).inflate(viewId, parent, false)
    }

    override fun getItemViewType(position: Int): Int {
        val item = posts[position]
        return when {
            item == NamedItem.Loading                                        -> VIEW_TYPE_PROGRESS
            showBigPreview == true && item is Post && item.image.url != null -> VIEW_TYPE_POST_BIG
            item is Comment                                                  -> VIEW_TYPE_COMMENT
            else                                                             -> VIEW_TYPE_POST_SMALL
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PostViewHolder) {
            holder.bind(posts[position], highlightStickied)
        } else if (holder is CommentViewHolder) {
            holder.bind(posts[position])
        }
    }

    override fun getItemCount(): Int {
        return posts.size
    }
}