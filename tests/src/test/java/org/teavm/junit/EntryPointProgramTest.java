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
package org.teavm.junit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.teavm.junit.spi.ExternalCompilationRequest;
import org.teavm.junit.spi.ExternalCompilationResult;
import org.teavm.junit.spi.ExternalTestPlatform;
import org.teavm.model.ReferenceCache;

public class EntryPointProgramTest {
    @Test
    public void rejectsJavaScriptStderrTail() throws Throwable {
        checkStderrTail("javaScriptStderr");
    }

    @Test
    public void rejectsWasmGCStderrTail() throws Throwable {
        checkStderrTail("wasmGCStderr");
    }

    private void checkStderrTail(String name) throws Throwable {
        var method = getClass().getDeclaredMethod(name);
        assumeTrue(TeaVMTestExecutionSupport.hasParticipatingPlatform(method, getClass()));
        var error = assertThrows(AssertionError.class,
                () -> TeaVMTestExecutionSupport.runOnAllPlatforms(method, getClass()));
        assertTrue(error.getMessage().contains("Unexpected stderr"), error.getMessage());
        assertTrue(error.getMessage().contains("unterminated error"), error.getMessage());
    }

    @OnlyPlatform(TestPlatform.JAVASCRIPT)
    @TeaVMEntryPoint(value = "org.teavm.vm.java25.unterminatedstderr.Main", stdout = "visible")
    public void javaScriptStderr() {
    }

    @OnlyPlatform(TestPlatform.WEBASSEMBLY_GC)
    @TeaVMEntryPoint(value = "org.teavm.vm.java25.unterminatedstderr.Main", stdout = "visible")
    public void wasmGCStderr() {
    }

    @Test
    public void rejectsUnsupportedExternalEntryPoint() throws Exception {
        var platform = new ExternalWasmGCPlatformSupport(null, new ReferenceCache(), new ExternalTestPlatform() {
            @Override
            public String getName() {
                return "test-external";
            }

            @Override
            public String getBasePlatform() {
                return TestPlatform.WEBASSEMBLY_GC;
            }

            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public String getBrowserRunner() {
                return null;
            }

            @Override
            public ExternalCompilationResult compile(ExternalCompilationRequest request) {
                throw new AssertionError("An unsupported program must not reach the external compiler");
            }
        });
        var method = getClass().getDeclaredMethod("wasmGCStderr");
        var result = platform.compile(spec(method.getAnnotation(TeaVMEntryPoint.class), false), "test",
                TeaVMTestConfiguration.WASM_GC_DEFAULT, new File(System.getProperty("java.io.tmpdir")), method);
        assertFalse(result.success);
        assertTrue(result.errorMessage.contains("not supported by external platform test-external"));
    }

    @Test
    public void rejectsUnsupportedAsyncWasmGCEntryPoint() throws Exception {
        var platform = new WebAssemblyGCPlatformSupport(null, new ReferenceCache(), false);
        var method = getClass().getDeclaredMethod("wasmGCStderr");
        var result = platform.compile(spec(method.getAnnotation(TeaVMEntryPoint.class), true), "test",
                TeaVMTestConfiguration.WASM_GC_DEFAULT, new File(System.getProperty("java.io.tmpdir")), method);
        assertFalse(result.success);
        assertTrue(result.errorMessage.contains("Async @TeaVMEntryPoint is not supported"));
    }

    private TestEntryPointSpec spec(TeaVMEntryPoint entryPoint, boolean asyncEnabled) {
        return new TestEntryPointSpec(getClass().getName(), List.of(), false, Map.of(), new Properties(),
                asyncEnabled, entryPoint);
    }
}
