import typescript from 'rollup-plugin-typescript2';
import {terser} from 'rollup-plugin-terser';
import path from 'path';
import postcss from 'rollup-plugin-postcss'

let module = "build/generated/resources/org/jgrapes/webconlet/jmxbrowser/jmxbrowser.js"

let pathsMap = {
    "@Vue": "../../page-resource/vue/vue.esm-browser.js",
    "@JGConsole": "../../console-base-resource/jgconsole.esm.js",
    "@Aash": "../../page-resource/aash-vue-components/lib/aash-vue-components.js"
}

export default {
  external: ['@Vue', '@Aash', '@JGConsole'],
  input: "src/org/jgrapes/webconlet/jmxbrowser/JmxBrowser-functions.ts",
  output: [
    {
      format: "esm",
      file: module,
      sourcemap: true,
      sourcemapPathTransform: (relativeSourcePath, sourcemapPath) => {
        return "./" + path.basename(relativeSourcePath);
      },
      paths: pathsMap
    },
    {
      format: "esm",
      file: module.replace(".js", ".min.js"),
      sourcemap: true,
      sourcemapPathTransform: (relativeSourcePath, sourcemapPath) => {
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
