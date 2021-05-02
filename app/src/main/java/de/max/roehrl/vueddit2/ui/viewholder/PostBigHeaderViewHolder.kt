package de.max.roehrl.vueddit2.ui.viewholder

import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.core.net.toUri
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.imagepipeline.request.ImageRequestBuilder
import de.max.roehrl.vueddit2.model.Post
import de.max.roehrl.vueddit2.service.Util
import de.max.roehrl.vueddit2.ui.viewmodel.PostDetailViewModel

class PostBigHeaderViewHolder(itemView: View, viewModel: PostDetailViewModel) :
    PostHeaderViewHolder(itemView, viewModel) {
    companion object {
        private const val TAG = "PostBigHeaderViewHolder"
    }

    override fun updatePreviewImage(post: Post) {
        val image = post.image.url
        if (image != null) {
            imageView.visibility = View.VISIBLE
            val aspectFixHeight = Util.getAspectFixHeight(post.image.width!!, post.image.height!!)
            imageView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, aspectFixHeight)
            val request = ImageRequestBuilder.newBuilderWithSource(image.toUri())
                .setProgressiveRenderingEnabled(true)
                .build()
            imageView.controller = Fresco.newDraweeControllerBuilder()
                .setImageRequest(request)
                .setOldController(imageView.controller)
                .setControllerListener(object : BaseControllerListener<ImageInfo>() {
                    override fun onFailure(id: String?, throwable: Throwable?) {
                        Log.w(TAG, "Failed to load image with id '$id'", throwable)
                        imageView.visibility = View.GONE
                    }
                })
                .build()
        } else {
            imageView.visibility = View.GONE
        }
    }
}