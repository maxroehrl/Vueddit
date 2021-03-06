import * as app from '@nativescript/core/application';
import {getJSON} from '@nativescript/core/http';
import Login from '../components/Login';
import store from '../store';
import Util from './Util';

export default class Reddit {
  static api = 'https://www.reddit.com';
  static oauthApi = 'https://oauth.reddit.com';
  static userAgent;
  static randomState = Math.random().toString(36).replace(/[^a-z]+/g, '').substr(0, 10);
  static clientId = 'm_gI8cFDcqC7uA';
  static redirectUri = 'http://localhost:8080';
  static redirectUriEncoded = encodeURIComponent(Reddit.redirectUri);
  static scope = encodeURIComponent('mysubreddits read vote save subscribe wikiread identity history flair');
  static frontpage = 'reddit front page';

  static getHeaders() {
    if (!this.userAgent) {
      this.userAgent = `${app.android.context.getPackageName()}:${app.android.context.getPackageManager().getPackageInfo(app.android.context.getPackageName(), 0).versionName} (by /u/MaxRoehrl)`;
    }
    return {
      'User-Agent': this.userAgent,
      'Authorization': `Bearer ${store.state.reddit.authToken}`,
    };
  }

  static authorize(page) {
    const url = `${this.api}/api/v1/authorize.compact?client_id=${this.clientId}&response_type=code&state=${this.randomState}&redirect_uri=${this.redirectUriEncoded}&scope=${this.scope}&duration=permanent`;
    page.$navigateTo(Login, {
      transition: 'slide',
      props: {
        url,
        onAuthorizationSuccessful: this.getRefreshToken.bind(this, page),
      },
    });
  }

  static getRefreshToken(page, responseUrl) {
    const state = responseUrl.match(/state=([^&#]+)/)[1];
    if (state === this.randomState) {
      if (responseUrl.includes('code=')) {
        const code = responseUrl.match(/code=([^&#]+)/)[1];
        const content = `grant_type=authorization_code&code=${code}&redirect_uri=${this.redirectUriEncoded}`;
        return this.updateToken(content).then(() => {
          page?.$navigateBack({
            transition: 'slide',
          });
        }).catch((error) => {
          console.error('Get refresh token error', error);
        });
      } else {
        const error = responseUrl.match(/error=([^&#]+)/)[1];
        if (error === 'access_denied') {
          app.android.foregroundActivity.finish();
        } else {
          console.error('Login error: ', error);
        }
      }
    }
  }

  static refreshAuthToken() {
    if (store.state.reddit.refreshToken) {
      const content = `grant_type=refresh_token&refresh_token=${store.state.reddit.refreshToken}`;
      return this.updateToken(content);
    } else {
      return Promise.reject(new Error('No refresh token'));
    }
  }

  static updateToken(content) {
    return getJSON({
      url: `${this.api}/api/v1/access_token`,
      method: 'POST',
      content,
      headers: {'Authorization': `Basic ${btoa(this.clientId + ':')}`},
    }).then((response) => {
      if (response.expires_in) {
        const payload = {
          authToken: response.access_token,
          validUntil: response.expires_in + Util.getUnixTime(),
          refreshToken: response.refresh_token || store.state.reddit.refreshToken,
        };
        return store.dispatch('login', payload);
      } else {
        return Promise.reject(new Error(response.error));
      }
    });
  }

  static getUser() {
    return this.get(`/api/v1/me?raw_json=1`);
  }

  static getSubredditPosts(subreddit, after=null, sorting=this.sortings.best, group='', time='') {
    return this.getPosts(`${subreddit === this.frontpage ? '' : '/r/' + subreddit}/${sorting}.json?raw_json=1`, after, sorting, time);
  }

  static getUserPosts(user, after=null, sorting=this.sortings.new, group='submitted', time='all', type='all') {
    let url = `/user/${user}/${group}.json?raw_json=1&sort=${sorting}`;
    url += type !== 'all' ? `&type=${type}`: '';
    return this.getPosts(url, after, sorting, time);
  }

  static getPosts(url, after, sorting, time, limit=25) {
    url += `&limit=${limit}` + (after ? `&after=${after}` : '');
    if (time && ['top', 'rising'].includes(sorting)) {
      url += `&t=${time}`;
    }
    return this.get(url);
  }

  static getPostAndComments(permalink, sort='top') {
    return this.get(`${permalink}.json?raw_json=1&sort=${sort}`).then((r) => {
      if (r?.[0]?.data?.children?.length === 1 && r?.[1]?.data?.children?.map) {
        return {
          post: r[0].data.children[0].data,
          comments: r[1].data.children.map((d) => d.data),
        };
      } else {
        return Promise.reject(new Error('Failed to fetch post and comments for ' + permalink));
      }
    });
  }

  static getMoreComments(link, children) {
    return this.post(`/api/morechildren`, `api_type=json&link_id=${link}&children=${children.join(',')}`);
  }

  static getSubscriptions() {
    return this.get('/subreddits/mine/subscriber?raw_json=1&limit=100');
  }

  static subscribe(subreddit, unsub=false) {
    return this.post(`/api/subscribe?action=${unsub ? 'un' : ''}sub&sr_name=${subreddit}`);
  }

  static getMultis() {
    return this.get('/api/multi/mine?raw_json=1');
  }

  static searchForSubreddit(query) {
    return this.get(`/api/search_reddit_names?raw_json=1&include_over_18=true&include_unadvertisable=true&query=${query}`);
  }

  static getSidebar(subreddit) {
    return this.get(`/r/${subreddit}/about.json?raw_json=1`);
  }

  static vote(id, dir) {
    return this.post(`/api/vote?id=${id}&dir=${dir}`);
  }

  static saveOrUnsave(saveable) {
    return this.post(`/api/${saveable.saved ? 'un' : ''}save?id=${saveable.name}`)
        .then(() => saveable.saved = !saveable.saved);
  }

  static get(url) {
    return this.request(url, 'GET');
  }

  static post(url, content) {
    return this.request(url, 'POST', content);
  }

  static request(url, method, content) {
    return this.refreshTokenIfNecessary().then(() => getJSON({
      url: `${this.oauthApi}${url}`,
      method,
      headers: this.getHeaders(),
      content,
    }));
  }

  static refreshTokenIfNecessary() {
    return store.state.reddit.validUntil <= Util.getUnixTime() ? this.refreshAuthToken() : Promise.resolve();
  }
}
