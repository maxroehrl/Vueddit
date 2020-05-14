<template>
  <Page @loaded="loaded" @unloaded="unloaded">
    <ScrollView class="sidebar">
      <MarkdownView :text="sidebar" />
    </ScrollView>
  </Page>
</template>

<script>
import * as application from 'tns-core-modules/application';
import {AndroidApplication} from 'tns-core-modules/application';
import MarkdownView from './MarkdownView';

export default {
  name: 'Sidebar',
  components: {MarkdownView},
  props: {
    sidebar: {
      type: String,
      required: true,
    },
  },
  methods: {
    loaded() {
      application.android.on(AndroidApplication.activityBackPressedEvent, (data) => {
        this.$modal.close();
      });
    },

    unloaded() {
      application.android.off(AndroidApplication.activityBackPressedEvent);
    },
  },
};
</script>

<style scoped>
  .sidebar {
    margin: 20px;
  }
</style>
