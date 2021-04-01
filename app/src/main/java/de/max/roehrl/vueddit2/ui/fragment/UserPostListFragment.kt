package de.max.roehrl.vueddit2.ui.fragment

import android.os.Bundle
import android.view.*
import androidx.fragment.app.viewModels
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
import java.util.*

class UserPostListFragment : PostListFragment() {
    override val viewModel: UserPostListViewModel by viewModels()
    override val layoutId = R.layout.fragment_user_posts
    override val isGroupTabLayoutVisible = true
    override var showGotoUser = false
    private val safeArgs: UserPostListFragmentArgs by navArgs()
    private var currentUser: String? = null
    private var currentGroup: String? = null

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
            val index = groups.indexOf(viewModel.userPostGroup.value)
            if (index != -1) {
                groupTabLayout!!.getTabAt(index)!!.select()
            }
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

    override fun initialize(postsAdapter: PostsAdapter) {
        viewModel.setSelectedUser(safeArgs.userName)
        viewModel.selectedUser.observe(viewLifecycleOwner) { userName ->
            if (userName != null && userName != "") {
                toolbar.title = "/u/${userName}"
                if (userName != currentUser) {
                    viewModel.refreshPosts()
                }
                currentUser = userName
            }
        }
        viewModel.userPostGroup.observe(viewLifecycleOwner) { group ->
            if (group != currentGroup || group == "saved") {
                if (listOf("overview", "submitted", "comments").contains(group)) {
                    sortingTabLayout.visibility = View.VISIBLE
                } else {
                    sortingTabLayout.visibility = View.GONE
                    viewModel.setPostSorting("new")
                }
                postsAdapter.showBigPreview = null
                if (group == "saved") {
                    val items = listOf("All", "Comments", "Links")
                    MaterialAlertDialogBuilder(requireContext()).apply {
                        setItems(items.toTypedArray()) { _, which ->
                            val type = items[which].toLowerCase(Locale.getDefault())
                            viewModel.setSavedPostsType(type)
                            viewModel.refreshPosts()
                        }
                        show()
                    }
                } else {
                    viewModel.refreshPosts()
                }
            }
            currentGroup = group
        }
    }

    private fun onGroupSelected(group: String) {
        viewModel.setUserPostGroup(group)
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.user_post_list, menu)
    }

    override fun onItemLongPressed(view: View, position: Int) {
        val item = viewModel.posts.value?.get(position)
        if (item is Post) {
            showGotoUser = currentUser != item.author
            super.onItemLongPressed(view, position)
        } else if (item is Comment) {
            val items = mutableListOf(
                view.context.getString(if (item.saved) R.string.unsave else R.string.save),
                view.context.getString(R.string.goto_sub, item.subreddit),
            )
            if (currentUser != item.author) {
                items.add(view.context.getString(R.string.goto_user, item.author))
            }
            MaterialAlertDialogBuilder(requireContext()).apply {
                setItems(items.toTypedArray()) { _, which ->
                    when (which) {
                        0 -> item.saveOrUnsave(context)
                        1 -> gotoSubreddit(item.subreddit)
                        2 -> gotoUser(item.author)
                    }
                }
                show()
            }
        }
    }
}