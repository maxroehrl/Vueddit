package de.max.roehrl.vueddit2.ui.postlist

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import de.max.roehrl.vueddit2.MainActivity
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.model.AppViewModel
import de.max.roehrl.vueddit2.model.NamedItem
import de.max.roehrl.vueddit2.model.Post
import de.max.roehrl.vueddit2.model.Subreddit
import de.max.roehrl.vueddit2.service.Reddit
import de.max.roehrl.vueddit2.service.Util.setActionBarUpIndicator
import kotlin.math.min


class PostListFragment : Fragment() {
    private val TAG = "PostListFragment"
    private val viewModel: AppViewModel by activityViewModels()
    private var currentSubreddit : Subreddit? = null
    private var toolbar: MaterialToolbar? = null
    private var collapsingToolbar: CollapsingToolbarLayout? = null
    private var tabLayout: TabLayout? = null
    private var init = false
    private var sortings = listOf("best", "hot", "top", "new", "controversial", "rising")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // postponeEnterTransition()

        val root = inflater.inflate(R.layout.fragment_posts, container, false)
        val postsAdapter = PostsAdapter()
        val layoutManager = LinearLayoutManager(this.context)
        viewModel.posts.observe(viewLifecycleOwner, { posts ->
            val oldSize = postsAdapter.posts.size
            val newSize = posts.size
            postsAdapter.posts = posts
            postsAdapter.showBigPreview = shouldShowBigTemplate(posts)
            if (newSize > oldSize) {
                if (oldSize > 0)
                    postsAdapter.notifyItemChanged(oldSize - 1)
                postsAdapter.notifyItemRangeInserted(oldSize, newSize - oldSize)
            } else {
                postsAdapter.notifyDataSetChanged()
            }
            // (view?.parent as? ViewGroup)?.doOnPreDraw { startPostponedEnterTransition() }
        })
        toolbar = root.findViewById(R.id.toolbar)
        collapsingToolbar = root.findViewById(R.id.collapsing_toolbar)
        tabLayout = root.findViewById(R.id.tab_layout)
        if (tabLayout != null) {
            for (sorting in sortings) {
                tabLayout?.addTab(tabLayout!!.newTab().setText(sorting))
            }
            tabLayout!!.getTabAt(0)!!.select()
            tabLayout!!.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    viewModel.setPostSorting(tab?.text.toString())
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
        }

        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = postsAdapter
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!viewModel.isPostListLoading() && layoutManager.findLastCompletelyVisibleItemPosition() + 2 == recyclerView.adapter?.itemCount && dy != 0) {
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
            toolbar?.title = subreddit?.name ?: Reddit.frontpage
            if (viewModel.isLoggedIn.value == true && subreddit != currentSubreddit && currentSubreddit != null) {
                viewModel.refreshPosts(true)
            }
            currentSubreddit = subreddit
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mainActivity = activity as MainActivity
        val drawerLayout = mainActivity.drawerLayout
        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)
        // collapsingToolbar.setupWithNavController(toolbar!!, navController, appBarConfiguration)
        if (!init) {
            navController.addOnDestinationChangedListener { _, destination, _ ->
                if (destination.id == R.id.postListFragment) {
                    mainActivity.setSupportActionBar(toolbar)
                    if (toolbar?.navigationIcon == null) {
                        toolbar?.setActionBarUpIndicator(true)
                    }
                    toolbar?.setNavigationOnClickListener { NavigationUI.navigateUp(navController, appBarConfiguration) }
                }
            }
            init = true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.post_list, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                Log.d(TAG, "Refresh item selected")
                true
            }
            R.id.action_sidebar -> {
                Log.d(TAG, "Sidebar item selected")
                true
            }
            R.id.action_logout -> {
                Log.d(TAG, "Logout item selected")
                viewModel.logoutUser()
                true
            }
            R.id.action_user_posts -> {
                Log.d(TAG, "User posts item selected")
                true
            }
            R.id.action_toggle_big_preview -> {
                Log.d(TAG, "Toggle preview size item selected")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun shouldShowBigTemplate(postList: List<NamedItem>): Boolean {
        return if (viewModel.isBigTemplatePreferred.value != null) {
            viewModel.isBigTemplatePreferred.value!!
        } else {
            val subHasMoreThanHalfPictures = postList
                    .subList(min(2, postList.size), min(10, postList.size))
                    .map { post -> if ((post as Post).image.url != null) 1 else 0 }
                    .fold(0) { acc, it -> acc + it } > 4
            val isNotFrontPage = currentSubreddit != null && currentSubreddit != Subreddit.frontPage
            subHasMoreThanHalfPictures && isNotFrontPage
        }
    }
}