<template>
  <Page actionBarHidden="true"
        @loaded="loaded($event)"
        @unloaded="unloaded($event)">
    <StackLayout>
      <WebView :src="url"
               @loadFinished="onLoadFinished"
               @loadStarted="onLoadStarted" />
    </StackLayout>
  </Page>
</template>

<script>
import * as application from 'tns-core-modules/application';
import {AndroidApplication} from 'tns-core-modules/application';

export default {
  name: 'Login',
  props: {
    url: {
      type: String,
      required: true,
    },
    onAuthorizationSuccessful: {
      type: Function,
      required: true,
    },
  },
  methods: {
    loaded() {
      application.android.on(AndroidApplication.activityBackPressedEvent, (data) => {
        data.cancel = true;
      });
    },

    unloaded() {
      application.android.off(AndroidApplication.activityBackPressedEvent);
    },

    onLoadStarted(args) {
      const androidWebView = args.object.android;
      if (androidWebView) {
        androidWebView.getSettings().setDisplayZoomControls(false);
        // androidWebView.getSettings().setDomStorageEnabled(true);
        androidWebView.getSettings().setLoadWithOverviewMode(true);
      }
    },

    onLoadFinished(args) {
      if (!args.error && args.url && (args.url.includes('code=') || args.url.includes('error='))) {
        this.onAuthorizationSuccessful(args.url);
      }
    },
  },
};
</script>
