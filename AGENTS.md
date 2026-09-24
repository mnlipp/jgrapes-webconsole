# JGrapes-WebConsole

## Build System

Build tool is **jdbld** (JDrupes-Builder).

- Build config lives as **Java code** in `_jdbld/src/jdbld/`. Entry point:
  `Root.java`.
- Sub-projects are Java classes extending `AbstractProject`. See
  `_jdbld/src/jdbld/console/`, `provider/`, `conlet/` for module definitions.

### Commands

```bash
./jdbld build               # Build all JARs (excludes WebConsoleExample)
./jdbld test                # Run all tests
./jdbld clean               # Remove generated resources
./jdbld version             # Show module versions
./jdbld javadoc             # Build merged Javadoc
./jdbld apidocs             # Build JS/TS API docs
./jdbld mavenInstallation   # Install to local Maven repo
./jdbld mavenPublication    # Publish to Codeberg Maven repo
./jdbld eclipse             # Generate Eclipse project files
./jdbld -h                  # List available commands
```

Command can be run for sub-projects only, e.g. `./jdbld aash-lit-components::build`.

### Java Requirements

- jdbld itself requires **Java 25+** (`.jdbld.properties` sets `javaHome = /usr/lib/jvm/java-25`).
- All compiled code targets **Java 21** (`--release 21`).
- CI installs `openjdk-25-jdk` for jdbld, sets up Adopt Java 21 for tests.

### Node.js

- `npm install` runs at **root level** first (managed by jdbld's NpmExecutor,
  node v25.7.0).
- **AlpineJS** does **not** use `{{ ... }}` mustache syntax — use
  `x-text="..."` for text interpolation and `x-bind:...` / `:` for
  attributes. Components in `aash-ajs-components` use Alpine directives
  in `innerHTML` templates, not imperative DOM manipulation.
- Modules with `rollup.config.mjs` compile TypeScript to JS
  via `npm run build` -> `rollup -c`.
- Compiled output lands in `build/generated/resources/` and is packaged into
  the JAR.

## Project Layout

Flat monorepo — each module is a root-level directory:

| Pattern | Type | Examples |
|---|---|---|
| `org.jgrapes.webconsole.*` | Console framework | `base`, `vuejs`, `lit` |
| `org.jgrapes.webconsole.provider.*` | JS/CSS library providers | `jquery`, `chartjs`, `lit` |
| `org.jgrapes.webconlet.*` | Conlets (web widgets) | `locallogin`, `oidclogin`, `sysinfo` |
| `aash-ajs-components` | Shared AlpineJS component library (unpublished) | — |
| `aash-vue-components` | Shared Vue component library (unpublished) | — |
| `aash-lit-components` | Shared Lit component library (unpublished) | — |
| `WebConsoleTest` | Merged test project (unpublished) | — |
| `WebConsoleExample` | Demo uber-jar (unpublished, excluded from `build`) | — |
| `WebConsoleOSGiTest` | OSGi runtime test (unpublished) | — |

### Source Layout (per module)

```
src/              main Java sources
resources/        main resources
test/             test sources
test-resources/   test resources
bnd.bnd           OSGi bundle manifest instructions (present → OSGi bundle)
package.json      npm scripts for TypeScript modules
tsconfig.json     TypeScript config
rollup.config.mjs Rollup bundler config
```

### Build Dependencies (important order)

Some modules depend on sibling component libraries built first:

- `aash-ajs-components` -> `org.jgrapes.webconsole.provider.aashalpinejs`
  (provider consumes `lib/` output from aash-ajs-components)
- `aash-lit-components` -> `org.jgrapes.webconsole.provider.aashlit`
  (provider consumes `lib/` output from aash-lit-components)
- `aash-vue-components` -> `org.jgrapes.webconsole.provider.jgwcvuecomponents`
- `org.jgrapes.webconsole.base` -> `org.jgrapes.webconsole.lit`
- `org.jgrapes.webconsole.provider.gridstack` -> `org.jgrapes.webconsole.lit`

jdbld resolves these via `dependency()` declarations. Manual builds must
respect this order.

## TypeScript Modules

Modules that compile TypeScript (19 total):

