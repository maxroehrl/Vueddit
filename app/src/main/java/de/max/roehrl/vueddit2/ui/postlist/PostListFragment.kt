package de.max.roehrl.vueddit2.ui.postlist

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import androidx.appcompat.view.SupportMenuInflater
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
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
import de.max.roehrl.vueddit2.MainActivity
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.model.AppViewModel
import de.max.roehrl.vueddit2.model.NamedItem
import de.max.roehrl.vueddit2.model.Post
import de.max.roehrl.vueddit2.model.Subreddit
import de.max.roehrl.vueddit2.service.Reddit
import de.max.roehrl.vueddit2.service.Store
import de.max.roehrl.vueddit2.ui.dialog.Sidebar
import de.max.roehrl.vueddit2.ui.listener.RecyclerOnTouchListener
import kotlinx.coroutines.launch
import java.util.*


open class PostListFragment : Fragment() {
    private val TAG = "PostListFragment"
    protected val viewModel: AppViewModel by activityViewModels()
    private var currentSubreddit: Subreddit? = null
    protected var toolbar: MaterialToolbar? = null
    private var collapsingToolbar: CollapsingToolbarLayout? = null
    protected var sortingTabLayout: TabLayout? = null
    protected open val isGroupTabLayoutVisible = false
    protected open val sortings = listOf("best", "hot", "top", "new", "controversial", "rising")
    private val safeArgs: PostListFragmentArgs by navArgs()
    protected open val layoutId = R.layout.fragment_posts
    protected open lateinit var sortingLiveData: LiveData<String>
    protected open lateinit var postsLiveData: LiveData<List<NamedItem>>
    protected open val showGotoUser = true

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // postponeEnterTransition()
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
                onRecyclerViewScrolled(recyclerView, dx, dy, layoutManager)
            }
        })
        recyclerView.addOnItemTouchListener(RecyclerOnTouchListener(requireContext(), recyclerView, { view, position ->
            onItemLongPressed(view, position)
        }, null))
        val swipeRefreshLayout: SwipeRefreshLayout = root.findViewById(R.id.swipe)
        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.post {
                if (swipeRefreshLayout.isRefreshing) {
                    onSwipeToRefresh {
                        swipeRefreshLayout.isRefreshing = false
                    }
                }
            }
        }
        viewModel.isBigTemplatePreferred.observe(viewLifecycleOwner) { isBigTemplatePreferred ->
            if (postsLiveData.value != null) {
                postsAdapter.showBigPreview = isBigTemplatePreferred
                        ?: viewModel.shouldShowBigTemplate(postsLiveData.value!!, currentSubreddit)
                postsAdapter.notifyItemRangeChanged(0, postsAdapter.itemCount)
            }
        }
        initialize(postsAdapter)

        for (sorting in sortings) {
            sortingTabLayout!!.addTab(sortingTabLayout!!.newTab().setText(sorting))
        }
        val index = sortings.indexOf(sortingLiveData.value)
        if (index != -1) {
            sortingTabLayout!!.getTabAt(index)!!.select()
        }
        sortingTabLayout!!.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                onSortingSelected(tab?.text.toString())
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        return root
    }

    open fun initialize(postsAdapter: PostsAdapter) {
        sortingLiveData = viewModel.postSorting
        postsLiveData = viewModel.posts
        viewModel.posts.observe(viewLifecycleOwner, { posts ->
            val oldSize = postsAdapter.posts.size
            val newSize = posts.size
            postsAdapter.posts = posts
            if (postsAdapter.showBigPreview == null && posts.any { item -> item != NamedItem.Loading }) {
                postsAdapter.showBigPreview = viewModel.shouldShowBigTemplate(posts, currentSubreddit)
            }
            if (newSize > oldSize) {
                if (oldSize > 0)
                    postsAdapter.notifyItemChanged(oldSize - 1)
                postsAdapter.notifyItemRangeInserted(oldSize, newSize - oldSize)
            } else {
                postsAdapter.notifyDataSetChanged()
            }
            // (view?.parent as? ViewGroup)?.doOnPreDraw { startPostponedEnterTransition() }
        })
        viewModel.isLoggedIn.observe(viewLifecycleOwner) { isLoggedIn ->
            if (isLoggedIn)
                viewModel.loadMorePosts()
        }
        viewModel.subreddit.observe(viewLifecycleOwner) { subreddit ->
            toolbar?.title = subreddit?.name ?: Reddit.frontpage
            if (subreddit != null && subreddit != currentSubreddit && viewModel.isLoggedIn.value == true) {
                postsAdapter.highlightStickied = subreddit != Subreddit.frontPage
                postsAdapter.showBigPreview = null
                viewModel.refreshPosts(true)
            }
            currentSubreddit = subreddit ?: currentSubreddit
        }
        if (viewModel.subreddit.value == null) {
            viewModel.selectSubreddit(safeArgs.subredditName, false)
        }
    }

    open fun onSortingSelected(sorting: String) {
        viewModel.setPostSorting(sorting)
        if (listOf("top", "rising").contains(sorting)) {
            val items = listOf("Hour", "Day", "Week", "Month", "Year", "All")
            MaterialAlertDialogBuilder(requireContext()).apply {
                setItems(items.toTypedArray()) { _, which ->
                    val time = items[which].toLowerCase(Locale.getDefault())
                    viewModel.setTopPostsTime(time)
                    viewModel.refreshPosts()
                }
            }.show()
        } else {
            viewModel.refreshPosts()
        }
    }

    open fun onSwipeToRefresh(cb: () -> Unit) {
        viewModel.refreshPosts(false, cb)
    }

    open fun onRecyclerViewScrolled(recyclerView: RecyclerView, dx: Int, dy: Int, layoutManager: LinearLayoutManager) {
        if (!viewModel.isPostListLoading() && layoutManager.findLastVisibleItemPosition() + 2 == recyclerView.adapter?.itemCount && dy != 0) {
            viewModel.loadMorePosts()
        }
    }

    open fun onItemLongPressed(view: View, position: Int) {
        val post = postsLiveData.value?.get(position)
        if (post is Post) {
            val items = mutableListOf<String>()
            items.add(view.context.getString(if (post.saved) R.string.unsave else R.string.save))
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
                        0 -> {
                            viewModel.saveOrUnsave(post)
                        }
                        1 -> {
                            if (showGotoSubreddit) {
                                gotoSubreddit(post.subreddit)
                            } else {
                                gotoUser(post.author)
                            }
                        }
                        2 -> {
                            gotoUser(post.author)
                        }
                    }
                }
                show()
            }
        }
    }

    open fun gotoUser(userName: String) {
        findNavController().navigate(PostListFragmentDirections.actionPostListFragmentToUserPostListFragment(userName))
    }

    open fun gotoSubreddit(subredditName: String) {
        val oldSubredditName = viewModel.subreddit.value ?: Subreddit.frontPage
        viewModel.selectSubreddit(subredditName, false)
        createGoBackSnackBar(oldSubredditName)
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

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mainActivity = activity as MainActivity
        val drawerLayout = mainActivity.drawerLayout
        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)
        collapsingToolbar!!.setupWithNavController(toolbar!!, navController, appBarConfiguration)
        onCreateOptionsMenu(toolbar!!.menu, SupportMenuInflater(context))
        toolbar!!.setOnMenuItemClickListener { onOptionsItemSelected(it) }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.post_list, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                viewModel.refreshPosts()
                true
            }
            R.id.action_sidebar -> {
                if (currentSubreddit != null && currentSubreddit != Subreddit.frontPage && !currentSubreddit!!.isMultiReddit) {
                    Sidebar(requireContext(), currentSubreddit!!.name, viewModel.viewModelScope).show()
                } else {
                    val view = activity?.findViewById<View>(R.id.nav_host_fragment)
                    val text = requireContext().getString(R.string.no_sidebar)
                    Snackbar.make(view!!, text, 3000).apply {
                        setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.snack_bar_background))
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.snack_bar_text))
                        show()
                    }
                }
                true
            }
            R.id.action_logout -> {
                viewModel.logoutUser()
                true
            }
            R.id.action_user_posts -> {
                viewModel.viewModelScope.launch {
                    val userName = Store.getInstance(requireContext()).getUsername()
                    val action = PostListFragmentDirections.actionPostListFragmentToUserPostListFragment(userName!!)
                    findNavController().navigate(action)
                }
                true
            }
            R.id.action_toggle_big_preview -> {
                viewModel.toggleBigPreview(postsLiveData.value!!)
                true
            }
            R.id.action_remove_visited -> {
                viewModel.removeSubredditFromVisited(currentSubreddit!!.name)
                val view = activity?.findViewById<View>(R.id.nav_host_fragment)
                val text = requireContext().getString(R.string.removed_from_visited, currentSubreddit!!.name)
                Snackbar.make(view!!, text, 3000).apply {
                    setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.snack_bar_background))
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.snack_bar_text))
                    show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}