import typescript from 'rollup-plugin-typescript2';
import {terser} from 'rollup-plugin-terser';
import path from 'path';
import postcss from 'rollup-plugin-postcss'

let module = "build/generated/resources/org/jgrapes/webconsole/vuejs/vuejsrenderer.js"

let pathsMap = {
    "@Vue": "../page-resource/vue/vue.esm-browser.js",
    "@JGConsole": "../console-base-resource/jgconsole.js",
    "@Aash": "../page-resource/aash-vue-components/lib/aash-vue-components.js"
}

export default {
  external: ['@Vue', '@Aash', '@JGConsole'],
  input: "src/org/jgrapes/webconsole/vuejs/VueJsRenderer.ts",
  output: [
    {
      format: "esm",
      file: module,
      sourcemap: true,
      sourcemapPathTransform: (relativeSourcePath, _sourcemapPath) => {
        return "./" + path.basename(relativeSourcePath);
      },
      paths: pathsMap
    },
    {
      format: "esm",
      file: module.replace(".js", ".min.js"),
      sourcemap: true,
      sourcemapPathTransform: (relativeSourcePath, _sourcemapPath) => {
        return "./" + path.basename(relativeSourcePath);
      },
      paths: pathsMap,
      plugins: [terser()]
    }
  ],
  plugins: [
    typescript(),
    postcss()
  ]
};
