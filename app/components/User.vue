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
                 @loadMoreDataRequested="onLoadMorePostsRequested"
                 @pullToRefreshInitiated="onPullDown">
      <v-template name="header">
        <SegmentedBar :items="sortings"
                      selectedIndex="0"
                      selectedBackgroundColor="#53ba82"
                      @selectedIndexChange="onSortingChange" />
      </v-template>
      <v-template>
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
                        :style="{color: post.stickied && subreddit && subreddit.created ? '#53ba82' : 'white'}" />
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
import Votes from './Votes';
import Reddit from '../services/Reddit';
import {SegmentedBarItem} from 'tns-core-modules/ui/segmented-bar';
import {ObservableArray} from 'tns-core-modules/data/observable-array';
import Comments from './Comments';
import {action} from 'tns-core-modules/ui/dialogs';
import {LoadingIndicator, Mode} from '@nstudio/nativescript-loading-indicator';

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
          this.lastPostId = items[items.length - 1].name;
          this.postList.push(...items);
          this.$refs.postList.nativeView.refresh();
        }
      });
    },

    getPreview(post) {
      return Reddit.getPreview(post);
    },

    openComments(post) {
      // explode (Android Lollipop(21) and up only), fade,
      // flip (same as flipRight), flipRight, flipLeft,
      // slide (same as slideLeft), slideLeft, slideRight, slideTop, slideBottom
      this.$navigateTo(Comments, {
        transition: 'slide',
        props: {
          post,
        },
      });
    },

    onLongPress(post) {
      action({actions: ['Save', 'Goto /r/' + post.subreddit, 'Goto /u/' + post.author, 'Copy', 'Share']}).then((action) => {
        if (action.startsWith('Goto /r/')) {
          this.app.setSubreddit({display_name: action.split('/r/')[1]});
        } else if (action.startsWith('Goto /u/')) {
          this.app.$navigateTo(User, {
            transition: 'slide',
            props: {
              user: action.split('/u/')[1],
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
