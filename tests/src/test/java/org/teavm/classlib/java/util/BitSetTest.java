/*
 *  Copyright 2017 Alexey Andreev.
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

/*
 *  Copyright 2014 Alexey Andreev.
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
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.teavm.classlib.java.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import java.util.BitSet;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class BitSetTest {
    BitSet eightbs;

    public BitSetTest() {
        eightbs = new BitSet();
        for (int i = 0; i < 8; i++) {
            eightbs.set(i);
        }
    }

    @Test
    public void constructor() {
        BitSet bs = new BitSet();
        assertEquals("{}", bs.toString(), "New BitSet had invalid string representation");
    }

    @Test
    public void constructorI() {
        BitSet bs = new BitSet(128);
        assertEquals("{}", bs.toString(), "New BitSet had invalid string representation: " + bs);
    }

    @Test
    public void constructFromBytes() {
        for (int i = 4; i < 8; ++i) {
            byte[] bytes = new byte[i];
            Arrays.fill(bytes, (byte) 0x80);
            BitSet bs = BitSet.valueOf(bytes);
            assertEquals(i * 8, bs.length(), "Wrong length of BitSet");
            for (int j = 0; j < bs.length(); ++j) {
                if (j % 8 == 7) {
                    assertTrue(bs.get(j), "Expected that " + j + "th bit is to be set");
                } else {
                    assertFalse(bs.get(j), "Expected that " + j + "th bit is not to be set");
                }
            }
        }
    }

    @Test
    public void toByteArray() throws Exception {
        assertEquals("[]", Arrays.toString(BitSet.valueOf(new long[0]).toByteArray()));
        assertEquals("[1]", Arrays.toString(BitSet.valueOf(new long[] { 1 }).toByteArray()));
        assertEquals("[-17, -51, -85, -112, 120, 86, 52, 18]",
                Arrays.toString(BitSet.valueOf(new long[] { 0x1234567890abcdefL }).toByteArray()));
        assertEquals("[1, 0, 0, 0, 0, 0, 0, 0, 2]",
                Arrays.toString(BitSet.valueOf(new long[] { 1, 2 }).toByteArray()));
    }

    @Test
    public void constructFromLongs() {
        BitSet bs = BitSet.valueOf(new long[] { 7, 2, 5, 1L << 36 });
        assertTrue(bs.get(0) && bs.get(1) && bs.get(2) && bs.get(Long.SIZE + 1)
                && bs.get(2 * Long.SIZE) && bs.get(2 * Long.SIZE + 2) && bs.get(3 * Long.SIZE + 36));
        assertFalse(bs.get(3) || bs.get(Long.SIZE + 6) || bs.get(2 * Long.SIZE + 15) || bs.get(3 * Long.SIZE));
    }

    @Test
    public void testStream() {
        assertArrayEquals(new int[] { 0, 1, 2, Long.SIZE + 1, 2 * Long.SIZE, 2 * Long.SIZE + 2, 3 * Long.SIZE + 36 },
                BitSet.valueOf(new long[] { 7, 2, 5, 1L << 36 }).stream().toArray());
        BitSet bs = new BitSet();
        assertEquals(0, bs.stream().count());
        bs.set(1);
        assertArrayEquals(new int[] { 1 }, bs.stream().toArray());
    }

    @Test
    public void clonePerformed() {
        BitSet bs;
        bs = (BitSet) eightbs.clone();
        assertEquals(bs, eightbs, "clone failed to return equal BitSet");
    }

    @Test
    public void equalityComputed() {
        BitSet bs;
        bs = (BitSet) eightbs.clone();
        assertEquals(eightbs, eightbs, "Same BitSet returned false");
        assertEquals(bs, eightbs, "Identical BitSet returned false");
        bs.clear(6);
        assertFalse(eightbs.equals(bs), "Different BitSets returned true");

        bs = (BitSet) eightbs.clone();
        bs.set(128);
        assertFalse(eightbs.equals(bs), "Different sized BitSet with higher bit set returned true");
        bs.clear(128);
        assertTrue(eightbs.equals(bs), "Different sized BitSet with higher bits not set returned false");
    }

    @Test
    public void hashCodeComputed() {
        // Test for method int java.util.BitSet.hashCode()
        BitSet bs = (BitSet) eightbs.clone();
        bs.clear(2);
        bs.clear(6);
        assertEquals(1129, bs.hashCode(), "BitSet returns wrong hash value");
        bs.set(10);
        bs.clear(3);
        assertEquals(97, bs.hashCode(), "BitSet returns wrong hash value");
    }

    @Test
    public void clear() {
        eightbs.clear();
        for (int i = 0; i < 8; i++) {
            assertFalse(eightbs.get(i), "Clear didn't clear bit " + i);
        }
        assertEquals(0, eightbs.length(), "Test1: Wrong length");

        BitSet bs = new BitSet(3400);
        bs.set(0, bs.size() - 1); // ensure all bits are 1's
        bs.set(bs.size() - 1);
        bs.clear();
        assertEquals(0, bs.length(), "Test2: Wrong length");
        assertTrue(bs.isEmpty(), "Test2: isEmpty() returned incorrect value");
        assertEquals(0, bs.cardinality(), "Test2: cardinality() returned incorrect value");
    }

    @Test
    public void clearI() {
        // Test for method void java.util.BitSet.clear(int)

        eightbs.clear(7);
        assertFalse(eightbs.get(7), "Failed to clear bit");

        // Check to see all other bits are still set
        for (int i = 0; i < 7; i++) {
            assertTrue(eightbs.get(i), "Clear cleared incorrect bits");
        }

        eightbs.clear(165);
        assertFalse(eightbs.get(165), "Failed to clear bit");

        BitSet bs = new BitSet(0);
        assertEquals(0, bs.length(), "Test1: Wrong length,");

        bs.clear(0);
        assertEquals(0, bs.length(), "Test2: Wrong length,");

        bs.clear(60);
        assertEquals(0, bs.length(), "Test3: Wrong length,");

        bs.clear(120);
        assertEquals(0, bs.length(), "Test4: Wrong length,");

        bs.set(25);
        assertEquals(26, bs.length(), "Test5: Wrong length,");

        bs.clear(80);
        assertEquals(26, bs.length(), "Test6: Wrong length,");

        bs.clear(25);
        assertEquals(0, bs.length(), "Test7: Wrong length,");
    }

    @Test
    public void clearII() throws IndexOutOfBoundsException {
        // Regression for HARMONY-98
        BitSet bitset = new BitSet();
        for (int i = 0; i < 20; i++) {
            bitset.set(i);
        }
        bitset.clear(10, 10);

        // Test for method void java.util.BitSet.clear(int, int)
        // pos1 and pos2 are in the same bitset element
        BitSet bs = new BitSet(16);
        int initialSize = bs.size();
        bs.set(0, initialSize);
        bs.clear(5);
        bs.clear(15);
        bs.clear(7, 11);
        for (int i = 0; i < 7; i++) {
            if (i == 5) {
                assertFalse(bs.get(i), "Shouldn't have flipped bit " + i);
            } else {
                assertTrue(bs.get(i), "Shouldn't have cleared bit " + i);
            }
        }
        for (int i = 7; i < 11; i++) {
            assertFalse(bs.get(i), "Failed to clear bit " + i);
        }

        for (int i = 11; i < initialSize; i++) {
            if (i == 15) {
                assertFalse(bs.get(i), "Shouldn't have flipped bit " + i);
            } else {
                assertTrue(bs.get(i), "Shouldn't have cleared bit " + i);
            }
        }

        for (int i = initialSize; i < bs.size(); i++) {
            assertFalse(bs.get(i), "Shouldn't have flipped bit " + i);
        }

        // pos1 and pos2 is in the same bitset element, boundry testing
        bs = new BitSet(16);
        initialSize = bs.size();
        bs.set(0, initialSize);
        bs.clear(7, 64);
        for (int i = 0; i < 7; i++) {
            assertTrue(bs.get(i), "Shouldn't have cleared bit " + i);
        }
        for (int i = 7; i < 64; i++) {
            assertFalse(bs.get(i), "Failed to clear bit " + i);
        }
        for (int i = 64; i < bs.size(); i++) {
            assertTrue(!bs.get(i), "Shouldn't have flipped bit " + i);
        }
        // more boundary testing
        bs = new BitSet(32);
        initialSize = bs.size();
        bs.set(0, initialSize);
        bs.clear(0, 64);
        for (int i = 0; i < 64; i++) {
            assertFalse(bs.get(i), "Failed to clear bit " + i);
        }
        for (int i = 64; i < bs.size(); i++) {
            assertFalse(bs.get(i), "Shouldn't have flipped bit " + i);
        }

        bs = new BitSet(32);
        initialSize = bs.size();
        bs.set(0, initialSize);
        bs.clear(0, 65);
        for (int i = 0; i < 65; i++) {
            assertFalse(bs.get(i), "Failed to clear bit " + i);
        }
        for (int i = 65; i < bs.size(); i++) {
            assertFalse(bs.get(i), "Shouldn't have flipped bit " + i);
        }

        // pos1 and pos2 are in two sequential bitset elements
        bs = new BitSet(128);
        initialSize = bs.size();
        bs.set(0, initialSize);
        bs.clear(7);
        bs.clear(110);
        bs.clear(9, 74);
        for (int i = 0; i < 9; i++) {
            if (i == 7) {
                assertFalse(bs.get(i), "Shouldn't have flipped bit " + i);
            } else {
                assertTrue(bs.get(i), "Shouldn't have cleared bit " + i);
            }
        }
        for (int i = 9; i < 74; i++) {
            assertFalse(bs.get(i), "Failed to clear bit " + i);
        }
        for (int i = 74; i < initialSize; i++) {
            if (i == 110) {
                assertFalse(bs.get(i), "Shouldn't have flipped bit " + i);
            } else {
                assertTrue(bs.get(i), "Shouldn't have cleared bit " + i);
            }
        }
        for (int i = initialSize; i < bs.size(); i++) {
            assertFalse(bs.get(i), "Shouldn't have flipped bit " + i);
        }

        // pos1 and pos2 are in two non-sequential bitset elements
        bs = new BitSet(256);
        bs.set(0, 256);
        bs.clear(7);
        bs.clear(255);
        bs.clear(9, 219);
        for (int i = 0; i < 9; i++) {
            if (i == 7) {
                assertFalse(bs.get(i), "Shouldn't have flipped bit " + i);
            } else {
                assertTrue(bs.get(i), "Shouldn't have cleared bit " + i);
            }
        }

        for (int i = 9; i < 219; i++) {
            assertFalse(bs.get(i), "failed to clear bit " + i);
        }

        for (int i = 219; i < 255; i++) {
            assertTrue(bs.get(i), "Shouldn't have cleared bit " + i);
        }

        for (int i = 255; i < bs.size(); i++) {
            assertFalse(bs.get(i), "Shouldn't have flipped bit " + i);
        }

        bs.set(2, 4);
        bs.clear(2, 2);
        assertTrue(bs.get(2), "Bit got cleared incorrectly ");

        /*try {
            bs.clear(4, 2);
            fail("Test4: Attempt to flip with illegal args failed to generate exception");
        } catch (IndexOutOfBoundsException e) {
            // excepted
        }*/

        bs = new BitSet(0);
        assertEquals(0, bs.length(), "Test1: Wrong length,");

        bs.clear(0, 2);
        assertEquals(0, bs.length(), "Test2: Wrong length,");

        bs.clear(60, 64);
        assertEquals(0, bs.length(), "Test3: Wrong length,");

        bs.clear(64, 120);
        assertEquals(0, bs.length(), "Test4: Wrong length,");

        bs.set(25);
        assertEquals(26, bs.length(), "Test5: Wrong length,");

        bs.clear(60, 64);
        assertEquals(26, bs.length(), "Test6: Wrong length,");

        bs.clear(64, 120);
        assertEquals(26, bs.length(), "Test7: Wrong length,");

        bs.clear(80);
        assertEquals(26, bs.length(), "Test8: Wrong length,");

        bs.clear(25);
        assertEquals(0, bs.length(), "Test9: Wrong length,");
    }

    @Test
    public void getI() {
        // Test for method boolean java.util.BitSet.get(int)

        BitSet bs = new BitSet();
        bs.set(8);
        assertFalse(eightbs.get(99), "Get returned true for index out of range");
        assertTrue(eightbs.get(3), "Get returned false for set value");
        assertFalse(bs.get(0), "Get returned true for a non set value");

        /*try {
            bs.get(-1);
            fail("Attempt to get at negative index failed to generate exception");
        } catch (IndexOutOfBoundsException e) {
            // Correct behaviour
        }*/

        bs = new BitSet(1);
        assertFalse(bs.get(64), "Access greater than size");

        bs = new BitSet();
        bs.set(63);
        assertTrue(bs.get(63), "Test highest bit");

        bs = new BitSet(0);
        assertEquals(0, bs.length(), "Test1: Wrong length,");

        bs.get(2);
        assertEquals(0, bs.length(), "Test2: Wrong length,");

        bs.get(70);
        assertEquals(0, bs.length(), "Test3: Wrong length,");
    }

    @Test
    public void getII() {
        BitSet bitset = new BitSet(30);
        bitset.get(3, 3);

        // Test for method boolean java.util.BitSet.get(int, int)
        BitSet bs;
        BitSet resultbs;
        BitSet correctbs;
        bs = new BitSet(512);
        bs.set(3, 9);
        bs.set(10, 20);
        bs.set(60, 75);
        bs.set(121);
        bs.set(130, 140);

        // pos1 and pos2 are in the same bitset element, at index0
        resultbs = bs.get(3, 6);
        correctbs = new BitSet(3);
        correctbs.set(0, 3);
        assertEquals(correctbs, resultbs, "Test1: Returned incorrect BitSet");

        // pos1 and pos2 are in the same bitset element, at index 1
        resultbs = bs.get(100, 125);
        correctbs = new BitSet(25);
        correctbs.set(21);
        assertEquals(correctbs, resultbs, "Test2: Returned incorrect BitSet");

        // pos1 in bitset element at index 0, and pos2 in bitset element at
        // index 1
        resultbs = bs.get(15, 125);
        correctbs = new BitSet(25);
        correctbs.set(0, 5);
        correctbs.set(45, 60);
        correctbs.set(121 - 15);
        assertEquals(correctbs, resultbs, "Test3: Returned incorrect BitSet");

        // pos1 in bitset element at index 1, and pos2 in bitset element at
        // index 2
        resultbs = bs.get(70, 145);
        correctbs = new BitSet(75);
        correctbs.set(0, 5);
        correctbs.set(51);
        correctbs.set(60, 70);
        assertEquals(correctbs, resultbs, "Test4: Returned incorrect BitSet");

        // pos1 in bitset element at index 0, and pos2 in bitset element at
        // index 2
        resultbs = bs.get(5, 145);
        correctbs = new BitSet(140);
        correctbs.set(0, 4);
        correctbs.set(5, 15);
        correctbs.set(55, 70);
        correctbs.set(116);
        correctbs.set(125, 135);
        assertEquals(correctbs, resultbs, "Test5: Returned incorrect BitSet");

        // pos1 in bitset element at index 0, and pos2 in bitset element at
        // index 3
        resultbs = bs.get(5, 250);
        correctbs = new BitSet(200);
        correctbs.set(0, 4);
        correctbs.set(5, 15);
        correctbs.set(55, 70);
        correctbs.set(116);
        correctbs.set(125, 135);
        assertEquals(correctbs, resultbs, "Test6: Returned incorrect BitSet");

        assertEquals(bs.get(0, bs.size()), bs, "equality principle 1 ");

        // more tests
        BitSet bs2 = new BitSet(129);
        bs2.set(0, 20);
        bs2.set(62, 65);
        bs2.set(121, 123);
        resultbs = bs2.get(1, 124);
        correctbs = new BitSet(129);
        correctbs.set(0, 19);
        correctbs.set(61, 64);
        correctbs.set(120, 122);
        assertEquals(correctbs, resultbs, "Test7: Returned incorrect BitSet");

        // equality principle with some boundary conditions
        bs2 = new BitSet(128);
        bs2.set(2, 20);
        bs2.set(62);
        bs2.set(121, 123);
        bs2.set(127);
        resultbs = bs2.get(0, bs2.size());
        assertEquals(resultbs, bs2, "equality principle 2 ");

        bs2 = new BitSet(128);
        bs2.set(2, 20);
        bs2.set(62);
        bs2.set(121, 123);
        bs2.set(127);
        bs2.flip(0, 128);
        resultbs = bs2.get(0, bs.size());
        assertEquals(resultbs, bs2, "equality principle 3 ");

        bs = new BitSet(0);
        assertEquals(0, bs.length(), "Test1: Wrong length,");

        bs.get(0, 2);
        assertEquals(0, bs.length(), "Test2: Wrong length,");

        bs.get(60, 64);
        assertEquals(0, bs.length(), "Test3: Wrong length,");

        bs.get(64, 120);
        assertEquals(0, bs.length(), "Test4: Wrong length,");

        bs.set(25);
        assertEquals(26, bs.length(), "Test5: Wrong length,");

        bs.get(60, 64);
        assertEquals(26, bs.length(), "Test6: Wrong length,");

        bs.get(64, 120);
        assertEquals(26, bs.length(), "Test7: Wrong length,");

        bs.get(80);
        assertEquals(26, bs.length(), "Test8: Wrong length,");

        bs.get(25);
        assertEquals(26, bs.length(), "Test9: Wrong length,");
    }

    @Test
    public void flipI() {
        // Test for method void java.util.BitSet.flip(int)
        BitSet bs = new BitSet();
        bs.clear(8);
        bs.clear(9);
        bs.set(10);
        bs.flip(9);
        assertFalse(bs.get(8), "Failed to flip bit");
        assertTrue(bs.get(9), "Failed to flip bit");
        assertTrue(bs.get(10), "Failed to flip bit");

        bs.set(8);
        bs.set(9);
        bs.clear(10);
        bs.flip(9);
        assertTrue(bs.get(8), "Failed to flip bit");
        assertFalse(bs.get(9), "Failed to flip bit");
        assertFalse(bs.get(10), "Failed to flip bit");

        /*try {
            bs.flip(-1);
            fail("Attempt to flip at negative index failed to generate exception");
        } catch (IndexOutOfBoundsException e) {
            // Correct behaviour
        }*/

        // Try setting a bit on a 64 boundary
        bs.flip(128);
        assertTrue(bs.get(128), "Failed to flip bit");

        bs = new BitSet(64);
        for (int i = bs.size(); --i >= 0;) {
            bs.flip(i);
            assertTrue(bs.get(i), "Test1: Incorrectly flipped bit" + i);
            assertEquals(i + 1, bs.length(), "Incorrect length");
            for (int j = bs.size(); --j > i;) {
                assertTrue(!bs.get(j), "Test2: Incorrectly flipped bit" + j);
            }
            for (int j = i; --j >= 0;) {
                assertTrue(!bs.get(j), "Test3: Incorrectly flipped bit" + j);
            }
            bs.flip(i);
        }

        BitSet bs0 = new BitSet(0);
        assertEquals(0, bs0.length(), "Test1: Wrong length");

        bs0.flip(0);
        assertEquals(1, bs0.length(), "Test2: Wrong length");

        bs0.flip(63);
        assertEquals(64, bs0.length(), "Test3: Wrong length");

        eightbs.flip(7);
        assertTrue(!eightbs.get(7), "Failed to flip bit 7");

        // Check to see all other bits are still set
        for (int i = 0; i < 7; i++) {
            assertTrue(eightbs.get(i), "Flip flipped incorrect bits");
        }

        eightbs.flip(127);
        assertTrue(eightbs.get(127), "Failed to flip bit 127");

        eightbs.flip(127);
        assertTrue(!eightbs.get(127), "Failed to flip bit 127");
    }

    @Test
    public void flipII() {
        BitSet bitset = new BitSet();
        for (int i = 0; i < 20; i++) {
            bitset.set(i);
        }
        bitset.flip(10, 10);

        // Test for method void java.util.BitSet.flip(int, int)
        // pos1 and pos2 are in the same bitset element
        BitSet bs = new BitSet(16);
        bs.set(7);
        bs.set(10);
        bs.flip(7, 11);
        for (int i = 0; i < 7; i++) {
            assertTrue(!bs.get(i), "Shouldn't have flipped bit " + i);
        }
        assertFalse(bs.get(7), "Failed to flip bit 7");
        assertTrue(bs.get(8), "Failed to flip bit 8");
        assertTrue(bs.get(9), "Failed to flip bit 9");
        assertFalse(bs.get(10), "Failed to flip bit 10");
        for (int i = 11; i < bs.size(); i++) {
            assertTrue(!bs.get(i), "Shouldn't have flipped bit " + i);
        }

        // pos1 and pos2 is in the same bitset element, boundry testing
        bs = new BitSet(16);
        bs.set(7);
        bs.set(10);
        bs.flip(7, 64);
        for (int i = 0; i < 7; i++) {
            assertTrue(!bs.get(i), "Shouldn't have flipped bit " + i);
        }
        assertFalse(bs.get(7), "Failed to flip bit 7");
        assertTrue(bs.get(8), "Failed to flip bit 8");
        assertTrue(bs.get(9), "Failed to flip bit 9");
        assertFalse(bs.get(10), "Failed to flip bit 10");
        for (int i = 11; i < 64; i++) {
            assertTrue(bs.get(i), "failed to flip bit " + i);
        }
        assertFalse(bs.get(64), "Shouldn't have flipped bit 64");

        // more boundary testing
        bs = new BitSet(32);
        bs.flip(0, 64);
        for (int i = 0; i < 64; i++) {
            assertTrue(bs.get(i), "Failed to flip bit " + i);
        }
        assertFalse(bs.get(64), "Shouldn't have flipped bit 64");

        bs = new BitSet(32);
        bs.flip(0, 65);
        for (int i = 0; i < 65; i++) {
            assertTrue(bs.get(i), "Failed to flip bit " + i);
        }
        assertFalse(bs.get(65), "Shouldn't have flipped bit 65");

        // pos1 and pos2 are in two sequential bitset elements
        bs = new BitSet(128);
        bs.set(7);
        bs.set(10);
        bs.set(72);
        bs.set(110);
        bs.flip(9, 74);
        for (int i = 0; i < 7; i++) {
            assertFalse(bs.get(i), "Shouldn't have flipped bit " + i);
        }
        assertTrue(bs.get(7), "Shouldn't have flipped bit 7");
        assertFalse(bs.get(8), "Shouldn't have flipped bit 8");
        assertTrue(bs.get(9), "Failed to flip bit 9");
        assertFalse(bs.get(10), "Failed to flip bit 10");
        for (int i = 11; i < 72; i++) {
            assertTrue(bs.get(i), "failed to flip bit " + i);
        }
        assertFalse(bs.get(72), "Failed to flip bit 72");
        assertTrue(bs.get(73), "Failed to flip bit 73");
        for (int i = 74; i < 110; i++) {
            assertFalse(bs.get(i), "Shouldn't have flipped bit " + i);
        }
        assertTrue(bs.get(110), "Shouldn't have flipped bit 110");
        for (int i = 111; i < bs.size(); i++) {
            assertFalse(bs.get(i), "Shouldn't have flipped bit " + i);
        }

        // pos1 and pos2 are in two non-sequential bitset elements
        bs = new BitSet(256);
        bs.set(7);
        bs.set(10);
        bs.set(72);
        bs.set(110);
        bs.set(181);
        bs.set(220);
        bs.flip(9, 219);
        for (int i = 0; i < 7; i++) {
            assertFalse(bs.get(i), "Shouldn't have flipped bit " + i);
        }
        assertTrue(bs.get(7), "Shouldn't have flipped bit 7");
        assertFalse(bs.get(8), "Shouldn't have flipped bit 8");
        assertTrue(bs.get(9), "Failed to flip bit 9");
        assertFalse(bs.get(10), "Failed to flip bit 10");
        for (int i = 11; i < 72; i++) {
            assertTrue(bs.get(i), "failed to flip bit " + i);
        }
        assertFalse(bs.get(72), "Failed to flip bit 72");
        for (int i = 73; i < 110; i++) {
            assertTrue(bs.get(i), "failed to flip bit " + i);
        }
        assertFalse(bs.get(110), "Failed to flip bit 110");
        for (int i = 111; i < 181; i++) {
            assertTrue(bs.get(i), "failed to flip bit " + i);
        }
        assertFalse(bs.get(181), "Failed to flip bit 181");
        for (int i = 182; i < 219; i++) {
            assertTrue(bs.get(i), "failed to flip bit " + i);
        }
        assertFalse(bs.get(219), "Shouldn't have flipped bit 219");
        assertTrue(bs.get(220), "Shouldn't have flipped bit 220");
        for (int i = 221; i < bs.size(); i++) {
            assertTrue(!bs.get(i), "Shouldn't have flipped bit " + i);
        }
    }

    @Test
    public void setI() {
        // Test for method void java.util.BitSet.set(int)

        BitSet bs = new BitSet();
        bs.set(8);
        assertTrue(bs.get(8), "Failed to set bit");

        // Try setting a bit on a 64 boundary
        bs.set(128);
        assertTrue(bs.get(128), "Failed to set bit");

        bs = new BitSet(64);
        for (int i = bs.size(); --i >= 0;) {
            bs.set(i);
            assertTrue(bs.get(i), "Incorrectly set");
            assertEquals(i + 1, bs.length(), "Incorrect length");
            for (int j = bs.size(); --j > i;) {
                assertFalse(bs.get(j), "Incorrectly set bit " + j);
            }
            int j = i;
            while (--j >= 0) {
                assertFalse(bs.get(j), "Incorrectly set bit " + j);
            }
            bs.clear(i);
        }

        bs = new BitSet(0);
        assertEquals(0, bs.length(), "Test1: Wrong length");
        bs.set(0);
        assertEquals(1, bs.length(), "Test2: Wrong length");
    }

    @Test
    public void setIZ() {
        // Test for method void java.util.BitSet.set(int, boolean)
        eightbs.set(5, false);
        assertFalse(eightbs.get(5), "Should have set bit 5 to true");

        eightbs.set(5, true);
        assertTrue(eightbs.get(5), "Should have set bit 5 to false");
    }

    @Test
    public void setII() throws IndexOutOfBoundsException {
        BitSet bitset = new BitSet(30);
        bitset.set(29, 29);

        // Test for method void java.util.BitSet.set(int, int)
        // pos1 and pos2 are in the same bitset element
        BitSet bs = new BitSet(16);
        bs.set(5);
        bs.set(15);
        bs.set(7, 11);
        for (int i = 0; i < 7; i++) {
            if (i == 5) {
                assertTrue(bs.get(i), "Shouldn't have flipped bit " + i);
            } else {
                assertFalse(bs.get(i), "Shouldn't have set bit " + i);
            }
        }
        for (int i = 7; i < 11; i++) {
            assertTrue(bs.get(i), "Failed to set bit " + i);
        }
        for (int i = 11; i < bs.size(); i++) {
            if (i == 15) {
                assertTrue(bs.get(i), "Shouldn't have flipped bit " + i);
            } else {
                assertFalse(bs.get(i), "Shouldn't have set bit " + i);
            }
        }

        // pos1 and pos2 is in the same bitset element, boundry testing
        bs = new BitSet(16);
        bs.set(7, 64);
        for (int i = 0; i < 7; i++) {
            assertFalse(bs.get(i), "Shouldn't have set bit " + i);
        }
        for (int i = 7; i < 64; i++) {
            assertTrue(bs.get(i), "Failed to set bit " + i);
        }
        assertFalse(bs.get(64), "Shouldn't have set bit 64");

        // more boundary testing
        bs = new BitSet(32);
        bs.set(0, 64);
        for (int i = 0; i < 64; i++) {
            assertTrue(bs.get(i), "Failed to set bit " + i);
        }
        assertFalse(bs.get(64), "Shouldn't have set bit 64");

        bs = new BitSet(32);
        bs.set(0, 65);
        for (int i = 0; i < 65; i++) {
            assertTrue(bs.get(i), "Failed to set bit " + i);
        }
        assertFalse(bs.get(65), "Shouldn't have set bit 65");

        // pos1 and pos2 are in two sequential bitset elements
        bs = new BitSet(128);
        bs.set(7);
        bs.set(110);
        bs.set(9, 74);
        for (int i = 0; i < 9; i++) {
            if (i == 7) {
                assertTrue(bs.get(i), "Shouldn't have flipped bit " + i);
            } else {
                assertFalse(bs.get(i), "Shouldn't have set bit " + i);
            }
        }
        for (int i = 9; i < 74; i++) {
            assertTrue(bs.get(i), "Failed to set bit " + i);
        }
        for (int i = 74; i < bs.size(); i++) {
            if (i == 110) {
                assertTrue(bs.get(i), "Shouldn't have flipped bit " + i);
            } else {
                assertFalse(bs.get(i), "Shouldn't have set bit " + i);
            }
        }

        // pos1 and pos2 are in two non-sequential bitset elements
        bs = new BitSet(256);
        bs.set(7);
        bs.set(255);
        bs.set(9, 219);
        for (int i = 0; i < 9; i++) {
            if (i == 7) {
                assertTrue(bs.get(i), "Shouldn't have set flipped " + i);
            } else {
                assertFalse(bs.get(i), "Shouldn't have set bit " + i);
            }
        }

        for (int i = 9; i < 219; i++) {
            assertTrue(bs.get(i), "failed to set bit " + i);
        }

        for (int i = 219; i < 255; i++) {
            assertFalse(bs.get(i), "Shouldn't have set bit " + i);
        }

        assertTrue(bs.get(255), "Shouldn't have flipped bit 255");

        // test illegal args
        bs = new BitSet(10);

        bs.set(2, 2);
        assertFalse(bs.get(2), "Bit got set incorrectly ");

        bs = new BitSet();
        bs.set(0, 0);
    }

    @Test
    public void setIIZ() {
        // Test for method void java.util.BitSet.set(int, int, boolean)
        eightbs.set(3, 6, false);
        assertTrue(!eightbs.get(3) && !eightbs.get(4) && !eightbs.get(5), "Should have set bits 3, 4, and 5 to false");

        eightbs.set(3, 6, true);
        assertTrue(eightbs.get(3) && eightbs.get(4) && eightbs.get(5), "Should have set bits 3, 4, and 5 to true");
    }

    @Test
    public void intersects() {
        // Test for method boolean java.util.BitSet.intersects(java.util.BitSet)
        BitSet bs = new BitSet(500);
        bs.set(5);
        bs.set(63);
        bs.set(64);
        bs.set(71, 110);
        bs.set(127, 130);
        bs.set(192);
        bs.set(450);

        BitSet bs2 = new BitSet(8);
        assertFalse(bs.intersects(bs2), "Test1: intersects() returned incorrect value");
        assertFalse(bs2.intersects(bs), "Test1: intersects() returned incorrect value");

        bs2.set(4);
        assertFalse(bs.intersects(bs2), "Test2: intersects() returned incorrect value");
        assertFalse(bs2.intersects(bs), "Test2: intersects() returned incorrect value");

        bs2.clear();
        bs2.set(5);
        assertTrue(bs.intersects(bs2), "Test3: intersects() returned incorrect value");
        assertTrue(bs2.intersects(bs), "Test3: intersects() returned incorrect value");

        bs2.clear();
        bs2.set(63);
        assertTrue(bs.intersects(bs2), "Test4: intersects() returned incorrect value");
        assertTrue(bs2.intersects(bs), "Test4: intersects() returned incorrect value");

        bs2.clear();
        bs2.set(80);
        assertTrue(bs.intersects(bs2), "Test5: intersects() returned incorrect value");
        assertTrue(bs2.intersects(bs), "Test5: intersects() returned incorrect value");

        bs2.clear();
        bs2.set(127);
        assertTrue(bs.intersects(bs2), "Test6: intersects() returned incorrect value");
        assertTrue(bs2.intersects(bs), "Test6: intersects() returned incorrect value");

        bs2.clear();
        bs2.set(192);
        assertTrue(bs.intersects(bs2), "Test7: intersects() returned incorrect value");
        assertTrue(bs2.intersects(bs), "Test7: intersects() returned incorrect value");

        bs2.clear();
        bs2.set(450);
        assertTrue(bs.intersects(bs2), "Test8: intersects() returned incorrect value");
        assertTrue(bs2.intersects(bs), "Test8: intersects() returned incorrect value");

        bs2.clear();
        bs2.set(500);
        assertFalse(bs.intersects(bs2), "Test9: intersects() returned incorrect value");
        assertFalse(bs2.intersects(bs), "Test9: intersects() returned incorrect value");
    }

    @Test
    public void and() {
        // Test for method void java.util.BitSet.and(java.util.BitSet)
        BitSet bs = new BitSet(128);
        // Initialize the bottom half of the BitSet

        for (int i = 64; i < 128; i++) {
            bs.set(i);
        }
        eightbs.and(bs);
        assertFalse(eightbs.equals(bs), "AND failed to clear bits");
        eightbs.set(3);
        bs.set(3);
        eightbs.and(bs);
        assertTrue(bs.get(3), "AND failed to maintain set bits");
        bs.and(eightbs);
        for (int i = 64; i < 128; i++) {
            assertFalse(bs.get(i), "Failed to clear extra bits in the receiver BitSet");
        }
    }

    @Test
    public void andNot() {
        BitSet bs = (BitSet) eightbs.clone();
        bs.clear(5);
        BitSet bs2 = new BitSet();
        bs2.set(2);
        bs2.set(3);
        bs.andNot(bs2);
        assertEquals("{0, 1, 4, 6, 7}", bs.toString(), "Incorrect bitset after andNot");

        bs = new BitSet(0);
        bs.andNot(bs2);
        assertEquals(0, bs.size(), "Incorrect size");
    }

    @Test
    public void or() {
        // Test for method void java.util.BitSet.or(java.util.BitSet)
        BitSet bs = new BitSet(128);
        bs.or(eightbs);
        for (int i = 0; i < 8; i++) {
            assertTrue(bs.get(i), "OR failed to set bits");
        }

        bs = new BitSet(0);
        bs.or(eightbs);
        for (int i = 0; i < 8; i++) {
            assertTrue(bs.get(i), "OR(0) failed to set bits");
        }

        eightbs.clear(5);
        bs = new BitSet(128);
        bs.or(eightbs);
        assertFalse(bs.get(5), "OR set a bit which should be off");
    }

    @Test
    public void xor() {
        // Test for method void java.util.BitSet.xor(java.util.BitSet)

        BitSet bs = (BitSet) eightbs.clone();
        bs.xor(eightbs);
        for (int i = 0; i < 8; i++) {
            assertFalse(bs.get(i), "XOR failed to clear bits");
        }

        bs.xor(eightbs);
        for (int i = 0; i < 8; i++) {
            assertTrue(bs.get(i), "XOR failed to set bits");
        }

        bs = new BitSet(0);
        bs.xor(eightbs);
        for (int i = 0; i < 8; i++) {
            assertTrue(bs.get(i), "XOR(0) failed to set bits");
        }

        bs = new BitSet();
        bs.set(63);
        assertEquals("{63}", bs.toString(), "Test highest bit");
    }

    @Test
    public void toStringComputed() {
        // Test for method java.lang.String java.util.BitSet.toString()
        assertEquals("{0, 1, 2, 3, 4, 5, 6, 7}", eightbs.toString(), "Returned incorrect string representation");
        eightbs.clear(2);
        assertEquals("{0, 1, 3, 4, 5, 6, 7}", eightbs.toString(), "Returned incorrect string representation");
    }

    @Test
    public void length() {
        BitSet bs = new BitSet();
        assertEquals(0, bs.length(), "BitSet returned wrong length");
        bs.set(5);
        assertEquals(6, bs.length(), "BitSet returned wrong length");
        bs.set(10);
        assertEquals(11, bs.length(), "BitSet returned wrong length");
        bs.set(432);
        assertEquals(433, bs.length(), "BitSet returned wrong length");
        bs.set(300);
        assertEquals(433, bs.length(), "BitSet returned wrong length");
    }

    @Test
    public void nextSetBitI() {
        // Test for method int java.util.BitSet.nextSetBit()
        BitSet bs = new BitSet(500);
        bs.set(5);
        bs.set(32);
        bs.set(63);
        bs.set(64);
        bs.set(71, 110);
        bs.set(127, 130);
        bs.set(193);
        bs.set(450);
        /*try {
            bs.nextSetBit(-1);
            fail("Expected IndexOutOfBoundsException for negative index");
        } catch (IndexOutOfBoundsException e) {
            // correct behavior
        }*/
        assertEquals(5, bs.nextSetBit(0), "nextSetBit() returned the wrong value");
        assertEquals(5, bs.nextSetBit(5), "nextSetBit() returned the wrong value");
        assertEquals(32, bs.nextSetBit(6), "nextSetBit() returned the wrong value");
        assertEquals(32, bs.nextSetBit(32), "nextSetBit() returned the wrong value");
        assertEquals(63, bs.nextSetBit(33), "nextSetBit() returned the wrong value");

        // boundary tests
        assertEquals(63, bs.nextSetBit(63), "nextSetBit() returned the wrong value");
        assertEquals(64, bs.nextSetBit(64), "nextSetBit() returned the wrong value");

        // at bitset element 1
        assertEquals(71, bs.nextSetBit(65), "nextSetBit() returned the wrong value");
        assertEquals(71, bs.nextSetBit(71), "nextSetBit() returned the wrong value");
        assertEquals(72, bs.nextSetBit(72), "nextSetBit() returned the wrong value");
        assertEquals(127, bs.nextSetBit(110), "nextSetBit() returned the wrong value");

        // boundary tests
        assertEquals(127, bs.nextSetBit(127), "nextSetBit() returned the wrong value");
        assertEquals(128, bs.nextSetBit(128), "nextSetBit() returned the wrong value");

        // at bitset element 2
        assertEquals(193, bs.nextSetBit(130), "nextSetBit() returned the wrong value");

        assertEquals(193, bs.nextSetBit(191), "nextSetBit() returned the wrong value");
        assertEquals(193, bs.nextSetBit(192), "nextSetBit() returned the wrong value");
        assertEquals(193, bs.nextSetBit(193), "nextSetBit() returned the wrong value");
        assertEquals(450, bs.nextSetBit(194), "nextSetBit() returned the wrong value");
        assertEquals(450, bs.nextSetBit(255), "nextSetBit() returned the wrong value");
        assertEquals(450, bs.nextSetBit(256), "nextSetBit() returned the wrong value");
        assertEquals(450, bs.nextSetBit(450), "nextSetBit() returned the wrong value");

        assertEquals(-1, bs.nextSetBit(451), "nextSetBit() returned the wrong value");
        assertEquals(-1, bs.nextSetBit(511), "nextSetBit() returned the wrong value");
        assertEquals(-1, bs.nextSetBit(512), "nextSetBit() returned the wrong value");
        assertEquals(-1, bs.nextSetBit(800), "nextSetBit() returned the wrong value");
    }

    @Test
    public void nextClearBitI() {
        // Test for method int java.util.BitSet.nextSetBit()
        BitSet bs = new BitSet(500);
        bs.set(0, bs.size() - 1); // ensure all the bits from 0 to bs.size()
        // -1
        bs.set(bs.size() - 1); // are set to true
        bs.clear(5);
        bs.clear(32);
        bs.clear(63);
        bs.clear(64);
        bs.clear(71, 110);
        bs.clear(127, 130);
        bs.clear(193);
        bs.clear(450);
        /*try {
            bs.nextClearBit(-1);
            fail("Expected IndexOutOfBoundsException for negative index");
        } catch (IndexOutOfBoundsException e) {
            // correct behavior
        }*/
        assertEquals(5, bs.nextClearBit(0), "nextClearBit() returned the wrong value");
        assertEquals(5, bs.nextClearBit(5), "nextClearBit() returned the wrong value");
        assertEquals(32, bs.nextClearBit(6), "nextClearBit() returned the wrong value");
        assertEquals(32, bs.nextClearBit(32), "nextClearBit() returned the wrong value");
        assertEquals(63, bs.nextClearBit(33), "nextClearBit() returned the wrong value");

        // boundary tests
        assertEquals(63, bs.nextClearBit(63), "nextClearBit() returned the wrong value");
        assertEquals(64, bs.nextClearBit(64), "nextClearBit() returned the wrong value");

        // at bitset element 1
        assertEquals(71, bs.nextClearBit(65), "nextClearBit() returned the wrong value");
        assertEquals(71, bs.nextClearBit(71), "nextClearBit() returned the wrong value");
        assertEquals(72, bs.nextClearBit(72), "nextClearBit() returned the wrong value");
        assertEquals(127, bs.nextClearBit(110), "nextClearBit() returned the wrong value");

        // boundary tests
        assertEquals(127, bs.nextClearBit(127), "nextClearBit() returned the wrong value");
        assertEquals(128, bs.nextClearBit(128), "nextClearBit() returned the wrong value");

        // at bitset element 2
        assertEquals(193, bs.nextClearBit(130), "nextClearBit() returned the wrong value");
        assertEquals(193, bs.nextClearBit(191), "nextClearBit() returned the wrong value");

        assertEquals(193, bs.nextClearBit(192), "nextClearBit() returned the wrong value");
        assertEquals(193, bs.nextClearBit(193), "nextClearBit() returned the wrong value");
        assertEquals(450, bs.nextClearBit(194), "nextClearBit() returned the wrong value");
        assertEquals(450, bs.nextClearBit(255), "nextClearBit() returned the wrong value");
        assertEquals(450, bs.nextClearBit(256), "nextClearBit() returned the wrong value");
        assertEquals(450, bs.nextClearBit(450), "nextClearBit() returned the wrong value");

        // bitset has 1 still the end of bs.size() -1, but calling nextClearBit
        // with any index value
        // after the last true bit should return bs.size(),
        assertEquals(512, bs.nextClearBit(451), "nextClearBit() returned the wrong value");
        assertEquals(512, bs.nextClearBit(511), "nextClearBit() returned the wrong value");
        assertEquals(512, bs.nextClearBit(512), "nextClearBit() returned the wrong value");

        // if the index is larger than bs.size(), nextClearBit should return
        // index;
        assertEquals(513, bs.nextClearBit(513), "nextClearBit() returned the wrong value");
        assertEquals(800, bs.nextClearBit(800), "nextClearBit() returned the wrong value");
    }

    @Test
    public void isEmpty() {
        BitSet bs = new BitSet(500);
        assertTrue(bs.isEmpty(), "Test: isEmpty() returned wrong value");

        // at bitset element 0
        bs.set(3);
        assertFalse(bs.isEmpty(), "Test0: isEmpty() returned wrong value");

        // at bitset element 1
        bs.clear();
        bs.set(12);
        assertFalse(bs.isEmpty(), "Test1: isEmpty() returned wrong value");

        // at bitset element 2
        bs.clear();
        bs.set(128);
        assertFalse(bs.isEmpty(), "Test2: isEmpty() returned wrong value");

        // boundary testing
        bs.clear();
        bs.set(459);
        assertFalse(bs.isEmpty(), "Test3: isEmpty() returned wrong value");

        bs.clear();
        bs.set(511);
        assertFalse(bs.isEmpty(), "Test4: isEmpty() returned wrong value");
    }

    @Test
    public void cardinality() {
        // test for method int java.util.BitSet.cardinality()
        BitSet bs = new BitSet(500);
        bs.set(5);
        bs.set(32);
        bs.set(63);
        bs.set(64);
        bs.set(71, 110);
        bs.set(127, 130);
        bs.set(193);
        bs.set(450);
        assertEquals(48, bs.cardinality(), "cardinality() returned wrong value");

        bs.flip(0, 500);
        assertEquals(452, bs.cardinality(), "cardinality() returned wrong value");

        bs.clear();
        assertEquals(0, bs.cardinality(), "cardinality() returned wrong value");

        bs.set(0, 500);
        assertEquals(500, bs.cardinality(), "cardinality() returned wrong value");

        bs = new BitSet();
        bs.set(31);
        assertEquals(1, bs.cardinality());
    }

    @Test
    public void previousSetBitFound() {
        BitSet bs = new BitSet();
        bs.set(2, 10);
        bs.set(16, 19);
        bs.set(31, 64);
        bs.set(96, 98);
        assertEquals(97, bs.previousSetBit(100));
        assertEquals(97, bs.previousSetBit(97));
        assertEquals(96, bs.previousSetBit(96));
        assertEquals(63, bs.previousSetBit(95));
        assertEquals(63, bs.previousSetBit(63));
        assertEquals(62, bs.previousSetBit(62));
        assertEquals(32, bs.previousSetBit(32));
        assertEquals(31, bs.previousSetBit(31));
        assertEquals(18, bs.previousSetBit(30));
        assertEquals(18, bs.previousSetBit(18));
        assertEquals(17, bs.previousSetBit(17));
        assertEquals(16, bs.previousSetBit(16));
        assertEquals(9, bs.previousSetBit(15));
        assertEquals(9, bs.previousSetBit(9));
        assertEquals(2, bs.previousSetBit(2));
        assertEquals(-1, bs.previousSetBit(1));
        assertEquals(-1, bs.previousSetBit(0));
        bs = new BitSet();
        bs.set(0);
        bs.set(1);
        bs.set(32);
        bs.set(192);
        bs.set(666);
        assertEquals(666, bs.previousSetBit(999));
        assertEquals(666, bs.previousSetBit(667));
        assertEquals(666, bs.previousSetBit(666));
        assertEquals(192, bs.previousSetBit(665));
        assertEquals(32, bs.previousSetBit(191));
        assertEquals(1, bs.previousSetBit(31));
        assertEquals(0, bs.previousSetBit(0));
        assertEquals(-1, bs.previousSetBit(-1));
    }

    @Test
    public void previousClearBitFound() {
        BitSet bs = new BitSet();
        bs.set(0, 10);
        bs.set(16, 19);
        bs.set(31, 64);
        bs.set(96, 98);
        assertEquals(100, bs.previousClearBit(100));
        assertEquals(98, bs.previousClearBit(98));
        assertEquals(95, bs.previousClearBit(97));
        assertEquals(95, bs.previousClearBit(95));
        assertEquals(64, bs.previousClearBit(64));
        assertEquals(30, bs.previousClearBit(63));
        assertEquals(30, bs.previousClearBit(32));
        assertEquals(30, bs.previousClearBit(31));
        assertEquals(30, bs.previousClearBit(30));
        assertEquals(29, bs.previousClearBit(29));
        assertEquals(20, bs.previousClearBit(20));
        assertEquals(19, bs.previousClearBit(19));
        assertEquals(15, bs.previousClearBit(17));
        assertEquals(15, bs.previousClearBit(15));
        assertEquals(-1, bs.previousClearBit(9));
        assertEquals(-1, bs.previousClearBit(1));
        assertEquals(-1, bs.previousClearBit(0));
    }
}
