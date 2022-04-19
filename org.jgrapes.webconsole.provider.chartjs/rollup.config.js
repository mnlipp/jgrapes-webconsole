let module = "build/generated/resources/org/jgrapes/webconsole/provider/chartjs/chart.js/adapters/chartjs-adapter-luxon.js";

let pathsMap = {
    "chart.js": "../dist/chart.esm.js",
    "luxon": "../../luxon/build/es6/luxon.js"
}

export default {
  external: ['chart.js', 'luxon'],
  input: "node_modules/chartjs-adapter-luxon/dist/chartjs-adapter-luxon.esm.js",
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
  ]
};
