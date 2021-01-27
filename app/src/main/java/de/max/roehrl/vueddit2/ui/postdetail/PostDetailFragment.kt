package de.max.roehrl.vueddit2.ui.postdetail

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.view.SupportMenuInflater
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.model.AppViewModel
import de.max.roehrl.vueddit2.model.Comment
import de.max.roehrl.vueddit2.model.NamedItem
import de.max.roehrl.vueddit2.service.Reddit
import de.max.roehrl.vueddit2.ui.dialog.Sidebar


class PostDetailFragment : Fragment() {
    private val TAG = "PostDetailFragment"
    private val viewModel: AppViewModel by activityViewModels()
    private var toolbar: MaterialToolbar? = null
    private var collapsingToolbar: CollapsingToolbarLayout? = null
    private lateinit var recyclerView: RecyclerView
    private val safeArgs: PostDetailFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // sharedElementEnterTransition = TransitionInflater.from(requireContext()).inflateTransition(R.transition.shared_header)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_post_detail, container, false)
        toolbar = root.findViewById(R.id.toolbar)
        collapsingToolbar = root.findViewById(R.id.collapsing_toolbar)
        recyclerView = root.findViewById(R.id.comments)
        val linearLayoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = linearLayoutManager
        val commentsAdapter = CommentsAdapter()
        if (viewModel.selectedPost.value != null && safeArgs.postName == viewModel.selectedPost.value?.name) {
            commentsAdapter.post = viewModel.selectedPost.value
            viewModel.resetComments()
        }
        recyclerView.adapter = commentsAdapter
        viewModel.comments.observe(viewLifecycleOwner) { comments ->
            val oldSize = commentsAdapter.comments.size
            val newSize = comments.size
            var result: DiffUtil.DiffResult? = null
            var comment: NamedItem? = null
            if (newSize == 1) {
                if (comments[0] is NamedItem.Loading) {
                    if (oldSize > 1) {
                        commentsAdapter.notifyItemRangeRemoved(2, oldSize - 1)
                    }
                    commentsAdapter.notifyItemChanged(1)
                }
            } else if (newSize == 0) {
                commentsAdapter.notifyItemRangeRemoved(1, oldSize)
            } else {
                if (oldSize == 1 && commentsAdapter.comments[0] == NamedItem.Loading) {
                    commentsAdapter.notifyItemChanged(1)
                    commentsAdapter.notifyItemRangeInserted(2, newSize - 1)
                    if (safeArgs.commentName != null) {
                        comment = comments.find { item ->
                            item is Comment && item.name == safeArgs.commentName
                        }
                    }
                } else {
                    result = DiffUtil.calculateDiff(CommentsDiffCallback(commentsAdapter.comments, comments))
                }
            }
            commentsAdapter.comments.clear()
            commentsAdapter.comments.addAll(comments)
            result?.dispatchUpdatesTo(commentsAdapter)
            if (comment != null && comment is Comment) {
                viewModel.selectComment(comment)
            }
        }
        viewModel.selectedComment.observe(viewLifecycleOwner) { comment ->
            Log.d(TAG, "Selecting comment ${safeArgs.commentName}")
            commentsAdapter.selectedComment = comment
            if (comment != null) {
                val index = commentsAdapter.comments.indexOf(comment)
                if (index >= 0
                        && (linearLayoutManager.findFirstVisibleItemPosition() > index
                        || linearLayoutManager.findLastVisibleItemPosition() < index)) {
                    recyclerView.post {
                        linearLayoutManager.scrollToPositionWithOffset(index + 1, 100)
                    }
                }
            }
        }
        viewModel.selectedPost.observe(viewLifecycleOwner) { post ->
            toolbar?.title = post?.title ?: Reddit.frontpage
            commentsAdapter.post = post
        }
        val swipeRefreshLayout: SwipeRefreshLayout = root.findViewById(R.id.swipe)
        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.post {
                if (swipeRefreshLayout.isRefreshing) {
                    viewModel.refreshComments {
                        swipeRefreshLayout.isRefreshing = false
                    }
                }
            }
        }
        viewModel.loadComments(safeArgs.subredditName, safeArgs.postName, safeArgs.commentName)
        return root
    }

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // postponeEnterTransition()
        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        collapsingToolbar!!.setupWithNavController(toolbar!!, navController, appBarConfiguration)
        onCreateOptionsMenu(toolbar!!.menu, SupportMenuInflater(context))
        toolbar!!.setOnMenuItemClickListener { onOptionsItemSelected(it) }
    }

    override fun onPause() {
        val headerVh = recyclerView.findViewHolderForAdapterPosition(0)
        if (headerVh is PostHeaderViewHolder) {
            headerVh.onPause()
        }
        super.onPause()
    }

    override fun onDestroyView() {
        val headerVh = recyclerView.findViewHolderForAdapterPosition(0)
        if (headerVh is PostHeaderViewHolder) {
            headerVh.onStop()
        }
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.post_detail, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                viewModel.refreshComments()
                true
            }
            R.id.action_sidebar -> {
                Sidebar(
                    requireContext(),
                    viewModel.selectedPost.value!!.subreddit,
                    viewModel.viewModelScope
                ).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}