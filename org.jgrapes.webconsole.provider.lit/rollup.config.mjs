import { globSync } from 'glob';
import path from 'node:path';
import { nodeResolve } from '@rollup/plugin-node-resolve';

let pathsMap = {
}

export default {
  external: [],
  input: Object.fromEntries(globSync('node_modules/**/*.js')
    .map(file => [path.relative('node_modules',
        path.dirname(file) + '/' + path.basename(file, ".js")), file])),
  output: [
    {
      format: "esm",
      dir: "build/generated/resources/org/jgrapes/webconsole/provider/lit/lit",
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
