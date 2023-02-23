import typescript from 'rollup-plugin-typescript2';
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
        return relativeSourcePath.replace(/^([^/]*\/){12}/, "./");
      },
      paths: pathsMap
    }
  ],
  plugins: [
    typescript(),
    postcss()
  ]
};
