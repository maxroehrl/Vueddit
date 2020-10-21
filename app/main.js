import Vue from 'nativescript-vue';
import VueDevtools from 'nativescript-vue-devtools';
import RadSideDrawerPlugin from 'nativescript-ui-sidedrawer/vue';
import RadListViewPlugin from 'nativescript-ui-listview/vue';
import {decode, encode} from 'base-64';
import store from './store';
import App from './components/App';
import Ripple from './components/Ripple';
import IndentedLabel from './components/IndentedLabel';
import CachedImage from './components/CachedImage';
import LoginWebView from './components/LoginWebView';

if (!global.btoa) {
  global.btoa = encode;
}

if (!global.atob) {
  global.atob = decode;
}

if (TNS_ENV !== 'production') {
  Vue.use(VueDevtools);
}

Vue.config.silent = (TNS_ENV === 'production');
Vue.config.suppressRenderLogs = true;

RadSideDrawerPlugin.install(Vue);
RadListViewPlugin.install(Vue, {});
Vue.registerElement('Ripple', () => Ripple);
Vue.registerElement('IndentedLabel', () => IndentedLabel);
Vue.registerElement('CachedImage', () => CachedImage);
Vue.registerElement('LoginWebView', () => LoginWebView);

new Vue({
  store,
  render: (h) => h('frame', [h(App)]),
}).$start();
