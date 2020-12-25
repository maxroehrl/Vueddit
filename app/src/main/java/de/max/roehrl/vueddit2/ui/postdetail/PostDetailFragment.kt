package de.max.roehrl.vueddit2.ui.postdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.transition.TransitionInflater
import de.max.roehrl.vueddit2.model.AppViewModel
import de.max.roehrl.vueddit2.model.Post
import de.max.roehrl.vueddit2.R


class PostDetailFragment(private val post: Post) : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(requireContext())
                .inflateTransition(R.transition.shared_header)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val viewModel: AppViewModel by activityViewModels()
        viewModel.selectedPost.value = post
        val root = inflater.inflate(R.layout.post_detail, container, false)
        val recyclerView = root.findViewById<RecyclerView>(R.id.comments)
        recyclerView.layoutManager = LinearLayoutManager(context)
        val commentsAdapter = CommentsAdapter(post)
        recyclerView.adapter = commentsAdapter
        viewModel.comments.observe(viewLifecycleOwner, {
            commentsAdapter.setComments(it)
            commentsAdapter.notifyDataSetChanged()
        })
        val swipeRefreshLayout : SwipeRefreshLayout = root.findViewById(R.id.swipe)
        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.post {
                if (swipeRefreshLayout.isRefreshing) {
                    viewModel.loadComments {
                        swipeRefreshLayout.isRefreshing = false
                    }
                }
            }
        }
        viewModel.loadComments {}
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // postponeEnterTransition()
    }
}