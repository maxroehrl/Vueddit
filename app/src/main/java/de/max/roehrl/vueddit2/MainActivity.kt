package de.max.roehrl.vueddit2

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.doOnTextChanged
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.setupWithNavController
import com.facebook.drawee.backends.pipeline.Fresco
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import de.max.roehrl.vueddit2.model.AppViewModel
import de.max.roehrl.vueddit2.model.Subreddit
import de.max.roehrl.vueddit2.ui.postlist.PostListFragmentDirections


// https://developer.android.com/guide/navigation/navigation-ui
class MainActivity : AppCompatActivity() {
    lateinit var drawerLayout: DrawerLayout
    lateinit var navView: NavigationView
    lateinit var navController: NavController
    private val viewModel: AppViewModel by viewModels()
    private val TAG = "MainActivity"
    private val multiReddits = mutableListOf<MenuItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Fresco.initialize(this)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        viewModel.isLoggedIn.observe(this) { isLoggedIn ->
            if (isLoggedIn) {
                viewModel.loadSubscriptions()
            } else {
                navController.navigate(PostListFragmentDirections.actionPostListFragmentToLoginActivity())
            }
        }
        viewModel.subreddits.observe(this) { subreddits ->
            setNavItems(subreddits)
        }
        viewModel.searchResults.observe(this) { subreddits ->
            if (subreddits != null && subreddits.isEmpty()) {
                Toast.makeText(this, "No search results", Toast.LENGTH_SHORT).show()
            }
            setNavItems(subreddits ?: viewModel.subreddits.value ?: listOf())
        }
    }

    private fun setNavItems(subreddits: List<Subreddit>) {
        multiReddits.clear()
        navView.menu.clear()
        for (sub in subreddits) {
            val item = navView.menu.add(sub.name)
            item.icon = ResourcesCompat.getDrawable(this@MainActivity.resources, sub.getIconId(), null)
            item.isCheckable = true
            if (sub.isMultiReddit) {
                multiReddits.add(item)
            }
            if (sub.name == viewModel.subreddit.value?.name) {
                navView.setCheckedItem(item.itemId)
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        navView.setupWithNavController(navController)

        val searchText = navView.getHeaderView(0).findViewById<TextInputEditText>(R.id.search_text)
        searchText.doOnTextChanged { text, _, _, _ ->
            viewModel.updateSearchText(text.toString())
        }

        navView.setNavigationItemSelectedListener { item: MenuItem ->
            Log.d(TAG, "Selecting subreddit: '${item.title}'")
            val isMultiReddit = multiReddits.contains(item)
            viewModel.selectSubreddit(item.title.toString(), isMultiReddit)
            drawerLayout.close()
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(currentFocus?.windowToken, 0)
            true
        }
    }
}