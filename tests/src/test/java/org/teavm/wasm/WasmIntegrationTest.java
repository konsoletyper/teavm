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
package org.teavm.wasm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.teavm.backend.wasm.WasmDebugInfoLevel;
import org.teavm.backend.wasm.WasmDebugInfoLocation;
import org.teavm.backend.wasm.WasmGCTarget;
import org.teavm.backend.wasm.disasm.Disassembler;
import org.teavm.backend.wasm.disasm.DisassemblyHTMLWriter;
import org.teavm.browserrunner.BrowserRunDescriptor;
import org.teavm.browserrunner.BrowserRunner;
import org.teavm.tooling.ConsoleTeaVMToolLog;
import org.teavm.tooling.TeaVMProblemRenderer;
import org.teavm.vm.TeaVMBuilder;
import org.teavm.vm.TeaVMOptimizationLevel;

public class WasmIntegrationTest {
    private static boolean wasmGCNeeded = Boolean.parseBoolean(System.getProperty("teavm.junit.wasm-gc", "true"));
    private static File targetFile = new File(new File(System.getProperty("teavm.junit.target")), 
            "wasm-gc-integration");
    private static BrowserRunner browserRunner = new BrowserRunner(
            targetFile,
            "JAVASCRIPT",
            BrowserRunner.pickBrowser(System.getProperty("teavm.junit.js.runner")),
            false
    );


    @BeforeClass
    public static void start() {
        if (wasmGCNeeded) {
            browserRunner.start();
        }
    }

    @AfterClass
    public static void stop() {
        if (wasmGCNeeded) {
            browserRunner.stop();
        }
    }
    
    @Test
    public void sabTest() {
        if (!wasmGCNeeded) {
            return;
        }
        
        try {
            var wasmGCTarget = new WasmGCTarget();
            wasmGCTarget.setObfuscated(false);
            wasmGCTarget.setDebugInfoLocation(WasmDebugInfoLocation.EMBEDDED);
            wasmGCTarget.setDebugInfoLevel(WasmDebugInfoLevel.DEOBFUSCATION);
            wasmGCTarget.setSharedBuffer(true);
            var teavm = new TeaVMBuilder(wasmGCTarget).build();
            var outputDir = new File(targetFile, "sab");
            teavm.installPlugins();
            teavm.setEntryPoint(SabWorker.class.getName());
            teavm.setOptimizationLevel(TeaVMOptimizationLevel.ADVANCED);
            outputDir.mkdirs();
            teavm.build(outputDir, "test.wasm");
            if (!teavm.getProblemProvider().getSevereProblems().isEmpty()) {
                var log = new ConsoleTeaVMToolLog(false);
                TeaVMProblemRenderer.describeProblems(teavm, log);
                throw new RuntimeException("TeaVM compilation error");
            }

            var disassemblyFile = new File(outputDir, "test.wast.html");
            try (var output = new PrintWriter(new OutputStreamWriter(new FileOutputStream(disassemblyFile)))) {
                var disassemblyWriter = new DisassemblyHTMLWriter(output);
                disassemblyWriter.setWithAddress(true);
                var disassembler = new Disassembler(disassemblyWriter);
                disassembler.disassemble(Files.readAllBytes(new File(outputDir, "test.wasm").toPath()));
            }

            var descriptor = new BrowserRunDescriptor("sab", "resources/org/teavm/wasm-integration/sab/launcher.js",
                    false, List.of(), null, false);
            browserRunner.runTest(descriptor);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
