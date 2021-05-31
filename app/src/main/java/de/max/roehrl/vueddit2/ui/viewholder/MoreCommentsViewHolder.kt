package de.max.roehrl.vueddit2.ui.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.model.Comment
import de.max.roehrl.vueddit2.ui.adapter.CommentsAdapter
import de.max.roehrl.vueddit2.ui.view.IndentedLabel
import de.max.roehrl.vueddit2.ui.viewmodel.PostDetailViewModel

class MoreCommentsViewHolder(
    itemView: View,
    private val adapter: CommentsAdapter,
    private val viewModel: PostDetailViewModel
) : RecyclerView.ViewHolder(itemView) {
    private val tv: IndentedLabel = itemView.findViewById(R.id.more)

    fun bind(comment: Comment) {
        tv.setDepth(comment.depth)
        tv.text = if (comment.count == 0) {
            "continue this thread →"
        } else {
            "load ${comment.count} more comment${if (comment.count == 1) " ↓" else "s ↓"}"
        }
        tv.setOnClickListener {
            comment.isLoading = true
            adapter.refreshComment(comment)
            viewModel.loadMoreComments(comment)
        }
    }
}