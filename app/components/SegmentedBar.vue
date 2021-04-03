<template>
  <ScrollView orientation="horizontal"
              scrollBarIndicatorVisible="false"
              padding="0">
    <StackLayout orientation="horizontal"
                 :style="{backgroundColor, padding: '0'}">
      <Ripple v-for="item in items"
              :key="item"
              @tap="tap(item, false)">
        <Label class="item"
               :text="item"
               :style="{borderBottomColor: item === selectedItem ? '#53ba82' : backgroundColor, backgroundColor, padding}" />
      </Ripple>
    </StackLayout>
  </ScrollView>
</template>
<script>
export default {
  name: 'SegmentedBar',
  props: {
    items: {
      type: Array,
      required: true,
    },
    onSelection: {
      type: Function,
      required: true,
    },
    backgroundColor: {
      type: String,
      required: false,
      default: '#3e3e3e',
    },
    padding: {
      type: String,
      required: false,
      default: '50px',
    },
  },
  data() {
    return {
      selectedItem: this.items[0],
    };
  },
  watch: {
    items: {
      handler(items) {
        if (items && !items.includes(this.selectedItem)) {
          this.tap(items[0], true);
        }
      },
    },
  },
  methods: {
    tap(item, noReload) {
      this.selectedItem = item;
      this.onSelection(item, noReload);
    },
  },
};
</script>
<style scoped>
  .item {
    color: #c6c6c6;
    font-size: 12px;
    border-bottom-width: 6px;
    text-transform: uppercase;
  }
</style>
