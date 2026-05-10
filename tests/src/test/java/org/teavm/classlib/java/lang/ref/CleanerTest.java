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
package org.teavm.classlib.java.lang.ref;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.lang.ref.Cleaner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.classlib.support.GCSupport;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@EachTestCompiledSeparately
@SkipPlatform(TestPlatform.C)
public class CleanerTest {
    @Test
    public void cleanerCreated() {
        var cleaner = Cleaner.create();
        assertTrue(cleaner != null);
    }

    @Test
    public void manualClean() {
        var cleaner = Cleaner.create();
        var counter = new int[1];
        var obj = new Object();
        var cleanable = cleaner.register(obj, () -> counter[0]++);

        cleanable.clean();
        assertEquals(1, counter[0]);

        // Second call to clean() must be a no-op
        cleanable.clean();
        assertEquals(1, counter[0]);
    }

    @Test
    public void gcTriggersClean() {
        var cleaner = Cleaner.create();
        var counter = new int[1];
        registerForCleanup(cleaner, counter);
        GCSupport.tryToTriggerGC();
        assertEquals(1, counter[0]);
    }

    private void registerForCleanup(Cleaner cleaner, int[] counter) {
        var obj = new Object();
        cleaner.register(obj, () -> counter[0]++);
    }
}
