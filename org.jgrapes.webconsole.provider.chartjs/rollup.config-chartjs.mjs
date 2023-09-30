import { nodeResolve } from '@rollup/plugin-node-resolve';

let module = "build/generated/resources/org/jgrapes/webconsole/provider/chartjs/chart.js/chart.js"

let pathsMap = {
}

export default {
  external: [],
  input: "node_modules/chart.js/dist/chart.js",
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
    nodeResolve()
  ]
};
