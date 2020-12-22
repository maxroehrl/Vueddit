package de.max.roehrl.vueddit2.ui.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.facebook.drawee.drawable.ProgressBarDrawable
import com.facebook.drawee.view.SimpleDraweeView
import de.max.roehrl.vueddit2.Post
import de.max.roehrl.vueddit2.PostDetailFragment
import de.max.roehrl.vueddit2.R
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
    private var loading: Post = Post(JSONObject("{loading: true}"))
    private var posts : MutableList<Post> = mutableListOf(loading)

    companion object {
        private const val VIEW_TYPE_DATA = 0
        private const val VIEW_TYPE_PROGRESS = 1
    }

    fun setPosts(posts: MutableList<Post>) {
        this.posts = posts
    }

    fun addPosts(posts: MutableList<Post>) {
        this.posts.addAll(posts)
    }

    inner class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val postHeader: RelativeLayout = itemView.findViewById(R.id.post_header)
        private val title: TextView = itemView.findViewById(R.id.title)
        private val meta: TextView = itemView.findViewById(R.id.meta)
        private val imageView: SimpleDraweeView = itemView.findViewById(R.id.preview)
        private val votes: TextView = itemView.findViewById(R.id.votes)
        private lateinit var post: Post

        init {
            val progress = ProgressBarDrawable()
            progress.backgroundColor = 0x30FFFFFF
            progress.color = 0x8053BA82.toInt()
            imageView.hierarchy.setProgressBarImage(progress)
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
        }

        @SuppressLint("SetTextI18n")
        fun bind(post: Post) {
            this.post = post
            ViewCompat.setTransitionName(postHeader, post.name)
            title.text = post.title
            meta.text = "(${post.domain})\n${post.num_comments} comment${if (post.num_comments != 1) "s" else ""} in /r/${post.subreddit}\n${post.created_utc} by /u/${post.author}\n"
            votes.text = if (post.score >= 10000) (post.score / 1000).toString() + 'k' else post.score.toString()
            val preview = post.previewUrl
            if (preview != null) {
                imageView.setImageURI(preview)
            } else {
                imageView.setActualImageResource(R.drawable.ic_comment_text_multiple_outline)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            VIEW_TYPE_PROGRESS -> {
                ProgressViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.loading_item,
                        parent,
                        false
                    )
                )
            }
            VIEW_TYPE_DATA -> {
                PostViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.post_item,
                        parent,
                        false
                    )
                )
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
            val index = posts.indexOf(loading)
            if (posts.remove(loading)) {
                notifyItemRemoved(index)
            }
        }
    }
}