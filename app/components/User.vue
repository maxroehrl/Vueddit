<template>
  <Page>
    <ActionBar :title="user">
      <NavigationButton text="Back"
                        icon="res://ic_arrow_left"
                        @tap="$navigateBack" />
    </ActionBar>
    <RadListView id="post-list"
                 ref="postList"
                 for="post in postList"
                 loadOnDemandMode="Auto"
                 loadOnDemandBufferSize="5"
                 pullToRefresh="true"
                 :itemTemplateSelector="templateSelector"
                 @loadMoreDataRequested="onLoadMorePostsRequested"
                 @pullToRefreshInitiated="onPullDown">
      <v-template name="header">
        <SegmentedBar :items="sortings"
                      selectedIndex="0"
                      selectedBackgroundColor="#53ba82"
                      @selectedIndexChange="onSortingChange" />
      </v-template>
      <v-template name="big">
        <StackLayout>
          <FlexboxLayout flexDirection="row"
                         alignSelf="flex-start"
                         justifyContent="flex-start">
            <Votes :post="post" width="15%" />
            <Ripple rippleColor="#53ba82"
                    width="85%"
                    @longPress="onLongPress(post)"
                    @tap="openComments(post)">
              <Label textWrap="true">
                <FormattedString>
                  <Span :style="{'background-color': post.link_flair_background_color || defaultFlairColor}"
                        :text="post.link_flair_text"
                        class="post-flair" />
                  <Span :text="post.link_flair_text ? ' ' + post.title : post.title"
                        class="post-title"
                        :style="{color: post.stickied ? '#53ba82' : 'white'}" />
                  <Span :text="' (' + post.domain + ') \n'" class="post-domain" />
                  <Span :text="post.over_18 ? 'nsfw ' : ''" class="post-nsfw" />
                  <Span :text="post.spoiler ? 'spoiler ' : ''" class="post-spoiler" />
                  <Span :text="post.num_comments + ' comments '" class="post-num-comments" />
                  <Span :text="post.subreddit" class="post-subreddit" />
                </FormattedString>
              </Label>
            </Ripple>
          </FlexboxLayout>
          <Ripple rippleColor="#53ba82"
                  width="100%"
                  @longPress="onLongPress(post)"
                  @tap="openComments(post)">
            <Image :src="getImage(post)"
                   stretch="aspectFit"
                   class="post-image"
                   loadMode="async" />
          </Ripple>
        </StackLayout>
      </v-template>
      <v-template name="small">
        <FlexboxLayout flexDirection="row"
                       alignSelf="flex-start"
                       justifyContent="flex-start">
          <Votes :post="post" />
          <Ripple rippleColor="#53ba82"
                  width="80%"
                  @longPress="onLongPress(post)"
                  @tap="openComments(post)">
            <FlexboxLayout flexDirection="row"
                           alignSelf="flex-start"
                           justifyContent="flex-start">
              <Label textWrap="true"
                     style="width: 80%">
                <FormattedString>
                  <Span :style="{'background-color': post.link_flair_background_color || defaultFlairColor}"
                        :text="post.link_flair_text"
                        class="post-flair" />
                  <Span :text="post.link_flair_text ? ' ' + post.title : post.title"
                        class="post-title"
                        :style="{color: post.stickied ? '#53ba82' : 'white'}" />
                  <Span :text="' (' + post.domain + ') \n'" class="post-domain" />
                  <Span :text="post.over_18 ? 'nsfw ' : ''" class="post-nsfw" />
                  <Span :text="post.spoiler ? 'spoiler ' : ''" class="post-spoiler" />
                  <Span :text="post.num_comments + ' comments '" class="post-num-comments" />
                  <Span :text="post.subreddit" class="post-subreddit" />
                </FormattedString>
              </Label>
              <Image :src="getPreview(post)"
                     stretch="aspectFit"
                     width="20%"
                     height="300px"
                     loadMode="async" />
            </FlexboxLayout>
          </Ripple>
        </FlexboxLayout>
      </v-template>
    </RadListView>
  </Page>
</template>

