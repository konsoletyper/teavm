# TeaVM benchmarks

Benchmarks that measure performance of code produced by TeaVM. They are written as ordinary
[JMH](https://github.com/openjdk/jmh) benchmarks and run by `:tools:perf`, which compiles each benchmark
with TeaVM and runs it in a browser (JS, Wasm GC) or as a native executable (C).
The same benchmarks can be run with JMH on the JVM, so results can be compared.

## Running

```shell
./gradlew :benchmarks:teavmBenchmark
```

Reports go to `benchmarks/build/reports/teavm-perf`:

* `results.txt`: JMH-like table (also printed to stdout);
* `results-<backend>.json`: results in JMH JSON format (same as `-rf json` in JMH);
* `report.html`: human-readable report comparing all backends and baselines.

To run benchmarks on JVM with JMH, use `./gradlew :benchmarks:jmh`. Results go to
`benchmarks/build/reports/jmh/jvm.json`.

### Gradle properties

| Property                 | Description                                                                                   |
|--------------------------|-----------------------------------------------------------------------------------------------|
| `benchmark.args`         | JMH-style arguments, used by both `teavmBenchmark` and `jmh` tasks (see below)                |
| `benchmark.backends`     | Comma-separated list of `js`, `wasm-gc`, `c`. Default is `js`                                 |
| `benchmark.browser`      | `browser-chrome` (default), `browser-firefox` or `browser` (prints URL to open manually)       |
| `benchmark.optimization` | TeaVM optimization level: `simple`, `advanced`, `full` (default)                              |
| `benchmark.formats`      | Comma-separated list of report formats: `text`, `json`, `html`. Default is all of them        |
| `benchmark.compareJvm`   | When `true`, runs `jmh` task first and includes JVM results into the report                   |
| `benchmark.baseline`     | Comma-separated list of JMH JSON files to compare with, optionally labeled: `label=file.json` |

Supported options in `benchmark.args` (the same as in JMH):

* positional arguments: regular expressions that select benchmarks (matched against any part
  of the fully qualified `Class.method` name);
* `-e <regexp>`: exclude benchmarks;
* `-wi <n>`, `-w <time>`: number and duration of warmup iterations, e.g. `-w 500ms`;
* `-i <n>`, `-r <time>`: number and duration of measurement iterations;
* `-f <n>`: number of forks; in browser every fork runs in a new frame, for C in a new process;
* `-bm <modes>`: `thrpt`, `avgt`, `ss`, `all`;
* `-tu <unit>`: `ns`, `us`, `ms`, `s`, `min`;
* `-p <param>=<v1>,<v2>`: override values of `@Param` fields;
* `-opi <n>`, `-bs <n>`, `-wbs <n>`: operations per invocation, batch sizes;
* `-l`: list benchmarks.

Example:

```shell
./gradlew :benchmarks:teavmBenchmark -Pbenchmark.backends=js,wasm-gc,c \
    -Pbenchmark.compareJvm=true -Pbenchmark.args="-wi 2 -i 3 -p digits=100 StringSplit|Pi"
```

Browser backends need Chrome (`google-chrome-stable`) or Firefox; C backend needs `cc`
(Linux and macOS only, unless custom build script is passed to `:tools:perf` via `--c-build-script`).

## Comparing TeaVM versions

Two scripts help to measure effect of changes in TeaVM. Both run benchmarks twice, then produce
a report where results of the first run are labeled `baseline-<backend>`, and every result of the second
run is compared to the baseline of the same backend. Both take Gradle arguments, like the ones above.

* `benchmarks/compare-stash.sh [gradle args...]`: compares uncommitted changes with `HEAD`. Stashes
  all changes (including untracked files) except for the `benchmarks` module, runs baseline,
  then restores changes.
* `benchmarks/compare-commit.sh <commit> [gradle args...]`: compares current working tree with the given
  commit. Checks out the commit into a temporary git worktree, replaces `benchmarks` module there with
  the current one and runs baseline there. The current working tree is not modified.

In both cases baseline and current runs use the same benchmark code. The baseline commit must already
contain `:tools:perf` and `:benchmarks` modules.

```shell
benchmarks/compare-commit.sh HEAD~1 -Pbenchmark.backends=js,wasm-gc -Pbenchmark.args="String"
```

## Supported subset of JMH

Benchmarks are written using JMH annotations, but only the following subset is supported:

* `@Benchmark`, including methods inherited from superclasses. Return value is consumed by blackhole.
  Benchmark methods may take `Blackhole` and `@State` classes as parameters.
* `@State`. Scope is ignored, since benchmarks always run in a single thread; every state class
  is instantiated once per fork.
* `@Param` on fields of primitive types, boxed primitive types, `String` and enums. Enum and boolean
  parameters without explicit values get all possible values.
* `@Setup` and `@TearDown` with `Level.Trial` and `Level.Iteration`. `Level.Invocation` is not supported.
  Setup and teardown methods must not take parameters.
* `@BenchmarkMode`: `Throughput`, `AverageTime`, `SingleShotTime`, `All`. `SampleTime` falls back to
  `AverageTime`.
* `@OutputTimeUnit`, `@Warmup`, `@Measurement`, `@Fork` (only `value`), `@OperationsPerInvocation`.
* `Blackhole.consume(...)` and `Blackhole.consumeCPU(long)`. `Blackhole` is replaced with
  TeaVM-friendly implementation.

Everything else (`@Threads`, `@Group`, `@CompilerControl`, `@AuxCounters`, JVM arguments of `@Fork`,
injection of `BenchmarkParams` and similar infrastructure objects, profilers) is not supported
and ignored.

Defaults differ from JMH: when neither annotations nor command line specify them, benchmarks run 5 warmup
and 5 measurement iterations, 1 second each, in a single fork. Mode and time unit defaults are the same
as in JMH (throughput, seconds). Command line options take priority over annotations.

Score error is computed like in JMH, as a 99.9% confidence interval using Student's t-distribution.

## Writing benchmarks

Put benchmarks into `src/main/java`. The same rules as for JMH apply: benchmark classes and methods
must be public, state classes must have a public no-arg constructor. Keep in mind that the code is
compiled by TeaVM, so it can only use APIs supported by TeaVM class library.
