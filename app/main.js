import Vue from 'nativescript-vue';
import App from './components/App';

import store from './store';
import VueDevtools from 'nativescript-vue-devtools';

import RadSideDrawerPlugin from 'nativescript-ui-sidedrawer/vue';
import RadListViewPlugin from 'nativescript-ui-listview/vue';
import {Ripple} from 'nativescript-ripple2';
import IndentedLabel from './components/IndentedLabel';

import {decode, encode} from 'base-64';

if (!global.btoa) {
  global.btoa = encode;
}

if (!global.atob) {
  global.atob = decode;
}

if (TNS_ENV !== 'production') {
  Vue.use(VueDevtools);
}

// Prints Vue logs when --env.production is *NOT* set while building
// Vue.config.silent = (TNS_ENV === 'production')
// Prints Colored logs when --env.production is *NOT* set while building
// Vue.config.debug = (TNS_ENV !== 'production')

RadSideDrawerPlugin.install(Vue);
RadListViewPlugin.install(Vue, {});
Vue.registerElement('Ripple', () => Ripple);
Vue.registerElement('IndentedLabel', () => IndentedLabel);

new Vue({
  store,
  render: (h) => h('frame', [h(App)]),
}).$start();
