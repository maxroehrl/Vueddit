import * as app from '@nativescript/core/application';

export default class Markdown {
  static markwon;
  static urlOpenCallback = (url) => console.log(url);
  static malformedHadingRegex = /^#+(?=[^#\s])/gm;

  static getInstance() {
    if (!this.markwon) {
      const activity = app.android.foregroundActivity;
      this.markwon = io.noties.markwon.Markwon.builder(activity)
          .usePlugin(this.getUrlPlugin())
          .usePlugin(this.getLinkResolverPlugin())
          .build();
    }
    return this.markwon;
  }

  static getUrlPlugin() {
    // https://developer.android.com/reference/android/text/util/Linkify
    const Pattern = java.util.regex.Pattern;
    // Match users (/u(ser)?/username) and subreddits (/r/subreddit) and valid urls
    const mask = Pattern.compile('(\\/(r|u|user)\\/[^\\s;.,:]+)|((?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)' +
      '(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*' +
      '[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};\']*))',
    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
    const transformFilter = new android.text.util.Linkify.TransformFilter({
      transformUrl(match, url) {
        if (url.startsWith('/r/') || url.startsWith('/u/') || url.startsWith('/user/')) {
          return 'https://reddit.com' + url;
        }
        return url;
      },
    });
    const onClick = (url) => this.urlOpenCallback(url);
    const ClickableSpan = android.text.style.ClickableSpan.extend({
      url: null,
      setURL(url) {
        this.url = url;
      },
      onClick(view) {
        onClick(this.url);
      },
    });
    const LinkifyTextAddedListener = new io.noties.markwon.core.CorePlugin.OnTextAddedListener({
      builder: new android.text.SpannableStringBuilder(),
      onTextAdded(visitor, text, start) {
        this.builder.clear();
        this.builder.clearSpans();
        this.builder.append(text);
        if (android.text.util.Linkify.addLinks(this.builder, mask, null, null, transformFilter)) {
          const spans = this.builder.getSpans(0, this.builder.length(), java.lang.Object.class);
          if (spans != null && spans.length > 0) {
            const spannableBuilder = visitor.builder();
            for (let i = 0; i < spans.length; i++) {
              const span = spans[i];
              const clickableSpan = new ClickableSpan();
              clickableSpan.setURL(span.getURL());
              spannableBuilder.setSpan(
                  clickableSpan,
                  start + this.builder.getSpanStart(span),
                  start + this.builder.getSpanEnd(span),
                  this.builder.getSpanFlags(span));
            }
          }
        }
      },
    });
    const UrlResolverPlugin = io.noties.markwon.AbstractMarkwonPlugin.extend({
      configure(registry) {
        registry.require(io.noties.markwon.core.CorePlugin.class, new io.noties.markwon.MarkwonPlugin.Action({
          apply(corePlugin) {
            corePlugin.addOnTextAddedListener(LinkifyTextAddedListener);
          },
        }));
      },
    });
    return new UrlResolverPlugin();
  }

  static getLinkResolverPlugin() {
    const resolve = (view, link) => this.urlOpenCallback(link);
    const linkResolver = new io.noties.markwon.LinkResolver({resolve});
    const LinkResolverPlugin = io.noties.markwon.AbstractMarkwonPlugin.extend({
      configureConfiguration(builder) {
        builder.linkResolver(linkResolver);
      },
    });
    return new LinkResolverPlugin();
  }

  static setUrlOpenCallback(urlOpenCallback) {
    this.urlOpenCallback = urlOpenCallback;
  }

  static toMarkdown(text) {
    return this.getInstance().toMarkdown(this.fixMarkdownHeadings(text));
  }

  static setMarkdown(tv, text) {
    this.getInstance().setMarkdown(tv, this.fixMarkdownHeadings(text));
  }

  static setParsedMarkdown(tv, spanned) {
    this.getInstance().setParsedMarkdown(tv, spanned);
  }

  static fixMarkdownHeadings(text) {
    return (text || '').replace(this.malformedHadingRegex, (m) => m + ' ');
  }

  static setOnTouchListener(tv, listener) {
    tv.setOnTouchListener(new android.view.View.OnTouchListener({
      onTouch(v, event) {
        listener(event);
        return false;
      },
    }));
  }

  static setOnClickListener(tv, listener) {
    tv.setOnClickListener(new android.view.View.OnClickListener({
      onClick(v) {
        listener();
      },
    }));
  }

  static setSpannableFactory(tv) {
    tv.setSpannableFactory(io.noties.markwon.utils.NoCopySpannableFactory.getInstance());
  }
}
