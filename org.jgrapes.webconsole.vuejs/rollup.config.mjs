import typescript from 'rollup-plugin-typescript2';
import postcss from 'rollup-plugin-postcss';

let packagePath = "org/jgrapes/webconsole/vuejs";
let baseName = "vuejsrenderer"
let module = "build/generated/resources/" + packagePath 
    +  "/" + baseName + ".js";

let pathsMap = {
    "gridstack": "../page-resource/gridstack/gridstack.js",
    "vue": "../page-resource/vue/vue.esm-browser.js",
    "jgconsole": "../console-base-resource/jgconsole.js",
    "aash-plugin": "../page-resource/aash-vue-components/lib/aash-vue-components.js"
}

export default {
  external: ['jgconsole', 'vue', 'aash-plugin', 'jgconsole'],
  input: "src/" + packagePath + "/VueJsRenderer.ts",
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
