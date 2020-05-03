import Vue from 'vue';
import Vuex from 'vuex';
import * as ApplicationSettings from 'tns-core-modules/application-settings';

Vue.use(Vuex);

export default new Vuex.Store({
  state: {
    reddit: {
      user: null,
      authToken: null,
      validUntil: null,
      refreshToken: null,
    },
    lastVisitedSubreddits: [],
    starredSubreddits: [],
  },
  getters: {
    getReddit(state) {
      return state.reddit;
    },
  },
  mutations: {
    setReddit(state, {authToken = null, validUntil = null, refreshToken = null}) {
      state.reddit.authToken = authToken;
      state.reddit.validUntil = validUntil;
      state.reddit.refreshToken = refreshToken;
    },
    setRedditUser(state, {user = null}) {
      state.reddit.user = user;
    },
    visitSubreddit(state, {subreddit = null}) {
      if (subreddit && !state.lastVisitedSubreddits.includes(subreddit)) {
        state.lastVisitedSubreddits.push(subreddit);
      }
    },
    unVisitSubreddit(state, {subreddit = null}) {
      if (subreddit && state.lastVisitedSubreddits.includes(subreddit)) {
        state.lastVisitedSubreddits = state.lastVisitedSubreddits.filter((s) => s !== subreddit);
      }
    },
    starSubreddit(state, {subreddit = null}) {
      if (subreddit && !state.starredSubreddits.includes(subreddit)) {
        state.starredSubreddits.push(subreddit);
      }
    },
    unStarSubreddit(state, {subreddit = null}) {
      if (subreddit && state.starredSubreddits.includes(subreddit)) {
        state.starredSubreddits = state.starredSubreddits.filter((s) => s !== subreddit);
      }
    },
    load(state) {
      const savedState = ApplicationSettings.getString('store');
      if (savedState) {
        this.replaceState(Object.assign(state, JSON.parse(savedState)));
      }
    },
  },
  actions: {
    login({commit}, {authToken, validUntil, refreshToken}) {
      commit('setReddit', {authToken, validUntil, refreshToken});
    },
    setRedditUser({commit}, {user}) {
      commit('setRedditUser', {user});
    },
    logout({commit}) {
      commit('setReddit', {authToken: null, validUntil: null, refreshToken: null});
      commit('setRedditUser', {user: null});
    },
    visitSubreddit({commit}, {subreddit}) {
      commit('visitSubreddit', {subreddit: subreddit.display_name});
    },
    unVisitSubreddit({commit}, {subreddit}) {
      commit('unVisitSubreddit', {subreddit: subreddit.display_name});
    },
    starSubreddit({commit}, {subreddit}) {
      commit('starSubreddit', {subreddit: subreddit.display_name});
    },
    unStarSubreddit({commit}, {subreddit}) {
      commit('unStarSubreddit', {subreddit: subreddit.display_name});
    },
  },
});
