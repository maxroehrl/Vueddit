package de.max.roehrl.vueddit2.ui.postlist

import android.annotation.SuppressLint
import android.graphics.Color
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.TypedValue
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.drawable.ProgressBarDrawable
import com.facebook.drawee.view.SimpleDraweeView
import de.max.roehrl.vueddit2.MainActivity
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.model.AppViewModel
import de.max.roehrl.vueddit2.model.NamedItem
import de.max.roehrl.vueddit2.model.Post
import de.max.roehrl.vueddit2.service.Util

open class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val postHeader: RelativeLayout = itemView.findViewById(R.id.post_header)
    private val title: TextView = itemView.findViewById(R.id.title)
    private val meta: TextView = itemView.findViewById(R.id.meta)
    protected val imageView: SimpleDraweeView = itemView.findViewById(R.id.preview)
    private val votes: TextView = itemView.findViewById(R.id.votes)
    protected val progress = ProgressBarDrawable()
    protected lateinit var post: Post
    open val highlightAuthor = false

    init {
        progress.backgroundColor = 0x30FFFFFF
        progress.color = 0x8053BA82.toInt()
        title.setOnClickListener { view -> onClick(view) }
        meta.setOnClickListener { view -> onClick(view) }
        imageView.setOnClickListener { view -> onClick(view) }
    }

    open fun onClick(view: View) {
        val viewModel: AppViewModel by (view.context as MainActivity).viewModels()
        viewModel.selectedPost.value = post
        view.findNavController().navigate(R.id.action_postListFragment_to_postDetailFragment,
                //null,
                //null,
                //FragmentNavigatorExtras(postHeader to "header")
        )
    }

    fun Int.toDips() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), postHeader.resources.displayMetrics).toInt()

    @SuppressLint("SetTextI18n")
    open fun bind(post: NamedItem) {
        this.post = post as Post
        // ViewCompat.setTransitionName(postHeader, post.name)
        val builder = SpannableStringBuilder()

        if (post.link_flair_text != "null") {
            val flairString = SpannableString(post.link_flair_text)
            val color = if (post.link_flair_background_color != "" && post.link_flair_background_color != "null") post.link_flair_background_color else "#767676"
            flairString.setSpan(BackgroundColorSpan(Color.parseColor(color)), 0, flairString.length, 0)
            flairString.setSpan(RelativeSizeSpan(0.85f), 0, flairString.length, 0)
            builder.append(flairString)
            builder.append(" ")
        }

        val titleString = SpannableString(post.title)
        val titleColor = if (post.stickied) "#53ba82" else "#ffffff"
        titleString.setSpan(ForegroundColorSpan(Color.parseColor(titleColor)), 0, titleString.length, 0)
        builder.append(titleString)
        builder.append(" ")

        val domainString = SpannableString(" (${post.domain})")
        domainString.setSpan(ForegroundColorSpan(Color.parseColor("#767676")), 0, domainString.length, 0)
        domainString.setSpan(RelativeSizeSpan(0.85f), 0, domainString.length, 0)
        builder.append(domainString)

        title.setText(builder, TextView.BufferType.SPANNABLE)

        val metaBuilder = SpannableStringBuilder()

        if (post.over18) {
            val nsfwString = SpannableString("nsfw")
            nsfwString.setSpan(BackgroundColorSpan(Color.RED), 0, nsfwString.length, 0)
            metaBuilder.append(nsfwString)
            metaBuilder.append(" ")
        }

        if (post.spoiler) {
            val spoilerString = SpannableString("spoiler")
            spoilerString.setSpan(BackgroundColorSpan(Color.YELLOW), 0, spoilerString.length, 0)
            metaBuilder.append(spoilerString)
            metaBuilder.append(" ")
        }

        metaBuilder.append("${post.num_comments} comment${if (post.num_comments != 1) "s" else ""} in /r/${post.subreddit}\n${Util.getTimeFromNow(post.created_utc.toLong())} by ")


        val authorString = SpannableString("/u/${post.author}")
        val authorColor = if (highlightAuthor) "#53ba82" else "#767676"
        authorString.setSpan(ForegroundColorSpan(Color.parseColor(authorColor)), 0, authorString.length, 0)
        metaBuilder.append(authorString)
        metaBuilder.append(" ")

        if (highlightAuthor && post.author_flair_text != "null") {
            val authorFlairString = SpannableString(post.author_flair_text)
            val color = if (post.author_flair_background_color != "" && post.author_flair_background_color != "null") post.author_flair_background_color else "#767676"
            authorFlairString.setSpan(BackgroundColorSpan(Color.parseColor(color)), 0, authorFlairString.length, 0)
            metaBuilder.append(authorFlairString)
        }
        metaBuilder.append("\n")

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

        votes.text = post.getScore()
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
}