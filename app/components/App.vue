<template>
  <Page actionBarHidden="false"
        @loaded="loaded($event)"
        @unloaded="unloaded($event)"
        @navigatedTo="navigatedTo($event)">
    <ActionBar :title="subreddit.display_name" flat="false">
      <ActionItem text="Refresh"
                  icon="res://ic_menu_refresh"
                  @tap="refreshPosts()" />
      <ActionItem text="Show sidebar"
                  android.position="popup"
                  @tap="showSidebar(subreddit)" />
      <ActionItem text="Remove from visited"
                  android.position="popup"
                  @tap="clearVisited" />
      <ActionItem text="Toggle list item size"
                  android.position="popup"
                  @tap="toggleTemplate" />
      <ActionItem :text="'Goto /u/' + getCurrentUserName() + '\'s profile'"
                  android.position="popup"
                  @tap="gotoMyProfile" />
      <ActionItem :text="'Logout /u/' + getCurrentUserName()"
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
                    :select-subreddit="setSubreddit" />
      </StackLayout>

      <StackLayout ~mainContent>
        <Posts ref="postList"
               :subreddit="subreddit"
               :app="this"
               :sortings="['best', 'hot', 'top', 'new', 'controversial', 'rising']" />
      </StackLayout>
    </RadSideDrawer>
  </Page>
</template>

