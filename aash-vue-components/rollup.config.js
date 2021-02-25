import resolve from "@rollup/plugin-node-resolve";
import typescript from "rollup-plugin-typescript2";
import vue from "rollup-plugin-vue";
import replace from '@rollup/plugin-replace';

import packageJson from "./package.json";

export default {
  external: ['vue'],
  input: "src/index.ts",
  output: [
    {
      format: "cjs",
      file: packageJson.main,
      sourcemap: true
    },
    {
      format: "esm",
      file: packageJson.module,
      sourcemap: true,
      paths: {
        'vue': './vue.js'
      }
    }
  ],
  plugins: [resolve({
      'mainFields': ['module', 'browser'], 
      'browser': true
    }),
    replace({
      'process.env.NODE_ENV': JSON.stringify('production')
    }),
    typescript(), vue()]
};
