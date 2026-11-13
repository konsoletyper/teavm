/*
 *  Copyright 2026 TeaVM contributors.
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
package org.teavm.vm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.teavm.backend.c.CTarget;
import org.teavm.backend.c.generate.CNameProvider;
import org.teavm.backend.javascript.JSModuleType;
import org.teavm.backend.javascript.JavaScriptTarget;
import org.teavm.backend.wasm.WasmGCTarget;
import org.teavm.browserrunner.BrowserRunDescriptor;
import org.teavm.browserrunner.BrowserRunParams;
import org.teavm.browserrunner.BrowserRunner;
import org.teavm.model.ReferenceCache;
import org.teavm.parsing.ClasspathClassHolderSource;
import org.teavm.parsing.ClasspathResourceProvider;
import org.teavm.tooling.ConsoleTeaVMToolLog;
import org.teavm.tooling.TeaVMProblemRenderer;

public class Java25EntryPointTest {
    private static final String RESOURCE_ROOT = "org/teavm/vm/java25/";
    private static final Path TARGET = Path.of(System.getProperty("teavm.junit.target"), "java25-entry-point");
    private static final boolean JS_ENABLED = Boolean.parseBoolean(System.getProperty("teavm.junit.js", "true"));
    private static final boolean WASM_GC_ENABLED = Boolean.parseBoolean(
            System.getProperty("teavm.junit.wasm-gc", "true"));
    private static final boolean C_ENABLED = Boolean.parseBoolean(System.getProperty("teavm.junit.c", "true"));
    private static final boolean OPTIMIZED = Boolean.parseBoolean(
            System.getProperty("teavm.junit.optimized", "true"));
    private static final List<Fixture> FIXTURES = List.of(
            new Fixture("public-static-args", "args=first,second\n"),
            new Fixture("package-static-args", "args=first,second\n"),
            new Fixture("instance-args", "constructor\n1:42:first,second\n"),
            new Fixture("instance-noargs", "constructor\n1:42\n"),
            new Fixture("static-noargs", "static\n"),
            new Fixture("static-throwing-constructor", "initialized\nstatic\n"),
            new Fixture("static-no-default-constructor", "static\n"),
            new Fixture("overloaded", "args=first,second\n"),
            new Fixture("compact", "42\n"),
            new Fixture("instance-gc-suspending-constructor", "constructor\n1:42\n", true));
    private static BrowserRunner browser;
    private static Map<String, String> nativeEnvironment = Map.of();

    @BeforeAll
    public static void prepare() throws IOException, InterruptedException {
        if (!JS_ENABLED && !WASM_GC_ENABLED && !C_ENABLED) {
            return;
        }
        Files.createDirectories(TARGET);
        for (var fixture : FIXTURES) {
            if (fixture.nativeOnly() && !C_ENABLED) {
                continue;
            }
            compileJava(fixture);
            runOnJvm(fixture);
        }
        if (C_ENABLED) {
            nativeEnvironment = nativeCompilerEnvironment();
        }
        if (JS_ENABLED || WASM_GC_ENABLED) {
            var runnerProperty = JS_ENABLED ? "teavm.junit.js.runner" : "teavm.junit.wasm-gc.runner";
            var chromium = System.getenv("TEAVM_TEST_CHROMIUM");
            browser = new BrowserRunner(TARGET.toFile(), "JAVASCRIPT",
                    chromium == null || chromium.isBlank()
                            ? BrowserRunner.pickBrowser(System.getProperty(runnerProperty))
                            : params -> startChromium(chromium, params), false);
            browser.start();
        }
    }

    @AfterAll
    public static void stop() {
        if (browser != null) {
            browser.stop();
        }
    }

    @TestFactory
    public Stream<DynamicTest> launchers() {
        var tests = new ArrayList<DynamicTest>();
        for (var backend : List.of("js", "wasm-gc", "c")) {
            if (!backendEnabled(backend)) {
                continue;
            }
            for (var level : TeaVMOptimizationLevel.values()) {
                if (level != TeaVMOptimizationLevel.SIMPLE && !(OPTIMIZED && level == TeaVMOptimizationLevel.FULL)) {
                    continue;
                }
                for (var fixture : FIXTURES) {
                    if (fixture.nativeOnly() && !backend.equals("c")) {
                        continue;
                    }
                    var name = backend + "/" + level.name().toLowerCase() + "/" + fixture.name();
                    tests.add(DynamicTest.dynamicTest(name, () -> compileAndRun(backend, level, fixture, name)));
                }
            }
        }
        return tests.stream();
    }

    private static boolean backendEnabled(String backend) {
        return switch (backend) {
            case "js" -> JS_ENABLED;
            case "wasm-gc" -> WASM_GC_ENABLED;
            case "c" -> C_ENABLED;
            default -> false;
        };
    }

    private static void compileJava(Fixture fixture) throws IOException {
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "Entry-point fixtures require a JDK 25 compiler");
        var source = TARGET.resolve("sources").resolve(fixture.name()).resolve("Main.java");
        Files.createDirectories(source.getParent());
        Files.createDirectories(fixture.classes());
        try (var input = Java25EntryPointTest.class.getClassLoader()
                .getResourceAsStream(RESOURCE_ROOT + fixture.name() + "/Main.java")) {
            assertNotNull(input, fixture.name());
            Files.write(source, input.readAllBytes());
        }
        var diagnostics = new ByteArrayOutputStream();
        var exitCode = compiler.run(null, diagnostics, diagnostics, "--release", "25", "-proc:none", "-d",
                fixture.classes().toString(), source.toString());
        assertEquals(0, exitCode, diagnostics.toString(StandardCharsets.UTF_8));
        try (var input = new DataInputStream(Files.newInputStream(fixture.classes().resolve("Main.class")))) {
            assertEquals(0xCAFEBABE, input.readInt());
            assertEquals(0, input.readUnsignedShort(), "Fixtures must not require preview features");
            assertEquals(69, input.readUnsignedShort(), "Fixtures must use Java 25 bytecode");
        }
    }

    private static void runOnJvm(Fixture fixture) throws IOException, InterruptedException {
        var executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        var process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", executable).toString(),
                "-cp", fixture.classes().toString(), "Main", "first", "second").redirectErrorStream(true).start();
        try {
            assertTrue(process.waitFor(15, TimeUnit.SECONDS), "JDK launcher timed out for " + fixture.name());
            var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
                    .replace("\r\n", "\n");
            assertEquals(0, process.exitValue(), output);
            assertEquals(fixture.output(), output, "JDK launcher: " + fixture.name());
        } finally {
            process.destroyForcibly();
        }
    }

    private static void compileAndRun(String backend, TeaVMOptimizationLevel level, Fixture fixture, String name)
            throws IOException {
        var output = TARGET.resolve(name);
        Files.createDirectories(output);
        var target = createTarget(backend);
        var extension = backend.equals("c") ? "" : backend.equals("js") ? ".js" : ".wasm";
        try (var loader = new URLClassLoader(new java.net.URL[] { fixture.classes().toUri().toURL() },
                Java25EntryPointTest.class.getClassLoader())) {
            var resources = new ClasspathResourceProvider(loader);
            var cache = new ReferenceCache();
            var vm = new TeaVMBuilder(target)
                    .setClassLoader(loader)
                    .setReferenceCache(cache)
                    .setResourceProvider(resources)
                    .setClassSource(new ClasspathClassHolderSource(resources, cache, loader))
                    .setStrict(true)
                    .build();
            vm.installPlugins();
            // The fixture itself, never the JUnit wrapper, must be the entry point.
            vm.setEntryPoint("Main");
            vm.setAsyncEnabled(true);
            vm.setOptimizationLevel(level);
            vm.build(output.toFile(), "program" + extension);
            if (!vm.getProblemProvider().getSevereProblems().isEmpty()) {
                TeaVMProblemRenderer.describeProblems(vm, new ConsoleTeaVMToolLog(false));
            }
            assertTrue(vm.getProblemProvider().getSevereProblems().isEmpty(), "Compilation failed for " + name);
        }
        if (backend.equals("c")) {
            compileAndRunNative(output, fixture);
            return;
        }
        var stdout = new StringBuffer();
        var stderr = new StringBuffer();
        var descriptor = new BrowserRunDescriptor(name, "resources/" + RESOURCE_ROOT + "runner.js", true,
                List.of(), "/tests/" + name + "/program" + extension, false);
        assertTimeoutPreemptively(Duration.ofSeconds(60), () -> browser.runTest(descriptor,
                (error, line) -> (error ? stderr : stdout).append(line).append('\n')));
        assertEquals("", stderr.toString(), name);
        assertEquals(fixture.output(), stdout.toString(), name);
    }

    private static TeaVMTarget createTarget(String backend) {
        if (backend.equals("c")) {
            return new CTarget(new CNameProvider());
        }
        if (backend.equals("js")) {
            var target = new JavaScriptTarget();
            target.setModuleType(JSModuleType.ES2015);
            target.setObfuscated(false);
            return target;
        }
        var target = new WasmGCTarget();
        target.setObfuscated(false);
        target.setStrict(true);
        return target;
    }

    private static void compileAndRunNative(Path output, Fixture fixture) throws IOException {
        var compiler = new File(System.getProperty("teavm.junit.c.compiler")).getAbsolutePath();
        var compilerProcess = new ProcessBuilder(compiler).directory(output.toFile());
        compilerProcess.environment().putAll(nativeEnvironment);
        runProcess(compilerProcess, output.resolve("compile.log"), 120);
        var executable = System.getProperty("os.name").startsWith("Windows") ? "run_test.exe" : "run_test";
        var stdout = output.resolve("stdout.log");
        var stderr = output.resolve("stderr.log");
        var process = new ProcessBuilder(output.resolve(executable).toAbsolutePath().toString(), "first", "second")
                .redirectOutput(stdout.toFile()).redirectError(stderr.toFile()).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Native launcher timed out for " + fixture.name());
            assertEquals("", Files.readString(stderr, StandardCharsets.UTF_8), fixture.name());
            assertEquals(0, process.exitValue(), fixture.name());
            assertEquals(fixture.output(), Files.readString(stdout, StandardCharsets.UTF_8).replace("\r\n", "\n"),
                    "C launcher: " + fixture.name());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } finally {
            process.destroyForcibly();
        }
    }

    private static Map<String, String> nativeCompilerEnvironment() throws IOException, InterruptedException {
        var script = System.getProperty("teavm.junit.c.envScript", "");
        if (script.isEmpty()) {
            return Map.of();
        }
        var file = new File(script);
        var path = file.exists() ? file.getAbsolutePath() : script;
        var windows = System.getProperty("os.name").startsWith("Windows");
        var builder = windows
                ? new ProcessBuilder("cmd", "/c", "call \"" + path + "\" && set")
                : new ProcessBuilder("bash", "-c", "source \"$1\" && env", "teavm-c-env", path);
        var process = builder.redirectError(TARGET.resolve("compiler-env.stderr.log").toFile()).start();
        var bytes = new ByteArrayOutputStream();
        var readError = new AtomicReference<IOException>();
        var reader = Thread.ofVirtual().start(() -> {
            try {
                process.getInputStream().transferTo(bytes);
            } catch (IOException e) {
                readError.set(e);
            }
        });
        try {
            assertTrue(process.waitFor(15, TimeUnit.SECONDS), "C compiler environment setup timed out");
            reader.join(15_000);
            assertTrue(!reader.isAlive(), "C compiler environment output did not finish");
            assertEquals(0, process.exitValue(), "C compiler environment setup failed");
            if (readError.get() != null) {
                throw readError.get();
            }
            var environment = new HashMap<String, String>();
            for (var line : bytes.toString(StandardCharsets.UTF_8).split("\\r?\\n")) {
                var separator = line.indexOf('=');
                if (separator > 0) {
                    environment.put(line.substring(0, separator), line.substring(separator + 1));
                }
            }
            return environment;
        } finally {
            process.destroyForcibly();
        }
    }

    private static void runProcess(ProcessBuilder builder, Path log, int timeoutSeconds) throws IOException {
        var process = builder.redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue(process.waitFor(timeoutSeconds, TimeUnit.SECONDS), "C compiler timed out");
            assertEquals(0, process.exitValue(), () -> {
                try {
                    return Files.readString(log, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } finally {
            process.destroyForcibly();
        }
    }

    private static Process startChromium(String executable, BrowserRunParams params) {
        try {
            var profile = Files.createTempDirectory(TARGET, "chromium-");
            var process = new ProcessBuilder(executable, "--headless", "--disable-gpu", "--no-first-run",
                    "--no-sandbox", "--disable-dev-shm-usage", "--user-data-dir=" + profile, params.url());
            process.redirectOutput(params.stdoutFile() != null ? params.stdoutFile() : new File(profile.toFile(),
                    "stdout.log"));
            process.redirectError(params.stderrFile() != null ? params.stderrFile() : new File(profile.toFile(),
                    "stderr.log"));
            return process.start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private record Fixture(String name, String output, boolean nativeOnly) {
        Fixture(String name, String output) {
            this(name, output, false);
        }

        Path classes() {
            return TARGET.resolve("sources").resolve(name).resolve("classes");
        }
    }
}
