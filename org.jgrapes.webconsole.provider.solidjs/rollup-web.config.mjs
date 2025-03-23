import { nodeResolve } from '@rollup/plugin-node-resolve';

let module = "build/generated/resources/org/jgrapes/webconsole/provider/solidjs/solidjs/web/web.js"

let pathsMap = {
    "solid-js": "../solid.js"
}

export default {
  external: ["solid-js"],
  input: "node_modules/solid-js/web/dist/web.js",
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
