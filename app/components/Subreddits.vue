<template>
  <StackLayout @loaded="loaded($event)">
    <RadListView id="subreddit-list"
                 ref="subredditList"
                 for="subreddit in subredditList"
                 @load="subredditListLoaded">
      <v-template name="header">
        <StackLayout @loaded="searchLayoutLoaded">
          <SearchBar ref="searchbar"
                     hint="Search for subreddit"
                     class="searchbar"
                     text=""
                     @loaded="searchbarLoaded"
                     @textChange="onSearchTextChanged"
                     @submit="onSubmit"
                     @clear="onClear" />
        </StackLayout>
      </v-template>
      <v-template>
        <FlexboxLayout class="subreddit-item" :style="{backgroundColor: isSelected(subreddit) ? '#53ba82': '#2d2d2d'}">
          <Ripple rippleColor="#53ba82"
                  width="20%"
                  @tap="star(subreddit)">
            <Image :src="getStarredSrc(subreddit)"
                   class="star"
                   stretch="fill"
                   loadMode="async" />
          </Ripple>
          <Ripple rippleColor="#53ba82"
                  width="80%"
                  @tap="setSubreddit(subreddit)">
            <Label :text="subreddit.display_name"
                   class="subreddit-label" />
          </Ripple>
        </FlexboxLayout>
      </v-template>
    </RadListView>
  </StackLayout>
</template>

<script>
import Reddit from '../services/Reddit';
import {ObservableArray} from 'tns-core-modules/data/observable-array';
import SidebarDialog from './SidebarDialog';
import store from '../store';
import {ad} from 'tns-core-modules/utils/utils';

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
      subscriptions: [],
      multis: [],
      subredditList: new ObservableArray([]),
      defaultSubreddits: [Reddit.frontpage, 'popular', 'all', 'random'].map((s) => ({display_name: s})),
      searchText: '',
      subscribedSubredditNames: [],
    };
  },
  methods: {
    loaded(event) {
      if (!this.subscriptions.length) {
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
        }
      });
    },

    fetchMultiReddits() {
      return Reddit.getMultis().then((result) => {
        this.multis = result.map((r) => r.data);
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
      this.$refs.subredditList.nativeView.refresh();
    },

    setSubreddit(subreddit) {
      this.selected = subreddit;
      this.visitSubreddit(subreddit);
      this.refreshList();
      this.onSelection(subreddit);
    },

    visitSubreddit(subreddit) {
      if (!this.isSubscribedTo(subreddit) && !this.isMultireddit(subreddit)) {
        store.dispatch('visitSubreddit', {subreddit});
      }
    },

    isSubscribedTo(subreddit) {
      return this.subscribedSubredditNames.includes(subreddit.display_name) && !this.isMultireddit(subreddit);
    },

    onSearchTextChanged(event) {
      if (event.value) {
        this.searchText = event.value;
        Reddit.searchForSubreddit(event.value).then((result) => {
          if (result && result.names) {
            this.subredditList = new ObservableArray(result.names.map((s) => ({display_name: s})));
            this.refreshList();
            // setTimeout(this.$refs.searchbar.nativeView.focus, 1000);
          }
        });
      }
    },

    onSubmit() {
      this.setSubreddit({display_name: this.searchText});
    },

    onClear() {
      this.displaySubscriptions();
      ad.dismissSoftInput();
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

    getStarredSrc(subreddit) {
      if (this.isMultireddit(subreddit)) {
        return 'res://ic_folder_multiple_outline_white_48dp';
      } else if (this.defaultSubreddits.includes(subreddit)) {
        return '';
      } else if (this.isSubscribedTo(subreddit)) {
        return this.isStarred(subreddit) ? 'res://ic_star_face_white_48dp' : 'res://ic_star_outline_white_48dp';
      } else {
        return 'res://ic_plus_white_48dp';
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

    showSidebar(subreddit) {
      this.$showModal(SidebarDialog, {props: {subreddit}});
    },
  },
};
</script>

<style scoped>
  #subreddit-list {
    height: 100%;
    width: 100%;
  }

  .subreddit-item {
    flex-directionDirection: row;
    align-selfSelf: flex-start;
    justify-contentContent: flex-start;
  }

  .subreddit-label {
    font-size: 20px;
    padding: 50px;
  }

  .star {
    margin: 30px;
  }

  .searchbar {
    font-size: 16px;
    width: 100%;
  }
</style>
