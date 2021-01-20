package de.max.roehrl.vueddit2.ui.postlist

import android.view.View
import androidx.core.net.toUri
import androidx.navigation.NavDeepLinkRequest
import de.max.roehrl.vueddit2.MainActivity
import de.max.roehrl.vueddit2.service.Reddit
import de.max.roehrl.vueddit2.ui.postdetail.CommentViewHolder

class UserCommentViewHolder(itemView: View) : CommentViewHolder(itemView) {
    override fun onClick() {
        val request = NavDeepLinkRequest.Builder.fromUri((Reddit.api + comment.permalink).toUri()).build()
        (header.context as MainActivity).navController.navigate(request)
    }
}