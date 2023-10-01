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
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class ShortTest {
    @Test
    public void hashCodeTest() {
        assertEquals(Short.hashCode((short) 0), Short.valueOf((byte) 0).hashCode());
        assertEquals(Short.hashCode((byte) 23), Short.valueOf((byte) 23).hashCode());
    }

    @Test
    public void compareUnsigned() {
        assertTrue(Short.compareUnsigned((byte) 5, (byte) 3) > 0);
        assertTrue(Short.compareUnsigned((byte) 3, (byte) 5) < 0);
        assertTrue(Short.compareUnsigned((byte) 3, (byte) 3) == 0);
        assertTrue(Short.compareUnsigned((byte) -23, (byte) 23) > 0);
        assertTrue(Short.compareUnsigned((byte) 23, (byte) -23) < 0);
    }

    @Test
    public void toUnsigned() {
        assertEquals(65534, Short.toUnsignedInt((short) -2));
        assertEquals(65534L, Short.toUnsignedLong((short) -2));
    }

    @Test
    public void bytes() {
        assertEquals(2, Short.BYTES);
    }

    @Test
    public void reverseBytes() {
        assertEquals((short) 12405, Short.reverseBytes((short) 30000));
        assertEquals((short) -12150, Short.reverseBytes((short) -30000));
    }
}
