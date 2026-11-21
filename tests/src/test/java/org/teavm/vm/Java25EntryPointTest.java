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
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.DataInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.JsModuleTest;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.TeaVMEntryPoint;
import org.teavm.junit.TeaVMTest;
import org.teavm.junit.TestPlatform;

@TeaVMTest
@EachTestCompiledSeparately
@JsModuleTest
@OnlyPlatform({ TestPlatform.JAVASCRIPT, TestPlatform.WEBASSEMBLY_GC })
public class Java25EntryPointTest {
    @Test
    @TeaVMEntryPoint(value = "org.teavm.vm.java25.publicstaticargs.Main", args = { "first", "second" },
            stdout = "args=first,second\n")
    public void publicStaticArgs(TestInfo info) throws Exception {
        assertJvmLauncher(info);
    }

    @Test
    @TeaVMEntryPoint(value = "org.teavm.vm.java25.packagestaticargs.Main", args = { "first", "second" },
            stdout = "args=first,second\n")
    public void packageStaticArgs(TestInfo info) throws Exception {
        assertJvmLauncher(info);
    }

    @Test
    @TeaVMEntryPoint(value = "org.teavm.vm.java25.instanceargs.Main", args = { "first", "second" },
            stdout = "constructor\n1:42:first,second\n")
    public void instanceArgs(TestInfo info) throws Exception {
        assertJvmLauncher(info);
    }

    @Test
    @TeaVMEntryPoint(value = "org.teavm.vm.java25.instancenoargs.Main", args = { "first", "second" },
            stdout = "constructor\n1:42\n")
    public void instanceNoArgs(TestInfo info) throws Exception {
        assertJvmLauncher(info);
    }

    @Test
    @TeaVMEntryPoint(value = "org.teavm.vm.java25.staticnoargs.Main", args = { "first", "second" },
            stdout = "static\n")
    public void staticNoArgs(TestInfo info) throws Exception {
        assertJvmLauncher(info);
    }

    @Test
    @TeaVMEntryPoint(value = "org.teavm.vm.java25.staticthrowingconstructor.Main", args = { "first", "second" },
            stdout = "initialized\nstatic\n")
    public void staticThrowingConstructor(TestInfo info) throws Exception {
        assertJvmLauncher(info);
    }

    @Test
    @TeaVMEntryPoint(value = "org.teavm.vm.java25.staticnodefaultconstructor.Main", args = { "first", "second" },
            stdout = "static\n")
    public void staticNoDefaultConstructor(TestInfo info) throws Exception {
        assertJvmLauncher(info);
    }

    @Test
    @TeaVMEntryPoint(value = "org.teavm.vm.java25.overloaded.Main", args = { "first", "second" },
            stdout = "args=first,second\n")
    public void overloaded(TestInfo info) throws Exception {
        assertJvmLauncher(info);
    }

    @Test
    @TeaVMEntryPoint(value = "Java25CompactMain", args = { "first", "second" },
            stdout = "42\n")
    public void compact(TestInfo info) throws Exception {
        assertJvmLauncher(info);
    }

    @Test
    @TeaVMEntryPoint(value = "org.teavm.vm.java25.unterminatedoutput.Main", args = { "first", "second" },
            stdout = "prefix:2\nfirst/second")
    public void unterminatedOutput(TestInfo info) throws Exception {
        assertJvmLauncher(info);
    }

    private void assertJvmLauncher(TestInfo info) throws Exception {
        var entryPoint = info.getTestMethod().orElseThrow().getAnnotation(TeaVMEntryPoint.class);
        var resource = entryPoint.value().replace('.', '/') + ".class";
        try (var stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(stream, resource);
            var input = new DataInputStream(stream);
            assertEquals(0xCAFEBABE, input.readInt());
            assertEquals(0, input.readUnsignedShort(), "Fixtures must not require preview features");
            assertEquals(69, input.readUnsignedShort(), "Fixtures must use Java 25 bytecode");
        }
        var classes = Class.forName(entryPoint.value(), false, getClass().getClassLoader())
                .getProtectionDomain().getCodeSource().getLocation();
        var executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        var command = new ArrayList<>(List.of(
                Path.of(System.getProperty("java.home"), "bin", executable).toString(),
                "-cp", Path.of(classes.toURI()).toString(), entryPoint.value()));
        command.addAll(List.of(entryPoint.args()));
        var process = new ProcessBuilder(command).redirectErrorStream(true).start();
        try {
            assertTrue(process.waitFor(15, TimeUnit.SECONDS), "JDK launcher timed out");
            var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
                    .replace("\r\n", "\n");
            assertEquals(0, process.exitValue(), output);
            assertEquals(entryPoint.stdout(), output);
        } finally {
            process.destroyForcibly();
        }
    }
}
