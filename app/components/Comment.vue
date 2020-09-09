<template>
  <AbsoluteLayout class="comment"
                  :style="{borderWidth: isSelectedComment() ? '6px' : '0 6px',
                           borderColor: isSelectedComment() ? '#53ba82' : '#53ba82 #080808'}">
    <FlexboxLayout v-if="isSelectedComment()" class="button-bar">
      <Votes :voteable="comment" />
      <Label text="Done"
             class="button-bar-label"
             @tap="setSelectedComment(null)" />
      <Label :text="comment.depth === 0 ? '▲ Prev' : '▲ Parent'"
             class="button-bar-label"
             @tap="selectOther(0)" />
      <Label :text="comment.depth === 0 ? 'Next ▼' : '▲ Root'"
             class="button-bar-label"
             @tap="selectOther(1)" />
    </FlexboxLayout>
    <IndentedLabel ref="labelHeader"
                   class="comment-author"
                   :top="isSelectedComment() ? buttonBarHeight.toString() : '0'"
                   @tap="setSelectedComment(comment)"
                   @loaded="loadedHeader($event)">
      <FormattedString>
        <Span :text="comment.likes == null ? '' : (comment.likes ? '▲' : '▼')"
              :style="{color: comment.likes ? '#53ba82' : '#bf5826'}" />
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
                   :top="((isSelectedComment() ? buttonBarHeight : 0) + 18).toString()"
                   width="100%"
                   @tap="setSelectedComment(comment)"
                   @loaded="loaded($event)" />
  </AbsoluteLayout>
</template>

<script>
import Reddit from '../services/Reddit';
import Markdown from '../services/Markdown';
import Votes from './Votes';

export default {
  name: 'Comment',
  components: {Votes},
  props: {
    comment: {
      type: Object,
      required: true,
    },
    post: {
      type: Object,
      required: true,
    },
    deselectSelectedComment: {
      type: Function,
      required: true,
    },
    selectOtherComment: {
      type: Function,
      required: true,
    },
  },
  data() {
    return {
      isInitialized: false,
      selectedComment: null,
      buttonBarHeight: 90,
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
      this.setSelectedComment(comment.isSelected ? comment : null);
    },

    setSelectedComment(comment) {
      if (comment && comment !== this.selectedComment) {
        this.deselectSelectedComment(this.setSelectedComment.bind(this, null));
        comment.isSelected = true;
      }
      if (comment == null) {
        this.comment.isSelected = false;
      }
      this.selectedComment = comment;
    },

    isSelectedComment() {
      return this.selectedComment === this.comment;
    },

    selectOther(buttonIndex) {
      let depth = this.comment.depth;
      const next = buttonIndex === 1 && depth === 0;
      depth = (buttonIndex === 1 || depth === 0) ? 0 : depth - 1;
      this.selectOtherComment(this.comment, depth, next, this.setSelectedComment.bind(this, null));
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
  .comment {
    border-radius: 30px;
  }

  .button-bar {
    padding: 0 40px;
    width: 100%;
    flex-direction: row;
    justify-content: space-between;
    border-bottom-color: #53ba82;
    border-bottom-width: 6px;
  }

  .button-bar-label {
    align-self: center;
    font-size: 13px;
    padding: 40px;
  }

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
