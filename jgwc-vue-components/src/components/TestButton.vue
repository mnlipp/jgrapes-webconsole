<template>
  <button class="test-button" type="button" @click="click()">
    <slot>{{ text }}</slot>
  </button>
</template>

<script lang="ts">
import Vue from "vue";
import Component from "vue-class-component";

const TestButtonProps = Vue.extend({
  props: {
    text: String
  }
})

@Component
export default class TestButton extends TestButtonProps {

  click() {
    this.$emit("click");
  }
}
</script>

<style scoped>
.test-button {
  border-color: black;
}
</style>
