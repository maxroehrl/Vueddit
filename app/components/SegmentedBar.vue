<template>
  <ScrollView orientation="horizontal"
              scrollBarIndicatorVisible="false"
              padding="0">
    <StackLayout orientation="horizontal"
                 :style="{backgroundColor: background, padding: '0'}">
      <Ripple v-for="item in items"
              :key="item"
              @tap="tap(item, false)">
        <Label class="item"
               :text="item"
               :style="{borderBottomColor: item === selectedItem ? '#53ba82' : background, backgroundColor: background}" />
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
    background: {
      type: String,
      required: true,
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
    padding: 70px;
  }
</style>
