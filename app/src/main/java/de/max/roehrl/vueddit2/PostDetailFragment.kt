package de.max.roehrl.vueddit2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import com.facebook.drawee.drawable.ProgressBarDrawable
import com.facebook.drawee.view.SimpleDraweeView


class PostDetailFragment(private val post: Post) : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(requireContext())
                .inflateTransition(R.transition.shared_header)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.post_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()

        val postHeader = view.findViewById<RelativeLayout>(R.id.header)
        ViewCompat.setTransitionName(postHeader, "header")

        val title: TextView = view.findViewById(R.id.title)
        title.text = post.title

        val imageView: SimpleDraweeView = view.findViewById(R.id.preview)
        val preview = post.previewUrl
        if (preview != null) {
            imageView.setImageURI(preview)
            val progress = ProgressBarDrawable()
            progress.backgroundColor = 0x30FFFFFF
            progress.color = 0x8053BA82.toInt()
            imageView.hierarchy.setProgressBarImage(progress)
            startPostponedEnterTransition()
        } else {
            imageView.setActualImageResource(R.drawable.ic_comment_text_multiple_outline)
            startPostponedEnterTransition()
        }
    }
}