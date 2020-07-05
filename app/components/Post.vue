<template>
  <StackLayout>
    <FlexboxLayout flexDirection="row"
                   flexWrap="nowrap"
                   alignSelf="flex-start"
                   justifyContent="flex-start"
                   class="post-header">
      <Votes :post="post" />
      <Ripple rippleColor="#53ba82"
              class="post-info"
              @tap="showMoreOptions(post)">
        <Label textWrap="true">
          <FormattedString>
            <Span :style="{'background-color': post.link_flair_background_color}"
                  :text="post.link_flair_text ? post.link_flair_text : ''"
                  class="post-flair" />
            <Span :text="post.link_flair_text ? ' ' + post.title : post.title" />
            <Span :text="' (' + post.domain + ')\n'" class="post" />
            <Span :text="post.num_comments + ' comments '" class="post" />
            <Span :text="post.subreddit + '\n'" class="post" />
            <Span :text="getTimeFromNow(post) + ' by '" class="post" />
            <Span :text="post.author + ' '" class="post-author" />
            <Span :text="post.author_flair_text ? post.author_flair_text : ''"
                  class="post-author-flair"
                  :style="{'background-color': post.author_flair_background_color || '#767676'}" />
          </FormattedString>
        </Label>
      </Ripple>
      <Ripple rippleColor="#53ba82"
              class="post-preview"
              @tap="openUrl(post)">
        <Image :src="getPreview(post)"
               stretch="aspectFit"
               loadMode="async" />
      </Ripple>
    </FlexboxLayout>
    <WebView v-if="getVideo(post)"
             :height="getVideoHeight(post)"
             :src="getVideo(post).src"
             @loadFinished="onVideoPreviewLoadFinished"
             @loaded="onVideoPreviewLoaded" />
    <Image v-else-if="post.preview && post.preview.enabled"
           :src="getImage(post)"
           :style="{height: getImageHeight(post)}"
           stretch="aspectFit"
           class="post-image"
           loadMode="async" />
    <MarkdownView v-if="post.selftext !== ''"
                  :text="post.selftext"
                  class="post-text" />
    <Label class="post-comment-num-label"
           :text="post.num_comments + ' comment' + (post.num_comments === 1 ? '' : 's')" />
  </StackLayout>
</template>

<script>
import {action} from 'tns-core-modules/ui/dialogs';
import {screen} from 'tns-core-modules/platform';
import MarkdownView from './MarkdownView';
import CustomTabs from '../services/CustomTabs';
import Reddit from '../services/Reddit';
import Votes from './Votes';
import User from './User';
import Comments from './Comments';

export default {
  name: 'Post',
  components: {Votes, MarkdownView},
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

    getImage(post) {
      const preview = Reddit.getImage(post);
      return preview ? preview.url: '';
    },

    getImageHeight(post) {
      return Reddit.getAspectFixHeight(Reddit.getImage(post));
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
              video {
                  width: ${screen.mainScreen.widthDIPs}px;
              }
            </style>
          </head>
          <body>
            <script src="https://cdn.dashjs.org/latest/dash.all.min.js"><` + `/script>
            <video data-dashjs-player autoplay controls src="${video.dash_url}"></video>
          </body>
        </html>`;
    },

    getVideoHeight(post) {
      return Reddit.getAspectFixHeight(this.getVideo(post));
    },

    getTimeFromNow(post) {
      return Reddit.getTimeFromNow(post);
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
        if (action === 'Save') {
          const promise = post.saved ? Reddit.unsave(post.name) : Reddit.save(post.name);
          promise.then(() => post.saved = !post.saved);
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
  .post {
    color: #767676;
  }

  .post-header {
    background-color: #080808;
  }

  .post-info {
    width: 65%;
  }

  .post-preview {
    width: 20%;
  }

  .post-image {
    width: 100%;
  }

  .post-flair {
    font-size: 12px;
    color: #f3f3f3;
  }

  .post-text {
    border-width: 6px;
    border-color: #767676;
    margin: 10px;
    padding: 30px;
    border-radius: 30px;
  }

  .post-author {
    color: #53ba82;
  }

  .post-author-flair {
    color: #c2c2c2;
  }

  .post-comment-num-label {
    background-color: #3e3e3e;
    color: white;
    font-size: 14px;
    margin-top: 20px;
    margin-bottom: 20px;
    width: 100%;
    text-align: center;
  }
</style>
