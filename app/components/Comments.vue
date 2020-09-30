<template>
  <Page @loaded="loaded" @unloaded="unloaded">
    <ActionBar :title="isShowingSubtree ? 'Single comment thread' : post.title">
      <NavigationButton text="Back"
                        icon="res://ic_arrow_left"
                        @tap="isShowingSubtree ? navigateBack({}) : $navigateBack()" />
      <ActionItem text="Refresh"
                  icon="res://ic_menu_refresh"
                  @tap="getComments()" />
      <ActionItem text="Show sidebar"
                  android.position="popup"
                  @tap="app.showSidebar({display_name: post.subreddit})" />
    </ActionBar>
    <RadListView id="comment-list"
                 ref="commentList"
                 for="comment in commentList"
                 pullToRefresh="true"
                 :itemTemplateSelector="templateSelector"
                 @pullToRefreshInitiated="onPullDown">
      <v-template name="header">
        <StackLayout padding="0">
          <Post :post="post" :app="app" />
          <SegmentedBar :items="sortings"
                        :padding="'40px'"
                        marginBottom="20px"
                        :on-selection="setSorting" />
        </StackLayout>
      </v-template>
      <v-template name="comment">
        <Comment :comment="comment"
                 :selected="comment === selectedComment"
                 :show-more-dialog="app.showMoreDialog"
                 :select-comment="selectComment"
                 :select-neighboring-comment="selectNeighboringComment"
                 :collapse="collapse"
                 :markdown-cache="markdownCache" />
      </v-template>
      <v-template name="more">
        <More :comment="comment" :on-click="loadMore" />
      </v-template>
      <v-template name="load">
        <ActivityIndicator busy="true" color="#53ba82" />
      </v-template>
    </RadListView>
  </Page>
</template>

<script>
import * as application from '@nativescript/core/application';
import {AndroidApplication} from '@nativescript/core/application';
import {ObservableArray} from '@nativescript/core/data/observable-array';
import Reddit from '../services/Reddit';
import Comment from './Comment';
import Post from './Post';
import More from './More';
import SegmentedBar from './SegmentedBar';
import Markdown from '../services/Markdown';
import showSnackbar from './Snackbar';

export default {
  name: 'Comments',
  components: {SegmentedBar, Post, Comment, More},
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
  data() {
    return {
      commentList: new ObservableArray([{loading: true}]),
      markdownCache: {},
      isShowingSubtree: false,
      selectedComment: null,
      sortings: ['top', 'new', 'controversial', 'old', 'random', 'qa'],
      sorting: 'top',
    };
  },
  methods: {
    templateSelector(item) {
      return (item && item.loading) ? 'load' : (item && item.body) ? 'comment' : 'more';
    },

    onPullDown(args) {
      this.getComments().finally(() => args.object.notifyPullToRefreshFinished(true));
    },

    loaded() {
      if (this.commentList.getItem(0) && this.commentList.getItem(0).loading) {
        this.getComments();
      }
      application.android.on(AndroidApplication.activityBackPressedEvent, this.navigateBack, this);
    },

    unloaded() {
      application.android.off(AndroidApplication.activityBackPressedEvent, this.navigateBack, this);
    },

    navigateBack(data) {
      if (this.isShowingSubtree) {
        this.getComments();
        data.cancel = true;
      }
    },

    getComments(comment='') {
      this.commentList = new ObservableArray([{loading: true}]);
      this.post.shown_comments = 0;
      return Reddit.getPostAndComments(this.post.permalink, comment, this.sorting).then(({comments}) => {
        this.isShowingSubtree = comment !== '';
        this.selectedComment = null;
        this.commentList.splice(0, 1, ...this.processComments(comments));
        this.post.shown_comments = this.commentList.length;
        this.$refs.commentList.nativeView.refresh();
        if (!this.commentList.length) {
          showSnackbar({snackText: 'No comments'});
        }
      });
    },

    refreshCommentList() {
      setTimeout(() => {
        if (this.$refs.commentList) {
          this.$refs.commentList.nativeView._listViewAdapter.notifyDataSetChanged();
        }
      });
    },

    processComments(items) {
      const commentList = [];
      const addAllChildren = (comment) => {
        commentList.push(comment);
        if (comment.body && (comment.edited || !this.markdownCache[comment.name])) {
          this.markdownCache[comment.name] = Markdown.toMarkdown(comment.body);
        }
        if (comment.replies && comment.replies !== '') {
          comment.replies.data.children.map((d) => d.data).forEach(addAllChildren);
        }
        delete comment.replies;
      };
      items.forEach(addAllChildren);
      return commentList;
    },

    loadMore(comment) {
      if (comment.count === 0) {
        return this.getComments(comment.parent_id.split('_')[1]);
      } else {
        return Reddit.getMoreComments(this.post.name, comment.children).then((r) => {
          if (r && r.json && r.json.data && r.json.data.things) {
            const items = r.json.data.things.map((r) => r.data);
            const index = this.commentList.indexOf(comment);
            this.commentList.splice(index, 1, ...items);
            this.refreshCommentList();
          }
        });
      }
    },

    selectComment(comment, scrollTo=false) {
      if (comment !== this.selectedComment) {
        const oldComment = this.selectedComment;
        this.selectedComment = comment;
        this.notifyCommentChanged(oldComment, false);
        this.notifyCommentChanged(comment, scrollTo);
      }
    },

    notifyCommentChanged(comment, scrollTo) {
      if (comment) {
        const index = this.commentList.indexOf(comment);
        if (index >= 0 && index < this.commentList.length) {
          this.$refs.commentList.nativeView._listViewAdapter.notifyItemChanged(index);
          if (scrollTo) {
            this.$refs.commentList.scrollToIndex(index, false, 'Start');
          }
        }
      }
    },

    selectNeighboringComment(comment, depth, next) {
      let commentCandidates;
      const index = this.commentList.indexOf(comment);
      if (next) {
        commentCandidates = this.commentList.slice(index + 1, this.commentList.length);
      } else {
        commentCandidates = this.commentList.slice(0, index);
      }
      commentCandidates = commentCandidates.filter((c) => c.depth === depth);
      if (commentCandidates.length !== 0) {
        const newlySelectedComment = commentCandidates[next ? 0 : commentCandidates.length - 1];
        if (newlySelectedComment) {
          this.selectComment(newlySelectedComment, true);
        }
      }
    },

    collapse(comment) {
      const index = this.commentList.indexOf(comment);
      if (comment.children) {
        // Restore current and child comments
        if (comment.children.length) {
          this.commentList.splice(index + 1, 0, ...comment.children);
          delete comment.children;
          this.refreshCommentList();
        } else {
          delete comment.children;
        }
        this.selectComment(comment);
      } else {
        // Collapse current and child comments
        let nextIndex = index;
        for (let i = index + 1; i < this.commentList.length; i++) {
          if (this.commentList.getItem(i).depth <= comment.depth) {
            nextIndex = i;
            break;
          }
        }
        if (nextIndex - index > 1) {
          comment.children = this.commentList.splice(index + 1, nextIndex - index - 1);
          this.refreshCommentList();
        } else {
          comment.children = [];
        }
        this.selectComment(null);
      }
    },

    setSorting(sorting) {
      if (sorting !== this.sorting) {
        this.sorting = sorting;
        this.getComments();
      }
    },
  },
};
</script>

<style scoped>
  ActionBar {
    background-color: #3e3e3e;
    color: #ffffff;
  }

  #comment-list {
    background-color: #080808;
  }
</style>
