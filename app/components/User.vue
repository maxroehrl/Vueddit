<template>
  <Page>
    <ActionBar :title="user">
      <NavigationButton text="Back"
                        icon="res://ic_arrow_left"
                        @tap="$navigateBack" />
      <ActionItem text="Refresh"
                  icon="res://ic_menu_refresh"
                  @tap="refreshPosts()" />
      <ActionItem text="Toggle list item size"
                  android.position="popup"
                  @tap="toggleTemplate" />
    </ActionBar>
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
        <MarkdownView :text="post.comment" />
      </v-template>
    </RadListView>
  </Page>
</template>

<script>
import {SegmentedBarItem} from '@nativescript/core/ui/segmented-bar';
import {ObservableArray} from '@nativescript/core/data/observable-array';
import {action} from '@nativescript/core/ui/dialogs';
import {LoadingIndicator, Mode} from '@nstudio/nativescript-loading-indicator';
import Reddit from '../services/Reddit';
import Comments from './Comments';
import MarkdownView from './MarkdownView';
import PostHeader from './PostHeader';

export default {
  name: 'User',
  components: {MarkdownView, PostHeader},
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
      postList: new ObservableArray([]),
      lastPostId: null,
      sortings: ['new', 'top', 'hot', 'controversial'].map((sorting) => {
        const item = new SegmentedBarItem();
        item.title = sorting;
        return item;
      }),
      selectedTemplate: null,
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
    templateSelector(item) {
      return item.body ? 'comment' : 'post';
    },

    shouldShowBigPreview(post) {
      return Reddit.getPreview(post, 300, false) && this.getDefaultTemplate() === 'big';
    },

    getDefaultTemplate() {
      const subHasMoreThanHalfPictures = this.postList
          .slice(2, 10)
          .map((post) => Boolean(post && post.preview && post.preview.images && post.preview.images.length))
          .reduce((a, b) => a + b, 0) > 4;
      return this.selectedTemplate ? this.selectedTemplate : (subHasMoreThanHalfPictures ? 'big' : 'small');
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
        this.$refs.postList.nativeView.refresh();
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
      this.sorting = this.sortings[args.value].title;
      this.refreshPosts();
    },

    refreshPosts() {
      this.loadingIndicator.show(this.loadingIndicatorOptions);
      this.refresh().finally(() => this.loadingIndicator.hide());
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
      return Reddit.getUserPosts(this.user, lastPostId, this.sorting).then((r) => {
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
    background-color: #080808;
  }
</style>
