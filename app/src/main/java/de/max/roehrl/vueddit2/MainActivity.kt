package de.max.roehrl.vueddit2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.Navigation
import androidx.navigation.ui.setupWithNavController
import com.facebook.drawee.backends.pipeline.Fresco
import com.google.android.material.navigation.NavigationView
import de.max.roehrl.vueddit2.model.AppViewModel
import de.max.roehrl.vueddit2.ui.login.LoginActivity

// https://developer.android.com/guide/navigation/navigation-ui
class MainActivity : AppCompatActivity() {
    lateinit var drawerLayout: DrawerLayout
    lateinit var navView: NavigationView
    private val viewModel: AppViewModel by viewModels()
    private val TAG = "MainActivity"

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
                Log.d(TAG, "Start login activity")
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }
        viewModel.subreddits.observe(this) { subreddits ->
            navView.menu.clear()
            for (sub in subreddits) {
                navView.menu.add(sub.name)
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        val navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        navView.setupWithNavController(navController)

        val searchText = navView.getHeaderView(0).findViewById<EditText>(R.id.search_text)
        searchText.doOnTextChanged { text, _, _, _ ->
            viewModel.updateSearchText(text.toString())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }
}