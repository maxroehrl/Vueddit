<template>
  <Page>
    <ActionBar :title="user">
      <NavigationButton text="Back"
                        icon="res://ic_arrow_left"
                        @tap="$navigateBack" />
      <ActionItem text="Refresh"
                  icon="res://ic_menu_refresh"
                  @tap="refresh" />
      <ActionItem text="Toggle list item size"
                  android.position="popup"
                  @tap="toggleTemplate" />
    </ActionBar>
    <StackLayout padding="0">
      <Posts ref="posts"
             :subreddit="{user}"
             :app="app"
             :sortings="['new', 'top', 'hot', 'controversial']" />
    </StackLayout>
  </Page>
</template>

<script>
export default {
  name: 'User',
  components: {Posts: () => import('./Posts')},
  props: {
    user: {
      type: String,
      required: true,
    },
    app: {
      type: Object,
      required: true,
    },
  },
  methods: {
    refresh() {
      this.$refs.posts.refresh();
    },

    toggleTemplate() {
      this.$refs.posts.toggleTemplate();
    },
  },
};
</script>

<style scoped>
  ActionBar {
    background-color: #3e3e3e;
    color: #ffffff;
  }
</style>
