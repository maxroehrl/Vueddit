package de.max.roehrl.vueddit2.ui.postlist

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.service.Store
import kotlinx.coroutines.launch
import java.util.*


class UserPostListFragment : PostListFragment() {
    override val sortings = listOf("new", "top", "hot", "controversial")
    override val isGroupTabLayoutVisible = true
    private var groupTabLayout: TabLayout? = null
    override val layoutId = R.layout.fragment_user_posts
    private val safeArgs: UserPostListFragmentArgs by navArgs()
    private var currentUser: String? = null
    private var currentGroup: String? = null
    override val showGotoUser = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState)
        groupTabLayout = root?.findViewById(R.id.group_tab_layout)
            viewModel.viewModelScope.launch {
            var groups = listOf("overview", "submitted", "comments", "gilded")
            if (Store.getInstance(requireContext()).getUsername() == safeArgs.userName) {
                groups = listOf("overview", "submitted", "comments", "saved", "upvoted", "downvoted", "hidden", "gilded")
            }
            for (group in groups) {
                groupTabLayout!!.addTab(groupTabLayout!!.newTab().setText(group))
            }
            val index = groups.indexOf(viewModel.userPostGroup.value)
            if (index != -1) {
                groupTabLayout!!.getTabAt(index)!!.select()
            }
            groupTabLayout!!.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    onGroupSelected(tab?.text.toString())
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
        }
        return root
    }

    override fun initialize(postsAdapter: PostsAdapter) {
        sortingLiveData = viewModel.userSorting
        postsLiveData = viewModel.userPostsAndComments
        viewModel.userPostsAndComments.observe(viewLifecycleOwner) { posts ->
            val oldSize = postsAdapter.itemCount
            val newSize = posts.size
            postsAdapter.posts = posts
            if (newSize > oldSize) {
                if (oldSize > 0)
                    postsAdapter.notifyItemChanged(oldSize - 1)
                postsAdapter.notifyItemRangeInserted(oldSize, newSize - oldSize)
            } else {
                postsAdapter.notifyDataSetChanged()
            }
        }
        viewModel.selectedUser.observe(viewLifecycleOwner) { userName ->
            if (userName != "") {
                toolbar?.title = "/u/${userName}"
                if (userName != currentUser) {
                    viewModel.refreshUserPosts()
                }
                currentUser = userName
            }
        }
        if (viewModel.selectedUser.value != safeArgs.userName) {
            viewModel.setUserPostGroup("overview")
        }
        viewModel.setSelectedUser(safeArgs.userName)
        viewModel.userPostGroup.observe(viewLifecycleOwner) { group ->
            if (group != currentGroup) {
                if (listOf("overview", "submitted", "comments").contains(group)) {
                    sortingTabLayout?.visibility = View.VISIBLE
                } else {
                    sortingTabLayout?.visibility = View.GONE
                    viewModel.setUserPostSorting("new")
                }
                if (group == "saved") {
                    val items = listOf("All", "Comments", "Links")
                    AlertDialog.Builder(requireContext()).apply {
                        setItems(items.toTypedArray()) { _, which ->
                            val type = items[which].toLowerCase(Locale.getDefault())
                            viewModel.setSavedPostsType(type)
                            viewModel.refreshUserPosts()
                        }
                    }.show()
                } else {
                    viewModel.refreshUserPosts()
                }
            }
            currentGroup = group
        }
    }

    private fun onGroupSelected(group: String) {
        viewModel.setUserPostGroup(group)
    }

    override fun onSortingSelected(sorting: String) {
        viewModel.setUserPostSorting(sorting)
        if (listOf("top", "rising").contains(sorting)) {
            val items = listOf("Hour", "Day", "Week", "Month", "Year", "All")
            AlertDialog.Builder(requireContext()).apply {
                setItems(items.toTypedArray()) { _, which ->
                    val time = items[which].toLowerCase(Locale.getDefault())
                    viewModel.setTopPostsTime(time)
                    viewModel.refreshUserPosts()
                }
            }.show()
        } else {
            viewModel.refreshUserPosts()
        }
    }

    override fun onSwipeToRefresh(cb: () -> Unit) {
        viewModel.refreshUserPosts(false, cb)
    }

    override fun onRecyclerViewScrolled(recyclerView: RecyclerView, dx: Int, dy: Int, layoutManager: LinearLayoutManager) {
        if (!viewModel.isUserPostListLoading() && layoutManager.findLastVisibleItemPosition() + 2 == recyclerView.adapter?.itemCount && dy != 0) {
            viewModel.loadMoreUserPosts()
        }
    }

    override fun gotoUser(userName: String) {
        viewModel.setSelectedUser(userName)
        viewModel.refreshUserPosts()
    }

    override fun gotoSubreddit(subredditName: String) {
        findNavController().navigate(UserPostListFragmentDirections.actionUserPostListFragmentToPostListFragment(subredditName))
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.user_post_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_refresh -> {
                viewModel.refreshUserPosts()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}