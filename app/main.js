import Vue from 'nativescript-vue';
import RadSideDrawerPlugin from 'nativescript-ui-sidedrawer/vue';
import RadListViewPlugin from 'nativescript-ui-listview/vue';
import {decode, encode} from 'base-64';
import store from './store';
import App from './components/App';
import Ripple from './components/Ripple';
import IndentedLabel from './components/IndentedLabel';
import CachedImage from './components/CachedImage';
import AdvancedWebView from './components/AdvancedWebView';
import {Video} from '@nstudio/nativescript-exoplayer';

if (!global.btoa) {
  global.btoa = encode;
}

if (!global.atob) {
  global.atob = decode;
}

RadSideDrawerPlugin.install(Vue);
RadListViewPlugin.install(Vue);
Vue.registerElement('Ripple', () => Ripple);
Vue.registerElement('IndentedLabel', () => IndentedLabel);
Vue.registerElement('CachedImage', () => CachedImage);
Vue.registerElement('AdvancedWebView', () => AdvancedWebView);
Vue.registerElement('ExoPlayer', () => Video);

new Vue({
  store,
  render: (h) => h('frame', [h(App)]),
}).$start();
