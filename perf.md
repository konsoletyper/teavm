New JMH-compatible (subset of) benchmark runner.

* `:tools:perf` - tool (like `:tools:junit`) that TeaVM users can use to run benchmarks
* `:benchmarks` - module with microbenchmarks (and macrobenchmarks too) to measure actual TeaVM code patterns

`:tools:perf` should be inspired by `:tools:junit`. It can also reuse `:tools:browser-runner` module
to run JS/Wasm benchmarks in browser.

The whole architecture of `:tools:perf`:

* A template entry point with some native methods that performs benchmark phases
* An IR transformer (ClassHolderTransformer) that turns template entry point native methods with actual
  calls to benchmark worker methods
* Runner can run these self-contained modules, collect data and write reports (both machine-readable and 
  human-readable report, e.g. HTML and stdout text)
* A CLI entry point that takes parameters: which benchmarks to run, on which backends, how many iterations,
  output format (e.g. need to produce human-readable HTML report), report output folder, etc.
* This tool should understand some reasonable subset of most frequently used JMH annotations (consider it's an MVP),
  like: `@Benchmark`, `@State` (with ignored scope or forced scope = thread requirements), `Blackhole.consume`
* `Blackhole` class should be replaced with TeaVM-friendly implementation using substitution policy
  

`:benchmarks` should define Gradle tasks to easily run the `:tools:perf` CLI, with the ability to pass parameters
to the task that runs the benchmark.

Benchmark should also contain few simple benchmarks, like:

* `StringBuilder.append(double)`
* `String.replace(String)` (including case of no-op)
* `String.split(String)` (including case of single-char)
* PI calculator from `samples` directory - as a macrobenchmark

*Highly desirable*: the ability to compare results with actual JMH. For instance, if it's possible to
make JMH produce machine-readable output, produce own machine-readable output in the same format.
