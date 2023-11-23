/*
 *  Copyright 2023 Alexey Andreev.
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
import static org.teavm.junit.PropertyNames.WASM_ENABLED;
import static org.teavm.junit.PropertyNames.WASM_RUNNER;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.teavm.backend.wasm.WasmRuntimeType;
import org.teavm.backend.wasm.WasmTarget;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.MethodReference;
import org.teavm.model.ReferenceCache;

class WebAssemblyPlatformSupport extends BaseWebAssemblyPlatformSupport {
    WebAssemblyPlatformSupport(ClassHolderSource classSource, ReferenceCache referenceCache) {
        super(classSource, referenceCache);
    }

    @Override
    TestRunStrategy createRunStrategy(File outputDir) {
        var runStrategyName = System.getProperty(WASM_RUNNER);
        if (runStrategyName != null) {
            switch (runStrategyName) {
                case "browser":
                    return new BrowserRunStrategy(outputDir, "WASM", BrowserRunStrategy::customBrowser);
                case "chrome":
                case "browser-chrome":
                    return new BrowserRunStrategy(outputDir, "WASM", BrowserRunStrategy::chromeBrowser);
                case "browser-firefox":
                    return new BrowserRunStrategy(outputDir, "WASM", BrowserRunStrategy::firefoxBrowser);
                default:
                    throw new RuntimeException("Unknown run strategy: " + runStrategyName);
            }
        }
        return null;
    }

    @Override
    TestPlatform getPlatform() {
        return TestPlatform.WEBASSEMBLY;
    }

    @Override
    String getPath() {
        return "wasm";
    }

    @Override
    boolean isEnabled() {
        return Boolean.getBoolean(WASM_ENABLED);
    }

    @Override
    List<TeaVMTestConfiguration<WasmTarget>> getConfigurations() {
        List<TeaVMTestConfiguration<WasmTarget>> configurations = new ArrayList<>();
        configurations.add(TeaVMTestConfiguration.WASM_DEFAULT);
        if (Boolean.getBoolean(OPTIMIZED)) {
            configurations.add(TeaVMTestConfiguration.WASM_OPTIMIZED);
        }
        return configurations;
    }

    @Override
    protected WasmRuntimeType getRuntimeType() {
        return WasmRuntimeType.TEAVM;
    }

    @Override
    void additionalOutput(File outputPath, File outputPathForMethod, TeaVMTestConfiguration<?> configuration,
            MethodReference reference) {
        htmlOutput(outputPath, outputPathForMethod, configuration, reference, "teavm-run-test-wasm.html");
    }

    @Override
    void additionalSingleTestOutput(File outputPathForMethod, TeaVMTestConfiguration<?> configuration,
            MethodReference reference) {
        htmlSingleTestOutput(outputPathForMethod, configuration, "teavm-run-test-wasm.html");
    }
}
