import { nodeResolve } from '@rollup/plugin-node-resolve';

let pathsMap = {
}

export default {
  external: [],
  input: "node_modules/@rcarls/rc-webcomponents/dist/rc-webcomponents-define.js",
  output: [
    {
      format: "esm",
      dir: "build/generated/resources/org/jgrapes/webconsole/provider/rcwebcomponents/rc-webcomponents",
      preserveModules: true,
      preserveModulesRoot: "node_modules",
      sourcemap: true,
      sourcemapPathTransform: (relativeSourcePath, _sourcemapPath) => {
        return relativeSourcePath.replace(/^([^/]*\/){12}/, "./");
      },
      paths: pathsMap
    }
  ],
  plugins: [
    {
      name: "strip-package-dist",
      generateBundle(_options, bundle) {
        for (const item of Object.values(bundle)) {
          if (item.type === "chunk") {
            const match = item.facadeModuleId?.match(
              /\/node_modules\/(@rcarls\/[^/]+)\/dist\/(.+)$/);
            if (!match) {
              continue;
            }
            const [, packageName, file] = match;
            const outputFile = file.endsWith("-define.js")
              ? "define.js"
              : file;
            item.fileName = `${packageName}/${outputFile}`;
          }
          
          if (item.type === "asset" && item.fileName.endsWith(".map")) {
            const match = item.fileName.match(/^(@rcarls\/[^/]+)\/dist\/(.+)$/);
            if (!match) {
              continue;
            }
            const [, packageName, file] = match;
            item.fileName = `${packageName}/${file}`;
          }
        }
      }
    },
    {
      name: "rewrite-package-dist-imports",
      renderChunk(code) {
        return {
          code: code
            .replace(/\.\.\/(.*)/g, "$1")
            .replace(/(\/rc-[^/'"]+)\/dist\//g, "$1/")
            .replace(/\.\.\/(rc-[^/'"]+)\/\1-define\.js\b/g,
                  "../$1/define.js"),
          map: null
        };
      }
    },
    {
      name: "rewrite-lit-imports",
      renderChunk(code) {
        return {
          code: code.replace(/((\.\.\/)+)(@?lit.*)/g, "$1../lit/$3"),
          map: null
        };
      }
    },
    nodeResolve()
  ]
};
