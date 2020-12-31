package de.max.roehrl.vueddit2.ui.postlist

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import de.max.roehrl.vueddit2.MainActivity
import de.max.roehrl.vueddit2.model.AppViewModel
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.model.Subreddit


class PostListFragment : Fragment() {
    private val TAG = "PostListFragment"
    val viewModel: AppViewModel by activityViewModels()
    var currentSubreddit : Subreddit? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // postponeEnterTransition()

        val root = inflater.inflate(R.layout.fragment_posts, container, false)
        val postsAdapter = PostsAdapter()
        postsAdapter.showBigPreview = true
        val layoutManager = LinearLayoutManager(this.context)
        viewModel.posts.observe(viewLifecycleOwner, { posts ->
            val oldSize = postsAdapter.posts.size
            val newSize = posts.size
            postsAdapter.posts = posts
            if (newSize > oldSize) {
                if (oldSize > 0)
                    postsAdapter.notifyItemChanged(oldSize - 1)
                postsAdapter.notifyItemRangeInserted(oldSize, newSize - oldSize)
            } else {
                postsAdapter.notifyDataSetChanged()
            }
            /*(view?.parent as? ViewGroup)?.doOnPreDraw {
                startPostponedEnterTransition()
            }*/
        })
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = postsAdapter
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!viewModel.isPostListLoading() && layoutManager.findLastCompletelyVisibleItemPosition() + 1 == recyclerView.adapter?.itemCount) {
                    viewModel.loadMorePosts()
                }
            }
        })
        val swipeRefreshLayout: SwipeRefreshLayout = root.findViewById(R.id.swipe)
        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.post {
                if (swipeRefreshLayout.isRefreshing) {
                    viewModel.refreshPosts(false) {
                        swipeRefreshLayout.isRefreshing = false
                    }
                }
            }
        }
        viewModel.isLoggedIn.observe(viewLifecycleOwner) { isLoggedIn ->
            if (isLoggedIn)
                viewModel.loadMorePosts()
        }

        viewModel.subreddit.observe(viewLifecycleOwner) { subreddit ->
            if (viewModel.isLoggedIn.value == true && subreddit != currentSubreddit) {
                currentSubreddit = subreddit
                viewModel.refreshPosts(true)
            }
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbar: MaterialToolbar = view.findViewById(R.id.toolbar)
        val collapsingToolbar: CollapsingToolbarLayout = view.findViewById(R.id.collapsing_toolbar)
        val drawerLayout = (activity as MainActivity).drawerLayout
        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)
        collapsingToolbar.setupWithNavController(toolbar, navController, appBarConfiguration)
        toolbar.setOnMenuItemClickListener {
            Log.d(TAG, it.title.toString())
            true
        }
    }

    fun logout() {
        viewModel.logoutUser()
    }
}