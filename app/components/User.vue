<template>
  <Page>
    <ActionBar :title="'/u/' + user">
      <NavigationButton text="Back"
                        icon="res://ic_arrow_left"
                        @tap="$navigateBack" />
      <ActionItem text="Refresh"
                  icon="res://ic_menu_refresh"
                  @tap="refresh" />
      <ActionItem text="Toggle list item size"
                  android.position="popup"
                  @tap="toggleTemplate" />
    </ActionBar>
    <StackLayout padding="0">
      <SegmentedBar v-if="isLoggedInUser()"
                    class="segmented-bar"
                    :items="segmentedBarItems"
                    selectedIndex="0"
                    selectedBackgroundColor="#53ba82"
                    @selectedIndexChange="onGroupChange" />
      <Posts ref="posts"
             :subreddit="{user}"
             :app="app"
             :group="selectedGroup"
             :sortings="['new', 'top', 'hot', 'controversial']" />
    </StackLayout>
  </Page>
</template>

<script>
import {SegmentedBarItem} from '@nativescript/core/ui/segmented-bar';
import store from '../store';
import Reddit from '../services/Reddit';

export default {
  name: 'User',
  components: {Posts: () => import('./Posts')},
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
      selectedGroup: Reddit.groups[0],
      segmentedBarItems: Reddit.groups.map((sorting) => {
        const item = new SegmentedBarItem();
        item.title = sorting;
        return item;
      }),
    };
  },
  methods: {
    isLoggedInUser() {
      return this.user === store.state.reddit.user;
    },

    onGroupChange(args) {
      this.selectedGroup = Reddit.groups[args.value];
      setTimeout(() => this.$refs.posts.refreshWithLoadingIndicator());
    },

    refresh() {
      this.$refs.posts.refreshWithLoadingIndicator();
    },

    toggleTemplate() {
      this.$refs.posts.toggleTemplate();
    },
  },
};
</script>

<style scoped>
  ActionBar {
    background-color: #3e3e3e;
    color: #ffffff;
  }

  .segmented-bar {
    background-color: #3e3e3e;
  }
</style>
