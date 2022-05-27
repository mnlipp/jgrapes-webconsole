import typescript from 'rollup-plugin-typescript2';
import postcss from 'rollup-plugin-postcss';

let packagePath = "org/jgrapes/webconlet/examples/login";
let baseName = "Login"
let module = "build/generated/resources/" + packagePath 
    +  "/" + baseName + "-functions.js";

let pathsMap = {
    "vue": "../../page-resource/vue/vue.esm-browser.js",
    "jgconsole": "../../console-base-resource/jgconsole.js",
    "jgwc": "../../page-resource/jgwc-vue-components/jgwc-components.js",
    "l10nBundles": "./" + baseName + "-l10nBundles.ftl.js"
}

export default {
  external: ['vue', 'aash-plugin', 'jgconsole', 'jgwc', 'l10nBundles'],
  input: "src/" + packagePath + "/browser/" + baseName + "-functions.ts",
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
