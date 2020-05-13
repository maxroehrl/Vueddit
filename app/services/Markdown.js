import * as app from 'tns-core-modules/application';

export default class Markdown {
  static markwon;
  static urlOpenCallback = () => {};

  static getInstance() {
    if (!this.markwon) {
      const LinkResolver = io.noties.markwon.LinkResolverDef.extend({
        resolve(view, url) {
          this.urlOpenCallback(url, view);
        },
      });
      const LinkResolverPlugin = io.noties.markwon.AbstractMarkwonPlugin.extend({
        configureConfiguration(builder) {
          builder.linkResolver(new LinkResolver());
        },
      });
      const types = android.text.util.Linkify.WEB_URLS;
      const weburls = io.noties.markwon.linkify.LinkifyPlugin.create(types);
      const activity = app.android.foregroundActivity;
      this.markwon = io.noties.markwon.Markwon.builder(activity)
          .usePlugin(weburls)
          .usePlugin(new LinkResolverPlugin())
          .build();
    }
    return this.markwon;
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
