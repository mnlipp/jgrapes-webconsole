import resolve from "@rollup/plugin-node-resolve";
import typescript from 'rollup-plugin-typescript2';
import vue from "rollup-plugin-vue";
import replace from '@rollup/plugin-replace';
import postcss from 'rollup-plugin-postcss'

import packageJson from "./package.json";

export default {
  external: ['vue'],
  input: "src/AashPlugin.ts",
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
    }
  ],
  plugins: [resolve({
      'mainFields': ['module', 'browser'], 
      'browser': true
    }),
    vue({ 'preprocessStyles': true }),
    typescript(),
    replace({
      'process.env.NODE_ENV': JSON.stringify('production')
    }),
    postcss()
  ]
};
