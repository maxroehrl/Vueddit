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
        <Post :post="post" />
      </v-template>
      <v-template>
        <Comment :comment="comment" :post="post" />
      </v-template>
    </RadListView>
  </Page>
</template>

<script>
import {ObservableArray} from 'tns-core-modules/data/observable-array';
import Reddit from '../services/Reddit';
import Comment from './Comment';
import Post from './Post';

export default {
  name: 'Comments',
  components: {Post, Comment},
  props: {
    post: {
      type: Object,
      required: true,
    },
  },
  data() {
    return {
      commentList: new ObservableArray([]),
    };
  },
  methods: {
    onPullDown(args) {
      this.getComments().finally(() => args.object.notifyPullToRefreshFinished(true));
    },

    loaded() {
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
