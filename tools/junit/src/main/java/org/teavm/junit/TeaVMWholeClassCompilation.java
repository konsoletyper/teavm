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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Holds one class's whole-class compilation result in a {@link ExtensionContext.Store}, computed
 * once in {@link TeaVMJupiterExtension#beforeAll} and consumed by every intercepted test method of
 * that class. Implementing {@link ExtensionContext.Store.CloseableResource} lets Jupiter release
 * the underlying browser/run-strategy resources automatically when the class is done, without a
 * separate {@code AfterAllCallback}.
 */
final class TeaVMWholeClassCompilation implements ExtensionContext.Store.CloseableResource {
    private final Map<TestPlatform, Map<Method, List<TestRun>>> runsByPlatform;

    TeaVMWholeClassCompilation(Map<TestPlatform, Map<Method, List<TestRun>>> runsByPlatform) {
        this.runsByPlatform = runsByPlatform;
    }

    void runMethod(Method method) throws Throwable {
        TeaVMTestExecutionSupport.runWholeClassMethod(runsByPlatform, method);
    }

    @Override
    public void close() {
        TeaVMTestExecutionSupport.cleanupWholeClass(runsByPlatform);
    }
}
