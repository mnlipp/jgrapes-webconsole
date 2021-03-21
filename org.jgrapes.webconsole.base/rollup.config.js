import typescript from 'rollup-plugin-typescript2';
import {terser} from 'rollup-plugin-terser';

import packageJson from "./package.json";

export default {
  input: "resources/org/jgrapes/webconsole/base/JGConsole.ts",
  output: [
    {
      format: "esm",
      file: packageJson.module,
      sourcemap: true
    },
    {
      format: "esm",
      file: packageJson.module.replace(".js", ".min.js"),
      sourcemap: true,
      plugins: [terser()]
    }
  ],
  plugins: [
    typescript()
  ]
};
