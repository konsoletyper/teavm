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
package org.teavm.vm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.teavm.backend.c.CTarget;
import org.teavm.backend.c.generate.CNameProvider;
import org.teavm.backend.javascript.JavaScriptTarget;
import org.teavm.backend.wasm.WasmGCTarget;
import org.teavm.diagnostics.Problem;
import org.teavm.diagnostics.ProblemSeverity;
import org.teavm.model.ReferenceCache;
import org.teavm.parsing.ClasspathClassHolderSource;
import org.teavm.parsing.ClasspathResourceProvider;

public class AsyncDisabledTest {
    @Test
    public void errorReportedWhenDisabledJs() {
        checkErrorReported(JavaScriptTarget::new);
    }

    @Test
    public void errorReportedWhenDisabledWasmGC() {
        checkErrorReported(WasmGCTarget::new);
    }

    @Test
    public void errorReportedWhenDisabledC() {
        checkErrorReported(() -> new CTarget(new CNameProvider()));
    }

    @Test
    public void noErrorWhenEnabledJs() {
        assertEquals(List.of(), compile(JavaScriptTarget::new, SleepingProgram.class, true));
    }

    @Test
    public void noErrorWhenEnabledWasmGC() {
        assertEquals(List.of(), compile(WasmGCTarget::new, SleepingProgram.class, true));
    }

    @Test
    public void synchronizationAllowedWhenDisabled() {
        assertEquals(List.of(), compile(JavaScriptTarget::new, SynchronizedProgram.class, false));
        assertEquals(List.of(), compile(WasmGCTarget::new, SynchronizedProgram.class, false));
    }

    private void checkErrorReported(Supplier<TeaVMTarget> target) {
        var problems = compile(target, SleepingProgram.class, false);
        assertTrue(!problems.isEmpty(), "Expected error for async method");
        assertTrue(problems.stream().allMatch(p -> p.getText().contains("coroutine transformation is disabled")),
                problems::toString);
    }

    private List<Problem> compile(Supplier<TeaVMTarget> target, Class<?> mainClass, boolean asyncEnabled) {
        var classLoader = AsyncDisabledTest.class.getClassLoader();
        var vm = new TeaVMBuilder(target.get())
                .setClassLoader(classLoader)
                .setClassSource(new ClasspathClassHolderSource(new ReferenceCache()))
                .setResourceProvider(new ClasspathResourceProvider(classLoader))
                .build();
        vm.setAsyncEnabled(asyncEnabled);
        vm.installPlugins();
        vm.setEntryPoint(mainClass.getName());
        vm.build(fileName -> new ByteArrayOutputStream(), "out");
        return vm.getProblemProvider().getProblems().stream()
                .filter(p -> p.getSeverity() == ProblemSeverity.ERROR)
                .toList();
    }

    public static class SleepingProgram {
        public static void main(String[] args) throws InterruptedException {
            System.out.println("before");
            Thread.sleep(100);
            System.out.println("after");
        }
    }

    public static class SynchronizedProgram {
        private static final Object LOCK = new Object();
        private static int counter;

        public static void main(String[] args) {
            synchronized (LOCK) {
                counter++;
            }
            var sb = new StringBuffer();
            sb.append(counter);
            System.out.println(sb);
        }
    }
}
