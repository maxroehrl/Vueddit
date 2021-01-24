package de.max.roehrl.vueddit2.ui.postlist

import android.view.View
import androidx.core.net.toUri
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.findNavController
import androidx.navigation.navOptions
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.service.Reddit
import de.max.roehrl.vueddit2.ui.postdetail.CommentViewHolder

class UserCommentViewHolder(itemView: View) : CommentViewHolder(itemView) {
    override val showSubreddit = true

    override fun onClick() {
        val request = NavDeepLinkRequest.Builder.fromUri((Reddit.api + comment.permalink).toUri()).build()
        val options = navOptions {
            anim {
                enter = R.anim.slide_in_right
                exit = R.anim.slide_out_left
                popEnter = R.anim.slide_in_left
                popExit = R.anim.slide_out_right
            }
            popUpTo = R.id.userPostListFragment
        }
        header.findNavController().navigate(request, options)
    }
}