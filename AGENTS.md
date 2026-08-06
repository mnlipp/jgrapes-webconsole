# JGrapes-WebConsole

## Build System

- **Primary build tool: `jdbld`** — NOT Gradle. The `build.gradle` files are legacy artifacts, not used.
- Build: `./jdbld build`
- Test: `./jdbld test`
- Eclipse project files: `./jdbld eclipse`
- Javadoc: `./jdbld javadoc`
- Build configuration lives in `_jdbld/src/jdbld/` (Java classes, one per module).

### Java Requirements

- Code targets **Java 21** (`--release 21`).
- `jdbld` itself needs Java 25+ (`.jdbld.properties` sets `javaHome`).
- CI uses `openjdk-25` to run jdbld, tests run on Java 21.

## Project Layout

Flat monorepo — each module is a directory at root level:

| Pattern | Type | Examples |
|---|---|---|
| `org.jgrapes.webconsole.*` | Console framework | `org.jgrapes.webconsole.base`, `org.jgrapes.webconsole.vuejs` |
| `org.jgrapes.webconsole.provider.*` | JS/CSS library providers | `jquery`, `chartjs`, `vue`, `gridstack` |
| `org.jgrapes.webconlet.*` | Conlets (web widgets) | `locallogin`, `oidclogin`, `sysinfo` |
| `aash-vue-components` | Shared Vue component library (unpublished) | — |
| `WebConsoleTest` | Merged test project (unpublished) | — |
| `WebConsoleExample` | Demo uber-jar app (unpublished) | — |
| `WebConsoleOSGiTest` | OSGi runtime test (unpublished) | — |

### Source Layout (per module)

```
src/              — main Java sources
resources/        — main resources
test/             — test sources
test-resources/   — test resources
bnd.bnd           — OSGi bundle manifest instructions (if present → OSGi bundle)
package.json      — npm scripts for TypeScript modules (if present)
tsconfig.json     — TypeScript config (if present)
rollup.config.mjs — Rollup bundler config (if present)
```

## TypeScript/JS Modules

Some modules ship compiled JavaScript from TypeScript sources. The build pipeline is:

1. `npm install` (root-level, run by jdbld's NpmExecutor, node v25.7.0)
2. `npm run build` → `rollup -c` compiles TypeScript to JS
3. Compiled output goes to `build/generated/resources/` and is packaged into the JAR

Modules with TypeScript: `org.jgrapes.webconsole.base`, `org.jgrapes.webconsole.vuejs`, `org.jgrapes.webconsole.provider.jgwcvuecomponents`, `aash-vue-components`, and several conlets.

## Code Quality

- **Checkstyle**: config in `checkstyle.xml`, excludes `_jdbld/`, `test/`, `node_modules/`, `build/`, `generated/`.
- **PMD**: config in `ruleset.xml`, same exclusions plus `bin/`.
- Line limit: 100 chars (ignoring URLs and import/package lines).

## Versioning

Git tag-based via jdbld's gitversioning extension. Each module has its own tag prefix derived from the module name (dots → hyphens), e.g. `webconsole-base-1.2.0`.

## Common Gotchas

- The `build.gradle` at root is **not** used — do not run `./gradlew`.
- `bnd.bnd` in a module directory means it's built as an OSGi bundle.
- Modules implementing `Unpublishable` in their jdbld config class are not published to Maven Central.
- `WebConsoleExample` is excluded from the `build` command alias (only `WebConsoleTest` and libraries are built).
- The `_jdbld/` directory contains the build tool's own project and is excluded from all static analysis.
