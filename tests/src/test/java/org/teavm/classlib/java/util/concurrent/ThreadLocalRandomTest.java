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
package org.teavm.classlib.java.util.concurrent;

import static org.junit.Assert.assertTrue;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class ThreadLocalRandomTest {
    // A smoke test that ensures that ThreadLocalRandom produces *some* numbers and does not crash
    @Test
    public void doesNotCrash() {
        var ints = new HashSet<Integer>();
        for (var i = 0; i < 5; ++i) {
            ints.add(ThreadLocalRandom.current().nextInt());
        }
        assertTrue("Different numbers generated", ints.size() > 1);
    }
}
