import {android} from 'tns-core-modules/application';
import {request} from 'tns-core-modules/http';
import Login from '../components/Login';
import store from '../store';

export default class Reddit {
  static api = 'https://www.reddit.com';
  static oauthApi = 'https://oauth.reddit.com';
  static userAgent;
  static randomState = Math.random().toString(36).replace(/[^a-z]+/g, '').substr(0, 10);
  static clientId = 'm_gI8cFDcqC7uA';
  static redirectUri = encodeURIComponent('http://localhost:8080');
  static scope = encodeURIComponent('mysubreddits read vote save subscribe wikiread identity');
  static frontpage = 'reddit front page';
  static sortings = {
    best: 'best',
    hot: 'hot',
    top: 'top',
    new: 'new',
    rising: 'rising',
    random: 'random',
    controversial: 'controversial',
  };

  static getHeaders() {
    if (!this.userAgent) {
      this.userAgent = `${android.context.getPackageName()}:${android.context.getPackageManager().getPackageInfo(android.context.getPackageName(), 0).versionName} (by /u/MaxRoehrl)`;
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
        return request({
          url: `${this.api}/api/v1/access_token`,
          method: 'POST',
          content: `grant_type=authorization_code&code=${code}&redirect_uri=${this.redirectUri}`,
          headers: {'Authorization': `Basic ${btoa(this.clientId + ':')}`},
        }).then(this.handleResponse).then(this.updateToken.bind(this)).then(() => {
          if (page) {
            page.$navigateBack({
              transition: 'slide',
            });
          }
        });
      } else {
        const error = responseUrl.match(/error=([^&]+)/)[1];
        if (error === 'access_denied') {
          android.foregroundActivity.finish();
        } else {
          console.log('Login error: ', error);
        }
      }
    }
  }

  static refreshAuthToken() {
    if (store.state.reddit.refreshToken) {
      return request({
        url: `${this.api}/api/v1/access_token`,
        method: 'POST',
        content: `grant_type=refresh_token&refresh_token=${store.state.reddit.refreshToken}`,
        headers: {'Authorization': `Basic ${btoa(this.clientId + ':')}`},
      }).then(this.handleResponse).then(this.updateToken.bind(this));
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
    return this.get(`/api/v1/me`);
  }

  static getPosts(subreddit, after=null, sorting=this.sortings.best, limit=20) {
    subreddit = subreddit === this.frontpage ? '' : `/r/${subreddit}`;
    let url = `${subreddit}/${sorting}.json?limit=${limit}&raw_json=1`;
    url = after ? `${url}&after=${after}` : url;
    return this.get(url);
  }

  static getComments(post) {
    return this.get(`${post.permalink}.json?raw_json=1`);
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

  static getMultisSubreddits(multipath) {
    return this.get(`/api/multi/${multipath}`);
  }

  static searchForSubreddit(query) {
    return this.get(`/api/search_reddit_names?raw_json=1&include_over_18=true&include_unadvertisable=true&query=${query}`);
  }

  static getSidebar(subreddit) {
    return this.get(`/r/${subreddit}/about.json`);
  }

  static vote(id, dir) {
    return this.post(`/api/vote?id=${id}&dir=${dir}`);
  }

  static save(id, category) {
    return this.post(`/api/save?id=${id}&category=${category}`);
  }

  static unsave(id) {
    return this.post(`/api/unsave?id=${id}`);
  }

  static getEmojis(subreddit) {
    return this.get(`/api/v1/${subreddit}/emojis/all`);
  }

  static get(url) {
    return this.refreshTokenIfNecessary().then(() => request({
      url: `${this.oauthApi}${url}`,
      method: 'GET',
      headers: this.getHeaders(),
    })).then(this.handleResponse);
  }

  static post(url, content) {
    return this.refreshTokenIfNecessary().then(() => request({
      url: `${this.oauthApi}${url}`,
      method: 'POST',
      headers: this.getHeaders(),
      content,
    })).then(this.handleResponse);
  }

  static refreshTokenIfNecessary() {
    return store.state.reddit.validUntil <= this.getUnixTime() ? this.refreshAuthToken() : Promise.resolve();
  }

  static handleResponse(response) {
    if (response.statusCode === 200) {
      return response.content.toJSON();
    } else {
      throw new Error('Error: ' + response.statusCode);
    }
  }

  static getPreview(post) {
    const previews = post.preview && post.preview.images;
    if (previews &&
      previews.length &&
      previews[0].resolutions &&
      previews[0].resolutions.length >= 3 &&
      previews[0].resolutions[2].url) {
      return previews[0].resolutions[2].url;
    } else if (previews &&
      previews.length &&
      previews[0].resolutions &&
      previews[0].resolutions.length &&
      previews[0].resolutions[0].url) {
      return previews[0].resolutions[0].url;
    } else {
      return 'res://ic_comment_text_multiple_outline_white_48dp';
    }
  }

  static getUnixTime() {
    return Math.round(new Date().getTime() / 1000);
  }
}
