package de.max.roehrl.vueddit2.ui.viewholder

import android.view.View
import android.widget.LinearLayout
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.model.Post
import de.max.roehrl.vueddit2.service.Util
import de.max.roehrl.vueddit2.ui.viewmodel.PostDetailViewModel

class PostBigHeaderViewHolder(itemView: View, viewModel: PostDetailViewModel) :
    PostHeaderViewHolder(itemView, viewModel) {
    override fun updatePreviewImage(post: Post) {
        val image = post.image.url
        if (image != null) {
            imageView.visibility = View.VISIBLE
            val aspectFixHeight = Util.getAspectFixHeight(post.image.width!!, post.image.height!!)
            imageView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, aspectFixHeight)
            imageView.hierarchy.setProgressBarImage(progress)
            imageView.setImageURI(image)
        } else {
            imageView.visibility = View.GONE
            imageView.hierarchy.setProgressBarImage(null)
            imageView.setActualImageResource(R.drawable.ic_comment_text_multiple_outline)
        }
    }
}