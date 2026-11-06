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
| `benchmark.optimization` | TeaVM optimization level: `simple`, `advanced` (default), `full`                              |
| `benchmark.formats`      | Comma-separated list of report formats: `text`, `json`, `html`. Default is all of them        |
| `benchmark.profiler`     | Profilers, same as `-prof` option. Only `cpu` is supported, see [Profiling](#profiling)       |
| `benchmark.c.compiler`   | Script that compiles generated C code. Default is selected by OS, see below                   |
| `benchmark.c.envScript`  | Script that sets up C compiler environment. Default is `setup-msvc-env.bat` on Windows        |
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
* `-prof cpu`: record CPU profiles of measurement iterations, see [Profiling](#profiling);
* `-l`: list benchmarks.

Example:

```shell
./gradlew :benchmarks:teavmBenchmark -Pbenchmark.backends=js,wasm-gc,c \
    -Pbenchmark.compareJvm=true -Pbenchmark.args="-wi 2 -i 3 -p digits=100 StringSplit|Pi"
```

Browser backends need Chrome (`google-chrome-stable`) or Firefox.

C backend compiles generated code with a script that is selected depending on OS, like in the `tests` module:

* Linux: `compile-c-unix.sh` (`gcc -O2`);
* macOS: `compile-c-macos.sh` (`clang -O2`);
* Windows: `compile-c-windows.bat` (MSVC `cl /O2`); the developer environment is set up by
  `setup-msvc-env.bat`, which requires Visual Studio with C++ build tools.

A script runs in the directory with generated C code and must produce `benchmark` executable
(`benchmark.exe` on Windows). Use `benchmark.c.compiler` to pass your own script.

## Profiling

`-prof cpu` records a CPU profile of the measurement iterations of every benchmark (warmup iterations are not
included). It's supported for `js` and `wasm-gc` backends running in Chrome; other backends run without profiling.

```shell
./gradlew :benchmarks:teavmBenchmark -Pbenchmark.backends=js,wasm-gc -Pbenchmark.profiler=cpu \
    -Pbenchmark.args="-p kind=random StringBuilderBenchmark"
```

`-Pbenchmark.profiler=cpu` is the same as passing `-prof cpu` in `benchmark.args`.

For every benchmark (and every combination of parameters and every fork), the run log shows functions that take
most of the time, and the full profile is written to
`benchmarks/build/reports/teavm-perf/profiles/<backend>/<benchmark>.cpuprofile`. These files can be opened in Chrome
DevTools (Performance panel, *Load profile*) or in tools like [speedscope](https://www.speedscope.app/).

Profiles are collected via Chrome DevTools protocol: benchmark calls `console.profile()` before the first measurement
iteration and `console.profileEnd()` after the last one. Note that when profiling is on, JS code is not obfuscated,
so that profiles show meaningful function names, and the sampling profiler itself slows down benchmarks a little.
So don't compare scores of runs with and without profiling.

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

Defaults differ from JMH: when neither annotations nor command line specify them, benchmarks run 3 warmup
iterations, 300 ms each, and 5 measurement iterations, 500 ms each, in a single fork. Code produced by TeaVM
needs much less warmup than JVM: C code is compiled ahead of time, and browsers tier up hot code quickly.
Mode and time unit defaults are the same as in JMH (throughput, seconds).
Command line options take priority over annotations.

Score error is computed like in JMH, as a 99.9% confidence interval using Student's t-distribution.

## Writing benchmarks

Put benchmarks into `src/main/java`. The same rules as for JMH apply: benchmark classes and methods
must be public, state classes must have a public no-arg constructor. Keep in mind that the code is
compiled by TeaVM, so it can only use APIs supported by TeaVM class library.
