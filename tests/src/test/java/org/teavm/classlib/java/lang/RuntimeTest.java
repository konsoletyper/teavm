/*
 *  Copyright 2026 Ethan Resnick.
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
package org.teavm.classlib.java.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTest;
import org.teavm.junit.TestPlatform;

@TeaVMTest
public class RuntimeTest {
    // Runtime.maxMemory(): "Returns the maximum amount of memory that the Java virtual machine will attempt
    // to use. If there is no inherent limit then the value Long.MAX_VALUE will be returned."
    @Test
    public void maxMemoryIsPositiveAndBoundsFreeMemory() {
        var runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        assertTrue(max > 0);
        assertTrue(runtime.freeMemory() <= max);
        assertEquals(max, runtime.maxMemory());
    }

    // The JS and Wasm GC heaps are managed by the host, which exposes no limit.
    @Test
    @SkipJVM
    @OnlyPlatform({TestPlatform.JAVASCRIPT, TestPlatform.WEBASSEMBLY_GC})
    public void maxMemoryIsUnlimitedOnHostManagedHeaps() {
        assertEquals(Long.MAX_VALUE, Runtime.getRuntime().maxMemory());
    }

    // The C backend's heap grows up to a configured maximum, which is an inherent limit.
    @Test
    @OnlyPlatform(TestPlatform.C)
    public void maxMemoryReportsConfiguredHeapLimitOnC() {
        long max = Runtime.getRuntime().maxMemory();
        assertNotEquals(Long.MAX_VALUE, max);
        assertTrue(max >= Runtime.getRuntime().freeMemory());
    }
}
