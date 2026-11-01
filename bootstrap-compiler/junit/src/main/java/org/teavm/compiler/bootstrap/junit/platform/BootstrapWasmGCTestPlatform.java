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
package org.teavm.compiler.bootstrap.junit.platform;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.teavm.junit.TestPlatform;
import org.teavm.junit.spi.ExternalCompilationRequest;
import org.teavm.junit.spi.ExternalCompilationResult;
import org.teavm.junit.spi.ExternalTestPlatform;

/**
 * Platform that compiles tests using TeaVM compiled into Wasm GC module and running in Node.js.
 * Tests are run exactly the same way they are run on Wasm GC platform.
 *
 * <p>Configured via following system properties:
 *
 * <ul>
 *   <li>{@code teavm.junit.bootstrap-wasm-gc} - {@code true} to enable platform;</li>
 *   <li>{@code teavm.junit.bootstrap-wasm-gc.compiler} - directory with compiler,
 *     i.e. output of {@code :bootstrap-compiler:junit-compiler:assembleCompiler};</li>
 *   <li>{@code teavm.junit.bootstrap-wasm-gc.runner} - browser to run tests in (like
 *     {@code teavm.junit.wasm-gc.runner}); if not specified, tests are only compiled;</li>
 *   <li>{@code teavm.junit.bootstrap-wasm-gc.node} - Node.js executable, {@code node} by default.</li>
 * </ul>
 *
 * <p>Single Node.js process is started per JVM and reused for all compilations.
 */
public class BootstrapWasmGCTestPlatform implements ExternalTestPlatform {
    public static final String ID = "bootstrap-wasm-gc";
    private static final String PREFIX = "teavm.junit.bootstrap-wasm-gc";
    private static final String RESPONSE_PREFIX = "@@teavm-bootstrap-response@@";
    private Process process;
    private BufferedWriter input;
    private BufferedReader output;

    @Override
    public String getName() {
        return ID;
    }

    @Override
    public String getBasePlatform() {
        return TestPlatform.WEBASSEMBLY_GC;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.getBoolean(PREFIX);
    }

    @Override
    public String getBrowserRunner() {
        var runner = System.getProperty(PREFIX + ".runner");
        return runner != null && !runner.isEmpty() ? runner : null;
    }

    @Override
    public synchronized ExternalCompilationResult compile(ExternalCompilationRequest request) throws IOException {
        ensureStarted(request.getClassLoader());

        var json = new StringBuilder("{\"type\":\"compile\"");
        var outputFile = request.getOutputFile();
        appendProperty(json, "outputDirectory", outputFile.getParentFile().getAbsolutePath());
        appendProperty(json, "outputFileName", outputFile.getName());
        appendProperty(json, "entryPoint", request.getEntryPointClassName());
        appendProperty(json, "testClass", request.getTestClassName());
        var methods = new ArrayList<String>();
        for (var method : request.getTestMethods()) {
            methods.add(method.toString());
        }
        appendProperty(json, "testMethods", methods);
        json.append(",\"wholeClass\":").append(request.isWholeClass());
        appendProperty(json, "parameterizedPlans", request.getParameterizedPlans());
        var properties = new ArrayList<String>();
        for (var key : request.getProperties().stringPropertyNames()) {
            properties.add(key);
            properties.add(request.getProperties().getProperty(key));
        }
        appendProperty(json, "properties", properties);
        appendProperty(json, "optimizationLevel", request.getOptimizationLevel().name());
        json.append("}");

        var error = send(json.toString());
        return error == null ? ExternalCompilationResult.success() : ExternalCompilationResult.failure(error);
    }

    private void ensureStarted(ClassLoader classLoader) throws IOException {
        if (process != null) {
            if (process.isAlive()) {
                return;
            }
            process = null;
        }

        var compilerDirName = System.getProperty(PREFIX + ".compiler");
        if (compilerDirName == null || compilerDirName.isEmpty()) {
            throw new IllegalStateException("Location of bootstrap compiler is not specified. "
                    + "Set " + PREFIX + ".compiler system property");
        }
        var compilerDir = new File(compilerDirName);
        var node = System.getProperty(PREFIX + ".node", "node");
        var builder = new ProcessBuilder(node, new File(compilerDir, "junit-compiler.js").getAbsolutePath(),
                new File(compilerDir, "junit-compiler.wasm").getAbsolutePath());
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        process = builder.start();
        input = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        output = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

        var json = new StringBuilder("{\"type\":\"init\"");
        appendProperty(json, "classpath", getClasspath(classLoader));
        json.append("}");
        var error = send(json.toString());
        if (error != null) {
            throw new IOException("Error initializing bootstrap compiler: " + error);
        }
    }

    private String send(String request) throws IOException {
        input.write(request);
        input.write("\n");
        input.flush();
        while (true) {
            var line = output.readLine();
            if (line == null) {
                process = null;
                throw new IOException("Bootstrap compiler process terminated unexpectedly");
            }
            if (line.startsWith(RESPONSE_PREFIX)) {
                var response = line.substring(RESPONSE_PREFIX.length());
                if (response.equals("OK")) {
                    return null;
                }
                if (response.startsWith("ERROR ")) {
                    return new String(Base64.getDecoder().decode(response.substring("ERROR ".length())),
                            StandardCharsets.UTF_8);
                }
                throw new IOException("Unexpected response from bootstrap compiler: " + response);
            } else {
                System.out.println("[bootstrap compiler] " + line);
            }
        }
    }

    private static List<String> getClasspath(ClassLoader classLoader) {
        Set<String> result = new LinkedHashSet<>();
        for (var loader = classLoader; loader != null; loader = loader.getParent()) {
            if (loader instanceof URLClassLoader) {
                for (var url : ((URLClassLoader) loader).getURLs()) {
                    if (url.getProtocol().equals("file")) {
                        try {
                            result.add(new File(url.toURI()).getAbsolutePath());
                        } catch (URISyntaxException e) {
                            // skip
                        }
                    }
                }
            }
        }
        var classpath = System.getProperty("java.class.path");
        if (classpath != null) {
            for (var entry : classpath.split(File.pathSeparator)) {
                if (!entry.isEmpty()) {
                    result.add(new File(entry).getAbsolutePath());
                }
            }
        }
        return new ArrayList<>(result);
    }

    @Override
    public synchronized void close() {
        if (process != null) {
            process.destroy();
            process = null;
        }
    }

    private static void appendProperty(StringBuilder sb, String name, String value) {
        sb.append(",");
        appendString(sb, name);
        sb.append(":");
        appendString(sb, value);
    }

    private static void appendProperty(StringBuilder sb, String name, List<String> values) {
        sb.append(",");
        appendString(sb, name);
        sb.append(":[");
        for (var i = 0; i < values.size(); ++i) {
            if (i > 0) {
                sb.append(",");
            }
            appendString(sb, values.get(i));
        }
        sb.append("]");
    }

    private static void appendString(StringBuilder sb, String s) {
        sb.append('"');
        for (var i = 0; i < s.length(); ++i) {
            var c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        sb.append('"');
    }
}
