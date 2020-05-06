<template>
  <Page @loaded="loaded($event)">
    <ActionBar :title="post.title">
      <NavigationButton text="Back"
                        icon="res://ic_arrow_left"
                        @tap="$navigateBack" />
    </ActionBar>
    <RadListView id="comment-list"
                 for="comment in commentList"
                 pullToRefresh="true"
                 @pullToRefreshInitiated="onPullDown">
      <v-template name="header">
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
                        class="author-flair"
                        :style="{'background-color': post.author_flair_background_color || defaultFlairColor}" />
                </FormattedString>
              </Label>
            </Ripple>
            <Ripple rippleColor="#53ba82"
                    class="post-preview"
                    @tap="openUrl(post)">
              <Image id="postPreview"
                     :src="getPreview(post)"
                     stretch="aspectFit" />
            </Ripple>
          </FlexboxLayout>
          <WebView v-if="Object.keys(post.secure_media_embed).length"
                   height="2000px"
                   :src="getSrc(post)"
                   @loadStarted="onLoadStarted" />
          <Image v-if="post.preview && post.preview.enabled && !Object.keys(post.secure_media_embed).length"
                 :src="getImage(post)"
                 stretch="aspectFit"
                 class="post-image"
                 loadMode="async" />
          <MarkdownView v-if="post.selftext !== ''"
                        :markdown="post.selftext"
                        textWrap="true"
                        class="post-text" />
          <Label class="comment-label" :text="post.num_comments <= 1 ? '1 comment' : post.num_comments + ' comments'" />
        </StackLayout>
      </v-template>
      <v-template>
        <StackLayout :style="{marginLeft: ((comment.depth * 70) + 'px')}" class="comment">
          <Label>
            <FormattedString>
              <Span :text="comment.author_flair_text"
                    class="author-flair"
                    :style="{'background-color': comment.author_flair_background_color || defaultFlairColor}" />
              <Span :text="(comment.author_flair_text ? ' ' : '') + comment.author + ' '"
                    :style="{color: getUserColor(comment, post)}" />
              <Span :text="comment.ups + ' points '" class="comment-votes" />
              <Span :text="getHours(comment.created) + ' hours ago'" class="comment-created" />
            </FormattedString>
          </Label>
          <MarkdownView :markdown="comment.body" class="comment-body" />
        </StackLayout>
      </v-template>
    </RadListView>
  </Page>
</template>

<script>
import {ObservableArray} from 'tns-core-modules/data/observable-array';
import {ad} from 'tns-core-modules/utils/utils';
import {action} from 'tns-core-modules/ui/dialogs';
import * as app from 'tns-core-modules/application';
import Reddit from '../services/Reddit';
import Votes from './Votes';

export default {
  name: 'Comments',
  components: {Votes},
  props: {
    post: {
      type: Object,
      required: true,
    },
  },
  data() {
    return {
      commentList: new ObservableArray([]),
      defaultFlairColor: '#767676',
    };
  },
  computed: {},
  methods: {
    onPullDown(args) {
      this.getComments().finally(() => args.object.notifyPullToRefreshFinished(true));
    },

    onLoadStarted(args) {
      const androidWebView = args.object.android;
      if (androidWebView) {
        androidWebView.getSettings().setDomStorageEnabled(true);
        androidWebView.getSettings().setLoadWithOverviewMode(true);
        androidWebView.getSettings().setUseWideViewPort(true);
        androidWebView.getSettings().setDisplayZoomControls(false);
        androidWebView.getSettings().setBuiltInZoomControls(true);
      }
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

    getSrc(post) {
      if (post.secure_media_embed && post.secure_media_embed.content) {
        return post.secure_media_embed.content;
      } else {
        return '';
      }
    },

    getHours(unixTime) {
      return new Date(unixTime * 1000).getHours();
    },

    getUserColor(comment, post) {
      if (comment.author === post.author) {
        return '#53ba82';
      } else if (comment.distinguished === 'moderator') {
        return '#4afff5';
      } else if (comment.distinguished === 'admin') {
        return '#c40013';
      } else {
        return '#c4c4c4';
      }
    },

    loaded(event) {
      if (!this.commentList.length) {
        this.getComments();
      }
    },

    getComments() {
      return Reddit.getComments(this.post).then((r) => {
        if (r && r.length === 2 && r[1].data) {
          const items = r[1].data.children.map((d) => d.data);
          this.commentList = this.processComments(items);
        }
      });
    },

    processComments(items) {
      const commentList = [];
      const addAllChildren = (comment) => {
        let children;
        if (comment.replies && comment.replies !== '') {
          children = comment.replies.data.children.map((d) => d.data);
        }
        if (comment.body) {
          commentList.push(comment);
        }
        if (children) {
          children.forEach((comment) => addAllChildren(comment));
        }
      };
      items.forEach(addAllChildren);
      return new ObservableArray(commentList);
    },

    openUrl(post) {
      if (post.url) {
        const activity = app.android.startActivity || app.android.foregroundActivity;
        const backArrowId = ad.resources.getDrawableId('ic_arrow_left_white_48dp');
        const slideInRight = ad.resources.getId(':anim/slide_in_right');
        const slideOutLeft = ad.resources.getId(':anim/slide_out_left');
        const slideInLeft = ad.resources.getId(':anim/slide_in_left');
        const slideOutRight = ad.resources.getId(':anim/slide_out_right');
        const backArrow = androidx.core.content.ContextCompat.getDrawable(activity, backArrowId).getBitmap();
        const customTabsIntent = new androidx.browser.customtabs.CustomTabsIntent.Builder()
            .addDefaultShareMenuItem()
            .setShowTitle(true)
            .setStartAnimations(activity, slideInRight, slideOutLeft)
            .setExitAnimations(activity, slideInLeft, slideOutRight)
            .setCloseButtonIcon(backArrow)
            .enableUrlBarHiding()
            .build();
        const customTabsHelper = saschpe.android.customtabs.CustomTabsHelper.Companion;
        const uri = android.net.Uri.parse(post.url);
        const fallback = new saschpe.android.customtabs.WebViewFallback();
        customTabsHelper.addKeepAliveExtra(activity, customTabsIntent.intent);
        customTabsHelper.openCustomTab(activity, customTabsIntent, uri, fallback);
      }
    },

    showMoreOptions(post) {
      action({actions: ['Reply', 'Save', 'Goto ' + post.subreddit, 'Goto /u/' + post.author, 'Copy', 'Share']});
    },
  },
};
</script>

<style scoped>
  ActionBar {
    background-color: #53ba82;
    color: #ffffff;
  }

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

  #comment-list {
    background-color: #080808;
  }

  .comment {
    border-color: #767676;
    border-width: 5px;
    border-radius: 30px;
  }

  .post-author {
    color: #53ba82;
  }

  .author-flair {
    color: #c2c2c2;
  }

  .comment-votes {
    color: #767676;
  }

  .comment-votes {
    color: #767676;
  }

  .comment-created {
    color: #767676;
  }

  .comment-body {
    color: white;
    padding-left: 20px;
  }

  .comment-label {
    background-color: #3e3e3e;
    color: white;
    font-size: 14px;
    margin-top: 20px;
    margin-bottom: 20px;
    width: 100%;
    text-align: center;
  }
</style>
