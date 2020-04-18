<template>
  <Page actionBarHidden="false"
        @loaded="loaded($event)"
        @navigatedTo="navigatedTo($event)">
    <ActionBar :title="subreddit.display_name" flat="false">
      <NavigationButton text="Navigation"
                        icon="res://ic_menu_drawer"
                        @tap="onOpenDrawerTap" />
    </ActionBar>
    <RadSideDrawer ref="drawer" :drawerTransition="transition">
      <StackLayout ~drawerContent>
        <Subreddits ref="subredditList"
                    :selected="subreddit"
                    :on-selection="setSubreddit" />
      </StackLayout>

      <StackLayout ~mainContent>
        <Posts ref="postList" :subreddit.sync="subreddit" />
      </StackLayout>
    </RadSideDrawer>
  </Page>
</template>

<script>
import {PushTransition} from 'nativescript-ui-sidedrawer';
import * as ApplicationSettings from 'tns-core-modules/application-settings';
import Posts from './Posts';
import Subreddits from './Subreddits';
import Reddit from '../services/Reddit';
import store from '../store';
import {LoadingIndicator, Mode} from '@nstudio/nativescript-loading-indicator';

export default {
  name: 'App',
  components: {
    Posts,
    Subreddits,
  },
  data() {
    return {
      subreddit: {display_name: Reddit.frontpage},
      transition: new PushTransition(),
      refreshAll: true,
      lastSubreddits: [],
      loadingIndicator: new LoadingIndicator(),
      loadingIndicatorOptions: {
        hideBezel: true,
        color: '#53ba82',
        mode: Mode.Indeterminate,
      },
    };
  },
  methods: {
    loaded(event) {
      if (this.refreshAll) {
        this.loadingIndicator.show(this.loadingIndicatorOptions);
        store.commit('load');
        store.subscribe(((mutation, state) => {
          ApplicationSettings.setString('store', JSON.stringify(state));
        }));

        Reddit.getUser().then((result) => {
          if (result && result.name) {
            store.dispatch('setRedditUser', {user: result.name})
                .then(this.refreshChildren);
            this.refreshAll = false;
          }
        }, () => {
          this.loadingIndicator.hide();
          Reddit.authorize(this);
        });
      }
    },

    refreshChildren() {
      if (this.$refs.postList && this.$refs.subredditList) {
        this.$refs.postList.refresh().finally(() => this.loadingIndicator.hide());
        this.$refs.subredditList.refresh();
      }
    },

    navigatedTo(event) {
      if (this.refreshAll && event.isBackNavigation) {
        this.refreshAll = false;
        this.refreshChildren();
      }
    },

    onOpenDrawerTap() {
      this.$refs.drawer.nativeView.toggleDrawerState();
    },

    setSubreddit(subreddit) {
      this.loadingIndicator.show(this.loadingIndicatorOptions);
      this.lastSubreddits.push(this.subreddit);
      this.subreddit = subreddit;
      this.$refs.drawer.nativeView.closeDrawer();
      setTimeout(() => this.$refs.postList.refresh().finally(() => this.loadingIndicator.hide()));
    },
  },
};
</script>

<style scoped>
  ActionBar {
    background-color: #53ba82;
    color: #ffffff;
  }
</style>
