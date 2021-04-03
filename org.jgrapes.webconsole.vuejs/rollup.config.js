import typescript from 'rollup-plugin-typescript2';
import {terser} from 'rollup-plugin-terser';

let module = "build/generated/resources/org/jgrapes/webconsole/vuejs/vuejsrenderer.js"

let pathsMap = {
    "@Vue": "../page-resource/vue/vue.esm-browser.js",
    "@JGConsole": "../console-base-resource/jgconsole.esm.js",
    "@Aash": "../page-resource/aash-vue-components/lib/aash-vue-components.js"
}

export default {
  external: ['@Vue', '@Aash', '@JGConsole'],
  input: "resources/org/jgrapes/webconsole/vuejs/VueJsRenderer.ts",
  output: [
    {
      format: "esm",
      file: module,
      sourcemap: true,
      paths: pathsMap
    },
    {
      format: "esm",
      file: module.replace(".js", ".min.js"),
      sourcemap: true,
      paths: pathsMap,
      plugins: [terser()]
    }
  ],
  plugins: [
    typescript()
  ]
};
