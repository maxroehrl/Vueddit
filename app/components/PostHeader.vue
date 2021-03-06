<template>
  <StackLayout padding="0">
    <StackLayout orientation="horizontal" padding="0">
      <Votes :voteable="post" width="14%" />
      <Ripple :width="bigPreview ? '86%' : '61%'"
              @longPress="onLongPress(post)"
              @tap="onTab(post)">
        <Label textWrap="true"
               class="post"
               width="100%">
          <FormattedString>
            <Span :text="post.link_flair_text ? post.link_flair_text : ''"
                  class="post-flair"
                  :style="{'background-color': post.link_flair_background_color || defaultFlairColor}" />
            <Span :text="post.link_flair_text ? ' ' + post.title : post.title"
                  class="post-title"
                  :style="{color: post.stickied ? '#53ba82' : 'white'}" />
            <Span :text="' (' + post.domain + ')\n'" />
            <Span :text="post.over_18 ? 'nsfw ' : ''"
                  class="post-nsfw" />
            <Span :text="post.spoiler ? 'spoiler ' : ''"
                  class="post-spoiler" />
            <Span :text="post.num_comments + ' comment' + (post.num_comments === 1 ? '' : 's') + ' in /r/'" />
            <Span :text="post.subreddit" />
            <Span :text="'\n' + getTimeFromNow(post) + ' by '" />
            <Span :text="'/u/' + post.author + ' '"
                  :style="{'color': highlightAuthor ? '#53ba82' : '#767676'}" />
            <Span :text="highlightAuthor && post.author_flair_text ? post.author_flair_text + '\n' : '\n'"
                  class="post-author-flair"
                  :style="{'background-color': post.author_flair_background_color || defaultFlairColor}" />
            <Span :text="post.gildings && post.gildings.gid_1 ? ('🥈x' + post.gildings.gid_1 + ' ') : ''" />
            <Span :text="post.gildings && post.gildings.gid_2 ? ('🥇x' + post.gildings.gid_2 + ' ') : ''" />
            <Span :text="post.gildings && post.gildings.gid_3 ? ('🥉x' + post.gildings.gid_3 + ' ') : ''" />
          </FormattedString>
        </Label>
      </Ripple>
      <Ripple :hidden="bigPreview"
              height="200px"
              width="25%"
              @longPress="onLongPress(post)"
              @tap="onTab(post)">
        <CachedImage :src="getPreview(post)"
                     height="100%"
                     width="100%" />
      </Ripple>
    </StackLayout>
    <Ripple :hidden="!bigPreview"
            :height="getImageHeight(post)"
            width="100%"
            @longPress="onLongPress(post)"
            @tap="onTab(post)">
      <CachedImage :src="getImage(post)"
                   :height="getImageHeight(post)"
                   width="100%" />
    </Ripple>
  </StackLayout>
</template>

<script>
import Util from '../services/Util';
import Votes from './Votes';

export default {
  name: 'PostHeader',
  components: {Votes},
  props: {
    post: {
      type: Object,
      required: true,
    },
    bigPreview: {
      type: Boolean,
      required: true,
    },
    highlightAuthor: {
      type: Boolean,
      required: false,
      default: false,
    },
    onLongPress: {
      type: Function,
      required: true,
    },
    onTab: {
      type: Function,
      required: true,
    },
  },
  data() {
    return {
      defaultFlairColor: '#767676',
    };
  },
  methods: {
    getPreview(post) {
      const preview = Util.getPreview(post);
      return preview && !this.bigPreview ? preview.url: '';
    },

    getTimeFromNow(post) {
      return Util.getTimeFromNow(post.created_utc) + (post.edited ? ' *' : '');
    },

    getImage(post) {
      const image = Util.getImage(post);
      return image && this.bigPreview ? image.url: '';
    },

    getImageHeight(post) {
      const image = Util.getImage(post);
      return image && this.bigPreview ? Util.getAspectFixHeight(image) : '0px';
    },
  },
};
</script>

<style scoped>
  .post {
    color: #767676;
  }

  .post-flair {
    font-size: 12px;
    color: #f3f3f3;
  }

  .post-title {
    font-size: 14px;
  }

  .post-author-flair {
    color: #c2c2c2;
  }

  .post-nsfw {
    color: red;
  }

  .post-spoiler {
    color: yellow;
  }
</style>