<script>
import {PushTransition} from 'nativescript-ui-sidedrawer';
import {LoadingIndicator, Mode} from '@nstudio/nativescript-loading-indicator';
import * as ApplicationSettings from '@nativescript/core/application-settings';
import * as application from '@nativescript/core/application';
import {AndroidApplication} from '@nativescript/core/application';
import {ad} from '@nativescript/core/utils/utils';
import {action} from '@nativescript/core/ui/dialogs';
import showSnackbar from './Snackbar';
import Posts from './Posts';
import User from './User';
import Subreddits from './Subreddits';
import SidebarDialog from './SidebarDialog';
import Comments from './Comments';
import Reddit from '../services/Reddit';
import CustomTabs from '../services/CustomTabs';
import Markdown from '../services/Markdown';
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
      navigationDepth: 0,
    };
  },
  methods: {
    loaded() {
      if (this.refreshAll) {
        store.subscribe(((mutation, state) => ApplicationSettings.setString('store', JSON.stringify(state))));
        Markdown.setUrlOpenCallback(this.openUrl.bind(this));
        this.login();
      }
      application.android.on(AndroidApplication.activityBackPressedEvent, this.navigateBack, this);
    },

    unloaded() {
      application.android.off(AndroidApplication.activityBackPressedEvent, this.navigateBack, this);
    },

    login() {
      this.loadingIndicator.show(this.loadingIndicatorOptions);
      Reddit.getUser().then((result) => {
        if (result?.name) {
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

    toggleTemplate() {
      this.$refs.postList.toggleTemplate();
    },

    refreshSelectedSubreddit() {
      if (this.$refs.subredditList) {
        this.$refs.subredditList.refreshList();
        setTimeout(() => ad.dismissSoftInput(), 50);
      }
    },

    refreshSubredditList() {
      if (this.$refs.subredditList) {
        this.$refs.subredditList.displaySubscriptions();
      }
    },

    refreshPosts(last={}) {
      if (this.$refs.postList) {
        this.loadingIndicator.show(this.loadingIndicatorOptions);
        this.$refs.postList.setSubreddit(last).finally(() => this.loadingIndicator.hide());
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

    setSubreddit(subreddit, goBack=false) {
      let last = {};
      if (goBack) {
        last = this.lastSubreddits.pop();
        subreddit = last.subreddit;
      } else {
        this.lastSubreddits.push({
          subreddit: this.subreddit,
          postList: this.$refs.postList.postList,
          lastPostId: this.$refs.postList.lastPostId,
          index: this.$refs.postList.$refs.postList.nativeView.getFirstVisiblePosition(),
        });
        this.visitSubreddit(subreddit);
      }
      this.$refs.drawer.nativeView.closeDrawer();
      this.showGoBackSnackbar(this.subreddit, subreddit, !goBack);
      this.subreddit = subreddit;
      setTimeout(() => {
        this.refreshSelectedSubreddit();
        this.refreshPosts(last);
      });
    },

    showGoBackSnackbar(oldSubreddit, newSubreddit, goBack) {
      const actionText = 'Go back to ' + (oldSubreddit.subreddits ? '/m/' : '/r/') + oldSubreddit.display_name;
      showSnackbar({actionText}).then((args) => {
        if (args.command === 'Action') {
          this.setSubreddit(oldSubreddit, goBack);
        }
      });
    },

    showSidebar(subreddit) {
      if (subreddit &&
        ![Reddit.frontpage, 'popular', 'all', 'random'].includes(subreddit.display_name) &&
        !subreddit.subreddits) {
        Reddit.getSidebar(subreddit.display_name).then((response) => {
          if (response?.data?.description) {
            this.isSidebarDialogOpen = true;
            this.$showModal(SidebarDialog, {props: {sidebar: response.data.description}})
                .finally(() => this.isSidebarDialogOpen = false);
          }
        });
      } else {
        showSnackbar({snackText: 'No sidebar is available.'});
      }
    },

    openUrl(url, permalink=null) {
      const redditUrlPrefix = '^https?://(www\\.)?(old\\.|new\\.|m\\.)?reddit\\.com';
      let path = url;
      if (new RegExp(redditUrlPrefix + '.+').test(url) && !new RegExp(redditUrlPrefix + '/gallery/').test(url)) {
        path = url.replace(new RegExp(redditUrlPrefix), '');
      }
      if (!permalink || permalink.split('/')[4] !== path.split('/')[4]) {
        if (new RegExp('^/r/[^\\s;.]+/comments/[^\\s;.]+/[^\\s;.]+$').test(path)) {
          return this.openComments(path.split('?')[0], [], path.split('/')[6]);
        } else if (new RegExp('^/u(ser)?/[^\\s;.]+$').test(path)) {
          return this.gotoUserPosts(path.split('/')[2]);
        } else if (new RegExp('^/r/[^\\s;.]+$').test(path)) {
          const name = path.split('/')[2].toLowerCase();
          if (this.subreddit.display_name.toLowerCase() !== name) {
            this.gotoSubreddit(name);
          } else {
            showSnackbar({snackText: 'Already in subreddit /r/' + this.subreddit.display_name});
          }
          return;
        }
      }
      if (android.util.Patterns.WEB_URL.matcher(url).matches()) {
        CustomTabs.openUrl(url);
      } else {
        console.error('Invalid url: ' + url);
      }
    },

    openComments(post, comments=[], commentName='') {
      if (typeof post === 'string') { // Post is a permalink
        Reddit.getPostAndComments(post).then(({post, comments}) => {
          post.shown_comments = post.num_comments;
          this.openComments(post, comments, commentName);
        });
      } else {
        this.navigationDepth += 1;
        this.$navigateTo(Comments, {
          transition: 'slide',
          props: {app: this, post, comments, commentName},
        });
      }
    },

    gotoUserPosts(user) {
      this.navigationDepth += 1;
      this.$navigateTo(User, {
        transition: 'slide',
        props: {app: this, user},
      });
    },

    gotoSubreddit(subreddit) {
      for (let i = 0; i < this.navigationDepth; i++) {
        this.$navigateBack();
      }
      this.setSubreddit({display_name: subreddit});
      this.navigationDepth = 0;
    },

    showMoreDialog({saved, subreddit, author, name}, showGotoSubreddit=true, showGotoUser=true) {
      const actions = [saved ? 'Unsave' : 'Save'];
      if (showGotoSubreddit && subreddit !== this.subreddit.display_name) {
        actions.push('Goto /r/' + subreddit);
      }
      if (showGotoUser) {
        actions.push('Goto /u/' + author);
      }
      action({actions}).then((action) => {
        if (action === 'Save' || action === 'Unsave') {
          Reddit.saveOrUnsave({name, saved});
        } else if (action.startsWith('Goto /r/')) {
          this.gotoSubreddit(subreddit);
        } else if (action.startsWith('Goto /u/')) {
          this.gotoUserPosts(author);
        }
      });
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

    getCurrentUserName() {
      return store.state?.reddit?.user ?? '';
    },

    gotoMyProfile() {
      this.gotoUserPosts(this.getCurrentUserName());
    },

    logout() {
      store.dispatch('logout').then(() => {
        android.webkit.CookieManager.getInstance().removeAllCookies(null);
        this.refreshAll = true;
        this.login();
      });
    },
  },
};
</script>
