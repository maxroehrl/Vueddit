package de.max.roehrl.vueddit2.ui.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.distinctUntilChanged
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
import com.google.android.material.transition.MaterialElevationScale
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
    protected open val viewModel: PostListViewModel by viewModels {
        SavedStateViewModelFactory(requireActivity().application, this)
    }
    protected open val appViewModel: AppViewModel by activityViewModels {
        SavedStateViewModelFactory(requireActivity().application, this)
    }
    protected open lateinit var toolbar: MaterialToolbar
    protected open lateinit var sortingTabLayout: TabLayout
    protected open val isGroupTabLayoutVisible = false
    protected open val layoutId = R.layout.fragment_posts
    protected open val menuId = R.menu.post_list
    protected open var showGotoUser = true
    private val safeArgs: PostListFragmentArgs by navArgs()
    private lateinit var collapsingToolbar: CollapsingToolbarLayout

    override fun onCreate(savedInstanceState: Bundle?)  {
        super.onCreate(savedInstanceState)
        exitTransition = MaterialElevationScale(false)
        reenterTransition = MaterialElevationScale(true)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(layoutId, container, false)
        val postsAdapter = PostsAdapter(viewModel.viewModelScope)
        val linearLayoutManager = LinearLayoutManager(context)

        toolbar = root.findViewById(R.id.toolbar)
        collapsingToolbar = root.findViewById(R.id.collapsing_toolbar)
        sortingTabLayout = root.findViewById(R.id.tab_layout)

        postponeEnterTransition()
        (root as ViewGroup).isTransitionGroup = true

        root.findViewById<RecyclerView?>(R.id.recycler_view).apply {
            layoutManager = linearLayoutManager
            adapter = postsAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (!viewModel.isPostListLoading()
                        && linearLayoutManager.findLastVisibleItemPosition() + 2 == recyclerView.adapter?.itemCount
                        && dy != 0
                    ) {
                        viewModel.loadMorePosts()
                    }
                }
            })
            viewTreeObserver.addOnPreDrawListener {
                startPostponedEnterTransition()
                true
            }
            addOnItemTouchListener(
                RecyclerOnTouchListener(
                    requireContext(),
                    this,
                    { view, position ->
                        onItemLongPressed(view, position)
                    },
                    null
                )
            )
        }
        root.findViewById<SwipeRefreshLayout?>(R.id.swipe).apply {
            setOnRefreshListener {
                post {
                    if (isRefreshing) {
                        viewModel.refreshPosts(false) {
                            isRefreshing = false
                        }
                    }
                }
            }
        }

        // Specific init for post or user lists
        initialize(postsAdapter)

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
                postsAdapter.showBigPreview = if (viewModel.subreddit.value == Subreddit.frontPage)
                    false
                else
                    appViewModel.shouldShowBigTemplate(posts)
            }

            if (newSize > oldSize) {
                if (oldSize > 0)
                    postsAdapter.notifyItemChanged(oldSize - 1)
                postsAdapter.notifyItemRangeInserted(oldSize, newSize - oldSize)
            } else {
                postsAdapter.notifyDataSetChanged()
            }
        }

        viewModel.sortingList.observe(viewLifecycleOwner) { sortingList ->
            sortingTabLayout.removeAllTabs()
            for (sorting in sortingList) {
                sortingTabLayout.addTab(sortingTabLayout.newTab().setText(sorting))
            }
            val index = sortingList.indexOf(viewModel.postSorting)
            if (index != -1) {
                sortingTabLayout.selectTab(sortingTabLayout.getTabAt(index), true)
            }
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
        appViewModel.isLoggedIn.observe(viewLifecycleOwner) { isLoggedIn ->
            if (isLoggedIn)
                viewModel.loadMorePosts()
        }
        viewModel.subreddit.distinctUntilChanged().observe(viewLifecycleOwner) { subreddit ->
            toolbar.title = subreddit?.name ?: Reddit.frontpage

            if (subreddit != null && appViewModel.isLoggedIn.value == true) {
                postsAdapter.highlightStickied = subreddit != Subreddit.frontPage
                postsAdapter.showBigPreview = null

                if (safeArgs.rootFragment && appViewModel.subreddit.value != subreddit) {
                    appViewModel.selectSubreddit(subreddit.name, subreddit.isMultiReddit)
                }
            }
        }
        if (safeArgs.rootFragment) {
            appViewModel.subreddit.distinctUntilChanged().observe(viewLifecycleOwner) { subreddit ->
                if (viewModel.subreddit.value != subreddit) {
                    viewModel.selectSubreddit(subreddit)
                }
            }
        } else {
            viewModel.selectSubreddit(Subreddit.fromName(safeArgs.subredditName))
        }
    }

    open fun onSortingSelected(sorting: String, adapter: PostsAdapter) {
        viewModel.postSorting = sorting

        if (listOf("top", "rising").contains(sorting)) {
            val items = listOf("Hour", "Day", "Week", "Month", "Year", "All")
            MaterialAlertDialogBuilder(requireContext()).apply {
                setItems(items.toTypedArray()) { _, which ->
                    val time = items[which].lowercase()
                    viewModel.topPostsTime = time
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
                        0 -> post.saveOrUnsave(context, viewModel.viewModelScope)
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
        viewModel.selectSubreddit(Subreddit.fromName(subredditName))
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
        toolbar.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(menuId, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
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
                    else -> false
                }
            }

        })
    }

    private fun gotoLoggedInUserPosts() {
        viewModel.viewModelScope.launch {
            val userName = Store.getInstance(requireContext()).getUsername()
            val action = PostListFragmentDirections.actionPostListFragmentToUserPostListFragment(userName!!)
            findNavController().navigate(action)
        }
    }

    private fun showSidebar() {
        val currentSubreddit = viewModel.subreddit.value
        if (currentSubreddit != null
            && currentSubreddit != Subreddit.frontPage
            && !currentSubreddit.isMultiReddit
        ) {
            Sidebar.show(requireContext(), currentSubreddit.name, viewModel.viewModelScope)
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

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.saveBundle()
        super.onSaveInstanceState(outState)
    }
}