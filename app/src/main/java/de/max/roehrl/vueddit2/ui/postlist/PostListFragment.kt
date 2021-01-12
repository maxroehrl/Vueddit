package de.max.roehrl.vueddit2.ui.postlist

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.view.SupportMenuInflater
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
import com.google.android.material.tabs.TabLayout
import de.max.roehrl.vueddit2.MainActivity
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.model.AppViewModel
import de.max.roehrl.vueddit2.model.NamedItem
import de.max.roehrl.vueddit2.model.Post
import de.max.roehrl.vueddit2.model.Subreddit
import de.max.roehrl.vueddit2.service.Reddit
import de.max.roehrl.vueddit2.service.Store
import kotlinx.coroutines.launch
import kotlin.math.min


open class PostListFragment : Fragment() {
    private val TAG = "PostListFragment"
    protected val viewModel: AppViewModel by activityViewModels()
    private var currentSubreddit: Subreddit? = null
    protected var toolbar: MaterialToolbar? = null
    private var collapsingToolbar: CollapsingToolbarLayout? = null
    private var sortingTabLayout: TabLayout? = null
    protected open val isGroupTabLayoutVisible = false
    protected open val sortings = listOf("best", "hot", "top", "new", "controversial", "rising")
    private val safeArgs: PostListFragmentArgs by navArgs()
    protected open val layoutId = R.layout.fragment_posts
    protected open lateinit var sortingLiveData: LiveData<String>

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
                onScrolled(recyclerView, dx, dy, layoutManager)
            }
        })
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
        viewModel.selectSubreddit(safeArgs.subredditName, false)
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
    }

    open fun onSortingSelected(sorting: String) {
        viewModel.setPostSorting(sorting)
        viewModel.refreshPosts()
    }

    open fun onSwipeToRefresh(cb: () -> Unit) {
        viewModel.refreshPosts(false, cb)
    }

    open fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int, layoutManager: LinearLayoutManager) {
        if (!viewModel.isPostListLoading() && layoutManager.findLastCompletelyVisibleItemPosition() + 2 == recyclerView.adapter?.itemCount && dy != 0) {
            viewModel.loadMorePosts()
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
                viewModel.viewModelScope.launch {
                    val userName = Store.getInstance(requireContext()).getUsername()
                    val action = PostListFragmentDirections.actionPostListFragmentToUserPostListFragment(userName!!)
                    findNavController().navigate(action)
                }
                true
            }
            R.id.action_toggle_big_preview -> {
                Log.d(TAG, "Toggle preview size item selected")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    protected fun shouldShowBigTemplate(postList: List<NamedItem>): Boolean {
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