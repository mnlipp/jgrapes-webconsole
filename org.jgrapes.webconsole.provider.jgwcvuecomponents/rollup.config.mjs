import typescript from 'rollup-plugin-typescript2';
import postcss from 'rollup-plugin-postcss'

let module = "build/generated/resources/org/jgrapes/webconsole/provider/jgwcvuecomponents/jgwc-vue-components/jgwc-components.js"

let pathsMap = {
    "vue": "../vue/vue.esm-browser.js",
    "aash-plugin": "../aash-vue-components/lib/aash-vue-components.js",
    "jgconsole": "../../console-base-resource/jgconsole.js"
}

export default {
  external: ['vue', 'aash-plugin', 'jgconsole'],
  input: "src/org/jgrapes/webconsole/provider/jgwcvuecomponents/browser/jgwc-components.ts",
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
