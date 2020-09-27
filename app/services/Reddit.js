import * as app from '@nativescript/core/application';
import {getJSON} from '@nativescript/core/http';
import {Screen} from '@nativescript/core/platform';
import moment from 'moment';
import Login from '../components/Login';
import store from '../store';

export default class Reddit {
  static api = 'https://www.reddit.com';
  static oauthApi = 'https://oauth.reddit.com';
  static userAgent;
  static randomState = Math.random().toString(36).replace(/[^a-z]+/g, '').substr(0, 10);
  static clientId = 'm_gI8cFDcqC7uA';
  static redirectUri = encodeURIComponent('http://localhost:8080');
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
    const url = `${this.api}/api/v1/authorize.compact?client_id=${this.clientId}&response_type=code&state=${this.randomState}&redirect_uri=${this.redirectUri}&scope=${this.scope}&duration=permanent`;
    page.$navigateTo(Login, {
      transition: 'slide',
      props: {
        url,
        onAuthorizationSuccessful: this.getRefreshToken.bind(this, page),
      },
    });
  }

  static getRefreshToken(page, responseUrl) {
    const state = responseUrl.match(/state=([^&]+)/)[1];
    if (state === this.randomState) {
      if (responseUrl.includes('code=')) {
        const code = responseUrl.match(/code=([^&]+)/)[1];
        return getJSON({
          url: `${this.api}/api/v1/access_token`,
          method: 'POST',
          content: `grant_type=authorization_code&code=${code}&redirect_uri=${this.redirectUri}`,
          headers: {'Authorization': `Basic ${btoa(this.clientId + ':')}`},
        }).then(this.updateToken.bind(this)).then(() => {
          if (page) {
            page.$navigateBack({
              transition: 'slide',
            });
          }
        });
      } else {
        const error = responseUrl.match(/error=([^&]+)/)[1];
        if (error === 'access_denied') {
          app.android.foregroundActivity.finish();
        } else {
          console.log('Login error: ', error);
        }
      }
    }
  }

  static refreshAuthToken() {
    if (store.state.reddit.refreshToken) {
      return getJSON({
        url: `${this.api}/api/v1/access_token`,
        method: 'POST',
        content: `grant_type=refresh_token&refresh_token=${store.state.reddit.refreshToken}`,
        headers: {'Authorization': `Basic ${btoa(this.clientId + ':')}`},
      }).then(this.updateToken.bind(this));
    } else {
      return Promise.reject(new Error('No refresh token'));
    }
  }

  static updateToken(response) {
    const payload = {
      authToken: response.access_token,
      validUntil: response.expires_in + this.getUnixTime(),
      refreshToken: response.refresh_token || store.state.reddit.refreshToken,
    };
    return store.dispatch('login', payload);
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

  static getPostAndComments(permalink, comment='', sort='top') {
    return this.get(`${permalink}${comment}.json?raw_json=1&sort=${sort}`).then((r) => {
      if (r && r.length === 2 &&
        r[0].data && r[0].data.children && r[0].data.children.length === 1 &&
        r[1].data && r[1].data.children) {
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
    return store.state.reddit.validUntil <= this.getUnixTime() ? this.refreshAuthToken() : Promise.resolve();
  }

  static getImage(post) {
    return this.getPreview(post, Screen.mainScreen.widthPixels, {url: ''});
  }

  static getPreview(post, preferredWidth=300, noPreview={url: 'res://ic_comment_text_multiple_outline_white_48dp'}) {
    if (post && post.preview && post.preview.images && post.preview.images[0] && post.preview.images[0].resolutions) {
      const resolutions = post.preview.images[0].resolutions;
      const distArr = resolutions.map((resolution) => Math.abs(resolution.width - preferredWidth));
      return resolutions[distArr.indexOf(Math.min(...distArr))];
    } else if (post.thumbnail && !['self', 'default'].includes(post.thumbnail)) {
      return {url: post.thumbnail, height: post.thumbnail_height, width: post.thumbnail_width};
    } else {
      return noPreview;
    }
  }

  static getAspectFixHeight({height, width}) {
    if (height && width) {
      return (height * Screen.mainScreen.widthPixels / width).toFixed(0) + 'px';
    } else {
      return '0px';
    }
  }

  static getTimeFromNow(unixTime) {
    return moment(unixTime * 1000).fromNow();
  }

  static getUnixTime() {
    return Math.round(new Date().getTime() / 1000);
  }
}
