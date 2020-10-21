<template>
  <Page actionBarHidden="true"
        @loaded="loaded($event)"
        @unloaded="unloaded($event)">
    <StackLayout>
      <LoginWebView :src="url"
                    :shouldOverrideUrlLoading="onShouldOverrideUrlLoading"
                    @loadFinished="onLoadFinished"
                    @loadStarted="onLoadStarted" />
    </StackLayout>
  </Page>
</template>

<script>
import * as application from '@nativescript/core/application';
import {AndroidApplication} from '@nativescript/core/application';
import Reddit from '../services/Reddit';

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

    onShouldOverrideUrlLoading(url) {
      const redirectUri = Reddit.redirectUri;
      return !!(url?.startsWith?.(redirectUri) || url?.getUrl?.().toString?.().startsWith?.(redirectUri));
    },

    onLoadStarted(args) {
      const androidWebView = args.object.android;
      if (androidWebView) {
        androidWebView.getSettings().setDisplayZoomControls(false);
        androidWebView.getSettings().setLoadWithOverviewMode(true);
        androidWebView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
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
