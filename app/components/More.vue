<template>
  <Ripple class="more"
          @tap="onClick(comment)">
    <IndentedLabel ref="label"
                   :text="getText(comment)"
                   @loaded="loaded($event)" />
  </Ripple>
</template>

<script>
export default {
  name: 'Comment',
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
          this.$refs.label.nativeView.android.setDepth(props.comment.depth);
        }
      },
      immediate: true,
      deep: true,
    },
  },
  methods: {
    loaded(event) {
      if (this.comment) {
        event.object.nativeView.setDepth(this.comment.depth);
      }
    },

    getText(comment) {
      return comment.count === 0 ? 'continue this thread' : ('load ' + comment.count + ' more comment' + (comment.count === 1 ? '' : 's'));
    },
  },
};
</script>

<style scoped>
  .more {
    color: #53ba82;
  }
</style>
