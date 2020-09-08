<template>
  <AbsoluteLayout>
    <IndentedLabel ref="labelHeader"
                   class="comment-author"
                   @loaded="loadedHeader($event)">
      <FormattedString>
        <Span :text="comment.author + ' '"
              :style="{color: getUserColor(comment)}" />
        <Span :text="comment.ups + ' points '"
              class="comment-votes" />
        <Span :text="getTimeFromNow(comment) + ' '"
              class="comment-created" />
        <Span :text="comment.gildings && comment.gildings.gid_1 ? ('🥈x' + comment.gildings.gid_1 + ' ') : ''" />
        <Span :text="comment.gildings && comment.gildings.gid_2 ? ('🥇x' + comment.gildings.gid_2 + ' ') : ''" />
        <Span :text="comment.gildings && comment.gildings.gid_3 ? ('🥉x' + comment.gildings.gid_3 + ' ') : ''" />
        <Span :text="comment.author_flair_text ? comment.author_flair_text : ''"
              class="comment-author-flair"
              :style="{'background-color': comment.author_flair_background_color || '#767676'}" />
      </FormattedString>
    </IndentedLabel>
    <IndentedLabel ref="label"
                   class="comment-body"
                   textWrap="true"
                   top="18"
                   width="100%"
                   @loaded="loaded($event)" />
  </AbsoluteLayout>
</template>

<script>
import Reddit from '../services/Reddit';
import Markdown from '../services/Markdown';

export default {
  name: 'Comment',
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
      isInitialized: false,
    };
  },
  watch: {
    $props: {
      handler(props) {
        if (this.$refs.label && props.comment) {
          this.refreshLabel(this.$refs.label.nativeView.android, props.comment);
        }
        if (this.$refs.labelHeader && props.comment) {
          this.$refs.labelHeader.nativeView.android.setDepth(props.comment.depth);
        }
      },
      immediate: true,
      deep: true,
    },
  },
  methods: {
    loaded(event) {
      if (!this.isInitialized) {
        Markdown.setSpannableFactory(event.object.nativeView);
        this.isInitialized = true;
      }
      if (this.comment) {
        this.refreshLabel(event.object.nativeView, this.comment);
      }
    },

    loadedHeader(event) {
      if (this.comment) {
        event.object.nativeView.setDepth(this.comment.depth);
      }
    },

    refreshLabel(tv, comment) {
      tv.setDepth(comment.depth);
      Markdown.setMarkdown(tv, comment.body);
    },

    getTimeFromNow(comment) {
      return Reddit.getTimeFromNow(comment);
    },

    getUserColor(comment) {
      if (comment.is_submitter) {
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
  .comment-author {
    font-size: 13px;
  }

  .comment-body {
    font-size: 13px;
  }

  .comment-author-flair {
    color: #c2c2c2;
  }

  .comment-votes {
    color: #767676;
  }

  .comment-created {
    color: #767676;
  }
</style>
