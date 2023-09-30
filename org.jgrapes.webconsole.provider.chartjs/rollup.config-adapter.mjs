import typescript from 'rollup-plugin-typescript2';
import postcss from 'rollup-plugin-postcss'

let module = "build/generated/resources/org/jgrapes/webconsole/provider/chartjs/chart.js/chartjs-adapter-date-std.js"

let pathsMap = {
    "chart.js": "./chart.js",
    "chart.js/types/basic": "",
    "chart.js/types/utils": ""
}

export default {
  external: ['chart.js', 'chart.js/types/utils'],
  input: "src/org/jgrapes/webconsole/provider/chartjs/chartjs-adapter-date-std.ts",
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
   typescript({
        tsconfig: "tsconfig-adapter.json"
    }),
    postcss()
  ]
};
