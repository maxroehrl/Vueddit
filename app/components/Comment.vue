<template>
  <StackLayout :style="{marginLeft: ((comment.depth * 70) + 'px')}" class="comment">
    <Label textWrap="true">
      <FormattedString>
        <Span :text="comment.author_flair_text"
              class="comment-author-flair"
              :style="{'background-color': comment.author_flair_background_color || defaultFlairColor}" />
        <Span :text="(comment.author_flair_text ? ' ' : '') + comment.author + ' '"
              :style="{color: getUserColor(comment, post)}" />
        <Span :text="comment.ups + ' points '" class="comment-votes" />
        <Span :text="getHours(comment.created) + ' hours ago'" class="comment-created" />
      </FormattedString>
    </Label>
    <MarkdownView class="comment-body" :text="comment.body" />
  </StackLayout>
</template>

<script>
import MarkdownView from './MarkdownView';

export default {
  name: 'Comment',
  components: {MarkdownView},
  props: {
    comment: {
      type: Object,
      required: true,
    },
    post: {
      type: Object,
      required: true,
    },
  },
  data() {
    return {
      defaultFlairColor: '#767676',
    };
  },
  methods: {
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
  },
};
</script>

<style scoped>
  .comment {
    border-color: #767676;
    border-width: 6px;
    border-radius: 30px;
  }

  .comment-author-flair {
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
</style>
