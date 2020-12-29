package de.max.roehrl.vueddit2.ui.postlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.model.NamedItem

class PostsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private inner class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    var posts: List<NamedItem> = emptyList()

    companion object {
        private const val VIEW_TYPE_DATA = 0
        private const val VIEW_TYPE_PROGRESS = 1
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
            VIEW_TYPE_DATA -> {
                PostViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.post_item,
                        parent,
                        false
                    )
                )
            }
            else -> throw IllegalArgumentException("viewType not found")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (posts[position] == NamedItem.Loading) {
            VIEW_TYPE_PROGRESS
        } else {
            VIEW_TYPE_DATA
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