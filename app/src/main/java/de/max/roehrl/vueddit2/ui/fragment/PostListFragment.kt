package de.max.roehrl.vueddit2.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.model.NamedItem
import de.max.roehrl.vueddit2.model.Post
import de.max.roehrl.vueddit2.model.Subreddit
import de.max.roehrl.vueddit2.service.Reddit
import de.max.roehrl.vueddit2.service.Store
import de.max.roehrl.vueddit2.ui.activity.MainActivity
import de.max.roehrl.vueddit2.ui.adapter.PostsAdapter
import de.max.roehrl.vueddit2.ui.dialog.Sidebar
import de.max.roehrl.vueddit2.ui.listener.RecyclerOnTouchListener
import de.max.roehrl.vueddit2.ui.viewmodel.AppViewModel
import de.max.roehrl.vueddit2.ui.viewmodel.PostListViewModel
import kotlinx.coroutines.launch

open class PostListFragment : Fragment() {
    companion object {
        private const val TAG = "PostListFragment"
    }
    protected open val viewModel: PostListViewModel by viewModels()
    protected open val appViewModel: AppViewModel by activityViewModels()
    protected open lateinit var toolbar: MaterialToolbar
    protected open lateinit var sortingTabLayout: TabLayout
    protected open val isGroupTabLayoutVisible = false
    protected open val layoutId = R.layout.fragment_posts
    protected open val menuId = R.menu.post_list
    protected open var showGotoUser = true
    private val safeArgs: PostListFragmentArgs by navArgs()
    private lateinit var collapsingToolbar: CollapsingToolbarLayout
    private var currentSubreddit: Subreddit? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(layoutId, container, false)
        val postsAdapter = PostsAdapter()
        val layoutManager = LinearLayoutManager(context)

