package de.max.roehrl.vueddit2.ui.viewholder

import android.view.View
import android.widget.LinearLayout
import androidx.core.net.toUri
import de.max.roehrl.vueddit2.model.Post
import de.max.roehrl.vueddit2.service.Util
import de.max.roehrl.vueddit2.ui.extensions.loadWithPlaceholders
import kotlinx.coroutines.CoroutineScope

class PostBigViewHolder(itemView: View, scope: CoroutineScope) : PostViewHolder(itemView, scope) {
    override fun updatePreviewImage(post: Post) {
        val image = post.image.url
        if (image != null) {
            imageView.visibility = View.VISIBLE
            val aspectFixHeight = Util.getAspectFixHeight(post.image.width!!, post.image.height!!)
            imageView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, aspectFixHeight)
            imageView.loadWithPlaceholders(image.toUri())
        } else {
            imageView.visibility = View.GONE
        }
    }
}