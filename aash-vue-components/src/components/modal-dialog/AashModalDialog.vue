<template>
  <div class="aash-modal-dialog dialog__backdrop" :hidden="!isOpen" ref="dialog">
    <div :id="effectiveId" role="dialog" :aria-labelledby="effectiveId + '-label'" 
      aria-modal="true">
      <header :id="effectiveId + '-label'">
        <p>{{ effectiveTitle }}</p>
        <button v-if="showCancel" type="button" class="fa fa-times" 
          v-on:click="cancel()"></button>
      </header>
      <section v-if="content" v-html="content" :class="contentClasses"></section>
      <section v-else :class="contentClasses"><slot></slot></section>
      <footer v-if="applyLabel != '' || okayLabel != ''">
        <button v-if="applyLabel != ''"
            type="button" v-on:click="apply()">{{ applyLabel }}</button>
        <button v-if="okayLabel != ''"
            type="button" v-on:click="close()">{{ okayLabel }}</button>
      </footer>
    </div>
  </div>
</template>

<script lang="ts" src="./AashModalDialog.ts"></script>

<style>
.aash-modal-dialog {
    position: fixed;
    overflow-y: auto;
    top: 0;
    right: 0;
    bottom: 0;
    left: 0;
    z-index: 10000;
}

.aash-modal-dialog [role="dialog"] {
    min-height: 100vh;
    position: absolute;
    top: 2rem;
    left: 50vw;
    transform: translateX(-50%);
    min-width: calc(640px - (15px * 2));
    min-height: auto;
}

.aash-modal-dialog header {
    display: flex;
}

.aash-modal-dialog header > :first-child {
    flex-grow: 1;
}

.aash-modal-dialog footer {
    display: flex;
    justify-content: end;
}
</style>