<template>
  <Page @loaded="loaded" @unloaded="unloaded">
    <ActionBar :title="post.title">
      <NavigationButton text="Back"
                        icon="res://ic_arrow_left"
                        @tap="$navigateBack" />
    </ActionBar>
    <RadListView id="comment-list"
                 ref="commentList"
                 for="comment in commentList"
                 pullToRefresh="true"
                 :itemTemplateSelector="templateSelector"
                 @pullToRefreshInitiated="onPullDown">
      <v-template name="header">
        <Post :post="post" :app="app" />
      </v-template>
      <v-template name="comment">
        <Comment :comment="comment" :post="post" />
      </v-template>
      <v-template name="more">
        <More :comment="comment" :on-click="loadMore" />
      </v-template>
    </RadListView>
  </Page>
</template>

<script>
import * as application from 'tns-core-modules/application';
import {AndroidApplication} from 'tns-core-modules/application';
import {ObservableArray} from 'tns-core-modules/data/observable-array';
import Reddit from '../services/Reddit';
import Comment from './Comment';
import Post from './Post';
import More from './More';

export default {
  name: 'Comments',
  components: {Post, Comment, More},
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
      commentList: new ObservableArray([]),
      isShowingSubtree: false,
    };
  },
  methods: {
    templateSelector(item, index, items) {
      return item.body ? 'comment' : 'more';
    },

    onPullDown(args) {
      this.getComments().finally(() => args.object.notifyPullToRefreshFinished(true));
    },

    loaded() {
      if (!this.commentList.length) {
        this.getComments();
      }
      application.android.on(AndroidApplication.activityBackPressedEvent, this.navigateBack, this);
    },

    unloaded() {
      application.android.off(AndroidApplication.activityBackPressedEvent, this.navigateBack, this);
    },

    navigateBack(data) {
      if (this.isShowingSubtree) {
        this.isShowingSubtree = false;
        this.getComments();
        data.cancel = true;
      }
    },

    getComments() {
      return this.fetchComments().then((items) => {
        this.commentList = this.processComments(items);
        this.refreshCommentList();
      });
    },

    fetchComments(comment) {
      return Reddit.getComments(this.post.permalink, comment).then((r) => {
        if (r && r.length === 2 && r[1].data) {
          return r[1].data.children.map((d) => d.data);
        }
        return [];
      });
    },

    refreshCommentList() {
      setTimeout(() => {
        if (this.$refs.commentList) {
          this.$refs.commentList.nativeView.refresh();
        }
      });
    },

    processComments(items) {
      const commentList = [];
      const addAllChildren = (comment) => {
        commentList.push(comment);
        if (comment.replies && comment.replies !== '') {
          comment.replies.data.children.map((d) => d.data).forEach(addAllChildren);
        }
      };
      items.forEach(addAllChildren);
      return new ObservableArray(commentList);
    },

    loadMore(comment) {
      if (comment.count === 0) {
        return this.fetchComments(comment.parent_id.split('_')[1]).then((items) => {
          this.isShowingSubtree = true;
          this.commentList = this.processComments(items);
          this.refreshCommentList();
        });
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
