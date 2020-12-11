package de.max.roehrl.vueddit2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import coil.load
import coil.request.Disposable
import coil.size.ViewSizeResolver
import de.max.roehrl.vueddit2.service.Reddit
import org.json.JSONObject


class PostDetailFragment(private val post: JSONObject) : Fragment() {
    private var disposable: Disposable? = null

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
        title.text = post.getString("title")

        val imageView: ImageView = view.findViewById(R.id.preview)
        val preview = Reddit.getPreview(post)
        if (preview != null) {
            disposable = imageView.load(preview.getString("url")) {
                placeholder(R.drawable.ic_comment_text_multiple_outline)
                size(ViewSizeResolver(imageView))
                error(R.drawable.ic_comment_text_multiple_outline)
                listener(onSuccess = { _, _ -> startPostponedEnterTransition() })
            }
        } else {
            imageView.load(R.drawable.ic_comment_text_multiple_outline) {
                listener(onSuccess = { _, _ -> startPostponedEnterTransition() })
            }
        }
    }
}