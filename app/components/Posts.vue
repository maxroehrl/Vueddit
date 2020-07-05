<template>
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
                      :text="post.link_flair_text ? post.link_flair_text : ''"
                      class="post-flair" />
                <Span :text="post.link_flair_text ? ' ' + post.title : post.title"
                      class="post-title"
                      :style="{color: post.stickied && subreddit.created ? '#53ba82' : 'white'}" />
                <Span :text="' (' + post.domain + ') \n'"
                      class="post-domain" />
                <Span :text="post.over_18 ? 'nsfw ' : ''"
                      class="post-nsfw" />
                <Span :text="post.spoiler ? 'spoiler ' : ''"
                      class="post-spoiler" />
                <Span :text="post.num_comments + ' comment' + (post.num_comments === 1 ? ' ' : 's ')"
                      class="post-num-comments" />
                <Span :text="post.subreddit"
                      class="post-subreddit" />
              </FormattedString>
            </Label>
          </Ripple>
        </FlexboxLayout>
        <Ripple rippleColor="#53ba82"
                width="100%"
                @longPress="onLongPress(post)"
                @tap="openComments(post)">
          <Image :src="getImage(post)"
                 :style="{height: getImageHeight(post)}"
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
                      :text="post.link_flair_text ? post.link_flair_text : ''"
                      class="post-flair" />
                <Span :text="post.link_flair_text ? ' ' + post.title : post.title"
                      class="post-title"
                      :style="{color: post.stickied && subreddit.created ? '#53ba82' : 'white'}" />
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
</template>

<script>
import {ObservableArray} from 'tns-core-modules/data/observable-array';
import {SegmentedBarItem} from 'tns-core-modules/ui/segmented-bar';
import {LoadingIndicator, Mode} from '@nstudio/nativescript-loading-indicator';
import {action} from 'tns-core-modules/ui/dialogs';
import Reddit from '../services/Reddit';
import Votes from './Votes';
import Comments from './Comments';
import User from './User';

export default {
  name: 'Subreddits',
  components: {
    Votes,
  },
  props: {
    subreddit: {
      type: Object,
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
      sortings: Object.values(Reddit.sortings).slice(0, 4).map((sorting) => {
        const item = new SegmentedBarItem();
        item.title = sorting;
        return item;
      }),
      sorting: 'best',
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
      const isNotFrontPage = this.subreddit.display_name !== Reddit.frontpage;
      return subHasMoreThanHalfPictures && isNotFrontPage && itemHasPreview ? 'big' : 'small';
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

    setSubreddit({subreddit, postList, lastPostId, index}) {
      this.subreddit = subreddit;
      if (postList && lastPostId && index) {
        this.postList = postList;
        this.lastPostId = lastPostId;
        if (this.$refs.postList) {
          this.$refs.postList.nativeView.refresh();
          setTimeout(() => this.$refs.postList.scrollToIndex(index));
        }
        return Promise.resolve();
      } else {
        return this.refresh();
      }
    },

    refresh() {
      this.postList = new ObservableArray([]);
      this.lastPostId = null;
      return this.getPosts().then(() => {
        if (this.$refs.postList) {
          this.$refs.postList.nativeView.refresh();
        }
      });
    },

    getPosts(lastPostId) {
      let sub;
      if (this.subreddit.subreddits) {
        sub = this.subreddit.subreddits.map((s) => s.name).join('+');
      } else {
        sub = this.subreddit.display_name;
      }
      if (sub && sub !== '') {
        return Reddit.getPosts(sub, lastPostId, this.sorting).then((r) => {
          if (r && r.data && r.data.children) {
            const items = r.data.children.map((d) => d.data);
            if (items.length) {
              this.lastPostId = items[items.length - 1].name;
              this.postList.push(...items);
            }
          }
        });
      } else {
        return Promise.resolve();
      }
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

    openComments(post) {
      // explode (Android Lollipop(21) and up only), fade,
      // flip (same as flipRight), flipRight, flipLeft,
      // slide (same as slideLeft), slideLeft, slideRight, slideTop, slideBottom
      this.$navigateTo(Comments, {
        transition: 'slide',
        props: {
          app: this.app,
          post,
        },
      });
    },

    onLongPress(post) {
      const actions = [
        post.saved ? 'Unsave' : 'Save',
        'Goto /u/' + post.author,
      ];
      if (post.subreddit !== this.subreddit.display_name) {
        actions.push('Goto /r/' + post.subreddit);
      }
      action({actions}).then((action) => {
        if (action === 'Save') {
          const promise = post.saved ? Reddit.unsave(post.name) : Reddit.save(post.name);
          promise.then(() => post.saved = !post.saved);
        } else if (action.startsWith('Goto /r/')) {
          this.app.setSubreddit({display_name: post.subreddit});
        } else if (action.startsWith('Goto /u/')) {
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
  #post-list {
    height: 100%;
    width: 100%;
    background-color: #080808;
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
