/*
 *  Copyright 2026 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.perf;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.teavm.backend.c.CTarget;
import org.teavm.backend.c.generate.CNameProvider;
import org.teavm.perf.runtime.BenchmarkNativeEntryPoint;

/**
 * Compiles benchmarks to C, then compiles C code to an executable and runs it. Every fork runs in a separate
 * process.
 */
public class CBackend extends BenchmarkBackend {
    private static final boolean WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
    private static final boolean MACOS = System.getProperty("os.name").toLowerCase().startsWith("mac");
    private static final boolean LINUX = System.getProperty("os.name").toLowerCase().contains("linux");
    private static final String EXECUTABLE_NAME = WINDOWS ? "benchmark.exe" : "benchmark";

    private String compiler = WINDOWS ? "cl" : "cc";
    private List<String> compilerFlags = List.of(WINDOWS ? "/O2" : "-O2");
    private File buildScript;
    private File envScript;
    private Map<String, String> capturedEnvironment;

    public CBackend(BenchmarkEnvironment environment) {
        super(environment);
    }

    public void setCompiler(String compiler) {
        this.compiler = compiler;
    }

    public void setCompilerFlags(List<String> compilerFlags) {
        this.compilerFlags = List.copyOf(compilerFlags);
    }

    /**
     * Sets script that is used to compile C code instead of invoking C compiler directly. The script runs in
     * the directory with generated C code and must produce {@code benchmark} executable
     * ({@code benchmark.exe} on Windows) in the same directory.
     */
    public void setBuildScript(File buildScript) {
        this.buildScript = buildScript;
    }

    /**
     * Sets script that prepares environment for C compiler (for example, sets up MSVC developer environment).
     * The script runs once, then the environment it produces is used for every compiler invocation.
     */
    public void setEnvScript(File envScript) {
        this.envScript = envScript;
    }

    @Override
    public String getName() {
        return "c";
    }

    @Override
    public String getDescription() {
        return "TeaVM C";
    }

    @Override
    public CompiledBenchmark compile(BenchmarkInfo benchmark, File directory) throws BenchmarkException {
        var target = new CTarget(new CNameProvider());
        build(target, BenchmarkNativeEntryPoint.class.getName(), benchmark, directory, "");

        var command = buildScript != null ? scriptCommand(buildScript) : defaultCompilerCommand();
        try {
            var processBuilder = new ProcessBuilder(command)
                    .directory(directory)
                    .redirectErrorStream(true);
            processBuilder.environment().putAll(getEnvironment());
            var process = processBuilder.start();
            var output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new BenchmarkException("Error compiling C code of " + benchmark.getName() + " (command: "
                        + String.join(" ", command) + "):\n" + output);
            }
        } catch (IOException e) {
            throw new BenchmarkException("Error running C compiler: " + String.join(" ", command), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BenchmarkException("Interrupted");
        }

        var executable = new File(directory, EXECUTABLE_NAME);
        if (!executable.exists()) {
            throw new BenchmarkException("C compiler did not produce " + executable);
        }
        return new CompiledBenchmark(benchmark, directory, executable);
    }

    private List<String> defaultCompilerCommand() {
        var command = new ArrayList<String>();
        command.add(compiler);
        if (WINDOWS) {
            command.addAll(List.of("/nologo", "/std:c11", "/utf-8"));
            command.addAll(compilerFlags);
            command.add("all.c");
            command.add("/Fe:" + EXECUTABLE_NAME);
        } else {
            command.add("-std=c11");
            command.addAll(compilerFlags);
            if (MACOS) {
                command.add("-D_DARWIN_C_SOURCE");
            }
            command.add("-o");
            command.add(EXECUTABLE_NAME);
            command.add("all.c");
            command.add("-lm");
            if (LINUX) {
                command.add("-lrt");
            }
        }
        return command;
    }

    private static List<String> scriptCommand(File script) {
        var name = script.getName().toLowerCase();
        if (name.endsWith(".bat") || name.endsWith(".cmd")) {
            return List.of("cmd", "/c", script.getAbsolutePath());
        } else if (name.endsWith(".sh")) {
            return List.of("bash", script.getAbsolutePath());
        } else {
            return List.of(script.getAbsolutePath());
        }
    }

    private Map<String, String> getEnvironment() throws BenchmarkException {
        if (envScript == null) {
            return Map.of();
        }
        if (capturedEnvironment == null) {
            capturedEnvironment = captureEnvironment(envScript);
        }
        return capturedEnvironment;
    }

    // Runs script in a shell, then prints the resulting environment, so that it can be passed to
    // every subsequent compiler invocation without running the script again.
    private static Map<String, String> captureEnvironment(File script) throws BenchmarkException {
        var name = script.getName().toLowerCase();
        List<String> command;
        if (name.endsWith(".bat") || name.endsWith(".cmd")) {
            command = List.of("cmd", "/c", "call \"" + script.getAbsolutePath() + "\" && set");
        } else {
            command = List.of("bash", "-c", "source \"" + script.getAbsolutePath() + "\" && env");
        }
        String output;
        try {
            var process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.INHERIT).start();
            output = new String(process.getInputStream().readAllBytes());
            if (process.waitFor() != 0) {
                throw new BenchmarkException("Environment script " + script + " failed");
            }
        } catch (IOException e) {
            throw new BenchmarkException("Error running environment script " + script, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BenchmarkException("Interrupted");
        }
        var result = new HashMap<String, String>();
        for (var line : output.split("\\r?\\n")) {
            var eq = line.indexOf('=');
            if (eq > 0) {
                result.put(line.substring(0, eq), line.substring(eq + 1));
            }
        }
        return result;
    }

    @Override
    public void run(CompiledBenchmark benchmark, String argument, Consumer<String> outputConsumer)
            throws BenchmarkException {
        try {
            var process = new ProcessBuilder(benchmark.getFile().getAbsolutePath(), argument)
                    .directory(benchmark.getDirectory())
                    .start();
            var stderrThread = pipe(process.getErrorStream(), System.err::println);
            pipe(process.getInputStream(), outputConsumer).join();
            stderrThread.join();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new BenchmarkException("Benchmark " + benchmark.getBenchmark().getName()
                        + " exited with code " + exitCode);
            }
        } catch (IOException e) {
            throw new BenchmarkException("Error running " + benchmark.getFile(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BenchmarkException("Interrupted");
        }
    }

    private static Thread pipe(InputStream input, Consumer<String> consumer) {
        var thread = new Thread(() -> {
            try (var reader = new BufferedReader(new InputStreamReader(input))) {
                while (true) {
                    var line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    consumer.accept(line);
                }
            } catch (IOException e) {
                // stream closed
            }
        });
        thread.setDaemon(true);
        thread.start();
        return thread;
    }
}
