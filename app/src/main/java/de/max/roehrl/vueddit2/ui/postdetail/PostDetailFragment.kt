package de.max.roehrl.vueddit2.ui.postdetail

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.transition.TransitionInflater
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import de.max.roehrl.vueddit2.MainActivity
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.model.AppViewModel
import de.max.roehrl.vueddit2.service.Reddit
import de.max.roehrl.vueddit2.service.Util.setActionBarUpIndicator


class PostDetailFragment : Fragment() {
    private val TAG = "PostDetailFragment"
    private val viewModel: AppViewModel by activityViewModels()
    private var toolbar: MaterialToolbar? = null
    private var collapsingToolbar: CollapsingToolbarLayout? = null
    private var init = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        sharedElementEnterTransition = TransitionInflater.from(requireContext()).inflateTransition(R.transition.shared_header)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_post_detail, container, false)
        toolbar = root.findViewById(R.id.toolbar)
        collapsingToolbar = root.findViewById(R.id.collapsing_toolbar)
        val recyclerView = root.findViewById<RecyclerView>(R.id.comments)
        recyclerView.layoutManager = LinearLayoutManager(context)
        val commentsAdapter = CommentsAdapter(viewModel.selectedPost.value!!)
        recyclerView.adapter = commentsAdapter
        viewModel.resetComments()
        viewModel.comments.observe(viewLifecycleOwner, { comments ->
            commentsAdapter.comments = comments
            commentsAdapter.notifyDataSetChanged()
        })
        viewModel.selectedPost.observe(viewLifecycleOwner) { post ->
            toolbar?.title = post?.title ?: Reddit.frontpage
        }
        val swipeRefreshLayout : SwipeRefreshLayout = root.findViewById(R.id.swipe)
        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.post {
                if (swipeRefreshLayout.isRefreshing) {
                    viewModel.loadComments {
                        swipeRefreshLayout.isRefreshing = false
                    }
                }
            }
        }
        viewModel.loadComments {}
        return root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // postponeEnterTransition()
        val mainActivity = activity as MainActivity
        val drawerLayout = mainActivity.drawerLayout
        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)
        //  collapsingToolbar.setupWithNavController(toolbar, navController, appBarConfiguration)
        if (!init) {
            navController.addOnDestinationChangedListener { _, destination, _ ->
                if (destination.id == R.id.postDetailFragment) {
                    mainActivity.setSupportActionBar(toolbar)
                    toolbar?.setActionBarUpIndicator(false)
                    toolbar?.setNavigationOnClickListener { NavigationUI.navigateUp(navController, appBarConfiguration) }
                }
            }
            init = true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.post_detail, menu)
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
            else -> super.onOptionsItemSelected(item)
        }
    }
}