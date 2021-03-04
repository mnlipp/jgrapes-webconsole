import { createApp, ref } from "/node_modules/vue/dist/vue.esm-browser.js";
import AashPlugin from "/lib/aash-vue-components.js";

let RootComponent = {
}

const app = createApp(RootComponent);
app.use(AashPlugin);
app.config.globalProperties.window = window;

app.mount('#app');
