package de.max.roehrl.vueddit2.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuView
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.core.widget.doOnTextChanged
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.backends.pipeline.Fresco
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.model.Subreddit
import de.max.roehrl.vueddit2.service.Markdown
import de.max.roehrl.vueddit2.service.Reddit
import de.max.roehrl.vueddit2.service.Url
import de.max.roehrl.vueddit2.ui.fragment.PostListFragmentArgs
import de.max.roehrl.vueddit2.ui.fragment.PostListFragmentDirections
import de.max.roehrl.vueddit2.ui.listener.RecyclerOnTouchListener
import de.max.roehrl.vueddit2.ui.viewmodel.AppViewModel

// https://developer.android.com/guide/navigation/navigation-ui
class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }
    lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var navController: NavController
    private val viewModel: AppViewModel by viewModels {
        SavedStateViewModelFactory(application, this)
    }
    private val multiRedditMenuItems = mutableListOf<MenuItem>()
    private val userRedditMenuItems = mutableListOf<MenuItem>()
    private var searchTextLength: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            Fresco.initialize(this)
        }

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        viewModel.isLoggedIn.observe(this) { isLoggedIn ->
            if (isLoggedIn) {
                viewModel.loadSubscriptions()
            } else {
                navController.navigate(PostListFragmentDirections.actionPostListFragmentToLoginActivity())
                finish()
            }
        }
        viewModel.subreddit.observe(this) { subreddit ->
            if (subreddit != null) {
                navView.menu.forEach { item ->
                    if (item.title == subreddit.name && (subreddit.isMultiReddit == multiRedditMenuItems.contains(item))) {
                        item.isChecked = true
                    }
                }
            }
        }
        viewModel.subreddits.observe(this) { subreddits ->
            if (searchTextLength == 0) {
                updateNavItems(subreddits)
            }
        }
        viewModel.searchResults.observe(this) { subreddits ->
            if (subreddits != null && subreddits.isEmpty()) {
                Toast.makeText(this, "No search results", Toast.LENGTH_SHORT).show()
            }
            updateNavItems(if (subreddits != null) {
                listOf(subreddits)
            } else {
                viewModel.subreddits.value
            })
            if (subreddits != null) {
                val searchText = navView.getHeaderView(0).findViewById<TextInputEditText>(R.id.search_text)
                (navView[0] as RecyclerView).post {
                    searchText.requestFocus()
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        navController.setGraph(R.navigation.navigation_graph, PostListFragmentArgs(Reddit.frontpage, true).toBundle())
        navView.setupWithNavController(navController)
        Markdown.getInstance(this).urlOpenCallback = { url : String -> Url.openUrl(this, navController, url) }

        val searchText = navView.getHeaderView(0).findViewById<TextInputEditText>(R.id.search_text)
        searchText.doOnTextChanged { text, _, _, _ ->
            searchTextLength = text?.length ?: 0
            viewModel.updateSearchText(text)
        }
        navView.setNavigationItemSelectedListener { true }
        val recyclerView = navView[0] as RecyclerView
        recyclerView.addOnItemTouchListener(
            RecyclerOnTouchListener(this, recyclerView,
                { view, _ ->
                    onNavItemLongPressed((view as MenuView.ItemView).itemData)
                },
                { view, _, e ->
                    val tv = (view as ViewGroup).getChildAt(0)
                    if (tv is TextView) {
                        val outLocation = IntArray(2)
                        tv.getLocationOnScreen(outLocation)
                        val item = (view as MenuView.ItemView).itemData
                        if (e.rawX <= outLocation[0] + tv.totalPaddingLeft) {
                            onNavItemIconClicked(item)
                        } else {
                            onNavItemClicked(item)
                        }
                    }
                }
            )
        )
        if (savedInstanceState?.getBoolean(AppViewModel.WAS_LOGGED_IN, false) == true) {
            (viewModel.isLoggedIn as MutableLiveData).value = true
        }
        viewModel.selectSubreddit(Subreddit.frontPage.name, false)
    }

    private fun onNavItemClicked(item: MenuItem) {
        if (userRedditMenuItems.contains(item)) {
            Log.d(TAG, "Selecting user: '${item.title}'")
            viewModel.selectUser(item.title.toString())
            Url.openDeepLink("/u/${item.title}", navController)
        } else {
            Log.d(TAG, "Selecting subreddit: '${item.title}'")
            viewModel.selectSubreddit(item.title.toString(), multiRedditMenuItems.contains(item))
        }
        drawerLayout.close()
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    private fun onNavItemIconClicked(item: MenuItem) {
        var sub = viewModel.subreddits.value?.flatten()?.find {
            it.name == item.title && !it.isMultiReddit
        }
        if (sub == null) {
            sub = viewModel.searchResults.value?.find {
                it.name == item.title && !it.isMultiReddit
            }
        }
        if (sub != null) {
            if (sub.isSubscribedTo) {
                if (sub.isStarred) {
                    viewModel.removeSubredditFromStarred(sub.name)
                } else {
                    viewModel.addSubredditsToStarred(sub.name)
                }
            } else {
                AlertDialog.Builder(ContextThemeWrapper(this, R.style.Theme_Vueddit2_Dialog_Alert)).apply {
                    setMessage(context.getString(if (sub.isSubscribedTo) R.string.unsubscribe_confirm else R.string.subscribe_confirm, sub.name))
                    setPositiveButton(android.R.string.ok) { dialog, _ ->
                        viewModel.subscribeToSubreddit(sub)
                        dialog.dismiss()
                        val view = findViewById<View>(R.id.nav_host_fragment)
                        val text = if (sub.isSubscribedTo) R.string.unsubscribed else R.string.subscribed
                        Snackbar.make(view!!, text, 3000).apply {
                            setBackgroundTint(ContextCompat.getColor(context,
                                R.color.snack_bar_background
                            ))
                            setTextColor(ContextCompat.getColor(context, R.color.snack_bar_text))
                            show()
                        }
                    }
                    setNegativeButton(android.R.string.cancel, null)
                }.show()
            }
        }
    }

    private fun onNavItemLongPressed(item: MenuItem) {
        val sub = viewModel.subreddits.value?.flatten()?.find {
            it.name == item.title && !it.isMultiReddit && !Subreddit.defaultSubreddits.contains(it)
        }
        if (sub?.isSubscribedTo == true) {
            MaterialAlertDialogBuilder(this).apply {
                setItems(listOf(getString(R.string.unsubscribe, sub.name)).toTypedArray()) { _, which ->
                    when (which) {
                        0 -> viewModel.subscribeToSubreddit(sub)
                    }
                }
                show()
            }
        } else if (sub?.isVisited == true || sub?.user != null) {
            MaterialAlertDialogBuilder(this).apply {
                val subText = if (sub.user != null) {
                    "/u/${sub.user}"
                } else {
                    "/r/${sub.name}"
                }
                setItems(listOf(getString(R.string.remove_from_visited, subText)).toTypedArray()) { _, which ->
                    when (which) {
                        0 -> {
                            if (sub.user != null) {
                                viewModel.removeUserFromVisited(sub.user!!)
                            } else {
                                viewModel.removeSubredditFromVisited(sub.name)
                            }
                            val view = findViewById<View>(R.id.nav_host_fragment)
                            val text = getString(R.string.removed_from_visited, subText)
                            Snackbar.make(view!!, text, 3000).apply {
                                setBackgroundTint(ContextCompat.getColor(context, R.color.snack_bar_background))
                                setTextColor(ContextCompat.getColor(context, R.color.snack_bar_text))
                                show()
                            }
                        }
                    }
                }
                show()
            }
        }
    }

    private fun updateNavItems(subredditsGroups: List<List<Subreddit>>?) {
        if (subredditsGroups != null) {
            multiRedditMenuItems.clear()
            userRedditMenuItems.clear()
            navView.menu.clear()
            var groupId = Menu.FIRST
            for (group in subredditsGroups) {
                for (sub in group) {
                    val item = navView.menu.add(groupId, 0, 0, sub.name)
                    item.icon = ContextCompat.getDrawable(this, sub.getIconId())
                    item.isCheckable = true
                    if (sub.isMultiReddit) {
                        multiRedditMenuItems.add(item)
                    } else if (sub.user != null) {
                        userRedditMenuItems.add(item)
                    }
                    if (sub == viewModel.subreddit.value) {
                        item.isChecked = true
                    }
                }
                groupId += 1
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.saveBundle()
        super.onSaveInstanceState(outState)
    }
}