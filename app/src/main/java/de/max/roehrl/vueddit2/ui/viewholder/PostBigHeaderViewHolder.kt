package de.max.roehrl.vueddit2.ui.viewholder

import android.view.View
import android.widget.LinearLayout
import androidx.core.net.toUri
import coil.load
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
            imageView.load(image.toUri()) {
                crossfade(true)
                placeholder(R.drawable.ic_image)
                error(R.drawable.ic_broken_image)
            }
        } else {
            imageView.visibility = View.GONE
        }
    }
}