package de.max.roehrl.vueddit2.service

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.text.util.Linkify
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import de.max.roehrl.vueddit2.R
import io.noties.markwon.*
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.core.MarkwonTheme
import java.util.regex.Pattern

class Markdown private constructor(context: Context) {
    private val markwon: Markwon
    private val linkColor = ContextCompat.getColor(context, R.color.highlight)
    var urlOpenCallback = { url: String -> Url.openUrl(context, null, url) }
    private val malformedHeadingRegex = Regex("^#+(?=[^#\\s])", RegexOption.MULTILINE)

    init {
        markwon = Markwon.builder(context)
            .usePlugin(getUrlPlugin())
            .usePlugin(getLinkResolverPlugin())
            .build()
    }

    companion object : SingletonHolder<Markdown, Context>(::Markdown)

    private fun getUrlPlugin(): MarkwonPlugin {
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
                || url.startsWith("/user/")
            ) {
                return@TransformFilter "${Reddit.api}$url"
            }
            return@TransformFilter url
        }

        class ClickableUrlSpan : ClickableSpan() {
            lateinit var url: String

            override fun onClick(view: View) {
                urlOpenCallback.invoke(url)
            }
        }

        class LinkifyTextAddedListener : CorePlugin.OnTextAddedListener {
            val builder = SpannableStringBuilder()
            override fun onTextAdded(visitor: MarkwonVisitor, text: String, start: Int) {
                builder.clear()
                builder.clearSpans()
                builder.append(text)
                if (Linkify.addLinks(builder, mask, null, null, transformFilter)) {
                    val spans: Array<URLSpan>? =
                        builder.getSpans(0, builder.length, URLSpan::class.java)
                    if (spans != null && spans.isNotEmpty()) {
                        val spannableBuilder = visitor.builder()
                        for (span in spans) {
                            val clickableSpan = ClickableUrlSpan()
                            clickableSpan.url = span.url
                            spannableBuilder.setSpan(
                                clickableSpan,
                                start + builder.getSpanStart(span),
                                start + builder.getSpanEnd(span),
                                builder.getSpanFlags(span)
                            )
                        }
                    }
                }
            }
        }

        class UrlResolverPlugin : AbstractMarkwonPlugin() {
            override fun configure(registry: MarkwonPlugin.Registry) {
                registry.require(CorePlugin::class.java) { corePlugin ->
                    corePlugin.addOnTextAddedListener(LinkifyTextAddedListener())
                }
            }

            override fun configureTheme(builder: MarkwonTheme.Builder) {
                builder.linkColor(linkColor)
            }
        }
        return UrlResolverPlugin()
    }

    private fun getLinkResolverPlugin(): MarkwonPlugin {
        class LinkResolverPlugin : AbstractMarkwonPlugin() {
            override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                builder.linkResolver { _, url -> urlOpenCallback.invoke(url) }
            }
        }
        return LinkResolverPlugin()
    }

    fun setMarkdown(tv: TextView, text: String) {
        markwon.setMarkdown(tv, fixMarkdownSpacing(text))
    }

    fun toMarkDown(text: String): Spanned {
        return markwon.toMarkdown(fixMarkdownSpacing(text))
    }

    fun setMarkdown(tv: TextView, markdown: Spanned?) {
        markwon.setParsedMarkdown(tv, markdown!!)
    }

    private fun fixMarkdownSpacing(text: String): String {
        return text
            .replace(malformedHeadingRegex) { it.value + " " }
            .replace("]\n(", "](")
    }
}