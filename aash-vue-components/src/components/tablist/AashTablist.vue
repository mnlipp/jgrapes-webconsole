<template>
  <div v-bind:id="id" class="aash-tablist" role="tablist"
    @keydown="_onKey($event)" @keyup="_onKey($event)">
    <span v-for="panel of panels" :id="panel.id + '-tab'" role="tab"
        :aria-selected="panel.id == selected ? 'true' : 'false'"
        :aria-controls="panel.id">
      <button type="button" :tabindex="panel.id == selected ? 0 : -1"
        v-on:click="selectPanel(panel.id)">{{ _label(panel) }}</button><button
      type="button" v-if="panel.removeCallback" class="fa fa-times"
        v-on:click="panel.removeCallback()" tabindex="-1"></button>
    </span>
  </div>
</template>    

<script lang="ts" src="./AashTablist.ts"></script>
