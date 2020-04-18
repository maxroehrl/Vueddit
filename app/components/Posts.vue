<template>
  <RadListView id="post-list"
               ref="postList"
               for="post in postList"
               selectionBehavior="Press"
               itemSelectedBackgroundColor="#222222"
               loadOnDemandMode="Auto"
               loadOnDemandBufferSize="5"
               pullToRefresh="true"
               @loaded="loaded($event)"
               @loadMoreDataRequested="onLoadMorePostsRequested"
               @pullToRefreshInitiated="onPullDown">
    <v-template name="header">
      <SegmentedBar class="sorting"
                    :items="sortings"
                    selectedIndex="0"
                    selectedBackgroundColor="#53ba82"
                    @selectedIndexChange="onSortingChange" />
    </v-template>
    <v-template>
      <Ripple rippleColor="#53ba82" @tap="openComments($event, post)">
        <FlexboxLayout flexDirection="row"
                       alignSelf="flex-start"
                       justifyContent="flex-start">
          <Votes :post="post" />
          <Label textWrap="true"
                 style="width: 65%">
            <FormattedString>
              <Span :style="{'background-color': post.link_flair_background_color || defaultFlairColor}"
                    :text="post.link_flair_text"
                    class="post-flair" />
              <Span :text="post.link_flair_text ? ' ' + post.title : post.title"
                    class="post-title"
                    :style="{color: post.stickied && subreddit.created ? '#53ba82' : 'white'}" />
              <Span :text="' (' + post.domain + ') \n'" class="post-domain" />
              <Span :text="post.over_18 ? 'nsfw ' : ''" class="post-nsfw" />
              <Span :text="post.spoiler ? 'spoiler ' : ''" class="post-spoiler" />
              <Span :text="post.num_comments + ' comments '" class="post-num-comments" />
              <Span :text="post.subreddit" class="post-subreddit" />
            </FormattedString>
          </Label>
          <Image :src="getPreview(post)"
                 stretch="aspectFit"
                 width="20%"
                 height="300px"
                 loadMode="async" />
        </FlexboxLayout>
      </Ripple>
    </v-template>
  </RadListView>
</template>

<script>
import Reddit from '../services/Reddit';
import {ObservableArray} from 'tns-core-modules/data/observable-array';
import Votes from './Votes';
import Comments from './Comments';
import {SegmentedBarItem} from 'tns-core-modules/ui/segmented-bar';

export default {
  name: 'Subreddits',
  components: {
    Votes,
  },
  props: {
    subreddit: {
      type: Object,
      required: true,
    },
  },
  data() {
    return {
      defaultFlairColor: '#767676',
      postList: new ObservableArray([]),
      lastPostId: null,
      sortings: this.getSortings(),
      sorting: 'best',
    };
  },
  methods: {
    loaded(event) {
    },

    onPullDown(args) {
      this.refresh().finally(() => args.object.notifyPullToRefreshFinished(true));
    },

    onLoadMorePostsRequested(args) {
      this.getPosts(this.lastPostId)
          .then(() => args.object.notifyAppendItemsOnDemandFinished(this.postList.length, false));
    },

    getSortings() {
      return Object.values(Reddit.sortings).slice(0, 4).map((sorting) => {
        const item = new SegmentedBarItem();
        item.title = sorting;
        return item;
      });
    },

    onSortingChange(args) {
      this.sorting = this.getSortings()[args.value].title;
      this.refresh();
    },

    refresh() {
      this.postList = new ObservableArray([]);
      this.lastPostId = null;
      return this.getPosts();
    },

    getPosts(lastPostId) {
      let sub;
      if (this.subreddit.subreddits) {
        sub = this.subreddit.subreddits.map((s) => s.name).join('+');
      } else {
        sub = this.subreddit.display_name;
      }
      if (sub && sub !== '') {
        return Reddit.getPosts(sub, lastPostId, this.sorting).then((r) => {
          if (r && r.data && r.data.children) {
            const items = r.data.children.map((d) => d.data);
            this.lastPostId = items[items.length - 1].name;
            this.postList.push(...items);
          }
        });
      } else {
        return Promise.resolve();
      }
    },

    getPreview(post) {
      return Reddit.getPreview(post);
    },

    openComments(event, post) {
      // explode (Android Lollipop(21) and up only), fade,
      // flip (same as flipRight), flipRight, flipLeft,
      // slide (same as slideLeft), slideLeft, slideRight, slideTop, slideBottom
      this.$navigateTo(Comments, {
        transition: 'slide',
        props: {
          post,
        },
      });
    },
  },
};
</script>

<style scoped>
  #post-list {
    height: 100%;
    width: 100%;
    background-color: #080808;
    separator-color: #ff0000;
  }

  .post-flair {
    font-size: 12px;
    color: #f3f3f3;
  }

  .post-title {
    color: white;
    font-size: 14px;
  }

  .post-num-comments {
    color: #767676;
  }

  .post-domain {
    color: #767676;
  }

  .post-subreddit {
    color: #767676;
  }

  .post-nsfw {
    color: red;
  }

  .post-spoiler {
    color: yellow;
  }

  .sorting {
    height: 200px;
    width: 100%;
  }
</style>
