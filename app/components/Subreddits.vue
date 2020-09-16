<template>
  <StackLayout @loaded="loaded($event)">
    <RadListView ref="subredditList"
                 for="subreddit in subredditList"
                 @load="subredditListLoaded">
      <v-template name="header">
        <StackLayout @loaded="searchLayoutLoaded">
          <SearchBar ref="searchbar"
                     v-model="searchText"
                     hint="Search for subreddit"
                     class="searchbar"
                     @loaded="searchbarLoaded"
                     @textChange="onSearchTextChanged"
                     @submit="onSubmit"
                     @clear="onClear" />
        </StackLayout>
      </v-template>
      <v-template>
        <StackLayout orientation="horizontal"
                     padding="0"
                     :style="{backgroundColor: isSelected(subreddit) ? '#53ba82': '#2d2d2d'}">
          <Ripple width="20%" @tap="star(subreddit)">
            <Label :text="getSubredditIcon(subreddit)" class="subreddit-icon" />
          </Ripple>
          <Ripple width="80%" @tap="onSelection(subreddit)">
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
import {ad} from '@nativescript/core/utils/utils';

export default {
  name: 'Subreddits',
  props: {
    selected: {
      type: Object,
      required: true,
    },
    onSelection: {
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
        if (r && r.data && r.data.children) {
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
      this.subredditList = new ObservableArray(this.defaultSubreddits);
      this.subredditList.push(this.subscriptions);
      this.subredditList.push(...store.state.lastVisitedSubreddits.map((s) => ({display_name: s})));
      this.subredditList.push(this.multis);
      this.refreshList();
    },

    refreshList() {
      setTimeout(() => {
        if (this.$refs.subredditList) {
          this.$refs.subredditList.nativeView._listViewAdapter.notifyDataSetChanged();
        }
      });
    },

    setSubreddit(subreddit, callback) {
      this.selected = subreddit;
      this.refreshList();
      if (callback) {
        callback(subreddit);
      }
    },

    isSubscribedTo(subreddit) {
      return this.subscribedSubredditNames.includes(subreddit.display_name) && !this.isMultireddit(subreddit);
    },

    onSearchTextChanged(event) {
      if (event.value) {
        Reddit.searchForSubreddit(this.searchText).then((result) => {
          if (result && result.names) {
            this.subredditList = new ObservableArray(result.names.map((s) => ({display_name: s})));
            this.refreshList();
            setTimeout(() => this.$refs.searchbar.nativeView.focus(), 500);
          }
        });
      }
    },

    onSubmit() {
      this.onSelection({display_name: this.searchText});
    },

    onClear() {
      this.displaySubscriptions();
      setTimeout(() => ad.dismissSoftInput(), 50);
    },

    searchbarLoaded(event) {
      if (event.object.android) {
        event.object.android.clearFocus();
      }
    },

    searchLayoutLoaded(event) {
      if (event.object.android) {
        event.object.android.setFocusableInTouchMode(true);
      }
    },

    star(subreddit) {
      if (this.isSubscribedTo(subreddit)) {
        store.dispatch(this.isStarred(subreddit) ? 'unStarSubreddit' : 'starSubreddit', {subreddit}).then((r) => {
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
        return '';
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
  .subreddit-label {
    font-size: 20px;
    margin: 20px;
  }

  .subreddit-icon {
    font-size: 30px;
    text-align: center;
    margin: 20px;
  }

  .searchbar {
    font-size: 16px;
    height: 300px;
    width: 100%;
  }
</style>
