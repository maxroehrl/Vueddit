package de.max.roehrl.vueddit2.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.ViewSizeResolver
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.service.Reddit
import org.json.JSONObject

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var manager: RecyclerView.LayoutManager
    private lateinit var myAdapter: RecyclerView.Adapter<*>

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_posts, container, false)
        manager = LinearLayoutManager(this.context)
        val postsObserver = Observer<List<JSONObject>> {
            (myAdapter as PostAdapter).setPosts(it)
            myAdapter.notifyDataSetChanged()
        }
        homeViewModel.posts.observe(this.viewLifecycleOwner, postsObserver)
        myAdapter = PostAdapter(listOf())
        recyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = manager
        recyclerView.adapter = myAdapter
        return root
    }
}

class PostAdapter(): RecyclerView.Adapter<PostAdapter.ViewHolder>() {
    private lateinit var posts : List<JSONObject>

    constructor(posts: List<JSONObject>) : this() {
        setPosts(posts)
    }

    fun setPosts(posts: List<JSONObject>) {
        this.posts = posts
    }

    class ViewHolder(private val view: View): RecyclerView.ViewHolder(view){
        fun bind(post: JSONObject) {
            val tv = view.findViewById<TextView>(R.id.title)
            tv.text = post.getString("title")
            val imageView = view.findViewById<ImageView>(R.id.preview)
            val preview = Reddit.getPreview(post)
            if (preview != null) {
                val disposable = imageView.load(preview.getString("url")) {
                    placeholder(R.drawable.ic_comment_text_multiple_outline)
                    // size(ViewSizeResolver(imageView))
                    error(R.drawable.ic_comment_text_multiple_outline)
                }
            } else {
                imageView.load(R.drawable.ic_comment_text_multiple_outline)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val vh = LayoutInflater.from(parent.context).inflate(R.layout.post_item, parent,false)
        return ViewHolder(vh)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    override fun getItemCount(): Int {
        return posts.size
    }
}