import typescript from 'rollup-plugin-typescript2';
import postcss from 'rollup-plugin-postcss';
import vue from "rollup-plugin-vue";

let packagePath = "org/jgrapes/webconlet/jmxbrowser";
let baseName = "JmxBrowser"
let module = "build/generated/resources/" + packagePath 
    +  "/" + baseName + "-functions.js";

let pathsMap = {
    "vue": "../../page-resource/vue/vue.esm-browser.js",
    "jgconsole": "../../console-base-resource/jgconsole.js",
    "aash-plugin": "../../page-resource/aash-vue-components/lib/aash-vue-components.js"
}

export default {
  external: ['vue', 'aash-plugin', 'jgconsole'],
  input: "src/" + packagePath + "/" + baseName + "-functions.ts",
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
    vue({ 'preprocessStyles': true }),
    typescript(),
    postcss()
  ]
};
