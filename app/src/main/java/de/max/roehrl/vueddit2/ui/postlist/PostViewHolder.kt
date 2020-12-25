package de.max.roehrl.vueddit2.ui.postlist

import android.annotation.SuppressLint
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.fragment.app.commit
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.drawable.ProgressBarDrawable
import com.facebook.drawee.view.SimpleDraweeView
import de.max.roehrl.vueddit2.model.Post
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.model.NamedItem
import de.max.roehrl.vueddit2.ui.postdetail.PostDetailFragment

open class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val postHeader: RelativeLayout = itemView.findViewById(R.id.post_header)
    private val title: TextView = itemView.findViewById(R.id.title)
    private val meta: TextView = itemView.findViewById(R.id.meta)
    private val imageView: SimpleDraweeView = itemView.findViewById(R.id.preview)
    private val votes: TextView = itemView.findViewById(R.id.votes)
    protected lateinit var post: Post

    init {
        val progress = ProgressBarDrawable()
        progress.backgroundColor = 0x30FFFFFF
        progress.color = 0x8053BA82.toInt()
        imageView.hierarchy.setProgressBarImage(progress)
        title.setOnClickListener { view -> onClick(view) }
        meta.setOnClickListener { view -> onClick(view) }
        imageView.setOnClickListener { view -> onClick(view) }
    }

    open fun onClick(view: View) {
        val detailFragment = PostDetailFragment(post)
        (view.context as AppCompatActivity).supportFragmentManager.commit {
            setReorderingAllowed(true)
            setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
            addSharedElement(postHeader, "header")
            replace(R.id.nav_host_fragment, detailFragment)
            addToBackStack(null)
        }
    }

    @SuppressLint("SetTextI18n")
    open fun bind(post: NamedItem) {
        this.post = post as Post
        ViewCompat.setTransitionName(postHeader, post.name)
        title.text = post.title
        meta.text = "(${post.domain})\n${post.num_comments} comment${if (post.num_comments != 1) "s" else ""} in /r/${post.subreddit}\n${post.created_utc} by /u/${post.author}\n"
        votes.text = post.getScore()
        val preview = post.previewUrl
        if (preview != null) {
            imageView.setImageURI(preview)
        } else {
            imageView.setActualImageResource(R.drawable.ic_comment_text_multiple_outline)
        }
    }
}