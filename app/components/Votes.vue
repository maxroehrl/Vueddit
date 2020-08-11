<template>
  <FlexboxLayout flexDirection="column"
                 justifyContent="center"
                 alignContent="center">
    <Ripple rippleColor="#53ba82"
            class="chevron"
            @tap="vote(true)">
      <Image src="res://ic_chevron_up_white_18dp"
             loadMode="async"
             :tintColor="getColor(true)" />
    </Ripple>
    <Label :text="getScore(post.score)"
           class="vote-label"
           :style="{color: getColor()}" />
    <Ripple rippleColor="#53ba82"
            class="chevron"
            @tap="vote(false)">
      <Image src="res://ic_chevron_down_white_18dp"
             loadMode="async"
             :tintColor="getColor(false)" />
    </Ripple>
  </FlexboxLayout>
</template>

<script>
import Reddit from '../services/Reddit';

export default {
  name: 'Votes',
  props: {
    post: {
      type: Object,
      required: true,
    },
  },
  methods: {
    getScore(score) {
      return score >= 10000 ? (score / 1000).toFixed(1) + 'k' : score;
    },

    getColor(up) {
      if (this.post.likes === up || (up === undefined && this.post.likes !== null)) {
        return this.post.likes ? '#53ba82' : '#bf5826';
      } else {
        return '#b8b8b8';
      }
    },

    vote(up) {
      let dir = up ? 1 : -1;
      if (this.post.likes === up) {
        this.post.likes = null;
        this.post.score -= dir;
        dir = 0;
      } else {
        this.post.likes = up;
        this.post.score += dir;
      }
      Reddit.vote(this.post.name, dir);
    },
  },
};
</script>

<style scoped>
  .vote-label {
    text-align: center;
    height: 100px;
  }

  .chevron {
    height: 90px;
  }
</style>
