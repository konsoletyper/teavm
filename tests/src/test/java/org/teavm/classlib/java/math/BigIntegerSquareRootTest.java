/*
 *  Copyright 2023 Bernd Busse.
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

package org.teavm.classlib.java.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.math.BigInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@SkipPlatform(TestPlatform.WASI)
public class BigIntegerSquareRootTest {
    /**
     * sqrt: negative value
     */
    @Test
    public void testSqrtException() {
        BigInteger aNumber = new BigInteger("-8");
        try {
            aNumber.sqrt();
            fail("ArithmeticException has not been caught");
        } catch (ArithmeticException e) {
            assertEquals("Improper exception message", "Negative BigInteger", e.getMessage());
        }
    }

    /**
     * sqrt: special cases
     */
    @Test
    public void testSpecialCases() {
        BigInteger aNumber = new BigInteger("3");

        assertEquals(BigInteger.ZERO, BigInteger.ZERO.sqrt());
        assertEquals(BigInteger.ONE, BigInteger.ONE.sqrt());
        assertEquals(BigInteger.ONE, aNumber.sqrt());
    }

    /**
     * sqrt of small number
     */
    @Test
    public void testSmallNumbers() {
        byte[] aBytes = {39, -128, 127};
        int aSign = 1;
        byte[] rBytes = {6, 72};
        BigInteger aNumber = new BigInteger(aSign, aBytes);
        BigInteger result = aNumber.sqrt();
        byte[] resBytes = new byte[rBytes.length];
        resBytes = result.toByteArray();
        for (int i = 0; i < resBytes.length; i++) {
            assertTrue(resBytes[i] == rBytes[i]);
        }
        assertEquals("incorrect sign", 1, result.signum());
    }

    /**
     * sqrt of large number
     */
    @Test
    public void testBigNumbers() {
        byte[] aBytes = {1, 100, 56, 7, 98, -1, 39, -128, 127, 5, 6, 7, 8, 9};
        int aSign = 1;
        byte[] rBytes = {18, -33, -82, -48, -58, 93, 37};
        BigInteger aNumber = new BigInteger(aSign, aBytes);
        BigInteger result = aNumber.sqrt();
        byte[] resBytes = new byte[rBytes.length];
        resBytes = result.toByteArray();
        for (int i = 0; i < resBytes.length; i++) {
            assertTrue(resBytes[i] == rBytes[i]);
        }
        assertEquals("incorrect sign", 1, result.signum());
    }
}
