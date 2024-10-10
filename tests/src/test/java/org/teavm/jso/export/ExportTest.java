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
package org.teavm.jso.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.teavm.backend.javascript.JSModuleType;
import org.teavm.backend.javascript.JavaScriptTarget;
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

public class ExportTest {
    private static File targetFile = new File(new File(System.getProperty("teavm.junit.target")), "jso-export");
    private static File jsTargetFile = new File(targetFile, "js");
    private static File wasmGCTargetFile = new File(targetFile, "wasm-gc");

    private static boolean jsNeeded = Boolean.parseBoolean(System.getProperty("teavm.junit.js", "true"));
    private static boolean wasmGCNeeded = Boolean.parseBoolean(System.getProperty("teavm.junit.wasm-gc", "true"));
    private static BrowserRunner jsRunner = new BrowserRunner(
            jsTargetFile,
            "JAVASCRIPT",
            BrowserRunner.pickBrowser(System.getProperty("teavm.junit.js.runner")),
            false
    );
    private static BrowserRunner wasmGCRunner = new BrowserRunner(
            wasmGCTargetFile,
            "JAVASCRIPT",
            BrowserRunner.pickBrowser(System.getProperty("teavm.junit.js.runner")),
            false
    );

    @BeforeClass
    public static void start() {
        if (jsNeeded) {
            jsRunner.start();
        }
        if (wasmGCNeeded) {
            wasmGCRunner.start();
        }
    }

    @AfterClass
    public static void stop() {
        if (jsNeeded) {
            jsRunner.stop();
        }
        if (wasmGCNeeded) {
            wasmGCRunner.stop();
        }
    }

    @Test
    public void simple() {
        testExport("simple", SimpleModule.class);
    }

    @Test
    public void initializer() {
        testExport("initializer", ModuleWithInitializer.class);
    }

    @Test
    public void primitives() {
        testExport("primitives", ModuleWithPrimitiveTypes.class);
    }

    @Test
    public void exportClassMembers() {
        testExport("exportClassMembers", ModuleWithExportedClassMembers.class);
    }

    @Test
    public void importClassMembers() {
        testExport("importClassMembers", ModuleWithConsumedObject.class);
    }

    @Test
    public void exportClasses() {
        testExport("exportClasses", ModuleWithExportedClasses.class);
    }

    @Test
    public void varargs() {
        testExport("varargs", ModuleWithVararg.class, true);
    }

    private void testExport(String name, Class<?> moduleClass) {
        testExport(name, moduleClass, false);
    }

    private void testExport(String name, Class<?> moduleClass, boolean skipWasmGC) {
        if (jsNeeded) {
            testExportJs(name, moduleClass);
        }
        if (wasmGCNeeded && !skipWasmGC) {
            testExportWasmGC(name, moduleClass);
        }
    }

    private void testExportJs(String name, Class<?> moduleClass) {
        try {
            var jsTarget = new JavaScriptTarget();
            jsTarget.setModuleType(JSModuleType.ES2015);
            jsTarget.setObfuscated(false);
            var teavm = new TeaVMBuilder(jsTarget).build();
            var outputDir = new File(jsTargetFile, name);
            teavm.installPlugins();
            teavm.setEntryPoint(moduleClass.getName());
            teavm.setOptimizationLevel(TeaVMOptimizationLevel.ADVANCED);
            outputDir.mkdirs();
            teavm.build(outputDir, "test.js");
            if (!teavm.getProblemProvider().getSevereProblems().isEmpty()) {
                var log = new ConsoleTeaVMToolLog(false);
                TeaVMProblemRenderer.describeProblems(teavm, log);
                throw new RuntimeException("TeaVM compilation error");
            }

            var testRunnerFile = new File(outputDir, "runner.js");
            try (var writer = new OutputStreamWriter(new FileOutputStream(testRunnerFile), StandardCharsets.UTF_8)) {
                writer.write("import { test } from '/resources/org/teavm/jso/export/" + name + ".js';\n");
                writer.write("export function main(args, callback) {\n");
                writer.write("  test().then(() => callback()).catch(e => callback(e));\n");
                writer.write("}\n");
            }
            var testProviderFile = new File(outputDir, "provider.js");
            try (var writer = new OutputStreamWriter(new FileOutputStream(testProviderFile), StandardCharsets.UTF_8)) {
                writer.write("import * as obj from '/tests/" + name + "/test.js';\n");
                writer.write("export default Promise.resolve(obj);");
            }

            var descriptor = new BrowserRunDescriptor(name, "tests/" + name + "/runner.js", true,
                    List.of("resources/org/teavm/jso/export/assert.js"), null);
            jsRunner.runTest(descriptor);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void testExportWasmGC(String name, Class<?> moduleClass) {
        try {
            var wasmGCTarget = new WasmGCTarget();
            wasmGCTarget.setObfuscated(false);
            wasmGCTarget.setDebugLocation(WasmDebugInfoLocation.EMBEDDED);
            wasmGCTarget.setDebugLevel(WasmDebugInfoLevel.DEOBFUSCATION);
            var teavm = new TeaVMBuilder(wasmGCTarget).build();
            var outputDir = new File(wasmGCTargetFile, name);
            teavm.installPlugins();
            teavm.setEntryPoint(moduleClass.getName());
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

            var testRunnerFile = new File(outputDir, "runner.js");
            try (var writer = new OutputStreamWriter(new FileOutputStream(testRunnerFile), StandardCharsets.UTF_8)) {
                writer.write("import { test } from '/resources/org/teavm/jso/export/" + name + ".js';\n");
                writer.write("export function main(args, callback) {\n");
                writer.write("  test().then(() => callback()).catch(e => callback(e));\n");
                writer.write("}\n");
            }
            var testProviderFile = new File(outputDir, "provider.js");
            try (var writer = new OutputStreamWriter(new FileOutputStream(testProviderFile), StandardCharsets.UTF_8)) {
                writer.write("await import('/resources/org/teavm/backend/wasm/wasm-gc-runtime.js');\n");
                writer.write("let teavm = await TeaVM.wasmGC.load('/tests/" + name + "/test.wasm');\n");
                writer.write("export default teavm.exports;\n");
            }

            var descriptor = new BrowserRunDescriptor(name, "tests/" + name + "/runner.js", true,
                    List.of("resources/org/teavm/jso/export/assert.js"), null);
            wasmGCRunner.runTest(descriptor);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
