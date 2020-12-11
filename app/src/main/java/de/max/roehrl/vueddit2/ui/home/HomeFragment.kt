package de.max.roehrl.vueddit2.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import coil.load
import coil.request.Disposable
import coil.size.ViewSizeResolver
import de.max.roehrl.vueddit2.PostDetailFragment
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.service.Reddit
import org.json.JSONObject


class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutManager: RecyclerView.LayoutManager
    private lateinit var myAdapter: RecyclerView.Adapter<*>
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private var isLoading: Boolean = true

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        postponeEnterTransition()
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_posts, container, false)
        layoutManager = LinearLayoutManager(this.context)
        homeViewModel.posts.observe(this.viewLifecycleOwner, {
            (myAdapter as PostAdapter).setPosts(it)
            myAdapter.notifyDataSetChanged()
            (view?.parent as? ViewGroup)?.doOnPreDraw {
                startPostponedEnterTransition()
            }
        })
        myAdapter = PostAdapter()
        recyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = myAdapter
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!isLoading && (layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition() + 1 == recyclerView.adapter?.itemCount) {
                    recyclerView.post {
                        (myAdapter as PostAdapter).showLoading(true)
                    }
                    isLoading = true
                }
            }
        })
        swipeRefreshLayout = root.findViewById(R.id.swipe)
        swipeRefreshLayout.setOnRefreshListener {
            isLoading = true
            swipeRefreshLayout.post {
                if (swipeRefreshLayout.isRefreshing) {
                    swipeRefreshLayout.isRefreshing = false
                    isLoading = false
                }
            }
        }
        return root
    }
}

class PostAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var loading: JSONObject = JSONObject("{loading: true}")
    private var posts : MutableList<JSONObject> = mutableListOf(loading)

    companion object {
        private const val VIEW_TYPE_DATA = 0
        private const val VIEW_TYPE_PROGRESS = 1
    }

    fun setPosts(posts: MutableList<JSONObject>) {
        this.posts = posts
    }

    fun addPosts(posts: MutableList<JSONObject>) {
        this.posts.addAll(posts)
    }

    inner class ProgressViewHolder(view: View) : RecyclerView.ViewHolder(view)

    inner class PostViewHolder(view: View): RecyclerView.ViewHolder(view) {
        private val postHeader: RelativeLayout = view.findViewById(R.id.post_header)
        private val title: TextView = view.findViewById(R.id.title)
        private val imageView: ImageView = view.findViewById(R.id.preview)
        private var disposable: Disposable? = null
        private lateinit var post: JSONObject

        fun bind(post: JSONObject) {
            this.post = post
            val transitionName = post.getString("name")
            ViewCompat.setTransitionName(postHeader, transitionName)
            title.text = post.getString("title")
            title.setOnClickListener {
                val detailFragment = PostDetailFragment(post)
                (it.context as AppCompatActivity).supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    // setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    addSharedElement(postHeader, "header")
                    replace(R.id.nav_host_fragment, detailFragment)
                    addToBackStack(null)
                }
            }
            disposable?.dispose()
            val preview = Reddit.getPreview(post)
            if (preview != null) {
                disposable = imageView.load(preview.getString("url")) {
                    placeholder(R.drawable.ic_comment_text_multiple_outline)
                    size(ViewSizeResolver(imageView))
                    error(R.drawable.ic_comment_text_multiple_outline)
                }
            } else {
                imageView.load(R.drawable.ic_comment_text_multiple_outline)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            VIEW_TYPE_PROGRESS -> {
                ProgressViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.loading_item, parent, false))
            }
            VIEW_TYPE_DATA -> {
                PostViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.post_item, parent, false))
            }
            else -> throw IllegalArgumentException("viewType not found")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when(posts[position]) {
            loading -> VIEW_TYPE_PROGRESS
            else -> VIEW_TYPE_DATA
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PostViewHolder) {
            holder.bind(posts[position])
        }
    }

    override fun getItemCount(): Int {
        return posts.size
    }

    fun showLoading(show: Boolean) {
        if (show) {
            posts.add(loading)
            notifyItemInserted(itemCount - 1)
        } else {
            posts.remove(loading)
            notifyItemRemoved(itemCount)
        }
    }
}