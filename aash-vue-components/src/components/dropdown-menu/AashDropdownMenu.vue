<template>
  <div v-bind:id="id" class="dropdown-menu aash-dropdown-menu">
    <button type="button" aria-haspopup="menu"
      v-bind:aria-controls="id + '-menu'" 
      v-bind:aria-expanded="expanded ? 'true' : 'false'" 
      v-on:click="toggle"><span v-html="label"></span></button>
    <ul v-bind:id="id + '-menu'" role="menu">
    <template v-for="item of sortedItems">
      <li role="none"><button type="button" 
        role="menuitem" v-on:click="action(item[1])"
        >{{ item[0] }}</button></li>
    </template>
    </ul>
  </div>
</template>

<script lang="ts" src="./AashDropdownMenu.ts"></script>

<style>
.aash-dropdown-menu {
    display: inline-block;
    position: relative;
}

.aash-dropdown-menu button[aria-expanded=false] + ul {
    display: none;
}

.aash-dropdown-menu button[aria-expanded=true] + ul {
    display: block;
}

.aash-dropdown-menu > ul {
    list-style: none;
    position: absolute;
    z-index: var(--z-index-dropdown, 1000);
    top: 100%;
    left: 0;
    right: auto;    
}
</style>
