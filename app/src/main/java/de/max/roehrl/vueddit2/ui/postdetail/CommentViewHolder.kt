package de.max.roehrl.vueddit2.ui.postdetail

import android.annotation.SuppressLint
import android.graphics.Color
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.model.Comment
import de.max.roehrl.vueddit2.model.NamedItem
import de.max.roehrl.vueddit2.service.Markdown
import de.max.roehrl.vueddit2.service.Util
import de.max.roehrl.vueddit2.ui.view.IndentedLabel

class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val TAG = "CommentViewHolder"
    private val header: IndentedLabel = itemView.findViewById(R.id.comment_header)
    private val body: IndentedLabel = itemView.findViewById(R.id.comment_body)
    private lateinit var comment: Comment
    val showSubreddit = false

    @SuppressLint("SetTextI18n")
    fun bind(comment: NamedItem) {
        this.comment = comment as Comment

        val builder = SpannableStringBuilder()

        val authorString = SpannableString(comment.author)
        val authorColor = when {
            comment.is_submitter                 -> "#53ba82"
            comment.distinguished == "moderator" -> "#4afff5"
            comment.distinguished == "admin"     -> "#c40013"
            else                                 -> "#c4c4c4"
        }
        authorString.setSpan(ForegroundColorSpan(Color.parseColor(authorColor)), 0, authorString.length, 0)
        builder.append(authorString)
        val timeFromNow = Util.getTimeFromNow(comment.created_utc.toLong())
        val edited = if (comment.edited) " *" else ""
        builder.append(" ${comment.ups} points $timeFromNow$edited")

        if (showSubreddit) {
            builder.append("in /r/${comment.subreddit} ")
        }
        if (comment.gid_1 != null && comment.gid_1 > 0) {
            builder.append("\uD83E\uDD48x${comment.gid_1} ")
        }
        if (comment.gid_2 != null && comment.gid_2 > 0) {
            builder.append("\uD83E\uDD47x${comment.gid_2} ")
        }
        if (comment.gid_3 != null && comment.gid_3 > 0) {
            builder.append("\uD83E\uDD49x${comment.gid_3} ")
        }
        if (comment.author_flair_text != "" && comment.author_flair_text != "null") {
            builder.append(" ")
            val flairString = SpannableString(comment.author_flair_text)
            val color = if (comment.author_flair_background_color != "" && comment.author_flair_background_color != "null") comment.author_flair_background_color else "#767676"
            try {
                flairString.setSpan(BackgroundColorSpan(Color.parseColor(color)), 0, flairString.length, 0)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Failed to parse color: '$color'", e)
            }
            flairString.setSpan(ForegroundColorSpan(Color.WHITE), 0, flairString.length, 0)
            builder.append(flairString)
            builder.append(" ")
        }
        header.setText(builder, TextView.BufferType.SPANNABLE)
        header.setDepth(comment.depth, 30)

        Markdown.getInstance(body.context).setMarkdown(body, comment.getSpannedBody(body.context))
        body.setDepth(comment.depth, 10)
    }
}