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
           :text="post.num_comments + ' comment' + (post.num_comments === 1 ? '' : 's')" />
  </StackLayout>
</template>

<script>
import {action} from '@nativescript/core/ui/dialogs';
import {Screen} from '@nativescript/core/platform';
import MarkdownView from './MarkdownView';
import CustomTabs from '../services/CustomTabs';
import Reddit from '../services/Reddit';
import User from './User';
import Comments from './Comments';
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
        post.secure_media_embed.src = post.secure_media_embed.media_domain_url;
        return post.secure_media_embed;
      } else if (post.secure_media && post.secure_media.reddit_video) {
        post.secure_media.reddit_video.src = this.getRedditDashVideoPlayerHtml(post.secure_media.reddit_video);
        return post.secure_media.reddit_video;
      } else if (post.preview && post.preview.reddit_video_preview) {
        post.preview.reddit_video_preview.src = this.getRedditDashVideoPlayerHtml(post.preview.reddit_video_preview);
        return post.preview.reddit_video_preview;
      } else if (post.preview && post.preview.images && post.preview.images.length &&
          post.preview.images[0].variants && post.preview.images[0].variants.mp4) {
        post.preview.images[0].variants.mp4.source.src = post.preview.images[0].variants.mp4.source.url;
        return post.preview.images[0].variants.mp4.source;
      } else {
        return null;
      }
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
      if (post.domain === 'reddit.com') {
        const permalink = '/' + post.url.split('/').slice(3, 8).join('/') + '/';
        Reddit.getComments(permalink).then((r) => {
          if (r && r.length === 2 &&
            r[0].data &&
            r[0].data.children &&
            r[0].data.children.length === 1 &&
            r[0].data.children[0].data) {
            this.$navigateTo(Comments, {
              transition: 'slide',
              props: {
                app: this.app,
                post: r[0].data.children[0].data,
              },
            });
          }
        });
      } else {
        CustomTabs.openUrl(post.url);
      }
    },

    showMoreOptions(post) {
      const actions = [
        post.saved ? 'Unsave' : 'Save',
        // 'Goto /u/' + post.author, // Needs double back navigation if post is opened from user page
      ];
      if (post.subreddit !== this.app.subreddit.display_name && post.subreddit_type !== 'user') {
        actions.push('Goto /r/' + post.subreddit);
      }
      action({actions}).then((action) => {
        if (action === 'Save' || action === 'Unsave') {
          Reddit.saveOrUnsave(post);
        } else if (action.startsWith('Goto /r/')) {
          this.app.$navigateBack();
          this.app.setSubreddit({display_name: post.subreddit});
        } else if (action.startsWith('Goto /u/')) {
          this.app.$navigateBack();
          this.app.$navigateTo(User, {
            transition: 'slide',
            props: {
              user: post.author,
              app: this.app,
            },
          });
        }
      });
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
