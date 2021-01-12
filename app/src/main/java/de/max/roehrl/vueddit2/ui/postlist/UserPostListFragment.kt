package de.max.roehrl.vueddit2.ui.postlist

import android.os.Bundle
import android.view.*
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.service.Store
import kotlinx.coroutines.launch


class UserPostListFragment : PostListFragment() {
    override val sortings = listOf("new", "top", "hot", "controversial")
    override val isGroupTabLayoutVisible = true
    private var groupTabLayout: TabLayout? = null
    override val layoutId = R.layout.fragment_user_posts
    private val safeArgs: UserPostListFragmentArgs by navArgs()

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
            val index = groups.indexOf(viewModel.selectedGroup.value)
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
        viewModel.setSelectedUser(safeArgs.userName)
        toolbar?.title = "/u/${safeArgs.userName}"
        viewModel.userPostsAndComments.observe(viewLifecycleOwner) { posts ->
            val oldSize = postsAdapter.itemCount
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
        }
        viewModel.refreshUserPosts()
    }

    private fun onGroupSelected(group: String) {
        viewModel.setUserPostGroup(group)
        viewModel.refreshUserPosts()
    }

    override fun onSortingSelected(sorting: String) {
        viewModel.setUserPostSorting(sorting)
        viewModel.refreshUserPosts()
    }

    override fun onSwipeToRefresh(cb: () -> Unit) {
        viewModel.refreshUserPosts(false, cb)
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int, layoutManager: LinearLayoutManager) {
        if (!viewModel.isUserPostListLoading() && layoutManager.findLastCompletelyVisibleItemPosition() + 2 == recyclerView.adapter?.itemCount && dy != 0) {
            viewModel.loadMoreUserPosts()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.user_post_list, menu)
    }
}