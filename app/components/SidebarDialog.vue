<template>
  <Page @loaded="loaded" @unloaded="unloaded">
    <ScrollView class="sidebar">
      <Label :text="markdown(sidebar)" textWrap="true" />
    </ScrollView>
  </Page>
</template>

<script>
import * as application from 'tns-core-modules/application';
import {AndroidApplication} from 'tns-core-modules/application';
import Markdown from '../services/Markdown';

export default {
  name: 'Sidebar',
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

    markdown(text) {
      return Markdown.toMarkdown(text);
    },
  },
};
</script>

<style scoped>
  .sidebar {
    margin: 20px;
  }
</style>
