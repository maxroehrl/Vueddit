<template>
  <StackLayout padding="0">
    <PostHeader :post="post"
                :bigPreview="shouldShowBigPreview(post) && !getVideo(post)"
                :highlightAuthor="true"
                width="100%"
                :onLongPress="showMoreOptions"
                :onTab="openUrl" />
    <AdvancedWebView :hidden="!hasEmbeddedVideo(post)"
                     :height="getVideoHeight(post) + 'px'"
                     :src="getVideoSource(post)"
                     :shouldOverrideUrlLoading="shouldOverrideUrlLoading"
                     @loadFinished="onVideoPreviewLoadFinished"
                     @loaded="onVideoPreviewLoaded" />
    <ExoPlayer ref="exoplayer"
               :hidden="!hasExoPlayerVideo(post)"
               controls="true"
               autoplay="true"
               :src="getVideoSource(post)"
               :height="getVideoHeight(post)"
               loop="true" />
    <MarkdownView :hidden="post.selftext === ''"
                  :text="post.selftext"
                  class="post-text" />
    <Label class="num-comments"
           :text="'Showing ' + post.shown_comments + ' comment' + (post.shown_comments === 1 ? '' : 's')" />
  </StackLayout>
</template>

<script>
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
        androidWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        androidWebView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
      }
    },

    shouldOverrideUrlLoading(url) {
      return url !== this.getVideo(this.post).src;
    },

    onVideoPreviewLoadFinished(args) {
      args.object.android.zoomOut();
    },

    getPreview(post) {
      return Reddit.getPreview(post)?.url ?? '';
    },

    shouldShowBigPreview(post) {
      return !!Reddit.getPreview(post, 300, false);
    },

    getImage(post) {
      return Reddit.getImage(post)?.url ?? '';
    },

    getImageHeight(post) {
      const image = Reddit.getImage(post);
      return image ? Reddit.getAspectFixHeight(image) : '0px';
    },

    hasEmbeddedVideo(post) {
      const video = this.getVideo(post);
      return video && video.type === 'media_domain_url';
    },

    hasExoPlayerVideo(post) {
      const video = this.getVideo(post);
      return video && ['dash_url', 'url'].includes(video.type);
    },

    getVideo(post) {
      if (post.secure_media?.reddit_video) {
        return this.getVideoObject(post.secure_media.reddit_video, 'dash_url');
      } else if (post.preview?.reddit_video_preview) {
        return this.getVideoObject(post.preview.reddit_video_preview, 'dash_url');
      } else if (post.preview?.images?.[0]?.variants?.mp4) {
        return this.getVideoObject(post.preview.images[0].variants.mp4.source, 'url');
      } else if (post.secure_media_embed?.media_domain_url) {
        return this.getVideoObject(post.secure_media_embed, 'media_domain_url');
      } else {
        return null;
      }
    },

    getVideoSource(post) {
      return this.getVideo(post)?.src;
    },

    getVideoObject(source, srcProp) {
      if (source) {
        const src = source[srcProp];
        if (src && src !== '') {
          return {
            src,
            type: srcProp,
            height: source.height ?? 0,
            width: source.width ?? 0,
          };
        }
      }
      return null;
    },

    getVideoHeight(post) {
      const video = this.getVideo(post);
      return video ? Reddit.getAspectFixHeight(video) : '0px';
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
