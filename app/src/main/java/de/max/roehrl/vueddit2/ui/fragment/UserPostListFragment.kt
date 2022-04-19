package de.max.roehrl.vueddit2.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.model.Comment
import de.max.roehrl.vueddit2.model.Post
import de.max.roehrl.vueddit2.service.Store
import de.max.roehrl.vueddit2.ui.adapter.PostsAdapter
import de.max.roehrl.vueddit2.ui.viewmodel.UserPostListViewModel
import kotlinx.coroutines.launch

class UserPostListFragment : PostListFragment() {
    override val viewModel: UserPostListViewModel by viewModels {
        SavedStateViewModelFactory(requireActivity().application, this)
    }
    override val layoutId = R.layout.fragment_user_posts
    override val isGroupTabLayoutVisible = true
    override val menuId = R.menu.user_post_list
    override var showGotoUser = false
    private val safeArgs: UserPostListFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState)
        val groupTabLayout: TabLayout? = root?.findViewById(R.id.group_tab_layout)
        viewModel.viewModelScope.launch {
            var groups = listOf("overview", "submitted", "comments", "gilded")
            if (Store.getInstance(requireContext()).getUsername() == safeArgs.userName) {
                groups = listOf("overview", "submitted", "comments", "saved", "upvoted", "downvoted", "hidden", "gilded")
            }
            for (group in groups) {
                groupTabLayout!!.addTab(groupTabLayout.newTab().setText(group))
            }
            val index = groups.indexOf(viewModel.userPostGroup)
            if (index != -1) {
                groupTabLayout!!.getTabAt(index)!!.select()
            }
            refreshSortingBar(viewModel.userPostGroup)
            groupTabLayout!!.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    onGroupSelected(tab?.text.toString())
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {}

                override fun onTabReselected(tab: TabLayout.Tab?) {
                    onTabSelected(tab)
                }
            })
        }
        return root
    }

    private fun onGroupSelected(group: String) {
        if (group != viewModel.userPostGroup || group == "saved") {
            refreshSortingBar(group)

            if (group == "saved") {
                val items = listOf("All", "Comments", "Links")
                MaterialAlertDialogBuilder(requireContext()).apply {
                    setItems(items.toTypedArray()) { _, which ->
                        viewModel.selectedType = items[which].lowercase()
                        viewModel.refreshPosts()
                    }
                    show()
                }
            } else {
                viewModel.refreshPosts()
            }
        }
        viewModel.userPostGroup = group
    }

    private fun refreshSortingBar(group: String) {
        if (listOf("overview", "submitted", "comments").contains(group)) {
            sortingTabLayout.visibility = View.VISIBLE
        } else {
            sortingTabLayout.visibility = View.GONE
            viewModel.postSorting = "new"
        }
    }

    override fun initialize(postsAdapter: PostsAdapter) {
        viewModel.setSelectedUser(safeArgs.userName)
        appViewModel.selectUser(safeArgs.userName)
        viewModel.selectedUser.observe(viewLifecycleOwner) { userName ->
            if (!userName.isNullOrEmpty()) {
                toolbar.title = "/u/${userName}"
            }
        }
    }

    override fun gotoUser(userName: String) {
        findNavController().navigate(
            UserPostListFragmentDirections.actionUserPostListFragmentToUserPostListFragment(
                userName
            )
        )
    }

    override fun gotoSubreddit(subredditName: String) {
        findNavController().navigate(
            UserPostListFragmentDirections.actionUserPostListFragmentToPostListFragment(
                subredditName
            )
        )
    }

    override fun onItemLongPressed(view: View, position: Int) {
        val item = viewModel.posts.value?.get(position)
        if (item is Post) {
            showGotoUser = viewModel.selectedUser.value != item.author
            super.onItemLongPressed(view, position)
        } else if (item is Comment) {
            val items = mutableListOf(
                view.context.getString(if (item.saved) R.string.unsave else R.string.save),
                view.context.getString(R.string.goto_sub, item.subreddit),
            )
            if (viewModel.selectedUser.value != item.author) {
                items.add(view.context.getString(R.string.goto_user, item.author))
            }
            MaterialAlertDialogBuilder(requireContext()).apply {
                setItems(items.toTypedArray()) { _, which ->
                    when (which) {
                        0 -> item.saveOrUnsave(context, viewModel.viewModelScope)
                        1 -> gotoSubreddit(item.subreddit)
                        2 -> gotoUser(item.author)
                    }
                }
                show()
            }
        }
    }
}