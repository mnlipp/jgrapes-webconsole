import Vue from "/node_modules/vue/dist/vue.esm.browser.js";
import { AashDropdownMenu, AashTablist } from "/lib/aash-vue-components.js";

Vue.component('aash-dropdown-menu', AashDropdownMenu);
Vue.component('aash-tablist', AashTablist);

Vue.prototype.window = window;

var app = new Vue({
    el: '#app'
})
