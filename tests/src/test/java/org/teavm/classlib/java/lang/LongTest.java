/*
 *  Copyright 2017 konsoletyper.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class LongTest {
    @Test
    public void compares() {
        assertTrue(Long.compare(10, 5) > 0);
        assertTrue(Long.compare(5, 10) < 0);
        assertTrue(Long.compare(5, 5) == 0);
        assertTrue(Long.compare(Long.MAX_VALUE, Long.MIN_VALUE) > 0);
        assertTrue(Long.compare(Long.MIN_VALUE, Long.MAX_VALUE) < 0);
    }

    @Test
    @SkipJVM
    public void calculatesHashCode() {
        assertEquals(23 ^ 42, Long.hashCode((23L << 32) | 42));
    }

    @Test
    public void bitsReversed() {
        assertEquals(0, Long.reverse(0));
        assertEquals(0x8000000000000000L, Long.reverse(1));
        assertEquals(0x0020000000000000L, Long.reverse(0x400));
        assertEquals(0x0000000000000001L, Long.reverse(0x8000000000000000L));
        assertEquals(0x8888888888888888L, Long.reverse(0x1111111111111111L));
        assertEquals(0x0C0C0C0C0C0C0C0CL, Long.reverse(0x3030303030303030L));
        assertEquals(0x00000000000000FFL, Long.reverse(0xFF00000000000000L));
        assertEquals(0xFF00000000000000L, Long.reverse(0x00000000000000FFL));
        assertEquals(0xFFFFFFFFFFFFFFFFL, Long.reverse(0xFFFFFFFFFFFFFFFFL));
        assertEquals(0xF63BA00000000000L, Long.reverse(0x5DC6F));
    }
}
