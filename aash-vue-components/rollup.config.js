import resolve from "@rollup/plugin-node-resolve";
import typescript from "@rollup/plugin-typescript";
import vue from "rollup-plugin-vue";
import replace from '@rollup/plugin-replace';
import {terser} from 'rollup-plugin-terser';

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
        'vue': './vue.esm.re-export.js'
      }
    },
    {
      format: "esm",
      file: packageJson.module.replace(".js", ".min.js"),
      sourcemap: true,
      paths: {
        'vue': './vue.js'
      },
      plugins: [terser()]
    }
  ],
  plugins: [resolve({
      'mainFields': ['module', 'browser'], 
      'browser': true
    }),
    vue(), typescript(),
    replace({
      'process.env.NODE_ENV': JSON.stringify('production')
    })]
};
