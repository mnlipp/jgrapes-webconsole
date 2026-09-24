import typescript from 'rollup-plugin-typescript2';
import postcss from 'rollup-plugin-postcss';

let packagePath = "org/jgrapes/webconsole/alpinejs";
let baseName = "alpinejsrenderer"
let module = "build/generated/resources/" + packagePath 
    +  "/" + baseName + ".js";

export default {
  external: ['jgconsole', 'vue', 'alpinejs',
    'aash-plugin', 'aash-alpinejs', 'jgconsole',
    "lit", /^lit\/.*/, 
    "lit-element", /^lit-element\/.*/, 
    "lit-html", /^lit-html\/.*/,
    "@lit/reactive-element", /^@lit\/reactive-element\/.*/ ],
  input: "src/" + packagePath + "/AlpineJsRenderer.ts",
  output: [
    {
      format: "esm",
      file: module,
      sourcemap: true,
      sourcemapPathTransform: (relativeSourcePath, _sourcemapPath) => {
        return relativeSourcePath.replace(/^([^/]*\/){12}/, "./");
      },
      paths: (id) => {
        const exact = {
          'lit': '../lit/lit/index.js',
          'lit-element': '../lit/lit-element/index.js',
          'lit-html': '../lit/lit-html/lit-html.js',
          '@lit/reactive-element':
            '../lit/@lit/reactive-element/reactive-element.js',
          "gridstack": "../page-resource/gridstack/gridstack.js",
          "vue": "../page-resource/vue/vue.esm-browser.js",
          "jgconsole": "../console-base-resource/jgconsole.js",
          "aash-plugin": "../page-resource/aash-vue-components/lib/aash-vue-components.js",
          "aash-alpinejs": "../page-resource/aash-alpinejs/aash-alpinejs-components.js",
          "alpinejs": "../page-resource/alpinejs/module.esm.js"
        };
        if (id in exact) {
          return exact[id];
        }

        const prefixes = {
          'lit/': '../lit/lit/',
          'lit-element/': '../lit/lit-element/',
          'lit-html/': '../lit/lit-html/',
          '@lit/reactive-element/': '../lit/@lit/reactive-element/',
        };
        for (const [prefix, replacement] of Object.entries(prefixes)) {
          if (id.startsWith(prefix)) {
            return replacement + id.slice(prefix.length);
          }
        }

        return undefined;
      }
    }
  ],
  plugins: [
    typescript(),
    postcss()
  ]
};
