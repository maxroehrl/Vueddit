<template>
  <Page actionBarHidden="false"
        @loaded="loaded($event)"
        @unloaded="unloaded($event)"
        @navigatedTo="navigatedTo($event)">
    <ActionBar :title="subreddit.display_name" flat="false">
      <ActionItem text="Refresh"
                  icon="res://ic_menu_refresh"
                  @tap="refreshPosts" />
      <ActionItem text="Sidebar"
                  android.position="popup"
                  @tap="showSidebar" />
      <ActionItem text="Logout"
                  android.position="popup"
                  @tap="logout" />
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
        <Posts ref="postList"
               :subreddit="subreddit"
               :app="this" />
      </StackLayout>
    </RadSideDrawer>
  </Page>
</template>

<script>
import {PushTransition} from 'nativescript-ui-sidedrawer';
import {LoadingIndicator, Mode} from '@nstudio/nativescript-loading-indicator';
import {SnackBar} from '@nstudio/nativescript-snackbar';
import * as ApplicationSettings from 'tns-core-modules/application-settings';
import * as application from 'tns-core-modules/application';
import {AndroidApplication} from 'tns-core-modules/application';
import Posts from './Posts';
import Subreddits from './Subreddits';
import SidebarDialog from './SidebarDialog';
import Reddit from '../services/Reddit';
import store from '../store';

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
        this.login();
      }
      application.android.on(AndroidApplication.activityBackPressedEvent, (data) => {
        if (this.lastSubreddits.length) {
          this.loadingIndicator.show(this.loadingIndicatorOptions);
          this.subreddit = this.lastSubreddits.pop();
          setTimeout(() => this.$refs.postList.refresh().finally(() => this.loadingIndicator.hide()));
          data.cancel = true;
        }
      });
    },

    unloaded(event) {
      application.android.off(AndroidApplication.activityBackPressedEvent);
    },

    login() {
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
    },

    refreshChildren() {
      if (this.$refs.postList && this.$refs.subredditList) {
        this.$refs.postList.refresh().finally(() => this.loadingIndicator.hide());
        this.$refs.subredditList.refresh();
      }
    },

    refreshPosts() {
      if (this.$refs.postList) {
        this.loadingIndicator.show(this.loadingIndicatorOptions);
        this.$refs.postList.refresh().finally(() => this.loadingIndicator.hide());
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
      if (this.lastSubreddits.length) {
        this.showGoBackSnackbar(this.subreddit, subreddit);
      }
      this.lastSubreddits.push(this.subreddit);
      this.subreddit = subreddit;
      this.$refs.drawer.nativeView.closeDrawer();
      setTimeout(() => this.$refs.postList.refresh().finally(() => this.loadingIndicator.hide()));
    },

    showGoBackSnackbar(oldSubreddit, newSubreddit) {
      const snackbar = new SnackBar();
      snackbar.action({
        actionText: 'Go back',
        actionTextColor: '#53ba82',
        snackText: 'Showing ' + newSubreddit.display_name,
        hideDelay: 8000,
        backgroundColor: '#3e3e3e',
      }).then((args) => {
        if (args.command === 'Action') {
          this.setSubreddit(oldSubreddit);
          this.lastSubreddits.pop();
          this.lastSubreddits.pop();
        }
      });
    },

    showSidebar() {
      if (this.subreddit &&
        ![Reddit.frontpage, 'popular', 'all', 'random'].includes(this.subreddit.display_name) &&
        !this.subreddit.subreddits) {
        Reddit.getSidebar(this.subreddit.display_name).then((response) => {
          if (response && response.data && response.data.description) {
            this.$showModal(SidebarDialog, {props: {sidebar: response.data.description}});
          }
        });
      }
    },

    logout() {
      store.dispatch('logout').then(() => {
        this.refreshAll = true;
        this.login();
      });
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
