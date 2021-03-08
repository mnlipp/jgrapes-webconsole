<template>
  <component :is="type"
    v-bind="type === 'button' ? { type: 'button'}
      : { 'aria-role': 'button', tabindex: '0' }" 
    data-aash-role="disclosure-button"
    :aria-expanded="disclosed ? 'true' : 'false'"
    :aria-controls="idRef"
    @click="toggleDisclosed()"><slot></slot></component>
</template>

<script lang="ts" src="./AashDisclosureButton.ts"></script>