        toolbar = root.findViewById(R.id.toolbar)
        collapsingToolbar = root.findViewById(R.id.collapsing_toolbar)
        sortingTabLayout = root.findViewById(R.id.tab_layout)

        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = postsAdapter
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!viewModel.isPostListLoading()
                    && layoutManager.findLastVisibleItemPosition() + 2 == recyclerView.adapter?.itemCount
                    && dy != 0
                ) {
                    viewModel.loadMorePosts()
                }
            }
        })
        recyclerView.addOnItemTouchListener(
            RecyclerOnTouchListener(
                requireContext(),
                recyclerView,
                { view, position ->
                    onItemLongPressed(view, position)
                },
                null
            )
        )
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
        appViewModel.isBigTemplatePreferred.observe(viewLifecycleOwner) { isBigTemplatePreferred ->
            if (viewModel.posts.value != null && postsAdapter.showBigPreview != isBigTemplatePreferred) {
                postsAdapter.showBigPreview = isBigTemplatePreferred
                        ?: appViewModel.shouldShowBigTemplate(viewModel.posts.value!!)
                postsAdapter.notifyItemRangeChanged(0, postsAdapter.itemCount)
            }
        }
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            val oldSize = postsAdapter.posts.size
            val newSize = posts.size
            postsAdapter.posts = posts
            if (postsAdapter.showBigPreview == null && posts.any { item -> item != NamedItem.Loading }) {
                postsAdapter.showBigPreview = if (currentSubreddit == Subreddit.frontPage)
                    false
                else
                    appViewModel.shouldShowBigTemplate(posts)
                Log.i(TAG, "Showing ${if (postsAdapter.showBigPreview == true) "big" else "small"} preview")
            }
            if (newSize > oldSize) {
                if (oldSize > 0)
                    postsAdapter.notifyItemChanged(oldSize - 1)
                postsAdapter.notifyItemRangeInserted(oldSize, newSize - oldSize)
            } else {
                postsAdapter.notifyDataSetChanged()
            }
        }
        initialize(postsAdapter)

        for (sorting in viewModel.sortingList) {
            sortingTabLayout.addTab(sortingTabLayout.newTab().setText(sorting))
        }
        val index = viewModel.sortingList.indexOf(viewModel.postSorting.value)
        if (index != -1) {
            sortingTabLayout.selectTab(sortingTabLayout.getTabAt(index), true)
        }
        sortingTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                onSortingSelected(tab?.text.toString(), postsAdapter)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {
                onTabSelected(tab)
            }
        })
        return root
    }

    open fun initialize(postsAdapter: PostsAdapter) {
        if (currentSubreddit == null) {
            viewModel.selectSubreddit(safeArgs.subredditName)
        }
        appViewModel.isLoggedIn.observe(viewLifecycleOwner) { isLoggedIn ->
            if (isLoggedIn)
                viewModel.loadMorePosts()
        }
        viewModel.subreddit.observe(viewLifecycleOwner) { subreddit ->
            toolbar.title = subreddit?.name ?: Reddit.frontpage
            if (subreddit != null && subreddit != currentSubreddit && appViewModel.isLoggedIn.value == true) {
                postsAdapter.highlightStickied = subreddit != Subreddit.frontPage
                postsAdapter.showBigPreview = null
                viewModel.refreshPosts(true)
                if (safeArgs.rootFragment) {
                    appViewModel.selectSubreddit(subreddit.name, subreddit.isMultiReddit)
                }
            }
            currentSubreddit = subreddit ?: currentSubreddit
        }
        if (safeArgs.rootFragment) {
            appViewModel.subreddit.observe(viewLifecycleOwner) { subreddit ->
                viewModel.selectSubreddit(subreddit)
            }
        }
    }

    open fun onSortingSelected(sorting: String, adapter: PostsAdapter) {
        viewModel.setPostSorting(sorting)
        if (listOf("top", "rising").contains(sorting)) {
            val items = listOf("Hour", "Day", "Week", "Month", "Year", "All")
            MaterialAlertDialogBuilder(requireContext()).apply {
                setItems(items.toTypedArray()) { _, which ->
                    val time = items[which].lowercase()
                    viewModel.setTopPostsTime(time)
                    adapter.showBigPreview = null
                    viewModel.refreshPosts()
                }
                show()
            }
        } else {
            viewModel.refreshPosts()
        }
    }

    open fun onItemLongPressed(view: View, position: Int) {
        val post = viewModel.posts.value?.get(position)
        if (post is Post) {
            val items = mutableListOf(view.context.getString(if (post.saved) R.string.unsave else R.string.save))
            val showGotoSubreddit = post.subreddit != viewModel.subreddit.value?.name
            if (showGotoSubreddit) {
                items.add(view.context.getString(R.string.goto_sub, post.subreddit))
            }
            if (showGotoUser) {
                items.add(view.context.getString(R.string.goto_user, post.author))
            }
            MaterialAlertDialogBuilder(requireContext()).apply {
                setItems(items.toTypedArray()) { _, which ->
                    when (which) {
                        0 -> post.saveOrUnsave(context)
                        1 -> if (showGotoSubreddit) gotoSubreddit(post.subreddit) else gotoUser(post.author)
                        2 -> gotoUser(post.author)
                    }
                }
                show()
            }
        }
    }

    open fun gotoUser(userName: String) {
        findNavController().navigate(
            PostListFragmentDirections.actionPostListFragmentToUserPostListFragment(
                userName
            )
        )
    }

    open fun gotoSubreddit(subredditName: String) {
        val oldSubredditName = viewModel.subreddit.value ?: Subreddit.frontPage
        viewModel.selectSubreddit(subredditName)
        createGoBackSnackBar(oldSubredditName)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val topLevelDestinationIds = mutableSetOf<Int>()
        if (safeArgs.rootFragment)
            topLevelDestinationIds.add(R.id.postListFragment)
        val appBarConfiguration = AppBarConfiguration.Builder(topLevelDestinationIds).apply {
            if (safeArgs.rootFragment)
                setOpenableLayout((activity as MainActivity).drawerLayout)
        }.build()
        try {
            collapsingToolbar.setupWithNavController(toolbar, findNavController(), appBarConfiguration)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to setup collapsing toolbar with nav controller", e)
        }
        toolbar.inflateMenu(menuId)
        toolbar.setOnMenuItemClickListener { onOptionsItemSelected(it) }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                viewModel.refreshPosts()
                true
            }
            R.id.action_sidebar -> {
                showSidebar()
                true
            }
            R.id.action_logout -> {
                appViewModel.logoutUser()
                viewModel.resetPosts()
                true
            }
            R.id.action_user_posts -> {
                gotoLoggedInUserPosts()
                true
            }
            R.id.action_toggle_big_preview -> {
                appViewModel.toggleBigPreview(viewModel.posts.value!!)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun gotoLoggedInUserPosts() {
        viewModel.viewModelScope.launch {
            val userName = Store.getInstance(requireContext()).getUsername()
            val action = PostListFragmentDirections.actionPostListFragmentToUserPostListFragment(userName!!)
            findNavController().navigate(action)
        }
    }

    private fun showSidebar() {
        if (currentSubreddit != null
            && currentSubreddit != Subreddit.frontPage
            && !currentSubreddit!!.isMultiReddit
        ) {
            Sidebar.show(requireContext(), currentSubreddit!!.name, viewModel.viewModelScope)
        } else {
            val view = activity?.findViewById<View>(R.id.nav_host_fragment)
            val text = requireContext().getString(R.string.no_sidebar)
            Snackbar.make(view!!, text, 3000).apply {
                setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.snack_bar_background))
                setTextColor(ContextCompat.getColor(requireContext(), R.color.snack_bar_text))
                show()
            }
        }
    }

    private fun createGoBackSnackBar(goBackToSubreddit: Subreddit) {
        val view = activity?.findViewById<View>(R.id.nav_host_fragment)
        Snackbar.make(view!!, "", 8000).apply {
            setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.snack_bar_background))
            setActionTextColor(ContextCompat.getColor(requireContext(), R.color.snack_bar_text))
            setAction(requireContext().getString(R.string.go_back_to, goBackToSubreddit.name)) {
                gotoSubreddit(goBackToSubreddit.name)
            }
            show()
        }
    }
}