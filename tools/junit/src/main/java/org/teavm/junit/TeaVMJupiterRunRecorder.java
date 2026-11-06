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

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Collects every {@link TestRun} compiled for one test class — one at a time as each method is
 * intercepted (per-method mode), or all at once right after whole-class compilation — and, once the
 * class is done, writes them out as the same {@code tests.json} descriptor
 * {@link TeaVMTestRunner} writes for JUnit 4/TestNG (via the shared
 * {@link TeaVMTestExecutionSupport#writeRunsDescriptor}), for whatever external tooling consumes
 * it. Implementing {@link ExtensionContext.Store.CloseableResource} is what triggers that write
 * automatically when Jupiter tears the class down, without a separate {@code AfterAllCallback}.
 */
final class TeaVMJupiterRunRecorder implements ExtensionContext.Store.CloseableResource {
    private final Class<?> testClass;
    private final List<TestRun> runs = new ArrayList<>();

    TeaVMJupiterRunRecorder(Class<?> testClass) {
        this.testClass = testClass;
    }

    void record(TestRun run) {
        runs.add(run);
    }

    @Override
    public void close() {
        TeaVMTestExecutionSupport.writeRunsDescriptor(testClass, runs);
    }
}
