/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.classlib.java.util;

import static org.junit.Assert.assertTrue;
import java.security.SecureRandom;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@OnlyPlatform({TestPlatform.JAVASCRIPT, TestPlatform.WEBASSEMBLY_GC})
public class SecureRandomTest {
    @Test
    public void nextInt() {
        var array = new int[10];
        var random = new SecureRandom();
        for (var i = 0; i < 1000; ++i) {
            var n = Integer.remainderUnsigned(random.nextInt(), array.length);
            array[n]++;
        }
        for (var elem : array) {
            assertTrue("Each number must be generated at least once", elem > 0);
        }
    }
}
