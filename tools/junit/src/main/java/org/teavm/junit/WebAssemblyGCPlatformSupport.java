/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.junit;

import static org.teavm.junit.PropertyNames.OPTIMIZED;
import static org.teavm.junit.PropertyNames.SOURCE_DIRS;
import static org.teavm.junit.PropertyNames.WASM_GC_ENABLED;
import static org.teavm.junit.PropertyNames.WASM_RUNNER;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.teavm.backend.wasm.WasmGCTarget;
import org.teavm.browserrunner.BrowserRunner;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.MethodReference;
import org.teavm.model.ReferenceCache;
import org.teavm.vm.TeaVM;

class WebAssemblyGCPlatformSupport extends TestPlatformSupport<WasmGCTarget> {
    WebAssemblyGCPlatformSupport(ClassHolderSource classSource, ReferenceCache referenceCache) {
        super(classSource, referenceCache);
    }

    @Override
    String getExtension() {
        return ".wasm";
    }

    @Override
    TestRunStrategy createRunStrategy(File outputDir) {
        var runStrategyName = System.getProperty(WASM_RUNNER);
        return runStrategyName != null
                ? new BrowserRunStrategy(outputDir, "WASM_GC", BrowserRunner.pickBrowser(runStrategyName))
                : null;
    }

    @Override
    CompileResult compile(Consumer<TeaVM> additionalProcessing, String baseName,
            TeaVMTestConfiguration<WasmGCTarget> configuration, File path, AnnotatedElement element) {
        Supplier<WasmGCTarget> targetSupplier = () -> {
            var target = new WasmGCTarget();
            target.setObfuscated(false);
            var sourceDirs = System.getProperty(SOURCE_DIRS);
            if (sourceDirs != null) {
                var dirs = new ArrayList<File>();
                for (var tokenizer = new StringTokenizer(sourceDirs, Character.toString(File.pathSeparatorChar));
                     tokenizer.hasMoreTokens();) {
                    var dir = new File(tokenizer.nextToken());
                    if (dir.isDirectory()) {
                        dirs.add(dir);
                    }
                }
            }
            return target;
        };
        return compile(configuration, targetSupplier, TestWasmGCEntryPoint.class.getName(), path,
                ".wasm", null, additionalProcessing, baseName);
    }

    @Override
    TestPlatform getPlatform() {
        return TestPlatform.WEBASSEMBLY_GC;
    }

    @Override
    String getPath() {
        return "wasm-gc";
    }

    @Override
    boolean isEnabled() {
        return Boolean.getBoolean(WASM_GC_ENABLED);
    }

    @Override
    List<TeaVMTestConfiguration<WasmGCTarget>> getConfigurations() {
        List<TeaVMTestConfiguration<WasmGCTarget>> configurations = new ArrayList<>();
        configurations.add(TeaVMTestConfiguration.WASM_GC_DEFAULT);
        if (Boolean.getBoolean(OPTIMIZED)) {
            configurations.add(TeaVMTestConfiguration.WASM_GC_OPTIMIZED);
        }
        return configurations;
    }

    @Override
    boolean usesFileName() {
        return true;
    }

    @Override
    void additionalOutput(File outputPath, File outputPathForMethod, TeaVMTestConfiguration<?> configuration,
            MethodReference reference) {
        htmlOutput(outputPath, outputPathForMethod, configuration, reference, "teavm-run-test-wasm.html");
        var testPath = getOutputFile(outputPath, "classTest", configuration.getSuffix(),
                getExtension() + "-runtime.js");
        try {
            TestUtil.resourceToFile("org/teavm/backend/wasm/wasm-gc-runtime.js", testPath, Map.of());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    void additionalSingleTestOutput(File outputPathForMethod, TeaVMTestConfiguration<?> configuration,
            MethodReference reference) {
        htmlSingleTestOutput(outputPathForMethod, configuration, "teavm-run-test-wasm.html");
        var testPath = getOutputFile(outputPathForMethod, "test", configuration.getSuffix(),
                getExtension() + "-runtime.js");
        try {
            TestUtil.resourceToFile("org/teavm/backend/wasm/wasm-gc-runtime.js", testPath, Map.of());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