<script>
import {SegmentedBarItem} from 'tns-core-modules/ui/segmented-bar';
import {ObservableArray} from 'tns-core-modules/data/observable-array';
import {action} from 'tns-core-modules/ui/dialogs';
import {LoadingIndicator, Mode} from '@nstudio/nativescript-loading-indicator';
import Reddit from '../services/Reddit';
import Comments from './Comments';
import Votes from './Votes';

export default {
  name: 'User',
  components: {Votes},
  props: {
    user: {
      type: String,
      required: true,
    },
    app: {
      type: Object,
      required: true,
    },
  },
  data() {
    return {
      defaultFlairColor: '#767676',
      postList: new ObservableArray([]),
      lastPostId: null,
      sortings: ['new', 'top', 'hot', 'controversial'].map((sorting) => {
        const item = new SegmentedBarItem();
        item.title = sorting;
        return item;
      }),
      sorting: 'new',
      loadingIndicator: new LoadingIndicator(),
      loadingIndicatorOptions: {
        hideBezel: true,
        color: '#53ba82',
        mode: Mode.Indeterminate,
      },
    };
  },
  methods: {
    templateSelector(item, index, items) {
      const subHasMoreThanHalfPictures = items
          .slice(0, 10)
          .map((post) => Boolean(post && post.preview && post.preview.images && post.preview.images.length))
          .reduce((a, b) => a + b, 0) > 5;
      const itemHasPreview = Boolean(Reddit.getPreview(item, 300, false));
      return subHasMoreThanHalfPictures && itemHasPreview ? 'big' : 'small';
    },

    onPullDown(args) {
      this.refresh().finally(() => args.object.notifyPullToRefreshFinished(true));
    },

    onLoadMorePostsRequested(args) {
      this.getPosts(this.lastPostId)
          .then(() => args.object.notifyAppendItemsOnDemandFinished(this.postList.length, false));
    },

    onSortingChange(args) {
      this.loadingIndicator.show(this.loadingIndicatorOptions);
      this.sorting = this.sortings[args.value].title;
      this.refresh().finally(() => this.loadingIndicator.hide());
    },

    refresh() {
      this.postList = new ObservableArray([]);
      this.lastPostId = null;
      return this.getPosts();
    },

    getPosts(lastPostId) {
      return Reddit.getUserPosts(this.user, lastPostId, this.sorting).then((r) => {
        if (r && r.data && r.data.children) {
          const items = r.data.children.map((d) => d.data);
          if (items.length) {
            this.lastPostId = items[items.length - 1].name;
            this.postList.push(...items);
            this.$refs.postList.nativeView.refresh();
          }
        }
      });
    },

    getPreview(post) {
      return Reddit.getPreview(post);
    },

    getImage(post) {
      return Reddit.getImage(post);
    },

    openComments(post) {
      this.$navigateTo(Comments, {
        transition: 'slide',
        props: {
          post,
          app: this.app,
        },
      });
    },

    onLongPress(post) {
      const actions = [post.saved ? 'Unsave' : 'Save'];
      if (post.subreddit_type !== 'user') {
        actions.push('Goto /r/' + post.subreddit);
      }
      action({actions}).then((action) => {
        if (action === 'Save') {
          const promise = post.saved ? Reddit.unsave(post.name) : Reddit.save(post.name);
          promise.then(() => post.saved = !post.saved);
        } else if (action.startsWith('Goto /r/')) {
          this.$navigateBack();
          this.app.setSubreddit({display_name: post.subreddit});
        }
      });
    },
  },
};
</script>

<style scoped>
  ActionBar {
    background-color: #3e3e3e;
    color: #ffffff;
  }

  #post-list {
    height: 100%;
    width: 100%;
    background-color: #080808;
    separator-color: #ff0000;
  }

  .post-flair {
    font-size: 12px;
    color: #f3f3f3;
  }

  .post-title {
    color: white;
    font-size: 14px;
  }

  .post-num-comments {
    color: #767676;
  }

  .post-domain {
    color: #767676;
  }

  .post-subreddit {
    color: #767676;
  }

  .post-nsfw {
    color: red;
  }

  .post-spoiler {
    color: yellow;
  }
</style>
