package de.max.roehrl.vueddit2.ui.postlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.model.NamedItem
import de.max.roehrl.vueddit2.model.Post

class PostsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private inner class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    var posts: List<NamedItem> = emptyList()
    var showBigPreview = false

    companion object {
        private const val VIEW_TYPE_PROGRESS = 0
        private const val VIEW_TYPE_POST_SMALL = 1
        private const val VIEW_TYPE_POST_BIG = 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_PROGRESS -> {
                ProgressViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.loading_item,
                        parent,
                        false
                    )
                )
            }
            VIEW_TYPE_POST_SMALL -> {
                PostViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.post_item,
                        parent,
                        false
                    )
                )
            }
            VIEW_TYPE_POST_BIG -> {
                PostBigViewHolder(
                        LayoutInflater.from(parent.context).inflate(
                                R.layout.post_item_big,
                                parent,
                                false
                        )
                )
            }
            else -> throw IllegalArgumentException("viewType not found")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            posts[position] == NamedItem.Loading                          -> VIEW_TYPE_PROGRESS
            showBigPreview && (posts[position] as Post).image.url != null -> VIEW_TYPE_POST_BIG
            else                                                          -> VIEW_TYPE_POST_SMALL
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PostViewHolder) {
            holder.bind(posts[position])
        }
    }

    override fun getItemCount(): Int {
        return posts.size
    }
}