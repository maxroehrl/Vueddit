package de.max.roehrl.vueddit2.service

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.text.util.Linkify
import android.view.View
import io.noties.markwon.*
import io.noties.markwon.core.CorePlugin
import java.util.regex.Pattern

class Markdown(context: Context) {
    private val markwon: Markwon
    private var urlOpenCallback = { url : String -> CustomTabs.openUrl(context, url) }

    init {
        markwon = Markwon.builder(context)
            .usePlugin(getUrlPlugin())
            .usePlugin(getLinkResolverPlugin())
            .build()
    }

    private fun getUrlPlugin() : MarkwonPlugin {
        // https://developer.android.com/reference/android/text/util/Linkify
        // Match users (/u(ser)?/username) and subreddits (/r/subreddit) and valid urls
        val mask = Pattern.compile(
            "(\\/(r|u|user)\\/[^\\s;.,:]+)|((?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)" +
                    "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*" +
                    "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};\']*))",
            Pattern.CASE_INSENSITIVE or Pattern.MULTILINE or Pattern.DOTALL
        )
        val transformFilter = Linkify.TransformFilter { _, url ->
                if (url.startsWith("/r/")
                    || url.startsWith("/u/")
                    || url.startsWith("/user/")) {
                    return@TransformFilter "${Reddit.api}$url"
                }
            return@TransformFilter url
        }
        class ClickableSpan2 : ClickableSpan() {
            lateinit var url : String

            fun setURL(url : String) {
                this.url = url
            }

            override fun onClick(view: View) {
                urlOpenCallback(url)
            }
        }
        class LinkifyTextAddedListener : CorePlugin.OnTextAddedListener {
            val builder = SpannableStringBuilder()
            override fun onTextAdded(visitor: MarkwonVisitor, text: String, start: Int) {
                this.builder.clear()
                this.builder.clearSpans()
                this.builder.append(text)
                if (Linkify.addLinks(this.builder, mask, null, null, transformFilter)) {
                    val spans : Array<URLSpan>? = this.builder.getSpans(0, this.builder.length, URLSpan::class.java)
                    if (spans != null && spans.isNotEmpty()) {
                        val spannableBuilder = visitor.builder()
                        for (span in spans) {
                            val clickableSpan = ClickableSpan2()
                            clickableSpan.setURL(span.url)
                            spannableBuilder.setSpan(
                                clickableSpan,
                                start + this.builder.getSpanStart(span),
                                start + this.builder.getSpanEnd(span),
                                this.builder.getSpanFlags(span))
                        }
                    }
                }
            }
        }
        class UrlResolverPlugin : AbstractMarkwonPlugin() {
            override fun configure(registry : MarkwonPlugin.Registry) {
                registry.require(CorePlugin::class.java) { corePlugin ->
                    corePlugin.addOnTextAddedListener(LinkifyTextAddedListener())
                }
            }
        }
        return UrlResolverPlugin()
    }

    private fun getLinkResolverPlugin() : MarkwonPlugin {
        class LinkResolverPlugin : AbstractMarkwonPlugin() {
            override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                builder.linkResolver { _, url -> urlOpenCallback.invoke(url) }
            }
        }
        return LinkResolverPlugin()
    }
}