<template>
  <Page @loaded="refresh">
    <ActionBar :title="'/u/' + user" flat="true">
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
      <SegmentedBar :items="groups" :on-selection="setGroup" />
      <Posts ref="posts"
             :subreddit="{user}"
             :app="app"
             :group="selectedGroup"
             :type="selectedType"
             :sortings="sortings" />
    </StackLayout>
  </Page>
</template>

<script>
import {action} from '@nativescript/core/ui/dialogs';
import SegmentedBar from './SegmentedBar';
import store from '../store';

export default {
  name: 'User',
  components: {Posts: () => import('./Posts'), SegmentedBar},
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
      sortings: ['new', 'top', 'hot', 'controversial'],
      groups: this.getGroups(),
      selectedGroup: 'overview',
      selectedType: 'all',
    };
  },
  methods: {
    getGroups() {
      const groups = ['overview', 'submitted', 'comments', 'gilded'];
      if (this.user === store.state.reddit.user) {
        groups.splice(3, 0, 'saved', 'upvoted', 'downvoted', 'hidden');
      }
      return groups;
    },

    setGroup(group) {
      this.selectedGroup = group;
      this.updateSortings();
      let promise = Promise.resolve();
      if (this.selectedGroup === 'saved') {
        const actions = ['All', 'Comments', 'Links'];
        promise = action({title: 'Choose type:', actions});
      }
      promise.then((type) => {
        this.selectedType = type || 'all';
        this.refresh();
      });
    },

    updateSortings() {
      if (this.selectedGroup && !['overview', 'submitted', 'comments'].includes(this.selectedGroup)) {
        if (this.sortings.length === 4) {
          this.sortings.splice(1, 3);
        }
      } else if (this.sortings.length === 1) {
        this.sortings.splice(1, 0, 'top', 'hot', 'controversial');
      }
    },

    refresh() {
      setTimeout(() => this.$refs.posts.refreshWithLoadingIndicator());
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
</style>
