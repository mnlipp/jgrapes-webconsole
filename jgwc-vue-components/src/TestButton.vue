<template>
  <button class="test-button" type="button" @click="click()">
    <slot>{{ text }}</slot>
  </button>
</template>

<script lang="ts">
import Vue from "vue";
import { Component, Prop } from "vue-property-decorator";

@Component
export default class TestButton extends Vue {
  @Prop({ type: String, required: true }) readonly text!: string;

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
