package de.max.roehrl.vueddit2

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.ui.setupWithNavController
import com.facebook.drawee.backends.pipeline.Fresco
import com.google.android.material.navigation.NavigationView
import de.max.roehrl.vueddit2.service.Reddit
import kotlinx.coroutines.launch

// https://developer.android.com/guide/navigation/navigation-ui
class MainActivity : AppCompatActivity() {
    lateinit var drawerLayout: DrawerLayout
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Fresco.initialize(this)
        lifecycleScope.launch {
            Reddit.init(this@MainActivity)
        }

        drawerLayout = findViewById(R.id.drawer_layout)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        navView.setupWithNavController(navController)
        navView.setNavigationItemSelectedListener { item: MenuItem ->
            Log.d(TAG, item.title.toString())
            true
        }
        navView.menu.add("reddit front page")
        navView.menu.add("all")
        navView.menu.add("random")

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }
}