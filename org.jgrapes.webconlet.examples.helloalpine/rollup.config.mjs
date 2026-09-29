import typescript from 'rollup-plugin-typescript2';
import postcss from 'rollup-plugin-postcss';

let packagePath = "org/jgrapes/webconlet/examples/helloalpine";
let baseName = "HelloAlpine";
let module = "build/generated/resources/" + packagePath
    + "/" + baseName + "-functions.js";

let pathsMap = {
    "jgconsole": "../../console-base-resource/jgconsole.js",
    "alpinejs": "../../page-resource/alpinejs/module.esm.js"
};

export default {
  external: ["jgconsole", "alpinejs"],
  input: "src/" + packagePath + "/browser/" + baseName + "-functions.ts",
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
    typescript(),
    postcss()
  ]
};
