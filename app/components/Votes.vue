<template>
  <FlexboxLayout flexDirection="column"
                 justifyContent="center"
                 alignContent="center">
    <Label text="▲"
           class="chevron"
           :style="{color: getColor(true)}"
           @tap="vote(true)" />
    <Label :text="getScore(voteable.score)"
           class="vote-label"
           :style="{color: getColor()}" />
    <Label text="▼"
           class="chevron"
           :style="{color: getColor(false)}"
           @tap="vote(false)" />
  </FlexboxLayout>
</template>

<script>
import Reddit from '../services/Reddit';

export default {
  name: 'Votes',
  props: {
    voteable: {
      type: Object,
      required: true,
    },
  },
  methods: {
    getScore(score) {
      return score >= 10000 ? (score / 1000).toFixed(1) + 'k' : score;
    },

    getColor(up) {
      if (this.voteable.likes === up || (up === undefined && this.voteable.likes !== null)) {
        return this.voteable.likes ? '#53ba82' : '#bf5826';
      } else {
        return '#b8b8b8';
      }
    },

    vote(up) {
      let dir = up ? 1 : -1;
      if (this.voteable.likes === up) {
        this.voteable.likes = null;
        this.voteable.score -= dir;
        dir = 0;
      } else {
        this.voteable.likes = up;
        this.voteable.score += dir;
      }
      Reddit.vote(this.voteable.name, dir);
    },
  },
};
</script>

<style scoped>
  .vote-label {
    text-align: center;
    padding: 0;
  }

  .chevron {
    text-align: center;
  }
</style>
