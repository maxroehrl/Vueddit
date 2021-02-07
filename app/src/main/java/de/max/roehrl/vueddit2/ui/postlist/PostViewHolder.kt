package de.max.roehrl.vueddit2.ui.postlist

import android.graphics.Color
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.drawable.ProgressBarDrawable
import com.facebook.drawee.view.SimpleDraweeView
import de.max.roehrl.vueddit2.MainActivity
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.model.AppViewModel
import de.max.roehrl.vueddit2.model.NamedItem
import de.max.roehrl.vueddit2.model.Post
import de.max.roehrl.vueddit2.service.Reddit
import de.max.roehrl.vueddit2.service.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


open class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val TAG = "PostViewHolder"
    private val postHeader: RelativeLayout = itemView.findViewById(R.id.post_header)
    private val title: TextView = itemView.findViewById(R.id.title)
    private val meta: TextView = itemView.findViewById(R.id.meta)
    protected val imageView: SimpleDraweeView = itemView.findViewById(R.id.preview)
    private val votes: TextView = itemView.findViewById(R.id.votes)
    private val upvote: TextView = itemView.findViewById(R.id.up)
    private val downvote: TextView = itemView.findViewById(R.id.down)
    protected val progress = ProgressBarDrawable()
    protected lateinit var post: Post
    open val highlightAuthor = false

    init {
        progress.backgroundColor = 0x30FFFFFF
        progress.color = 0x8053BA82.toInt()
        title.setOnClickListener { view -> onClick(view) }
        meta.setOnClickListener { view -> onClick(view) }
        imageView.setOnClickListener { view -> onClick(view) }
        upvote.setOnClickListener { vote(true) }
        downvote.setOnClickListener { vote(false) }
    }

    open fun onClick(view: View) {
        val viewModel: AppViewModel by (view.context as MainActivity).viewModels()
        viewModel.selectedPost.value = post
        try {
            view.findNavController().navigate(
                    PostListFragmentDirections.actionPostListFragmentToPostDetailFragment(post.subreddit, post.name, null),
                    // null, null, FragmentNavigatorExtras(postHeader to "header")
            )
        } catch (error: IllegalArgumentException) {
            view.findNavController().navigate(UserPostListFragmentDirections.actionUserPostListFragmentToPostDetailFragment(post.subreddit, post.name, null))
        }
    }

    fun Int.toDips() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, toFloat(), postHeader.resources.displayMetrics).toInt()

    open fun bind(post: NamedItem, highlightSticky: Boolean = true) {
        this.post = post as Post
        // ViewCompat.setTransitionName(postHeader, post.name)
        val builder = SpannableStringBuilder()

        if (post.link_flair_text != "" && post.link_flair_text != "null") {
            val flairString = SpannableString(post.link_flair_text)
            var color = ContextCompat.getColor(meta.context, R.color.post_flair_bg)
            try {
                if (post.link_flair_background_color != "")
                    color = Color.parseColor(post.link_flair_background_color)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Failed to parse link_flair_background_color: '${post.link_flair_background_color}'")
            }
            flairString.setSpan(BackgroundColorSpan(color), 0, flairString.length, 0)
            flairString.setSpan(RelativeSizeSpan(0.85f), 0, flairString.length, 0)
            builder.append(flairString)
            builder.append(" ")
        }

        val titleString = SpannableString(post.title)
        val titleColor = ContextCompat.getColor(meta.context, if (highlightSticky && post.stickied) R.color.post_title_sticky else R.color.post_title)
        titleString.setSpan(ForegroundColorSpan(titleColor), 0, titleString.length, 0)
        builder.append(titleString)
        builder.append(" ")

        val domainString = SpannableString(" (${post.domain})")
        domainString.setSpan(ForegroundColorSpan(ContextCompat.getColor(meta.context, R.color.post_meta)), 0, domainString.length, 0)
        domainString.setSpan(RelativeSizeSpan(0.85f), 0, domainString.length, 0)

        builder.append(domainString)

        title.setText(builder, TextView.BufferType.SPANNABLE)

        val metaBuilder = SpannableStringBuilder()

        if (post.over18) {
            val nsfwString = SpannableString(meta.context.getString(R.string.nsfw))
            nsfwString.setSpan(ForegroundColorSpan(ContextCompat.getColor(meta.context, R.color.post_nsfw)), 0, nsfwString.length, 0)
            metaBuilder.append(nsfwString)
            metaBuilder.append(" ")
        }

        if (post.spoiler) {
            val spoilerString = SpannableString(meta.context.getString(R.string.spoiler))
            spoilerString.setSpan(ForegroundColorSpan(ContextCompat.getColor(meta.context, R.color.post_spoiler)), 0, spoilerString.length, 0)
            metaBuilder.append(spoilerString)
            metaBuilder.append(" ")
        }
        metaBuilder.append(meta.resources.getQuantityString(R.plurals.num_comments, post.num_comments, post.num_comments))
        metaBuilder.append(" in /r/")
        metaBuilder.append(post.subreddit)
        metaBuilder.append("\n")
        metaBuilder.append(Util.getTimeFromNow(post.created_utc.toLong()))
        if (post.edited) {
            metaBuilder.append(" *")
        }
        val authorString = SpannableString(" by /u/${post.author}")
        val authorColor = ContextCompat.getColor (meta.context, if (highlightAuthor) R.color.post_author_highlight else R.color.post_author)
        authorString.setSpan(ForegroundColorSpan(authorColor), 0, authorString.length, 0)
        metaBuilder.append(authorString)
        metaBuilder.append(" ")

        if (highlightAuthor && post.author_flair_text != "" && post.author_flair_text != "null") {
            val authorFlairString = SpannableString(post.author_flair_text)
            var color = ContextCompat.getColor(meta.context, R.color.post_flair_bg)
            try {
                if (post.author_flair_background_color != "")
                    color = Color.parseColor(post.author_flair_background_color)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Failed to parse author_flair_background_color: '${post.author_flair_background_color}'")
            }
            authorFlairString.setSpan(BackgroundColorSpan(color), 0, authorFlairString.length, 0)
            authorFlairString.setSpan(ForegroundColorSpan(ContextCompat.getColor(meta.context, R.color.post_flair_text)), 0, authorFlairString.length, 0)

            metaBuilder.append(authorFlairString)
        }
        if ((post.gid_1 != null && post.gid_1 > 0) || (post.gid_2 != null && post.gid_2 > 0) || (post.gid_3 != null && post.gid_3 > 0)) {
            metaBuilder.append("\n")
        }
        if (post.gid_1 != null && post.gid_1 > 0) {
            metaBuilder.append("\uD83E\uDD48x${post.gid_1} ")
        }
        if (post.gid_2 != null && post.gid_2 > 0) {
            metaBuilder.append("\uD83E\uDD47x${post.gid_2} ")
        }
        if (post.gid_3 != null && post.gid_3 > 0) {
            metaBuilder.append("\uD83E\uDD49x${post.gid_3} ")
        }
        meta.setText(metaBuilder, TextView.BufferType.SPANNABLE)

        updateVotes()
        updatePreviewImage(post)
    }

    open fun updatePreviewImage(post: Post) {
        val preview = post.image.url
        if (preview != null) {
            imageView.hierarchy.setProgressBarImage(progress)
            imageView.setImageURI(preview)
        } else {
            imageView.hierarchy.setProgressBarImage(null)
            imageView.setActualImageResource(R.drawable.ic_comment_text_multiple_outline)
        }
    }

    private fun updateVotes() {
        votes.text = post.getScore()
        votes.setTextColor(getVoteChevronColor(null))
        upvote.setTextColor(getVoteChevronColor(true))
        downvote.setTextColor(getVoteChevronColor(false))
    }

    private fun getVoteChevronColor(up: Boolean?): Int {
        val colorString = if ((post.likes == up && up != null) || (up == null && post.likes != null)) {
            if (post.likes == true) R.color.upvoted else R.color.downvoted
        } else {
            R.color.neutral
        }
        return ContextCompat.getColor(votes.context, colorString)
    }

    private fun vote(up: Boolean) {
        GlobalScope.launch(Dispatchers.IO) {
            var dir = if (up) 1 else -1
            if (post.likes == up) {
                post.likes = null
                post.score -= dir
                dir = 0
            } else {
                post.likes = up
                post.score += dir
            }
            Reddit.vote(post.name, dir.toString())
            GlobalScope.launch(Dispatchers.Main) {
                updateVotes()
            }
        }
    }
}