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
import java.util.ArrayList;
import java.util.HashMap;
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
    private final Map<Method, Throwable> errors;
    private final Map<Method, InvocationResults> invocationResults = new HashMap<>();

    /**
     * @param errors methods that were left out of compilation because of an error that is specific to them
     *               (e.g. unsupported argument source of a parameterized test); the error is reported when the
     *               method runs, so that it doesn't break other methods of the class.
     */
    TeaVMWholeClassCompilation(Map<TestPlatform, Map<Method, List<TestRun>>> runsByPlatform,
            Map<Method, Throwable> errors) {
        this.runsByPlatform = runsByPlatform;
        this.errors = errors;
    }

    void runMethod(Method method) throws Throwable {
        checkErrors(method);
        TeaVMTestExecutionSupport.runWholeClassMethod(runsByPlatform, method);
    }

    /**
     * Reports result of one invocation of a parameterized test. All invocations are run by the first call,
     * subsequent calls take results from the cache.
     */
    void runInvocation(Method method, int invocationIndex) throws Throwable {
        checkErrors(method);
        InvocationResults results;
        synchronized (invocationResults) {
            results = invocationResults.get(method);
            if (results == null) {
                var runs = new ArrayList<TestRun>();
                for (var platform : TeaVMTestInfrastructure.platforms) {
                    var methodRuns = runsByPlatform.get(platform.getPlatform());
                    if (methodRuns != null && methodRuns.containsKey(method)) {
                        runs.addAll(methodRuns.get(method));
                    }
                }
                results = InvocationResults.collect(runs, false);
                invocationResults.put(method, results);
            }
        }
        results.check(invocationIndex);
    }

    private void checkErrors(Method method) throws Throwable {
        var error = errors.get(method);
        if (error != null) {
            throw error;
        }
    }

    @Override
    public void close() {
        TeaVMTestExecutionSupport.cleanupWholeClass(runsByPlatform);
    }
}
