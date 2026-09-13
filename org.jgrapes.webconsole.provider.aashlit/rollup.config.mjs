import typescript from 'rollup-plugin-typescript2';
import sourcemaps from 'rollup-plugin-sourcemaps';
import postcss from 'rollup-plugin-postcss'

let module = "build/generated/resources/js/org/jgrapes/webconsole/provider/aashlit/aash-lit/aash-lit-components.js"

export default {
  external: ["lit", /lit\/.*/, 
    "lit-element", /lit-element\/.*/, 
    "lit-html", /lit-html\/.*/,
    "@lit/reactive-element", /@lit\/reactive-element\/.*/ ],
  input: "../aash-lit-components/lib/aash-lit-components.js",
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
    sourcemaps()
  ]
};
