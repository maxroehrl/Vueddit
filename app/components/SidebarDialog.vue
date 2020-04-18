<template>
  <Frame>
    <Page>
      <ActionBar title="Sidebar" />
      <StackLayout>
        <MarkdownView :markdown="sidebar"
                      textWrap="true"
                      class="sidebar-text" />
        <Button text="Close" @tap="$modal.close" />
      </StackLayout>
    </Page>
  </Frame>
</template>

<script>
import Reddit from '../services/Reddit';

export default {
  name: 'Sidebar',
  props: {
    subreddit: {
      type: String,
      required: true,
    },
  },
  data() {
    return {
      sidebar: '',
    };
  },
  methods: {
    loaded() {
      Reddit.getSidebar(this.subreddit).then((response) => {
        if (response && response.data) {
          this.sidebar = response.data;
        }
      });
    },
  },
};
</script>

<style scoped>
  .sidebar-text {
  }
</style>
