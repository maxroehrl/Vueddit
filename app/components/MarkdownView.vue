<template>
  <Ripple ref="ripple">
    <Label ref="label"
           textWrap="true"
           @loaded="loaded($event)" />
  </Ripple>
</template>

<script>
import Markdown from '../services/Markdown';

export default {
  name: 'MarkdownView',
  props: {
    text: {
      type: String,
      required: true,
    },
  },
  watch: {
    $props: {
      handler(props) {
        if (this.$refs.label && props.text) {
          this.updateMarkdown(this.$refs.label.nativeView.android, props.text);
        }
      },
      immediate: true,
      deep: true,
    },
  },
  methods: {
    loaded(event) {
      if (this.text) {
        Markdown.setOnTouchListener(event.object.nativeView, (event) => this.$refs.ripple.nativeView.android.onTouchEvent(event));
        this.updateMarkdown(event.object.nativeView, this.text);
      }
    },

    updateMarkdown(tv, text) {
      Markdown.setMarkdown(tv, text);
    },
  },
};
</script>
