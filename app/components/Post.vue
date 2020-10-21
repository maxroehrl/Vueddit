<template>
  <StackLayout padding="0">
    <PostHeader :post="post"
                :bigPreview="shouldShowBigPreview(post) && !getVideo(post)"
                :highlightAuthor="true"
                width="100%"
                :onLongPress="showMoreOptions"
                :onTab="openUrl" />
    <WebView v-if="getVideo(post)"
             :height="getVideoHeight(post)"
             :src="getVideo(post).src"
             @loadFinished="onVideoPreviewLoadFinished"
             @loaded="onVideoPreviewLoaded" />
    <MarkdownView v-if="post.selftext !== ''"
                  :text="post.selftext"
                  class="post-text" />
    <Label class="num-comments"
           :text="'Showing ' + post.shown_comments + ' comment' + (post.shown_comments === 1 ? '' : 's')" />
  </StackLayout>
</template>

<script>
import * as app from '@nativescript/core/application';
import {Screen} from '@nativescript/core/platform';
import MarkdownView from './MarkdownView';
import Reddit from '../services/Reddit';
import PostHeader from './PostHeader';

export default {
  name: 'Post',
  components: {MarkdownView, PostHeader},
  props: {
    post: {
      type: Object,
      required: true,
    },
    app: {
      type: Object,
      required: true,
    },
  },
  methods: {
    onVideoPreviewLoaded(args) {
      const androidWebView = args.object.android;
      if (androidWebView) {
        androidWebView.getSettings().setJavaScriptEnabled(true);
        androidWebView.getSettings().setDomStorageEnabled(true);
        androidWebView.getSettings().setLoadWithOverviewMode(true);
        androidWebView.getSettings().setUseWideViewPort(false);
        androidWebView.getSettings().setDisplayZoomControls(false);
        androidWebView.getSettings().setBuiltInZoomControls(true);
        androidWebView.getSettings().setAllowFileAccessFromFileURLs(true);
        androidWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        androidWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        androidWebView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        const ChromeClient = android.webkit.WebChromeClient.extend({
          view: null,
          callback: null,
          originalSystemUiVisibility: null,

          onHideCustomView() {
            const decorView = app.android.foregroundActivity.getWindow().getDecorView();
            decorView.removeView(this.view);
            this.view = null;
            decorView.setSystemUiVisibility(this.originalSystemUiVisibility);
            this.callback.onCustomViewHidden();
            this.callback = null;
          },

          onShowCustomView(view, callback) {
            if (this.view != null) {
              this.onHideCustomView();
            } else {
              this.view = view;
              const decorView = app.android.foregroundActivity.getWindow().getDecorView();
              this.originalSystemUiVisibility = decorView.getSystemUiVisibility();
              decorView.addView(this.view, new android.widget.FrameLayout.LayoutParams(-1, -1));
              decorView.setSystemUiVisibility(3846 | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
              this.callback = callback;
            }
          },
        });
        androidWebView.setWebChromeClient(new ChromeClient());
      }
    },

    onVideoPreviewLoadFinished(args) {
      args.object.android.zoomOut();
    },

    getPreview(post) {
      const preview = Reddit.getPreview(post);
      return preview ? preview.url: '';
    },

    shouldShowBigPreview(post) {
      return !!Reddit.getPreview(post, 300, false);
    },

    getImage(post) {
      const preview = Reddit.getImage(post);
      return preview ? preview.url: '';
    },

    getImageHeight(post) {
      const image = Reddit.getImage(post);
      return image ? Reddit.getAspectFixHeight(image) : '0px';
    },

    getVideo(post) {
      if (post.secure_media?.reddit_video) {
        return this.getVideoObject(post.secure_media.reddit_video, 'hls_url');
      } else if (post.preview?.reddit_video_preview) {
        return this.getVideoObject(post.preview.reddit_video_preview, 'hls_url');
      } else if (post.preview?.images?.[0]?.variants?.mp4) {
        return this.getVideoObject(post.preview.images[0].variants.mp4.source, 'url');
      } else if (post.secure_media_embed?.media_domain_url) {
        return this.getVideoObject(post.secure_media_embed, 'media_domain_url');
      } else {
        return null;
      }
    },

    getVideoObject(source, srcProp='src', heightProp='height', widthProp='width') {
      let src = source[srcProp];
      if (['hls_url', 'url'].includes(srcProp)) {
        const type = srcProp === 'hls_url' ? 'application/x-mpegURL' : 'video/mp4';
        src = this.getRedditDashVideoPlayerHtml(source[srcProp], type);
      }
      return {
        src,
        height: source[heightProp],
        width: source[widthProp],
      };
    },

    getRedditDashVideoPlayerHtml(src, type) {
      return `
        <!DOCTYPE html>
          <html lang="en">
          <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
            <title>Video Player</title>
            <style>
              body {
                margin: 0;
                background-color: black;
              }
            </style>
            <link href="https://unpkg.com/video.js/dist/video-js.min.css" rel="stylesheet">
          </head>
          <body>
            <video-js id="vid1" class="video-js vjs-default-skin" muted autoplay controls loop width="${Screen.mainScreen.widthDIPs}">
              <source src="${src}" type="${type}" />
            </video-js>
            <script src="https://unpkg.com/video.js/dist/video.min.js"><` + `/script>
            <script src="https://unpkg.com/@videojs/http-streaming/dist/videojs-http-streaming.min.js"><` + `/script>
            <script>
              const player = videojs('vid1');
            <` + `/script>
          </body>
        </html>`;
    },

    getVideoHeight(post) {
      return Reddit.getAspectFixHeight(this.getVideo(post));
    },

    openUrl(post) {
      this.app.openUrl(post.url, post.permalink);
    },

    showMoreOptions(post) {
      this.app.showMoreDialog(post, post.subreddit_type !== 'user', true);
    },
  },
};
</script>

<style scoped>
  .post-text {
    font-size: 13px;
    border-width: 6px;
    border-color: #767676;
    margin: 10px;
    padding: 30px;
    border-radius: 30px;
  }

  .num-comments {
    background-color: #3e3e3e;
    color: #ffffff;
    font-size: 14px;
    margin-top: 20px;
    padding-bottom: 0;
    width: 100%;
    text-align: center;
  }
</style>
