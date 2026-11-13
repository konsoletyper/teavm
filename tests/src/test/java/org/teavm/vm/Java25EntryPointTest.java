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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
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
            new Fixture("compact", "42\n"));
    private static BrowserRunner browser;

    @BeforeAll
    public static void prepare() throws IOException, InterruptedException {
        if (!JS_ENABLED && !WASM_GC_ENABLED) {
            return;
        }
        Files.createDirectories(TARGET);
        for (var fixture : FIXTURES) {
            compileJava(fixture);
            runOnJvm(fixture);
        }
        var runnerProperty = JS_ENABLED ? "teavm.junit.js.runner" : "teavm.junit.wasm-gc.runner";
        var chromium = System.getenv("TEAVM_TEST_CHROMIUM");
        browser = new BrowserRunner(TARGET.toFile(), "JAVASCRIPT",
                chromium == null || chromium.isBlank()
                        ? BrowserRunner.pickBrowser(System.getProperty(runnerProperty))
                        : params -> startChromium(chromium, params), false);
        browser.start();
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
        for (var backend : List.of("js", "wasm-gc")) {
            if (backend.equals("js") ? !JS_ENABLED : !WASM_GC_ENABLED) {
                continue;
            }
            for (var level : TeaVMOptimizationLevel.values()) {
                if (level != TeaVMOptimizationLevel.SIMPLE && !(OPTIMIZED && level == TeaVMOptimizationLevel.FULL)) {
                    continue;
                }
                for (var fixture : FIXTURES) {
                    var name = backend + "/" + level.name().toLowerCase() + "/" + fixture.name();
                    tests.add(DynamicTest.dynamicTest(name, () -> compileAndRun(backend, level, fixture, name)));
                }
            }
        }
        return tests.stream();
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
        var extension = backend.equals("js") ? ".js" : ".wasm";
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
            vm.setOptimizationLevel(level);
            vm.build(output.toFile(), "program" + extension);
            if (!vm.getProblemProvider().getSevereProblems().isEmpty()) {
                TeaVMProblemRenderer.describeProblems(vm, new ConsoleTeaVMToolLog(false));
            }
            assertTrue(vm.getProblemProvider().getSevereProblems().isEmpty(), "Compilation failed for " + name);
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

    private record Fixture(String name, String output) {
        Path classes() {
            return TARGET.resolve("sources").resolve(name).resolve("classes");
        }
    }
}
