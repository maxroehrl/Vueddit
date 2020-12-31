package de.max.roehrl.vueddit2.ui.postlist

import android.annotation.SuppressLint
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

open class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val postHeader: RelativeLayout = itemView.findViewById(R.id.post_header)
    private val title: TextView = itemView.findViewById(R.id.title)
    private val meta: TextView = itemView.findViewById(R.id.meta)
    protected val imageView: SimpleDraweeView = itemView.findViewById(R.id.preview)
    private val votes: TextView = itemView.findViewById(R.id.votes)
    protected val progress = ProgressBarDrawable()
    protected lateinit var post: Post

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
        title.text = post.title
        meta.text = "(${post.domain})\n${post.num_comments} comment${if (post.num_comments != 1) "s" else ""} in /r/${post.subreddit}\n${post.created_utc} by /u/${post.author}\n"
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