package de.max.roehrl.vueddit2.ui.postlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import de.max.roehrl.vueddit2.model.AppViewModel
import de.max.roehrl.vueddit2.R


class PostListFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        postponeEnterTransition()
        val viewModel: AppViewModel by activityViewModels()
        val root = inflater.inflate(R.layout.fragment_posts, container, false)
        val myAdapter = PostAdapter()
        val layoutManager = LinearLayoutManager(this.context)
        viewModel.posts.observe(viewLifecycleOwner, {
            myAdapter.setPosts(it)
            myAdapter.notifyDataSetChanged()
            (view?.parent as? ViewGroup)?.doOnPreDraw {
                startPostponedEnterTransition()
            }
        })
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = myAdapter
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!viewModel.isLoading() && layoutManager.findLastCompletelyVisibleItemPosition() + 1 == recyclerView.adapter?.itemCount) {
                    viewModel.loadMorePosts {}
                }
            }
        })
        val swipeRefreshLayout: SwipeRefreshLayout = root.findViewById(R.id.swipe)
        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.post {
                if (swipeRefreshLayout.isRefreshing) {
                    viewModel.refreshPosts {
                        swipeRefreshLayout.isRefreshing = false
                    }
                }
            }
        }
        viewModel.loadMorePosts {}
        return root
    }
}