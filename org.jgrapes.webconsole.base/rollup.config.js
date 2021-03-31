import typescript from 'rollup-plugin-typescript2';
import {terser} from 'rollup-plugin-terser';

let module = "build/generated/resources/org/jgrapes/webconsole/base/jgconsole.esm.js"

export default {
  input: "resources/org/jgrapes/webconsole/base/JGConsole.ts",
  output: [
    {
      format: "esm",
      file: module,
      sourcemap: true
    },
    {
      format: "esm",
      file: module.replace(".js", ".min.js"),
      sourcemap: true,
      plugins: [terser()]
    }
  ],
  plugins: [
    typescript()
  ]
};
