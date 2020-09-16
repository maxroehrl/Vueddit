<template>
  <RadListView id="post-list"
               ref="postList"
               for="post in postList"
               loadOnDemandMode="Auto"
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
    <v-template name="post">
      <PostHeader :post="post"
                  :bigPreview="shouldShowBigPreview(post)"
                  width="100%"
                  :onLongPress="onLongPress"
                  :onTab="openComments" />
    </v-template>
    <v-template name="comment">
      <MarkdownView :text="post.body" />
    </v-template>
  </RadListView>
</template>

<script>
import {ObservableArray} from '@nativescript/core/data/observable-array';
import {SegmentedBarItem} from '@nativescript/core/ui/segmented-bar';
import {LoadingIndicator, Mode} from '@nstudio/nativescript-loading-indicator';
import {action} from '@nativescript/core/ui/dialogs';
import Reddit from '../services/Reddit';
import Comments from './Comments';
import User from './User';
import PostHeader from './PostHeader';
import MarkdownView from './MarkdownView';

export default {
  name: 'Subreddits',
  components: {MarkdownView, PostHeader},
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
      postList: new ObservableArray([]),
      lastPostId: null,
      sortings: Object.values(Reddit.sortings).slice(0, 4).map((sorting) => {
        const item = new SegmentedBarItem();
        item.title = sorting;
        return item;
      }),
      selectedTemplate: null,
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
    templateSelector(item) {
      return (item && item.body) ? 'comment' : 'post';
    },

    shouldShowBigPreview(post) {
      return Reddit.getPreview(post, 300, false) && this.getDefaultTemplate() === 'big';
    },

    getDefaultTemplate() {
      const subHasMoreThanHalfPictures = this.postList
          .slice(2, 10)
          .map((post) => Boolean(post && post.preview && post.preview.images && post.preview.images.length))
          .reduce((a, b) => a + b, 0) > 4;
      const isNotFrontPage = this.subreddit.display_name !== Reddit.frontpage;
      return this.selectedTemplate ? this.selectedTemplate : (subHasMoreThanHalfPictures && isNotFrontPage ? 'big' : 'small');
    },

    toggleTemplate() {
      if (!this.selectedTemplate) {
        this.selectedTemplate = this.getDefaultTemplate();
      }
      if (this.selectedTemplate === 'big') {
        this.selectedTemplate = 'small';
      } else if (this.selectedTemplate === 'small') {
        this.selectedTemplate = 'big';
      }
      if (this.$refs.postList) {
        this.$refs.postList.nativeView._listViewAdapter.notifyDataSetChanged();
      }
    },

    onPullDown(args) {
      this.refresh().finally(() => args.object.notifyPullToRefreshFinished(true));
    },

    onLoadMorePostsRequested(args) {
      this.getPosts(this.lastPostId)
          .then((items) => args.object.notifyAppendItemsOnDemandFinished(items.length, false));
    },

    onSortingChange(args) {
      this.loadingIndicator.show(this.loadingIndicatorOptions);
      this.sorting = this.sortings[args.value].title;
      this.refresh().finally(() => this.loadingIndicator.hide());
    },

    setSubreddit({subreddit, postList, lastPostId, index}) {
      this.selectedTemplate = null;
      this.subreddit = subreddit;
      if (postList && lastPostId && index) {
        this.postList = postList;
        this.lastPostId = lastPostId;
        if (this.$refs.postList) {
          this.$refs.postList.nativeView._listViewAdapter.notifyDataSetChanged();
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
          this.$refs.postList.nativeView._listViewAdapter.notifyDataSetChanged();
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
              return items;
            }
          }
          return [];
        });
      } else {
        return Promise.resolve([]);
      }
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
    background-color: #080808;
  }
</style>
