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
      <ActionItem text="Remove from visited"
                  android.position="popup"
                  @tap="clearVisited" />
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
      isSidebarDialogOpen: false,
    };
  },
  methods: {
    loaded(event) {
      if (this.refreshAll) {
        store.subscribe(((mutation, state) => ApplicationSettings.setString('store', JSON.stringify(state))));
        this.login();
      }
      application.android.on(AndroidApplication.activityBackPressedEvent, this.navigateBack, this);
    },

    unloaded(event) {
      application.android.off(AndroidApplication.activityBackPressedEvent, this.navigateBack, this);
    },

    login() {
      this.loadingIndicator.show(this.loadingIndicatorOptions);
      Reddit.getUser().then((result) => {
        if (result && result.name) {
          store.dispatch('setRedditUser', {user: result.name}).then(() => {
            this.$refs.subredditList.refresh();
            this.refreshPosts();
          });
          this.refreshAll = false;
        }
      }, () => {
        this.loadingIndicator.hide();
        Reddit.authorize(this);
      });
    },

    navigateBack(data) {
      if (!this.isSidebarDialogOpen && this.lastSubreddits.length) {
        this.setSubreddit(null, true);
        data.cancel = true;
      }
    },

    refreshSelectedSubreddit() {
      if (this.$refs.subredditList) {
        this.$refs.subredditList.setSubreddit(this.subreddit);
      }
    },

    refreshSubredditList() {
      if (this.$refs.subredditList) {
        this.$refs.subredditList.displaySubscriptions();
      }
    },

    refreshPosts() {
      if (this.$refs.postList) {
        this.loadingIndicator.show(this.loadingIndicatorOptions);
        this.$refs.postList.setSubreddit(this.subreddit).finally(() => this.loadingIndicator.hide());
      }
    },

    navigatedTo(event) {
      if (this.refreshAll && event.isBackNavigation) {
        this.refreshAll = false;
        this.refreshSelectedSubreddit();
        this.refreshPosts();
      }
    },

    onOpenDrawerTap() {
      this.$refs.drawer.nativeView.toggleDrawerState();
    },

    setSubreddit(subreddit, goBack) {
      if (goBack) {
        subreddit = this.lastSubreddits.pop();
        this.showGoBackSnackbar(this.subreddit, subreddit, !goBack);
        this.subreddit = subreddit;
      } else {
        this.lastSubreddits.push(this.subreddit);
        this.showGoBackSnackbar(this.subreddit, subreddit, !goBack);
        this.subreddit = subreddit;
        this.visitSubreddit(subreddit);
      }
      this.$refs.drawer.nativeView.closeDrawer();
      this.refreshSelectedSubreddit();
      this.refreshPosts();
    },

    showGoBackSnackbar(oldSubreddit, newSubreddit, goBack) {
      const snackbar = new SnackBar();
      snackbar.action({
        actionText: 'Go back to ' + this.getPrefixedName(oldSubreddit),
        actionTextColor: '#53ba82',
        snackText: 'Showing ' + this.getPrefixedName(newSubreddit),
        hideDelay: 8000,
        backgroundColor: '#3e3e3e',
      }).then((args) => {
        if (args.command === 'Action') {
          this.setSubreddit(oldSubreddit, goBack);
        }
      });
    },

    getPrefixedName(subreddit) {
      return (subreddit.subreddits ? '/m/' : '/r/') + subreddit.display_name;
    },

    showSidebar() {
      if (this.subreddit &&
        ![Reddit.frontpage, 'popular', 'all', 'random'].includes(this.subreddit.display_name) &&
        !this.subreddit.subreddits) {
        Reddit.getSidebar(this.subreddit.display_name).then((response) => {
          if (response && response.data && response.data.description) {
            this.isSidebarDialogOpen = true;
            this.$showModal(SidebarDialog, {props: {sidebar: response.data.description}})
                .finally(() => this.isSidebarDialogOpen = false);
          }
        });
      }
    },

    visitSubreddit(subreddit) {
      if (subreddit && !subreddit.subreddits && !store.state.subscribedSubreddits
          .concat(this.$refs.subredditList.defaultSubreddits.map((s) => s.display_name))
          .includes(subreddit.display_name)) {
        store.dispatch('visitSubreddit', {subreddit})
            .then(() => this.refreshSubredditList());
      }
    },

    clearVisited() {
      if (this.subreddit && !this.subreddit.subreddits) {
        store.dispatch('unVisitSubreddit', {subreddit: this.subreddit})
            .then(() => this.refreshSubredditList());
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
