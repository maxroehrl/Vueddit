package de.max.roehrl.vueddit2.ui.viewholder

import android.graphics.Color
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.model.Comment
import de.max.roehrl.vueddit2.model.NamedItem
import de.max.roehrl.vueddit2.service.Markdown
import de.max.roehrl.vueddit2.service.Reddit
import de.max.roehrl.vueddit2.service.Util
import de.max.roehrl.vueddit2.ui.fragment.PostDetailFragmentDirections
import de.max.roehrl.vueddit2.ui.view.IndentedLabel
import de.max.roehrl.vueddit2.ui.viewmodel.PostDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

open class CommentViewHolder(itemView: View, private val viewModel: PostDetailViewModel?) :
    RecyclerView.ViewHolder(itemView) {
    companion object {
        private const val TAG = "CommentViewHolder"
    }
    protected val header: IndentedLabel = itemView.findViewById(R.id.comment_header)
    private val body: IndentedLabel = itemView.findViewById(R.id.comment_body)
    private val votes: TextView = itemView.findViewById(R.id.votes)
    private val upvote: TextView = itemView.findViewById(R.id.up)
    private val downvote: TextView = itemView.findViewById(R.id.down)
    private val done: TextView = itemView.findViewById(R.id.done)
    private val hide: TextView = itemView.findViewById(R.id.hide)
    private val more: TextView = itemView.findViewById(R.id.more)
    private val prev: TextView = itemView.findViewById(R.id.prev)
    private val next: TextView = itemView.findViewById(R.id.next)
    private val buttonBar: LinearLayout = itemView.findViewById(R.id.button_bar)
    private val commentLayout: RelativeLayout = itemView.findViewById(R.id.comment)
    protected lateinit var comment: Comment
    open val showSubreddit = false

    init {
        header.setOnClickListener { onClick() }
        body.setOnClickListener { onClick() }
        upvote.setOnClickListener { vote(true) }
        downvote.setOnClickListener { vote(false) }
        done.setOnClickListener { selectComment(null) }
        hide.setOnClickListener { collapse() }
        more.setOnClickListener { showMore() }
        prev.setOnClickListener { selectNeighboringComment(false) }
        next.setOnClickListener { selectNeighboringComment(true) }
    }

    fun bind(comment: NamedItem, isSelected: Boolean = false) {
        this.comment = comment as Comment

        if (isSelected) {
            buttonBar.visibility = View.VISIBLE
            commentLayout.background = ContextCompat.getDrawable(commentLayout.context, R.drawable.comment_bg)
            updateVotes()
            if (comment.depth == 0) {
                prev.text = prev.context.getString(R.string.comment_prev)
                next.text = next.context.getString(R.string.comment_next)
            } else {
                prev.text = prev.context.getString(R.string.comment_parent)
                next.text = next.context.getString(R.string.comment_root)
            }
        } else {
            buttonBar.visibility = View.GONE
            commentLayout.background = null
        }

        val builder = SpannableStringBuilder()

        val authorString = SpannableString(comment.author)
        val authorColor = ContextCompat.getColor(header.context, when {
            comment.is_submitter                 -> R.color.comment_author_op
            comment.distinguished == "moderator" -> R.color.comment_author_mod
            comment.distinguished == "admin"     -> R.color.comment_author_admin
            else                                 -> R.color.comment_author
        })
        authorString.setSpan(ForegroundColorSpan(authorColor), 0, authorString.length, 0)
        builder.append(authorString)

        if (comment.likes != null) {
            val textId = if (comment.likes == true) R.string.upvote_arrow else R.string.downvote_arrow
            val liked = SpannableString(header.context.getString(textId))
            liked.setSpan(ForegroundColorSpan(getVoteChevronColor(comment.likes)), 0, liked.length, 0)
            builder.append(liked)
        }

        builder.append(" ")
        builder.append(header.resources.getQuantityString(R.plurals.num_upvotes, comment.ups, comment.ups))
        builder.append(" ")
        builder.append(Util.getTimeFromNow(comment.created_utc.toLong()))
        if (comment.edited) {
            builder.append(" *")
        }
        if (comment.children != null) {
            builder.append(" [+] (")
            val num = comment.children?.size ?: 0
            builder.append(header.resources.getQuantityString(R.plurals.num_children, num, num))
            builder.append(") ")
        }
        if (showSubreddit) {
            builder.append(" in /r/${comment.subreddit} ")
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
        if (comment.author_flair_text != "" && comment.author_flair_text != "null" && comment.author_flair_background_color != "null") {
            builder.append(" ")
            val flairString = SpannableString(comment.author_flair_text)
            var color = ContextCompat.getColor(header.context, R.color.comment_flair_bg)
            try {
                if (comment.author_flair_background_color != "")
                    color = Color.parseColor(comment.author_flair_background_color)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Failed to parse author_flair_background_color: '${comment.author_flair_background_color}'")
            }
            flairString.setSpan(BackgroundColorSpan(color), 0, flairString.length, 0)
            flairString.setSpan(ForegroundColorSpan(ContextCompat.getColor(header.context, R.color.comment_flair_text)), 0, flairString.length, 0)
            builder.append(flairString)
            builder.append(" ")
        }
        header.setText(builder, TextView.BufferType.SPANNABLE)
        header.setDepth(comment.depth, 30)

        if (comment.children != null) {
            body.visibility = View.GONE
        } else {
            body.visibility = View.VISIBLE
            Markdown.getInstance(body.context).setMarkdown(body, comment.getSpannedBody(body.context))
            body.setDepth(comment.depth, 10)
        }
    }

    protected open fun onClick() {
        if (comment.children != null) {
            collapse()
        } else {
            selectComment(comment)
        }
    }

    private fun collapse() {
        viewModel?.collapse(comment)
    }

    private fun selectComment(comment: Comment?) {
        viewModel?.selectComment(comment)
    }

    private fun getVoteChevronColor(up: Boolean?): Int {
        val colorString = if ((comment.likes == up && up != null) || (up == null && comment.likes != null)) {
            if (comment.likes == true) R.color.upvoted else R.color.downvoted
        } else {
            R.color.neutral
        }
        return ContextCompat.getColor(header.context, colorString)
    }

    private fun vote(up: Boolean) {
        GlobalScope.launch(Dispatchers.IO) {
            var dir = if (up) 1 else -1
            if (comment.likes == up) {
                comment.likes = null
                comment.ups -= dir
                dir = 0
            } else {
                comment.likes = up
                comment.ups += dir
            }
            Reddit.getInstance(header.context).vote(comment.name, dir.toString())
            GlobalScope.launch(Dispatchers.Main) {
                updateVotes()
            }
        }
    }

    private fun updateVotes() {
        votes.text = comment.getScore()
        votes.setTextColor(getVoteChevronColor(null))
        upvote.setTextColor(getVoteChevronColor(true))
        downvote.setTextColor(getVoteChevronColor(false))
    }

    private fun showMore() {
        MaterialAlertDialogBuilder(more.context).apply {
            val items = mutableListOf<String>()
            items.add(more.context.getString(if (comment.saved) R.string.unsave else R.string.save))
            items.add(more.context.getString(R.string.goto_user, comment.author))
            setItems(items.toTypedArray()) { _, which ->
                when (which) {
                    0 -> {
                        comment.saveOrUnsave(context)
                    }
                    1 -> {
                        more.findNavController().navigate(
                            PostDetailFragmentDirections.actionPostDetailFragmentToUserPostListFragment(
                                comment.author
                            )
                        )
                    }
                }
            }
            show()
        }
    }

    private fun selectNeighboringComment(isNextButton: Boolean) {
        var depth = comment.depth
        val next = isNextButton && depth == 0
        depth = if (isNextButton || depth == 0) 0 else depth - 1
        viewModel?.selectNeighboringComment(comment, depth, next)
    }
}