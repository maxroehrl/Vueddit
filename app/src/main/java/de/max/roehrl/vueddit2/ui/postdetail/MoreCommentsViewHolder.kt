package de.max.roehrl.vueddit2.ui.postdetail

import android.view.View
import androidx.activity.viewModels
import androidx.recyclerview.widget.RecyclerView
import de.max.roehrl.vueddit2.MainActivity
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.model.AppViewModel
import de.max.roehrl.vueddit2.model.Comment
import de.max.roehrl.vueddit2.ui.view.IndentedLabel

class MoreCommentsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val tv: IndentedLabel = itemView.findViewById(R.id.more)

    fun bind(comment: Comment) {
        tv.setDepth(comment.depth, 10)
        tv.text = if (comment.count == 0) {
            "continue this thread →"
        } else {
            "load ${comment.count} more comment${if (comment.count == 1) " ↓" else "s ↓"}"
        }
        tv.setOnClickListener {
            val viewModel: AppViewModel by (itemView.context as MainActivity).viewModels()
            viewModel.loadMoreComments(comment)
        }
    }
}