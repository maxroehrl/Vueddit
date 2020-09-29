<template>
  <Ripple class="more"
          @tap="onClick(comment)">
    <IndentedLabel ref="label"
                   textWrap="true"
                   :text="getText(comment)"
                   @loaded="loaded($event)" />
  </Ripple>
</template>

<script>
export default {
  name: 'More',
  props: {
    comment: {
      type: Object,
      required: true,
    },
    onClick: {
      type: Function,
      required: true,
    },
  },
  watch: {
    $props: {
      handler(props) {
        if (this.$refs.label && props.comment) {
          this.$refs.label.nativeView.android.setDepth(props.comment.depth, 50);
        }
      },
      immediate: true,
      deep: true,
    },
  },
  methods: {
    loaded(event) {
      if (this.comment) {
        event.object.nativeView.setDepth(this.comment.depth, 50);
      }
    },

    getText(comment) {
      return comment.count === 0 ? 'continue this thread →' : ('load ' + comment.count + ' more comment' + (comment.count === 1 ? ' ↓' : 's ↓'));
    },
  },
};
</script>

<style scoped>
  .more {
    color: #53ba82;
    padding-left: 6px;
  }
</style>
