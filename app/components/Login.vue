<template>
  <Page>
    <ActionBar title="Login" />
    <StackLayout>
      <WebView :src="post.url"
               @loadFinished="onLoadFinished"
               @loadStarted="onLoadStarted" />
    </StackLayout>
  </Page>
</template>

<script>
export default {
  name: 'Login',
  props: {
    post: {
      type: Object,
      required: true,
    },
    onAuthorizationSuccessful: {
      type: Function,
      required: true,
    },
  },
  methods: {
    onLoadStarted(args) {
      const androidWebView = args.object.android;
      if (androidWebView) {
        androidWebView.getSettings().setDisplayZoomControls(false);
        // androidWebView.getSettings().setDomStorageEnabled(true);
        androidWebView.getSettings().setLoadWithOverviewMode(true);
      }
    },

    onLoadFinished(args) {
      if (!args.error && args.url && args.url.includes('#access_token=')) {
        this.onAuthorizationSuccessful(args.url);
      }
    },
  },
};
</script>

<style scoped>
  ActionBar {
    background-color: #53ba82;
    color: #ffffff;
  }

  WebView {
    height: 100%;
    width: 100%;
  }
</style>
