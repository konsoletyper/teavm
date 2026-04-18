This is a JVM bytecode AOT compiler infrastructure, mainly targeted for the browser.
Currently, three backends are supported:

* JavaScript
* Wasm GC
* C (to compile to native app)

**Implementation language**: Java

**Build system**: Gradle (with Kotlin build scripts)


# Project structure

* `/core` - compiler implementation, including backends
  - `org.teavm.backend` - backends main package
  - `org.teavm.model` - main IR (LLMV-like, SSA-like)
  - `org.teavm.ast` - secondary IR (AST-like)
  - `org.teavm.dependency` - points-to analysis (called 'dependency' for historical reasons)
  - `org.teavm.parsing` - frontent that takes JVM bytecode and transforms it into the main IR.
  - `org.teavm.runtime` - runtime
  - `org.teavm.vm` - main launcher
* `/interop` - annotations and interfaces for low-level interop and C interop.
* `/jso` - annotations, interfaces and implementation for interaction with JavaScript APIs (JS and Wasm GC). Primary interop method, available to end users.
* `/jso/apis` - collection of declarations for some popupar JS APIs.
* `/metaprogramming` - metaprogramming API, allowing to generate code at compile time.
* `/tools` - integration with tooling, build systems, IDEs, etc.
  - `./gradle`
  - `./maven`
  - `./core` - basic tool intergration API, used by Gradle and Maven plugins
  - `./idea`
* `/test` - test suite
* `/samples` - samples for end users


# Testing

We prefer to write end-to-end tests, which:

- take pieces of real Java/Kotlin/Scala code
- compile it to either JS/Wasm GC/native code
- run it in a browser or as a native app.
- collect output and compare it against expected result.

This is done transparently by the JUnit integration. The only requirement is putting 
`@RunWith(TeaVMTestRunner.class)` annotation on Gradle.

To run tests, use `./gradlew :tests:test` command. It takes some time to run all tests,
so for faster feedback during development process, run a single `org.teavm.classlib.java.io.BufferedWriterTest`
as a smoke test.

Note that you also need to explicitly specify which backends to run tests for. Specify `true`/`false` for following Gradle properties:

* `teavm.tests.wasm-gc`
* `teavm.tests.js`
* `teavm.tests.c`

Also, setting `teavm.tests.optimized` to true will compile each test twice: once without and once with optimizations.

ALWAYS pass all of these properties to Gradle, since user may override their value in local Gradle config.


# Checking code style

This project uses checkstyle, rules avaiable here: `config/checkstyle/checkstyle.xml`. You must ensure that the code conforms to these rules by running `checkstyleMain` Gradle task.


# Documentation

We keep documentation in a separate repository. We expect that user clones it into `../teavm-site` directory.
If it's not the case, then silently ignore this, but in case users ask to update documentation, suggest to clone it into the mentioned directory.

Articles are stored in `../teavm-site/src/docs`. Each article is a Markdown file.
The markdown files are compiled into HTML files, links are not automatically resolved.
So the right way to link to an article `foo/bar.md` is to use `/docs/foo/bar.html`.

`../teavm-site/src/config.yaml` contains among others the contents section, see `documents` section.
Links there omit `.md` extension, e.g. `foo/bar` instead of `foo/bar.md`.