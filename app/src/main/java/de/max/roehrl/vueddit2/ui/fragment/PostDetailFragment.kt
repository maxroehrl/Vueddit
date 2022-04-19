package de.max.roehrl.vueddit2.ui.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.transition.MaterialContainerTransform
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.model.Comment
import de.max.roehrl.vueddit2.model.NamedItem
import de.max.roehrl.vueddit2.model.Post
import de.max.roehrl.vueddit2.ui.adapter.CommentsAdapter
import de.max.roehrl.vueddit2.ui.dialog.Sidebar
import de.max.roehrl.vueddit2.ui.viewholder.PostHeaderViewHolder
import de.max.roehrl.vueddit2.ui.viewmodel.PostDetailViewModel
import kotlinx.coroutines.launch
import java.util.*
import kotlin.concurrent.schedule

class PostDetailFragment : Fragment() {
    companion object {
        private const val TAG = "PostDetailFragment"

        private class CommentsDiffCallback(
                private val oldList: List<NamedItem>,
                private val newList: List<NamedItem>,
        ) : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldList.size

            override fun getNewListSize(): Int = newList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldList[oldItemPosition].id == newList[newItemPosition].id
            }

            override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
                val item1 = oldList[oldPosition]
                val item2 = newList[newPosition]

                return if (item1 is Comment && item2 is Comment) {
                    item1.isLoading == item2.isLoading && item1.isCollapsed() == item2.isCollapsed()
                } else {
                    true
                }
            }
        }
    }
    private lateinit var toolbar: MaterialToolbar
    private lateinit var collapsingToolbar: CollapsingToolbarLayout
    private lateinit var recyclerView: RecyclerView
    private val viewModel: PostDetailViewModel by viewModels {
        SavedStateViewModelFactory(requireActivity().application, this)
    }
    private val safeArgs: PostDetailFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?)  {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.swipe
            fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
            interpolator = FastOutSlowInInterpolator()
            isDrawDebugEnabled = true
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
    ): View? {
        val root = inflater.inflate(R.layout.fragment_post_detail, container, false)
        toolbar = root.findViewById(R.id.toolbar)
        collapsingToolbar = root.findViewById(R.id.collapsing_toolbar)

        val linearLayoutManager = LinearLayoutManager(context)
        val commentsAdapter = CommentsAdapter(viewModel)

        recyclerView = root.findViewById(R.id.comments)
        recyclerView.apply {
            layoutManager = linearLayoutManager
            adapter = commentsAdapter
            if (safeArgs.post != null) {
                viewTreeObserver.addOnPreDrawListener {
                    startPostponedEnterTransition()
                    true
                }
            }
        }

        if (safeArgs.post != null) {
            postponeEnterTransition()
            viewModel.setSelectedPost(safeArgs.post as Post)
        }

        viewModel.comments.observe(viewLifecycleOwner) { comments ->
            val oldSize = commentsAdapter.comments.size
            val newSize = comments.size
            var result: DiffUtil.DiffResult? = null
            var comment: NamedItem? = null
            var refreshAll = false
            if (newSize == 1) {
                if (comments[0] == NamedItem.Loading) {
                    if (oldSize > 1) {
                        commentsAdapter.notifyItemRangeRemoved(2, oldSize - 1)
                    }
                    commentsAdapter.notifyItemChanged(1)
                } else {
                    commentsAdapter.notifyItemChanged(0)
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
                } else if (oldSize - newSize == 1) {
                    // HACK This fixed collapsing a comment with one child which is bugged in DiffUtil
                    refreshAll = true
                } else {
                    result = DiffUtil.calculateDiff(CommentsDiffCallback(commentsAdapter.comments, comments))
                }
            }
            commentsAdapter.comments.clear()
            commentsAdapter.comments.addAll(comments)
            result?.dispatchUpdatesTo(commentsAdapter)
            if (refreshAll) {
                commentsAdapter.notifyDataSetChanged()
            }
            if (comment != null && comment is Comment) {
                viewModel.selectComment(comment)
            }
            if (newSize > 0 && oldSize == 1) {
                val first = comments[0]
                if (first is Comment && first.distinguished == "moderator") {
                    viewModel.collapse(first)
                }
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
            if (post != null) {
                toolbar.title = post.title
                toolbar.subtitle = "/r/${post.subreddit}"
                toolbar.menu.findItem(R.id.action_save).title = requireContext().getString(if (post.saved) R.string.unsave else R.string.save)
                toolbar.menu.findItem(R.id.action_goto_subreddit).title = requireContext().getString(R.string.goto_sub, post.subreddit)
                toolbar.menu.findItem(R.id.action_goto_user).title = requireContext().getString(R.string.goto_user, post.author)
            } else {
                toolbar.title = ""
                toolbar.subtitle = ""
            }
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            collapsingToolbar.setupWithNavController(toolbar, findNavController())
        } catch (e: IllegalStateException) {
            // Workaround for graph not being ready
            Timer().schedule(300) {
                viewModel.viewModelScope.launch {
                    try {
                        collapsingToolbar.setupWithNavController(toolbar, findNavController())
                    } catch (e: IllegalStateException) {
                        Log.e(TAG, "Failed to setup collapsing toolbar with nav controller", e)
                    }
                }
            }
        }
        toolbar.inflateMenu(R.menu.post_detail)
        toolbar.setOnMenuItemClickListener { onOptionsItemSelected(it) }
    }

    override fun onPause() {
        val headerVh = recyclerView.findViewHolderForAdapterPosition(0)
        if (headerVh is PostHeaderViewHolder) {
            headerVh.onPause()
        }
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.saveBundle()
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        val headerVh = recyclerView.findViewHolderForAdapterPosition(0)
        if (headerVh is PostHeaderViewHolder) {
            headerVh.onStop()
        }
        super.onDestroyView()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                viewModel.refreshComments()
                true
            }
            R.id.action_sidebar -> {
                Sidebar.show(requireContext(),
                    viewModel.selectedPost.value!!.subreddit,
                    viewModel.viewModelScope)
                true
            }
            R.id.action_save -> {
                viewModel.selectedPost.value?.saveOrUnsave(requireContext(),
                    viewModel.viewModelScope)
                toolbar.menu
                true
            }
            R.id.action_goto_subreddit -> {
                gotoSubreddit(viewModel.selectedPost.value!!.subreddit)
                true
            }
            R.id.action_goto_user -> {
                gotoUser(viewModel.selectedPost.value!!.author)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun gotoUser(userName: String) {
        findNavController().navigate(
            PostDetailFragmentDirections.actionPostDetailFragmentToUserPostListFragment(
                userName,
            ),
        )
    }

    private fun gotoSubreddit(subreddit: String) {
        findNavController().navigate(
            PostDetailFragmentDirections.actionPostDetailFragmentToPostListFragment(
                subreddit,
            ),
        )
    }
}