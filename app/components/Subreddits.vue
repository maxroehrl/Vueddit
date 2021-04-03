<template>
  <StackLayout @loaded="loaded($event)">
    <SearchBar ref="searchbar"
               v-model="searchText"
               hint="Search for subreddit"
               class="searchbar"
               @loaded="searchbarLoaded"
               @textChange="onSearchTextChanged"
               @submit="onSubmit"
               @clear="onClear" />
    <RadListView ref="subredditList" for="subreddit in subredditList">
      <v-template>
        <StackLayout orientation="horizontal"
                     class="subreddit-item"
                     padding="0"
                     :style="{backgroundColor: isSelected(subreddit) ? '#53ba82': '#2d2d2d'}">
          <Ripple width="20%" @tap="star(subreddit)">
            <Label :text="getSubredditIcon(subreddit)" class="subreddit-icon" />
          </Ripple>
          <Ripple width="80%" @tap="selectSubreddit(subreddit)">
            <Label :text="subreddit.display_name" class="subreddit-label" />
          </Ripple>
        </StackLayout>
      </v-template>
    </RadListView>
  </StackLayout>
</template>

<script>
import Reddit from '../services/Reddit';
import {ObservableArray} from '@nativescript/core/data/observable-array';
import store from '../store';

export default {
  name: 'Subreddits',
  props: {
    selected: {
      type: Object,
      required: true,
    },
    selectSubreddit: {
      type: Function,
      required: true,
    },
  },
  data() {
    return {
      subscriptions: null,
      multis: [],
      subredditList: new ObservableArray([]),
      defaultSubreddits: [Reddit.frontpage, 'popular', 'all', 'random'].map((s) => ({display_name: s})),
      searchText: '',
      subscribedSubredditNames: [],
    };
  },
  methods: {
    loaded() {
      if (!this.subscriptions) {
        store.commit('load');
        this.subscriptions = store.state.subscribedSubreddits.map((s) => ({display_name: s}));
        this.multis = store.state.multireddits;
        this.displaySubscriptions();
        this.refresh();
      }
    },

    refresh() {
      return Promise.all([this.fetchSubscriptions(), this.fetchMultiReddits()]).then(this.displaySubscriptions);
    },

    fetchSubscriptions() {
      return Reddit.getSubscriptions().then((r) => {
        if (r?.data?.children?.map) {
          const items = r.data.children.map((d) => d.data);
          items.sort(this.sortSubredditByStarred);
          this.subscriptions = items;
          this.subscribedSubredditNames = items.concat(this.defaultSubreddits).map((s) => s.display_name);
          store.dispatch('setSubscribedSubreddits', {subscribedSubreddits: this.subscriptions.map((s) => s.display_name)});
        }
      });
    },

    fetchMultiReddits() {
      return Reddit.getMultis().then((result) => {
        this.multis = result.map((r) => r.data);
        store.dispatch('setMultireddits', {multireddits: this.multis.map((s) => ({display_name: s.display_name, subreddits: s.subreddits}))});
      });
    },

    displaySubscriptions() {
      if (!this.searchText.length) {
        this.subredditList = new ObservableArray();
        this.subredditList.push(...this.defaultSubreddits, ...this.subscriptions, ...store.state.lastVisitedSubreddits.map((s) => ({display_name: s})), ...this.multis);
        this.refreshList();
      }
    },

    refreshList() {
      setTimeout(() => {
        if (this.$refs.subredditList) {
          this.$refs.subredditList.nativeView._listViewAdapter.notifyDataSetChanged();
        }
      });
    },

    isSubscribedTo(subreddit) {
      return this.subscribedSubredditNames.includes(subreddit.display_name) && !this.isMultireddit(subreddit);
    },

    onSearchTextChanged(event) {
      if (event.value) {
        Reddit.searchForSubreddit(this.searchText).then((result) => {
          if (result?.names) {
            this.subredditList = new ObservableArray(result.names.map((s) => ({display_name: s})));
            this.refreshList();
          }
        });
      }
    },

    onSubmit() {
      this.selectSubreddit({display_name: this.searchText});
    },

    onClear() {
      this.displaySubscriptions();
    },

    searchbarLoaded(event) {
      if (event.object.android) {
        event.object.android.clearFocus();
      }
    },

    star(subreddit) {
      if (this.isSubscribedTo(subreddit)) {
        store.dispatch(this.isStarred(subreddit) ? 'unStarSubreddit' : 'starSubreddit', {subreddit}).then(() => {
          this.subscriptions.sort(this.sortSubredditByStarred);
          this.displaySubscriptions();
        });
      } else if (!this.isMultireddit(subreddit)) {
        Reddit.subscribe(subreddit.display_name).then(() => this.refresh());
      }
    },

    isSelected(subreddit) {
      return this.selected.display_name === subreddit.display_name && this.selected.subreddits === subreddit.subreddits;
    },

    isStarred(subreddit) {
      return store.state.starredSubreddits.includes(subreddit.display_name);
    },

    isMultireddit(subreddit) {
      return !!subreddit.subreddits;
    },

    getSubredditIcon(subreddit) {
      if (this.isMultireddit(subreddit)) {
        return '⧟';
      } else if (this.defaultSubreddits.includes(subreddit)) {
        return '⌂';
      } else if (this.isSubscribedTo(subreddit)) {
        return this.isStarred(subreddit) ? '★' : '☆';
      } else {
        return '＋';
      }
    },

    sortSubredditByStarred(a, b) {
      const isAStarred = this.isStarred(a);
      const isBStarred = this.isStarred(b);
      if (isAStarred && !isBStarred) {
        return -1;
      } else if (!isAStarred && isBStarred) {
        return 1;
      }
      return a.display_name.toLowerCase().localeCompare(b.display_name.toLowerCase());
    },
  },
};
</script>

<style scoped>
  .subreddit-item {
    height: 150px;
  }

  .subreddit-label {
    font-size: 14px;
    margin: 20px;
  }

  .subreddit-icon {
    font-size: 25px;
    text-align: center;
    margin: 20px;
  }

  .searchbar {
    font-size: 16px;
    height: 240px;
  }
</style>
