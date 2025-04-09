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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.teavm.backend.wasm.WasmRuntimeType;
import org.teavm.backend.wasm.WasmTarget;
import org.teavm.backend.wasm.disasm.Disassembler;
import org.teavm.backend.wasm.disasm.DisassemblyHTMLWriter;
import org.teavm.browserrunner.BrowserRunner;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.MethodReference;
import org.teavm.model.ReferenceCache;

class WebAssemblyPlatformSupport extends BaseWebAssemblyPlatformSupport {
    private boolean disassembly;

    WebAssemblyPlatformSupport(ClassHolderSource classSource, ReferenceCache referenceCache, boolean disassembly) {
        super(classSource, referenceCache);
        this.disassembly = disassembly;
    }

    @Override
    TestRunStrategy createRunStrategy(File outputDir) {
        var runStrategyName = System.getProperty(WASM_RUNNER);
        return runStrategyName != null
                ? new BrowserRunStrategy(outputDir, "WASM", BrowserRunner.pickBrowser(runStrategyName))
                : null;
    }

    @Override
    protected boolean exceptionsUsed() {
        return true;
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
    void additionalOutput(File outputPath, TeaVMTestConfiguration<?> configuration) {
        if (disassembly) {
            writeDisassembly(outputPath, "classTest", configuration);
        }
    }

    @Override
    void additionalSingleTestOutput(File outputPathForMethod, TeaVMTestConfiguration<?> configuration,
            MethodReference reference) {
        htmlSingleTestOutput(outputPathForMethod, configuration, "teavm-run-test-wasm.html");
        if (disassembly) {
            writeDisassembly(outputPathForMethod, "test", configuration);
        }
    }

    private void writeDisassembly(File outputPath, String name, TeaVMTestConfiguration<?> configuration) {
        var binPath = getOutputFile(outputPath, name, configuration.getSuffix(), getExtension());
        var htmlPath = getOutputFile(outputPath, name, configuration.getSuffix(), ".wast.html");
        try (var writer = new OutputStreamWriter(new FileOutputStream(htmlPath))) {
            var disasmWriter = new DisassemblyHTMLWriter(new PrintWriter(writer));
            disasmWriter.setWithAddress(true);
            new Disassembler(disasmWriter).disassemble(Files.readAllBytes(binPath.toPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
