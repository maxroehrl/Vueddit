package de.max.roehrl.vueddit2.ui.postlist

import android.view.View
import androidx.navigation.findNavController
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.service.Url
import de.max.roehrl.vueddit2.ui.postdetail.CommentViewHolder

class UserCommentViewHolder(itemView: View) : CommentViewHolder(itemView) {
    override val showSubreddit = true

    override fun onClick() {
        Url.openDeepLink(comment.permalink, header.findNavController(), R.id.userPostListFragment)
    }
}