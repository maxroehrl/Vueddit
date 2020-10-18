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
      if (post.secure_media_embed && post.secure_media_embed.media_domain_url) {
        return this.getVideoObject(post.secure_media_embed, 'media_domain_url');
      } else if (post.secure_media && post.secure_media.reddit_video) {
        return this.getVideoObject(post.secure_media.reddit_video, 'dash');
      } else if (post.preview && post.preview.reddit_video_preview) {
        return this.getVideoObject(post.preview.reddit_video_preview, 'dash');
      } else if (post.preview && post.preview.images && post.preview.images.length &&
          post.preview.images[0].variants && post.preview.images[0].variants.mp4) {
        return this.getVideoObject(post.preview.images[0].variants.mp4.source, 'url');
      } else {
        return null;
      }
    },

    getVideoObject(source, srcProp='src', heightProp='height', widthProp='width') {
      return {
        src: srcProp === 'dash' ? this.getRedditDashVideoPlayerHtml(source) : source[srcProp],
        height: source[heightProp],
        width: source[widthProp],
      };
    },

    getRedditDashVideoPlayerHtml(video) {
      return `
        <!DOCTYPE html>
          <html lang="en">
          <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
            <title>DashJS Player</title>
            <style>
              body {
                margin: 0;
                background-color: black;
              }
            </style>
          </head>
          <body>
            <script src="https://cdn.dashjs.org/latest/dash.all.min.js"><` + `/script>
            <video autoplay controls loop width="${Screen.mainScreen.widthDIPs}px"></video>
            <script>
              document.addEventListener("DOMContentLoaded", function () {
                const player = dashjs.MediaPlayer().create();
                player.initialize(document.querySelector("video"), '${video.dash_url}', true);
            });
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