- **Console**: `org.jgrapes.webconsole.alpinejs`, `org.jgrapes.webconsole.base`,
  `org.jgrapes.webconsole.vuejs`
- **Providers**: `org.jgrapes.webconsole.provider.aashalpinejs` (repackages
  aash-ajs-components), `org.jgrapes.webconsole.provider.aashlit` (repackages
  aash-lit-components),
  `org.jgrapes.webconsole.provider.chartjs`,
  `org.jgrapes.webconsole.provider.jgwcvuecomponents`,
  `org.jgrapes.webconsole.provider.lit` (repackages Lit npm dist),
  `org.jgrapes.webconsole.provider.rcwebcomponents` (repackages),
  `org.jgrapes.webconsole.provider.solidjs`
- **Conlets**: `org.jgrapes.webconlet.locallogin`,
  `org.jgrapes.webconlet.oidclogin`, `org.jgrapes.webconlet.messagebox`,
  `org.jgrapes.webconlet.jmxbrowser`,
  `org.jgrapes.webconlet.examples.hellosolid`
- **Libraries**: `aash-ajs-components`, `aash-lit-components`,
  `aash-vue-components`

Repackaging providers take pre-compiled JS from `node_modules` or sibling
`lib/` and rewrite import paths via Rollup's `paths` function. Their source
maps do NOT chain to the original TypeScript sources.

## Conventions

- **Line length:** max 80 (configured in Eclipse; checkstyle.xml is more
  permissive because sometimes lines cannot be split). Also applies to
  Markdown files.
- **Javadoc:** Markdown format (not JEP 467. Rather a special Javadoc
  processor treats all Javadoc comments as Markdown).
- Eclipse formatter prefs in `_jdbld/resources/jdbld/org.eclipse.jdt.core.formatter.prefs`.
  Copied to projects by `./jdbld eclipse`.

## Code Quality

- **Checkstyle**: `checkstyle.xml`, excludes `_jdbld/`, `test/`, `node_modules/`,
  `build/`, `generated/`.
- **PMD**: `ruleset.xml`, same exclusions plus `bin/`.
- Line limit: **100 chars** (ignoring URLs and import/package lines).

## Versioning

Git tag-based via jdbld's gitversioning extension. Tag prefix is derived from
the module name (dots become hyphens), e.g. `webconsole-base-1.2.0`.

## Testing

- Test dependencies: JUnit 4.13.2 + JUnit 5.14.2 (via BOM) + concurrentunit
  0.4.2.
- Merged test projects implement `MergedTestProject`, compile from `test/`.
- Tests run via `JUnitTestRunner` — just run `./jdbld test`.

## CI

GitHub Actions installs `openjdk-25-jdk`, `xmlstarlet`, `graphviz`; sets
`JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64/`; runs `./jdbld build test`.

## Common Gotchas

- **`bnd.bnd`** in a module directory means it's built as an OSGi bundle.
- Modules whose jdbld config class implements `Unpublishable` are not published
  to Maven.
- `WebConsoleExample` is excluded from `build` automatically.
- The `_jdbld/` directory is the build tool's own project and is excluded from
  all static analysis.
- `javaHome` in `.jdbld.properties` is hardcoded to `/usr/lib/jvm/java-25` —
  override via `JAVA_HOME` env or `-PjavaHome=...`.
- Maven publishing targets **Codeberg**
  (`https://codeberg.org/api/packages/JGrapes/maven`), not Maven Central.
- `cnf/` is a local workspace directory (Eclipse/external tools), not part of
  the build.

## Key jdbld Concepts (for this project)

- Read `_jdbld/src/jdbld/Root.java` for project structure and command definitions.
- Sub-projects are Java classes — each class extending `AbstractProject` is a
  sub-project.
- `prepareProject()` in `Root.java` applies common configuration (compiler, resource
  collector, Eclipse, etc.) to every project.
- Intents (`Expose`, `Consume`, `Reveal`, `Forward`) control dependency visibility.
  Use `Expose` for dependencies your module's consumers need, `Consume` for
  compile-only, `Reveal` for optional forwarding.
- Merged test projects implement `MergedTestProject` and compile from `test/`
  instead of `src/`.
