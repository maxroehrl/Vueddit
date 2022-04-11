package de.max.roehrl.vueddit2.ui.viewholder

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.viewModelScope
import androidx.navigation.findNavController
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.material.tabs.TabLayout
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.model.NamedItem
import de.max.roehrl.vueddit2.model.Post
import de.max.roehrl.vueddit2.model.VideoType
import de.max.roehrl.vueddit2.service.Markdown
import de.max.roehrl.vueddit2.service.Url
import de.max.roehrl.vueddit2.service.Util
import de.max.roehrl.vueddit2.ui.viewmodel.PostDetailViewModel

@SuppressLint("SetJavaScriptEnabled")
open class PostHeaderViewHolder(itemView: View, private val viewModel: PostDetailViewModel) :
    PostViewHolder(itemView, viewModel.viewModelScope) {
    companion object {
        private const val TAG = "PostHeaderViewHolder"
        private const val INTERFACE_NAME = "JsInterface"
    }
    private val selfText: TextView = itemView.findViewById(R.id.self_text)
    private val numComments: TextView = itemView.findViewById(R.id.num_comments)
    private val embeddedWebView: WebView = itemView.findViewById(R.id.embedded_web_view)
    private val videoView: StyledPlayerView = itemView.findViewById(R.id.video_view)
    private val videoPreviewLayout: LinearLayout = itemView.findViewById(R.id.video_preview_layout)
    private val sortingTabLayout: TabLayout = itemView.findViewById(R.id.tab_layout)
    private val sortings = listOf("top", "new", "controversial", "old", "random", "qa")
    private var currentUrl: String? = null
    override val highlightAuthor = true
    private inner class JsInterface(private val view: WebView?) {
        @JavascriptInterface @Suppress("unused")
        fun resize(heightInDp: String) {
            val heightInPx = Util.dpToPx(heightInDp.toInt())
            Log.d(TAG, "Setting new embedded web view height: $heightInDp dp = $heightInPx px")
            view?.post {
                videoPreviewLayout.visibility = View.GONE
                videoPreviewLayout.layoutParams.height = heightInPx
                embeddedWebView.visibility = View.VISIBLE
                videoPreviewLayout.visibility = View.VISIBLE
            }
        }
    }

    init {
        embeddedWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                view?.zoomOut()
                val code = """
                    var scrollHeight = 0;
                    setInterval(function() {
                        var newScrollHeight = document.body.scrollHeight;
                        if (scrollHeight !== newScrollHeight) {
                            scrollHeight = newScrollHeight;
                            $INTERFACE_NAME.resize(scrollHeight);
                        }
                    }, 1000);
                """.trimIndent()
                view?.loadUrl("javascript:$code")
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return !url.equals(currentUrl)
            }
        }
        embeddedWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = false
            displayZoomControls = false
            builtInZoomControls = true
            mediaPlaybackRequiresUserGesture = false
        }
        embeddedWebView.addJavascriptInterface(JsInterface(embeddedWebView), INTERFACE_NAME)
        videoView.player = ExoPlayer.Builder(itemView.context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true

        }
        videoView.apply {
            setShowNextButton(false)
            setShowFastForwardButton(false)
            setShowPreviousButton(false)
            setShowRewindButton(false)
            setShowBuffering(StyledPlayerView.SHOW_BUFFERING_ALWAYS)
            setShutterBackgroundColor(Color.TRANSPARENT)
            controllerHideOnTouch = true
            hideController()
        }
        for (sorting in sortings) {
            sortingTabLayout.addTab(sortingTabLayout.newTab().setText(sorting))
        }
        val index = sortings.indexOf(viewModel.commentSorting.value)
        if (index != -1) {
            sortingTabLayout.getTabAt(index)!!.select()
        }
        sortingTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                onSortingSelected(tab?.text.toString())
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {
                onTabSelected(tab)
            }
        })
    }

    fun onStop() {
        videoView.player?.release()
    }

    fun onPause() {
        videoView.player?.pause()
    }

    override fun bind(post: NamedItem, highlightSticky: Boolean) {
        super.bind(post, highlightSticky)
        if (this.post.selftext.isEmpty()) {
            selfText.visibility = View.GONE
            selfText.text = ""
        } else {
            selfText.visibility = View.VISIBLE
            Markdown.getInstance(selfText.context).setMarkdown(selfText, this.post.getSpannedSelftext(selfText.context))
        }
        numComments.text = numComments.resources.getQuantityString(R.plurals.showing_num_comments,
                this.post.num_comments, this.post.num_comments)
        updateVideoPreview(post as Post)
    }

    private fun updateVideoPreview(post: Post) {
        if (post.video.height == 0) {
            videoPreviewLayout.visibility = View.GONE
        } else {
            currentUrl = post.video.url!!
            when (post.video.type) {
                VideoType.DASH, VideoType.MP4 -> {
                    videoPreviewLayout.layoutParams.height = Util.getAspectFixHeight(post.video.width, post.video.height)
                    videoView.visibility = View.VISIBLE
                    embeddedWebView.visibility = View.GONE
                    videoPreviewLayout.visibility = View.VISIBLE
                    try {
                        videoView.player!!.setMediaItem(MediaItem.fromUri(currentUrl!!))
                        videoView.player!!.prepare()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load ${post.video.type} video from '$currentUrl'", e)
                    }
                }
                VideoType.EMBEDDED -> {
                    videoView.visibility = View.GONE
                    embeddedWebView.visibility = View.GONE
                    @Suppress("unused")
                    try {
                        embeddedWebView.loadUrl(currentUrl!!)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load embedded video from '$currentUrl'", e)
                    }
                }
                else -> {
                    Log.e(TAG, "Unsupported url: '$currentUrl'")
                }
            }
        }
    }

    private fun onSortingSelected(sorting: String) {
        viewModel.setCommentSorting(sorting)
        viewModel.refreshComments()
    }

    override fun onClick(view: View) {
        Url.openUrl(selfText.context, selfText.findNavController(), post.url)
    }
}