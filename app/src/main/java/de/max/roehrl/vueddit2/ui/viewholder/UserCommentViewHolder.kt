package de.max.roehrl.vueddit2.ui.viewholder

import android.view.View
import androidx.navigation.findNavController
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.service.Url
import kotlinx.coroutines.CoroutineScope

class UserCommentViewHolder(itemView: View, scope: CoroutineScope) :
    CommentViewHolder(itemView, null, scope) {
    override val showSubreddit = true

    override fun onClick() {
        Url.openDeepLink(comment.permalink, header.findNavController(), R.id.userPostListFragment)
    }
}