package de.max.roehrl.vueddit2.ui.viewholder

import android.view.View
import androidx.navigation.findNavController
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.service.Url

class UserCommentViewHolder(itemView: View) : CommentViewHolder(itemView, null) {
    override val showSubreddit = true

    override fun onClick() {
        Url.openDeepLink(comment.permalink, header.findNavController(), R.id.userPostListFragment)
    }
}