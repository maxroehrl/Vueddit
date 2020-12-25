package de.max.roehrl.vueddit2.ui.postdetail

import android.annotation.SuppressLint
import android.view.View
import android.widget.TextView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.drawable.ProgressBarDrawable
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.image.ImageInfo
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.model.NamedItem
import de.max.roehrl.vueddit2.model.Post
import de.max.roehrl.vueddit2.service.CustomTabs
import de.max.roehrl.vueddit2.ui.postlist.PostViewHolder

class PostHeaderViewHolder(itemView: View) : PostViewHolder(itemView) {
    private val selfText: TextView = itemView.findViewById(R.id.self_text)
    private val numComments: TextView = itemView.findViewById(R.id.num_comments)

    @SuppressLint("SetTextI18n")
    override fun bind(post: NamedItem) {
        super.bind(post)
        if (this.post.selftext.isEmpty()) {
            selfText.visibility = View.VISIBLE
            selfText.text = this.post.selftext
        } else {
            selfText.visibility = View.GONE
            selfText.text = ""
        }
        numComments.text = "Showing ${this.post.num_comments} comment${if (this.post.num_comments != 1) "s" else ""}"

        // val postHeader = view.findViewById<RelativeLayout>(R.id.header)
        // ViewCompat.setTransitionName(postHeader, "header")

        /*
        val imageView: SimpleDraweeView = itemView.findViewById(R.id.preview)
        imageView.controller = Fresco.newDraweeControllerBuilder().setControllerListener(object : BaseControllerListener<ImageInfo>() {
            override fun onFinalImageSet(id: String?, imageInfo: ImageInfo?, animatable: Animatable?) {
                startPostponedEnterTransition()
            }
        }).build() */
    }

    override fun onClick(view: View) {
        CustomTabs.openUrl(selfText.context, post.url)
    }
}