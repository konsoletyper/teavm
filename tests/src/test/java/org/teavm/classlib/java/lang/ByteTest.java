/*
 *  Copyright 2021 Alexey Andreev.
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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class ByteTest {
    @Test
    public void hashCodeTest() {
        assertEquals(Byte.hashCode((byte) 0), Byte.valueOf((byte) 0).hashCode());
        assertEquals(Byte.hashCode((byte) 23), Byte.valueOf((byte) 23).hashCode());
    }

    @Test
    public void compareUnsigned() {
        assertTrue(Byte.compareUnsigned((byte) 5, (byte) 3) > 0);
        assertTrue(Byte.compareUnsigned((byte) 3, (byte) 5) < 0);
        assertTrue(Byte.compareUnsigned((byte) 3, (byte) 3) == 0);
        assertTrue(Byte.compareUnsigned((byte) -23, (byte) 23) > 0);
        assertTrue(Byte.compareUnsigned((byte) 23, (byte) -23) < 0);
    }

    @Test
    public void toUnsigned() {
        assertEquals(254, Byte.toUnsignedInt((byte) -2));
        assertEquals(254L, Byte.toUnsignedLong((byte) -2));
    }

    @Test
    public void bytes() {
        assertEquals(1, Byte.BYTES);
    }

    @Test
    public void cache() {
        for (int b = Byte.MIN_VALUE; b <= Byte.MAX_VALUE; b++) {
            assertSame(Byte.valueOf((byte) b), (byte) b);
        }
    }
}
