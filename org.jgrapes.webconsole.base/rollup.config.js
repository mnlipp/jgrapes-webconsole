import typescript from 'rollup-plugin-typescript2';
import {terser} from 'rollup-plugin-terser';
import postcss from 'rollup-plugin-postcss'

let module = "build/generated/resources/org/jgrapes/webconsole/base/jgconsole.js"

export default {
  input: "src/org/jgrapes/webconsole/base/browser/JGConsole.ts",
  output: [
    {
      format: "esm",
      file: module,
      sourcemap: true,
      sourcemapPathTransform: (relativeSourcePath, _sourcemapPath) => {
        return relativeSourcePath.replace(/^([^/]*\/){12}/, "./");
      }
    },
    {
      format: "esm",
      file: module.replace(".js", ".min.js"),
      sourcemap: true,
      sourcemapPathTransform: (relativeSourcePath, _sourcemapPath) => {
        return relativeSourcePath.replace(/^([^/]*\/){12}/, "./");
      },
      plugins: [terser()]
    }
  ],
  plugins: [
    typescript(),
    postcss()
  ]
};
