import typescript from 'rollup-plugin-typescript2';
import {terser} from 'rollup-plugin-terser';
import path from 'path';
import postcss from 'rollup-plugin-postcss'

let module = "build/generated/resources/org/jgrapes/webconsole/base/jgconsole.js"

export default {
  input: "src/org/jgrapes/webconsole/base/browser/JGConsole.ts",
  output: [
    {
      format: "esm",
      file: module,
      sourcemap: true
      /* Not needed because we use inlineSources (see tsconfig.js):
      sourcemapPathTransform: (relativeSourcePath, _sourcemapPath) => {
        return "./" + path.basename(relativeSourcePath);
      }*/
    },
    {
      format: "esm",
      file: module.replace(".js", ".min.js"),
      sourcemap: true,
      /* Not needed because we use inlineSources (see tsconfig.js):
      sourcemapPathTransform: (relativeSourcePath, _sourcemapPath) => {
        return "./" + path.basename(relativeSourcePath);
      }*/
      plugins: [terser()]
    }
  ],
  plugins: [
    typescript(),
    postcss()
  ]
};
