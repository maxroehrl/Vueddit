<template>
  <StackLayout class="comment"
               :style="{borderWidth: selected ? '6px' : '0 6px',
                        borderColor: selected ? '#53ba82' : '#53ba82 #080808'}">
    <FlexboxLayout :hidden="!selected" class="button-bar">
      <Votes class="button-bar-child" :voteable="comment" />
      <Ripple v-for="i in buttonBar"
              :key="i.$index"
              class="button-bar-child"
              @tap="i.tap">
        <Label class="button-bar-label" :text="i.text()" />
      </Ripple>
    </FlexboxLayout>
    <Ripple ref="commentBodyRipple" @tap="tap(comment)">
      <StackLayout padding="0">
        <IndentedLabel ref="commentHeader"
                       class="comment-header"
                       textWrap="true"
                       @loaded="loadedHeader($event)">
          <FormattedString>
            <Span :text="comment.likes == null ? '' : (comment.likes ? '▲' : '▼')"
                  :style="{color: comment.likes ? '#53ba82' : '#bf5826'}" />
            <Span :text="comment.author + ' '"
                  :style="{color: getUserColor(comment)}" />
            <Span :text="comment.ups + ' points '"
                  class="comment-votes" />
            <Span :text="getTimeFromNow(comment)"
                  class="comment-created" />
            <Span :text="isCollapsed(comment) ? '[+] (' + comment.children.length + ` child${comment.children.length !== 1 ? 'ren' : ''}) ` : ''"
                  class="comment-subreddit" />
            <Span :text="showSubreddit ? 'in /r/' + comment.subreddit + ' ' : ''"
                  class="comment-subreddit" />
            <Span :text="comment.gildings && comment.gildings.gid_1 ? ('🥈x' + comment.gildings.gid_1 + ' ') : ''" />
            <Span :text="comment.gildings && comment.gildings.gid_2 ? ('🥇x' + comment.gildings.gid_2 + ' ') : ''" />
            <Span :text="comment.gildings && comment.gildings.gid_3 ? ('🥉x' + comment.gildings.gid_3 + ' ') : ''" />
            <Span :text="comment.author_flair_text ? comment.author_flair_text : ''"
                  class="comment-author-flair"
                  :style="{'background-color': comment.author_flair_background_color || '#767676'}" />
          </FormattedString>
        </IndentedLabel>
        <IndentedLabel ref="commentBody"
                       class="comment-body"
                       textWrap="true"
                       width="100%"
                       @loaded="loadedBody($event)" />
      </StackLayout>
    </Ripple>
  </StackLayout>
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
    selected: {
      type: Boolean,
      required: false,
      default: false,
    },
    showMoreDialog: {
      type: Function,
      required: true,
    },
    selectComment: {
      type: Function,
      required: false,
      default(comment) {},
    },
    selectNeighboringComment: {
      type: Function,
      required: false,
      default(comment, depth, next) {},
    },
    collapse: {
      type: Function,
      required: false,
      default(comment) {},
    },
    showSubreddit: {
      type: Boolean,
      required: false,
      default: false,
    },
    markdownCache: {
      type: Object,
      required: true,
    },
  },
  data() {
    return {
      isInitialized: false,
      buttonBarHeight: 70,
      buttonBar: [
        {text: () => 'Done', tap: () => this.selectComment(null)},
        {text: () => 'Hide', tap: () => this.collapse(this.comment)},
        {text: () => 'More', tap: () => this.more(this.comment)},
        {text: () => this.comment.depth === 0 ? '▲ Prev' : '▲ Parent', tap: () => this.selectOther(0)},
        {text: () => this.comment.depth === 0 ? 'Next ▼' : '▲ Root', tap: () => this.selectOther(1)},
      ],
      emptySpan: null,
    };
  },
  watch: {
    $props: {
      handler(props) {
        if (this.$refs.commentBody && props.comment) {
          this.refreshLabel(this.$refs.commentBody.nativeView.android, props.comment);
        }
        if (this.$refs.commentHeader && props.comment) {
          this.$refs.commentHeader.nativeView.android.setDepth(props.comment.depth, 10);
        }
      },
      immediate: true,
      deep: true,
    },
  },
  methods: {
    loadedBody(event) {
      if (!this.isInitialized) {
        Markdown.setOnTouchListener(event.object.nativeView, (event) => this.$refs.commentBodyRipple.nativeView.android.onTouchEvent(event));
        Markdown.setOnClickListener(event.object.nativeView, () => this.tap(this.comment));
        Markdown.setSpannableFactory(event.object.nativeView);
        this.isInitialized = true;
      }
      if (this.comment) {
        this.refreshLabel(event.object.nativeView, this.comment);
      }
    },

    loadedHeader(event) {
      if (this.comment) {
        event.object.nativeView.setDepth(this.comment.depth, 10);
      }
    },

    refreshLabel(tv, comment) {
      tv.setDepth(comment.depth, 50);
      let spanned = this.markdownCache[comment.name];
      if (!spanned) {
        spanned = Markdown.toMarkdown(comment.body);
        // eslint-disable-next-line vue/no-mutating-props
        this.markdownCache[comment.name] = spanned;
      }
      if (!this.emptySpan && this.isCollapsed(comment)) {
        this.emptySpan = Markdown.toMarkdown('');
      }
      Markdown.setParsedMarkdown(tv, this.isCollapsed(comment) ? this.emptySpan : spanned);
    },

    isCollapsed(comment) {
      return comment.children;
    },

    tap(comment) {
      if (comment && this.isCollapsed(comment)) {
        this.collapse(comment);
      } else {
        this.selectComment(comment);
      }
    },

    selectOther(buttonIndex) {
      let depth = this.comment.depth;
      const next = buttonIndex === 1 && depth === 0;
      depth = (buttonIndex === 1 || depth === 0) ? 0 : depth - 1;
      this.selectNeighboringComment(this.comment, depth, next);
    },

    more(comment) {
      this.showMoreDialog(comment, false, true);
    },

    getTimeFromNow(comment) {
      return Reddit.getTimeFromNow(comment.created_utc) + (comment.edited ? '* ' : ' ');
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
    padding: 0;
    border-radius: 30px;
  }

  .button-bar {
    width: 100%;
    flex-direction: row;
    justify-content: space-between;
    border-bottom-color: #53ba82;
    border-bottom-width: 6px;
  }

  .button-bar-child {
    width: 16%;
  }

  .button-bar-label {
    text-align: center;
    font-size: 11px;
    padding: 40px 0;
    text-transform: uppercase;
  }

  .comment-header, .comment-body {
    font-size: 13px;
  }

  .comment-author-flair {
    color: #c2c2c2;
  }

  .comment-votes, .comment-created, .comment-subreddit {
    color: #767676;
  }
</style>
