<template>
  <FlexboxLayout flexDirection="column"
                 justifyContent="center"
                 alignSelf="center"
                 alignContent="center"
                 style="width: 15%">
    <!--Up-->
    <Label :text="getScore(post.score)" class="vote-label" />
    <!--Down-->
  </FlexboxLayout>
</template>

<script>
export default {
  name: 'Votes',
  props: {
    post: {
      type: Object,
      required: true,
    },
  },
  data() {
    return {
    };
  },
  methods: {
    getScore(score) {
      return score >= 10000 ? (score / 1000).toFixed(1) + 'k' : score;
    },
  },
};
</script>

<style scoped>
  .vote-label {
    color: #53ba82;
    width: 100%;
    text-align: center;
  }
</style>
