<template>
  <RadListView id="post-list"
               ref="postList"
               for="post in postList"
               loadOnDemandMode="Auto"
               pullToRefresh="true"
               :itemTemplateSelector="templateSelector"
               @loaded="loaded"
               @loadMoreDataRequested="onLoadMorePostsRequested"
               @pullToRefreshInitiated="onPullDown">
    <v-template name="header">
      <SegmentedBar :items="sortings"
                    :backgroundColor="'#303030'"
                    :on-selection="setSorting" />
    </v-template>
    <v-template name="post">
      <PostHeader :post="post"
                  :bigPreview="shouldShowBigPreview(post)"
                  width="100%"
                  :onLongPress="onLongPress"
                  :onTab="openComments" />
    </v-template>
    <v-template name="comment">
      <Comment :comment="post"
               :show-subreddit="true"
               :show-more-dialog="app.showMoreDialog"
               :select-comment="openComments"
               :markdown-cache="markdownCache" />
    </v-template>
  </RadListView>
</template>

<script>
import {ObservableArray} from '@nativescript/core/data/observable-array';
import {LoadingIndicator, Mode} from '@nstudio/nativescript-loading-indicator';
import {action} from '@nativescript/core/ui/dialogs';
import Reddit from '../services/Reddit';
import PostHeader from './PostHeader';
import Comment from './Comment';
import showSnackbar from './Snackbar';
import SegmentedBar from './SegmentedBar';

export default {
  name: 'Posts',
  components: {SegmentedBar, Comment, PostHeader},
  props: {
    subreddit: {
      type: Object,
      required: true,
    },
    app: {
      type: Object,
      required: true,
    },
    sortings: {
      type: Array,
      required: true,
    },
    group: {
      type: String,
      required: false,
      default: 'submitted',
    },
    type: {
      type: String,
      required: false,
      default: 'all',
    },
  },
  data() {
    return {
      postList: new ObservableArray([]),
      lastPostId: null,
      selectedTemplate: null,
      sorting: this.sortings[0],
      time: 'all',
      markdownCache: {},
      loadingIndicator: new LoadingIndicator(),
      loadingIndicatorOptions: {
        hideBezel: true,
        color: '#53ba82',
        mode: Mode.Indeterminate,
      },
    };
  },
  methods: {
    loaded() {
      if (this.$refs?.postList?.nativeView?._listViewAdapter) {
        this.$refs.postList.nativeView._listViewAdapter.stateRestorationPolicy =
          androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY;
      }
    },

    templateSelector(item) {
      return item?.body ? 'comment' : 'post';
    },

    shouldShowBigPreview(post) {
      return !!(Reddit.getPreview(post, 300, false) && this.getDefaultTemplate() === 'big');
    },

    getDefaultTemplate() {
      const subHasMoreThanHalfPictures = this.postList
          .slice(2, 10)
          .map((post) => Boolean(post?.preview?.images?.length))
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

    setSorting(sorting, noReload) {
      this.sorting = sorting;
      if (!noReload) {
        let promise = Promise.resolve();
        if (['top', 'rising'].includes(sorting)) {
          const actions = ['Hour', 'Day', 'Week', 'Month', 'Year', 'All'];
          promise = action({title: 'Choose time:', actions});
        }
        promise.then((time) => {
          this.time = time || 'all';
          this.refreshWithLoadingIndicator();
        });
      }
    },

    isUserReddit() {
      return !!this.subreddit.user;
    },

    setSubreddit({postList, lastPostId, index}) {
      this.selectedTemplate = null;
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

    refreshWithLoadingIndicator() {
      this.loadingIndicator.show(this.loadingIndicatorOptions);
      this.refresh().finally(() => this.loadingIndicator.hide());
    },

    refresh() {
      this.postList = new ObservableArray([]);
      this.markdownCache = {};
      this.lastPostId = null;
      return this.getPosts().then(() => {
        if (this.$refs.postList) {
          this.$refs.postList.nativeView._listViewAdapter.notifyDataSetChanged();
        }
      });
    },

    getPosts(lastPostId) {
      let sub;
      if (this.isUserReddit()) {
        sub = this.subreddit.user;
      } else if (this.subreddit.subreddits) {
        sub = this.subreddit.subreddits.map((s) => s.name).join('+');
      } else {
        sub = this.subreddit.display_name;
      }
      if (sub && sub !== '') {
        const request = this.isUserReddit() ? Reddit.getUserPosts : Reddit.getSubredditPosts;
        const args = [sub, lastPostId, this.sorting, this.group, this.time, this.type];
        return request.apply(Reddit, args.map((s) => s?.toLowerCase())).then((r) => {
          if (r?.data?.children?.map) {
            const items = r.data.children.map((d) => d.data);
            if (items.length) {
              this.lastPostId = items[items.length - 1].name;
              items.forEach((post) => post.shown_comments = post.num_comments);
              this.postList.push(...items);
              return items;
            } else if (!lastPostId) {
              showSnackbar({snackText: 'No items received'});
            }
          }
          return [];
        });
      } else {
        return Promise.resolve([]);
      }
    },

    openComments(postOrComment) {
      this.app.openComments(postOrComment.body ? postOrComment.permalink : postOrComment);
    },

    onLongPress(post) {
      this.app.showMoreDialog(post, true, !this.isUserReddit());
    },
  },
};
</script>

<style scoped>
  #post-list {
    background-color: #080808;
  }
</style>
