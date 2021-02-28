import { createApp } from "/node_modules/vue/dist/vue.esm-browser.js";
import { AashDropdownMenu, AashTablist } from "/lib/aash-vue-components.js";

let RootComponent = {
}

const app = createApp(RootComponent);
app.config.globalProperties.window = window;

app.component('aash-dropdown-menu', AashDropdownMenu);
app.component('aash-tablist', AashTablist);

app.mount('#app');
