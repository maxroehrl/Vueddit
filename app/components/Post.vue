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
            <Span v-if="post.link_flair_text"
                  :style="{'background-color': post.link_flair_background_color}"
                  :text="post.link_flair_text"
                  class="post-flair" />
            <Span v-if="post.link_flair_text" text=" " />
            <Span :text="post.title" />
            <Span :text="' (' + post.domain + ')\n'" class="post" />
            <Span :text="post.num_comments + ' comments '" class="post" />
            <Span :text="post.subreddit + '\n'" class="post" />
            <Span :text="getHours(post.created) + ' hours ago by '" class="post" />
            <Span :text="post.author + ' '" class="post-author" />
            <Span :text="post.author_flair_text"
                  class="post-author-flair"
                  :style="{'background-color': post.author_flair_background_color || defaultFlairColor}" />
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
           stretch="aspectFit"
           class="post-image"
           loadMode="async" />
    <MarkdownView v-if="post.selftext !== ''"
                  :text="post.selftext"
                  class="post-text" />
    <Label class="post-comment-num-label" :text="post.num_comments <= 1 ? '1 comment' : post.num_comments + ' comments'" />
  </StackLayout>
</template>

<script>
import {action} from 'tns-core-modules/ui/dialogs';
import {screen} from 'tns-core-modules/platform';
import MarkdownView from './MarkdownView';
import CustomTabs from '../services/CustomTabs';
import Reddit from '../services/Reddit';
import Votes from './Votes';

export default {
  name: 'Post',
  components: {Votes, MarkdownView},
  props: {
    post: {
      type: Object,
      required: true,
    },
  },
  data() {
    return {
      defaultFlairColor: '#767676',
    };
  },
  methods: {
    onVideoPreviewLoaded(args) {
      const androidWebView = args.object.android;
      if (androidWebView) {
        androidWebView.getSettings().setDomStorageEnabled(true);
        androidWebView.getSettings().setLoadWithOverviewMode(true);
        androidWebView.getSettings().setUseWideViewPort(false);
        androidWebView.getSettings().setDisplayZoomControls(false);
        androidWebView.getSettings().setBuiltInZoomControls(true);
      }
    },

    onVideoPreviewLoadFinished(args) {
      args.object.android.zoomOut();
    },

    getPreview(post) {
      return Reddit.getPreview(post);
    },

    getImage(post) {
      if (post.preview.images && post.preview.images[0]) {
        const resolutions = post.preview.images[0].resolutions;
        return resolutions[resolutions.length - 1].url;
      } else {
        return '';
      }
    },

    getVideo(post) {
      if (post.secure_media_embed && post.secure_media_embed.media_domain_url) {
        post.secure_media_embed.src = post.secure_media_embed.media_domain_url;
        return post.secure_media_embed;
      } else if (post.preview && post.preview.reddit_video_preview) {
        post.preview.reddit_video_preview.src = post.preview.reddit_video_preview.fallback_url;
        return post.preview.reddit_video_preview;
      } else if (post.preview && post.preview.images && post.preview.images.length &&
          post.preview.images[0].variants && post.preview.images[0].variants.mp4) {
        post.preview.images[0].variants.mp4.source.src = post.preview.images[0].variants.mp4.source.url;
        return post.preview.images[0].variants.mp4.source;
      } else {
        return null;
      }
    },

    getVideoHeight(post) {
      const video = this.getVideo(post);
      return (video.height * screen.mainScreen.widthPixels / video.width).toFixed(0) + 'px';
    },

    getHours(unixTime) {
      return new Date(unixTime * 1000).getHours();
    },

    openUrl(post) {
      CustomTabs.openUrl(post.url);
    },

    showMoreOptions(post) {
      action({actions: ['Reply', 'Save', 'Goto ' + post.subreddit, 'Goto /u/' + post.author, 'Copy', 'Share']});
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