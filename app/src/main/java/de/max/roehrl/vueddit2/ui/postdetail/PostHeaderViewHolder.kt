package de.max.roehrl.vueddit2.ui.postdetail

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.model.NamedItem
import de.max.roehrl.vueddit2.model.Post
import de.max.roehrl.vueddit2.model.VideoType
import de.max.roehrl.vueddit2.service.CustomTabs
import de.max.roehrl.vueddit2.service.Markdown
import de.max.roehrl.vueddit2.service.Util
import de.max.roehrl.vueddit2.ui.postlist.PostViewHolder


@SuppressLint("SetJavaScriptEnabled")
open class PostHeaderViewHolder(itemView: View) : PostViewHolder(itemView) {
    private val selfText: TextView = itemView.findViewById(R.id.self_text)
    private val numComments: TextView = itemView.findViewById(R.id.num_comments)
    private val embeddedWebView: WebView = itemView.findViewById(R.id.embedded_web_view)
    private val videoView: PlayerView = itemView.findViewById(R.id.video_view)
    private val videoPreviewLayout: LinearLayout = itemView.findViewById(R.id.video_preview_layout)
    override val highlightAuthor = true

    private inner class Client : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            view?.zoomOut()
        }
    }

    private inner class ChromeClient : WebChromeClient() {
        var view: View? = null
        var callback: CustomViewCallback? = null
        var originalSystemUiVisibility: Int? = null

        override fun onHideCustomView() {
            val decorView = (itemView.context as Activity).window.decorView as ViewGroup
            decorView.removeView(this.view)
            view = null
            decorView.systemUiVisibility = originalSystemUiVisibility!!
            callback?.onCustomViewHidden()
            callback = null
        }

        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            if (this.view != null) {
                this.onHideCustomView()
            } else {
                this.view = view
                val decorView = (itemView.context as Activity).window.decorView as ViewGroup
                this.originalSystemUiVisibility = decorView.systemUiVisibility
                decorView.addView(this.view, FrameLayout.LayoutParams(-1, -1))
                decorView.systemUiVisibility = 3846 or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                this.callback = callback
            }
        }
    }

    init {
        embeddedWebView.webViewClient = Client()
        embeddedWebView.webChromeClient = ChromeClient()
        embeddedWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = false
            displayZoomControls = false
            builtInZoomControls = true
            mediaPlaybackRequiresUserGesture = false
        }
        videoView.player = SimpleExoPlayer.Builder(itemView.context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
        }
        videoView.apply {
            setShowNextButton(false)
            setShowFastForwardButton(false)
            setShowPreviousButton(false)
            setShowRewindButton(false)
            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
            setShutterBackgroundColor(Color.TRANSPARENT)
            controllerHideOnTouch = true
            hideController()
        }
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
            videoPreviewLayout.visibility = View.VISIBLE
            videoPreviewLayout.layoutParams.height = Util.getAspectFixHeight(post.video.width, post.video.height)
            when (post.video.type) {
                VideoType.DASH, VideoType.MP4 -> {
                    videoView.visibility = View.VISIBLE
                    embeddedWebView.visibility = View.GONE
                    try {
                        videoView.player!!.setMediaItem(MediaItem.fromUri(post.video.url!!))
                        videoView.player!!.prepare()
                    } catch (e: Exception) {
                        Log.e("PostHeaderViewHolder", "Failed to load ${post.video.type} video from '${post.video.url}'", e)
                    }
                }
                VideoType.EMBEDDED -> {
                    videoView.visibility = View.GONE
                    embeddedWebView.visibility = View.VISIBLE
                    try {
                        embeddedWebView.loadUrl(post.video.url!!)
                    } catch (e: Exception) {
                        Log.e("PostHeaderViewHolder", "Failed to load embedded video from '${post.video.url}'", e)
                    }
                }
                else -> {
                }
            }
        }
    }

    /*private fun addTransitionSupport() {
        val postHeader = view.findViewById<RelativeLayout>(R.id.header)
        ViewCompat.setTransitionName(postHeader, "header")

        val imageView: SimpleDraweeView = itemView.findViewById(R.id.preview)
        imageView.controller = Fresco.newDraweeControllerBuilder().setControllerListener(object : BaseControllerListener<ImageInfo>() {
            override fun onFinalImageSet(id: String?, imageInfo: ImageInfo?, animatable: Animatable?) {
                startPostponedEnterTransition()
            }
        }).build()
    }*/

    override fun onClick(view: View) {
        CustomTabs.openUrl(selfText.context, post.url)
    }
}