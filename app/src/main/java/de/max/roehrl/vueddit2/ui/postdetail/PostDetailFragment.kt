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
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.model.AppViewModel
import de.max.roehrl.vueddit2.service.Reddit
import de.max.roehrl.vueddit2.ui.dialog.Sidebar


class PostDetailFragment : Fragment() {
    private val TAG = "PostDetailFragment"
    private val viewModel: AppViewModel by activityViewModels()
    private var toolbar: MaterialToolbar? = null
    private var collapsingToolbar: CollapsingToolbarLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // sharedElementEnterTransition = TransitionInflater.from(requireContext()).inflateTransition(R.transition.shared_header)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_post_detail, container, false)
        toolbar = root.findViewById(R.id.toolbar)
        collapsingToolbar = root.findViewById(R.id.collapsing_toolbar)
        val recyclerView = root.findViewById<RecyclerView>(R.id.comments)
        recyclerView.layoutManager = LinearLayoutManager(context)
        val commentsAdapter = CommentsAdapter(viewModel.selectedPost.value!!)
        recyclerView.adapter = commentsAdapter
        viewModel.resetComments()
        viewModel.comments.observe(viewLifecycleOwner) { comments ->
            commentsAdapter.comments = comments
            commentsAdapter.notifyDataSetChanged()
        }
        viewModel.selectedPost.observe(viewLifecycleOwner) { post ->
            toolbar?.title = post?.title ?: Reddit.frontpage
        }
        val swipeRefreshLayout : SwipeRefreshLayout = root.findViewById(R.id.swipe)
        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.post {
                if (swipeRefreshLayout.isRefreshing) {
                    viewModel.refreshComments {
                        swipeRefreshLayout.isRefreshing = false
                    }
                }
            }
        }
        viewModel.loadComments {}
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
                Sidebar(requireContext(), viewModel.selectedPost.value!!.subreddit, viewModel.viewModelScope).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}