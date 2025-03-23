import { nodeResolve } from '@rollup/plugin-node-resolve';

let moduleBase = "build/generated/resources/org/jgrapes/webconsole/provider/solidjs/solidjs"

export default [{
  external: [],
  input: "node_modules/solid-js/dist/solid.js",
  output: [
    {
      format: "esm",
      file: moduleBase + "/solid.js",
      sourcemap: true,
      sourcemapPathTransform: (relativeSourcePath, _sourcemapPath) => {
        return relativeSourcePath.replace(/^([^/]*\/){12}/, "./");
      }
    }
  ],
  plugins: [
    nodeResolve()
  ]
},
{
  external: ["solid-js"],
  input: "node_modules/solid-js/web/dist/web.js",
  output: [
    {
      format: "esm",
      file: moduleBase + "/web/web.js",
      sourcemap: true,
      sourcemapPathTransform: (relativeSourcePath, _sourcemapPath) => {
        return relativeSourcePath.replace(/^([^/]*\/){12}/, "./");
      },
      paths: {
          "solid-js": "../solid.js"
      }
    }
  ],
  plugins: [
    nodeResolve()
  ]
},
{
  external: ["solid-js"],
  input: "node_modules/solid-js/store/dist/store.js",
  output: [
    {
      format: "esm",
      file: moduleBase + "/store/store.js",
      sourcemap: true,
      sourcemapPathTransform: (relativeSourcePath, _sourcemapPath) => {
        return relativeSourcePath.replace(/^([^/]*\/){12}/, "./");
      },
      paths: {
          "solid-js": "../solid.js"
      }
    }
  ],
  plugins: [
    nodeResolve()
  ]
}];
