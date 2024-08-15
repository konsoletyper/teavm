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
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.teavm.backend.javascript.JSModuleType;
import org.teavm.backend.javascript.JavaScriptTarget;
import org.teavm.browserrunner.BrowserRunDescriptor;
import org.teavm.browserrunner.BrowserRunner;
import org.teavm.tooling.ConsoleTeaVMToolLog;
import org.teavm.tooling.TeaVMProblemRenderer;
import org.teavm.vm.TeaVMBuilder;
import org.teavm.vm.TeaVMOptimizationLevel;

public class ExportTest {
    private static File targetFile = new File(new File(System.getProperty("teavm.junit.target")), "jso-export");
    private static BrowserRunner runner = new BrowserRunner(
            targetFile,
            "JAVASCRIPT",
            BrowserRunner.pickBrowser(System.getProperty("teavm.junit.js.runner")),
            false
    );

    @BeforeClass
    public static void start() {
        runner.start();
    }

    @AfterClass
    public static void stop() {
        runner.stop();
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
        testExport("varargs", ModuleWithVararg.class);
    }

    private void testExport(String name, Class<?> moduleClass) {
        if (!Boolean.parseBoolean(System.getProperty("teavm.junit.js", "true"))) {
            return;
        }
        try {
            var jsTarget = new JavaScriptTarget();
            jsTarget.setModuleType(JSModuleType.ES2015);
            jsTarget.setObfuscated(false);
            var teavm = new TeaVMBuilder(jsTarget).build();
            var outputDir = new File(targetFile, name);
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

            var descriptor = new BrowserRunDescriptor(name, "tests/" + name + "/runner.js", true,
                    List.of("resources/org/teavm/jso/export/assert.js"), null);
            runner.runTest(descriptor);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
