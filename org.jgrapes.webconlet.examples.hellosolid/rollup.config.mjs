import typescript from 'rollup-plugin-typescript2';
import { babel } from '@rollup/plugin-babel';
import postcss from 'rollup-plugin-postcss';
import withSolid from "rollup-preset-solid";

let packagePath = "org/jgrapes/webconlet/examples/hellosolid";
let baseName = "HelloSolid"
let module = "build/generated/resources/" + packagePath 
    +  "/" + baseName + "-functions.js";

let pathsMap = {
    "jgconsole": "../../console-base-resource/jgconsole.js",
    "solid-js": "../../page-resource/solidjs/solid.js",
    "solid-js/web": "../../page-resource/solidjs/web/web.js"
}

export default withSolid([{
  external: ['solidjs', 'solidjs/web', 'jgconsole'],
  input: "src/" + packagePath + "/" + baseName + "-functions.tsx",
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
    postcss()
  ]
}]);
