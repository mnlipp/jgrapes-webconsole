import resolve from "@rollup/plugin-node-resolve";
import typescript from 'rollup-plugin-typescript2';
import replace from '@rollup/plugin-replace';
import postcss from 'rollup-plugin-postcss';
import terser from '@rollup/plugin-terser';

import packageJson from "./package.json" with { type: "json" };

export default {
  external: ['lit', 'lit/decorators.js', '@lit/reactive-element' ],
  input: "src/AashComponents.ts",
  output: [
    {
      format: "cjs",
      file: packageJson.main,
      sourcemap: true
    },
    {
      format: "esm",
      file: packageJson.module,
      sourcemap: true
    },
    {
      format: "esm",
      file: packageJson.module.replace('.js', '.min.js'),
      sourcemap: true,
      plugins: [terser()]
    }
  ],
  plugins: [resolve({
      'mainFields': ['module', 'browser'],
      'browser': true
    }),
    typescript(),
    replace({
      'process.env.NODE_ENV': JSON.stringify('production')
    }),
    postcss()
  ]
};
