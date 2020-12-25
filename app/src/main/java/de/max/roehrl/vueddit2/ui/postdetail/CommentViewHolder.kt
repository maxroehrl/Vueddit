package de.max.roehrl.vueddit2.ui.postdetail

import android.annotation.SuppressLint
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import de.max.roehrl.vueddit2.model.Comment
import de.max.roehrl.vueddit2.ui.view.IndentedLabel
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.model.NamedItem

class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val header: IndentedLabel = itemView.findViewById(R.id.comment_header)
    private val body: IndentedLabel = itemView.findViewById(R.id.comment_body)
    private lateinit var comment: Comment

    @SuppressLint("SetTextI18n")
    fun bind(comment: NamedItem) {
        this.comment = comment as Comment
        header.text = "${comment.author} ${comment.ups} points"
        header.setDepth(comment.depth, 10)
        body.text = comment.body
        body.setDepth(comment.depth, 10)
    }
}