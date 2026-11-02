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
package org.teavm.compiler.bootstrap.junit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.teavm.backend.wasm.WasmDebugInfoLevel;
import org.teavm.compiler.bootstrap.TeaVMBootstrapCompiler;
import org.teavm.diagnostics.DefaultProblemTextConsumer;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSExport;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.junit.TestCompilationPlugins;
import org.teavm.model.MethodReference;
import org.teavm.vm.BuildTarget;
import org.teavm.vm.TeaVMOptimizationLevel;

/**
 * Entry point of the test compiler module. Node.js side ({@code junit-compiler.js}) receives requests
 * from JVM that runs tests and calls {@link #compile(CompileRequest)} for each of them.
 * Compilation is performed by {@link TeaVMBootstrapCompiler}, which takes class library from resources,
 * embedded into this module (see {@link ClasslibResourcesPolicy}), and the rest of classes from the class path
 * of tests. Single instance of compiler is reused for all requests, so class library is parsed only once.
 */
public final class TestCompiler {
    private static TeaVMBootstrapCompiler compiler;

    private TestCompiler() {
    }

    /**
     * Compiles test module.
     *
     * @return empty string in case of success, error message otherwise.
     */
    @JSExport
    public static String compile(CompileRequest request) {
        try {
            return doCompile(request);
        } catch (Throwable e) {
            var sb = new StringBuilder("Compiler crashed: ").append(e);
            for (var element : e.getStackTrace()) {
                sb.append("\n    at ").append(element);
            }
            return sb.toString();
        }
    }

    private static String doCompile(CompileRequest request) throws IOException {
        if (compiler == null) {
            compiler = new TeaVMBootstrapCompiler();
            compiler.setClasslibResourceProvider(TeaVMBootstrapCompiler.loadClasslib());
            compiler.setClassesResourceProvider(new NodeClasspathResourceProvider());
            compiler.setStrictMode(true);
            compiler.setObfuscated(false);
            compiler.setDebugInfo(true);
            compiler.setDebugInfoLevel(WasmDebugInfoLevel.DEOBFUSCATION);
            compiler.setSourceMap(true);
        }

        compiler.setOptimizationLevel(TeaVMOptimizationLevel.valueOf(request.getOptimizationLevel()));

        var properties = new Properties();
        var propertyArray = request.getProperties();
        for (var i = 0; i + 1 < propertyArray.length; i += 2) {
            properties.setProperty(propertyArray[i], propertyArray[i + 1]);
        }
        compiler.setProperties(properties);

        var testMethods = new ArrayList<MethodReference>();
        for (var method : request.getTestMethods()) {
            testMethods.add(MethodReference.parse(method));
        }
        compiler.setPlugins(List.of(
                TestCompilationPlugins.createEntryPointPlugin(request.getTestClass(), testMethods,
                        request.isWholeClass(), request.getParameterizedPlans()),
                TestCompilationPlugins.createExceptionPlugin()
        ));

        compiler.setMainClass(request.getEntryPoint());
        compiler.setOutputName(request.getOutputFileName());
        var outputFiles = new LinkedHashMap<String, ByteArrayOutputStream>();
        compiler.setBuildTarget(new MemoryBuildTarget(outputFiles));
        compiler.compile();

        var problems = compiler.getProblemProvider().getProblems();
        if (!problems.isEmpty()) {
            var sb = new StringBuilder();
            var consumer = new DefaultProblemTextConsumer();
            for (var problem : problems) {
                consumer.clear();
                problem.render(consumer);
                sb.append(problem.getSeverity()).append(": ").append(consumer.getText());
                if (problem.getLocation() != null && problem.getLocation().getMethod() != null) {
                    sb.append("\n    at ").append(problem.getLocation().getMethod());
                }
                sb.append("\n");
            }
            return sb.toString();
        }

        for (var entry : outputFiles.entrySet()) {
            writeFile(request.getOutputDirectory() + "/" + entry.getKey(),
                    Int8Array.copyFromJavaArray(entry.getValue().toByteArray()));
        }
        return "";
    }

    @JSBody(params = { "path", "data" }, script = "teavmBootstrapWriteFile(path, data);")
    private static native void writeFile(String path, Int8Array data);

    private static class MemoryBuildTarget implements BuildTarget {
        private final Map<String, ByteArrayOutputStream> files;

        MemoryBuildTarget(Map<String, ByteArrayOutputStream> files) {
            this.files = files;
        }

        @Override
        public OutputStream createResource(String fileName) {
            var output = new ByteArrayOutputStream();
            files.put(fileName, output);
            return output;
        }
    }
}
