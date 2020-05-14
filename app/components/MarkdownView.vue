<template>
  <Label ref="label"
         textWrap="true"
         @loaded="loaded($event)" />
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
          this.$refs.label.nativeView.android.setText(Markdown.toMarkdown(props.text));
        }
      },
      immediate: true,
      deep: true,
    },
  },
  methods: {
    loaded(event) {
      if (this.text) {
        event.object.nativeView.setText(Markdown.toMarkdown(this.text));
      }
    },
  },
};
</script>
