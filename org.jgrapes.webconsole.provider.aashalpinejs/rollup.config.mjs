import typescript from 'rollup-plugin-typescript2';
import sourcemaps from 'rollup-plugin-sourcemaps';
import postcss from 'rollup-plugin-postcss'

let module = "build/generated/resources/js/org/jgrapes/webconsole/provider/aashalpinejs/aash-alpinejs/aash-alpinejs-components.js"

let pathsMap = {
    'alpinejs': '../alpinejs/module.esm.js'    
}

export default {
  external: ["alpinejs" ],
  input: "../aash-ajs-components/lib/aash-alpinejs-components.js",
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
    sourcemaps()
  ]
};
