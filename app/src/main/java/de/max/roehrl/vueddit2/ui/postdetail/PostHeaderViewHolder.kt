package de.max.roehrl.vueddit2.ui.postdetail

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.model.NamedItem
import de.max.roehrl.vueddit2.service.CustomTabs
import de.max.roehrl.vueddit2.service.Markdown
import de.max.roehrl.vueddit2.ui.postlist.PostViewHolder

@SuppressLint("SetJavaScriptEnabled")
class PostHeaderViewHolder(itemView: View) : PostViewHolder(itemView) {
    private val selfText: TextView = itemView.findViewById(R.id.self_text)
    private val numComments: TextView = itemView.findViewById(R.id.num_comments)
    private val videoPreview: WebView = itemView.findViewById(R.id.video_preview)
    private val videoPreviewLayout: LinearLayout = itemView.findViewById(R.id.video_preview_layout)

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
        videoPreview.webViewClient = Client()
        videoPreview.webChromeClient = ChromeClient()
        videoPreview.settings.javaScriptEnabled = true
        videoPreview.settings.domStorageEnabled = true
        videoPreview.settings.loadWithOverviewMode = true
        videoPreview.settings.useWideViewPort = false
        videoPreview.settings.displayZoomControls = false
        videoPreview.settings.builtInZoomControls = true
        videoPreview.settings.mediaPlaybackRequiresUserGesture = false
    }

    @SuppressLint("SetTextI18n")
    override fun bind(post: NamedItem) {
        super.bind(post)
        if (this.post.selftext.isEmpty()) {
            selfText.visibility = View.GONE
            selfText.text = ""
        } else {
            selfText.visibility = View.VISIBLE
            Markdown.getInstance(selfText.context).setMarkdown(selfText, this.post.getSpannedSelftext(selfText.context))
        }
        numComments.text = "Showing ${this.post.num_comments} comment${if (this.post.num_comments != 1) "s" else ""}"
        // TODO videoPreviewLayout.height

        videoPreview.stopLoading()
        if (this.post.video.height == 0) {
            videoPreviewLayout.visibility = View.GONE
        } else {
            videoPreviewLayout.visibility = View.VISIBLE
            if (this.post.video.src.startsWith("http")) {
                videoPreview.loadUrl(this.post.video.src)
            } else {
                videoPreview.loadData(this.post.video.src, "text/html", "UTF-8")
            }
        }

        // val postHeader = view.findViewById<RelativeLayout>(R.id.header)
        // ViewCompat.setTransitionName(postHeader, "header")

        /*
        val imageView: SimpleDraweeView = itemView.findViewById(R.id.preview)
        imageView.controller = Fresco.newDraweeControllerBuilder().setControllerListener(object : BaseControllerListener<ImageInfo>() {
            override fun onFinalImageSet(id: String?, imageInfo: ImageInfo?, animatable: Animatable?) {
                startPostponedEnterTransition()
            }
        }).build() */
    }

    override fun onClick(view: View) {
        CustomTabs.openUrl(selfText.context, post.url)
    }
}