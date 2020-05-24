import * as app from 'tns-core-modules/application';

export default class Markdown {
  static markwon;
  static urlOpenCallback = (link, view) => console.log(link);

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
    // eslint-disable-next-line no-unused-vars
    const mask = Pattern.compile('(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)' +
      '(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*' +
      '[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};\']*)',
    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
    // eslint-disable-next-line no-unused-vars
    const transformFilter = new android.text.util.Linkify.TransformFilter({
      transformUrl(match, url) {
        console.log(match, url);
      },
    });
    // eslint-disable-next-line no-unused-vars
    const matchFilter = new android.text.util.Linkify.MatchFilter({
      acceptMatch(s, start, end) {
        console.log(s, start, end);
      },
    });
    const LinkifyTextAddedListener = new io.noties.markwon.core.CorePlugin.OnTextAddedListener({
      builder: new android.text.SpannableStringBuilder(),
      onTextAdded(visitor, text, start) {
        this.builder.clear();
        this.builder.clearSpans();
        this.builder.append(text);
        if (android.text.util.Linkify.addLinks(this.builder, 1)) {
          const spans = this.builder.getSpans(0, this.builder.length(), java.lang.Object.class);
          if (spans != null && spans.length > 0) {
            const spannableBuilder = visitor.builder();
            for (let i = 0; i < spans.length; i++) {
              const span = spans[i];
              spannableBuilder.setSpan(
                  span,
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
    const linkResolver = new io.noties.markwon.LinkResolver({
      resolve(view, link) {
        this.urlOpenCallback(link, view);
      },
    });
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
    if (!text) {
      return '';
    }
    return this.getInstance().toMarkdown(text);
  }
}