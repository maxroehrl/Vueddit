package de.max.roehrl.vueddit2.ui.postdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.transition.TransitionInflater
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import de.max.roehrl.vueddit2.MainActivity
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.model.AppViewModel


class PostDetailFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(requireContext())
                .inflateTransition(R.transition.shared_header)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val viewModel: AppViewModel by activityViewModels()
        val root = inflater.inflate(R.layout.fragment_post_detail, container, false)
        val recyclerView = root.findViewById<RecyclerView>(R.id.comments)
        recyclerView.layoutManager = LinearLayoutManager(context)
        val commentsAdapter = CommentsAdapter(viewModel.selectedPost.value!!)
        recyclerView.adapter = commentsAdapter
        viewModel.comments.observe(viewLifecycleOwner, {
            commentsAdapter.setComments(it)
            commentsAdapter.notifyDataSetChanged()
        })
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
        val toolbar: MaterialToolbar = view.findViewById(R.id.toolbar)
        val collapsingToolbar: CollapsingToolbarLayout = view.findViewById(R.id.collapsing_toolbar)
        val drawerLayout = (activity as MainActivity).drawerLayout
        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)
        collapsingToolbar.setupWithNavController(toolbar, navController, appBarConfiguration)
    }
}