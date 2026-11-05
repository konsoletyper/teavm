/*
 * Copyright 2026 TeaVM contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.teavm.classlib.java.util.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@EachTestCompiledSeparately
public class ExecutorServiceCompatibilityTest {
    @Test
    public void cachedThreadPoolSubmitAndShutdown() throws Exception {
        ExecutorService executor = Executors.newCachedThreadPool(task -> new Thread(task, "unciv-worker"));
        try {
            Future<Integer> result = executor.submit(() -> 42);
            assertEquals(Integer.valueOf(42), result.get());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void timedGetUsesJdkNonpositiveTimeoutSemantics() throws Exception {
        FutureTask<Integer> completed = new FutureTask<>(() -> 42);
        completed.run();
        assertEquals(Integer.valueOf(42), completed.get(-1, TimeUnit.SECONDS));

        FutureTask<Integer> incomplete = new FutureTask<>(() -> 7);
        try {
            incomplete.get(-1, TimeUnit.SECONDS);
            fail("Expected timeout for incomplete future with negative timeout");
        } catch (TimeoutException expected) {
            // Expected.
        }
    }
}
