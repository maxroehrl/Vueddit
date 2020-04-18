<template>
  <Page>
    <ActionBar title="ImageViewer">
      <NavigationButton text="Back"
                        icon="res://ic_arrow_left"
                        @tap="$navigateBack" />
    </ActionBar>
    <StackLayout>
      <WebView ref="webview"
               :src="post.url"
               @loadFinished="onLoadFinished"
               @loadStarted="onLoadStarted" />
    </StackLayout>
  </Page>
</template>

<script>
export default {
  name: 'ImageViewer',
  components: {},
  props: {
    post: {
      type: Object,
      required: true,
    },
  },
  methods: {
    onLoadStarted(args) {
      const androidWebView = args.object.android;
      if (androidWebView) {
        androidWebView.getSettings().setDisplayZoomControls(false);
        androidWebView.getSettings().setDomStorageEnabled(true);
        androidWebView.getSettings().setLoadWithOverviewMode(true);
      }
    },

    onLoadFinished(args) {

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
