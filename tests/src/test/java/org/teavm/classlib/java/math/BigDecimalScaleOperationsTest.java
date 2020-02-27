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
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/**
 * @author Elena Semukhina
 */

package org.teavm.classlib.java.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class BigDecimalScaleOperationsTest {
    /**
     * Check the default scale
     */
    @Test
    public void testScaleDefault() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int cScale = 0;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a));
        assertTrue("incorrect scale", aNumber.scale() == cScale);
    }

    /**
     * Check a negative scale
     */
    @Test
    public void testScaleNeg() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = -10;
        int cScale = -10;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        assertTrue("incorrect scale", aNumber.scale() == cScale);
    }

    /**
     * Check a positive scale
     */
    @Test
    public void testScalePos() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = 10;
        int cScale = 10;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        assertTrue("incorrect scale", aNumber.scale() == cScale);
    }

    /**
     * Check the zero scale
     */
    @Test
    public void testScaleZero() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = 0;
        int cScale = 0;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        assertTrue("incorrect scale", aNumber.scale() == cScale);
    }

    /**
     * Check the unscaled value
     */
    @Test
    public void testUnscaledValue() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = 100;
        BigInteger bNumber = new BigInteger(a);
        BigDecimal aNumber = new BigDecimal(bNumber, aScale);
        assertTrue("incorrect unscaled value", aNumber.unscaledValue().equals(bNumber));
    }

    /**
     * Set a greater new scale
     */
    @Test
    public void testSetScaleGreater() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = 18;
        int newScale = 28;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = aNumber.setScale(newScale);
        assertTrue("incorrect scale", bNumber.scale() == newScale);
        assertEquals("incorrect value", 0, bNumber.compareTo(aNumber));
    }

    /**
     * Set a less new scale; this.scale == 8; newScale == 5.
     */
    @Test
    public void testSetScaleLess() {
        String a = "2.345726458768760000E+10";
        int newScale = 5;
        BigDecimal aNumber = new BigDecimal(a);
        BigDecimal bNumber = aNumber.setScale(newScale);
        assertTrue("incorrect scale", bNumber.scale() == newScale);
        assertEquals("incorrect value", 0, bNumber.compareTo(aNumber));
    }

    /**
     * Verify an exception when setting a new scale
     */
    @Test
    public void testSetScaleException() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = 28;
        int newScale = 18;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        try {
            aNumber.setScale(newScale);
            fail("ArithmeticException has not been caught");
        } catch (ArithmeticException e) {
            assertEquals("Improper exception message", "Rounding necessary", e.getMessage());
        }
    }

    /**
     * Set the same new scale
     */
    @Test
    public void testSetScaleSame() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = 18;
        int newScale = 18;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = aNumber.setScale(newScale);
        assertTrue("incorrect scale", bNumber.scale() == newScale);
        assertTrue("incorrect value", bNumber.equals(aNumber));
    }

    /**
     * Set a new scale
     */
    @Test
    public void testSetScaleRoundUp() {
        String a = "1231212478987482988429808779810457634781384756794987";
        String b = "123121247898748298842980877981045763478139";
        int aScale = 28;
        int newScale = 18;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = aNumber.setScale(newScale, BigDecimal.ROUND_UP);
        assertTrue("incorrect scale", bNumber.scale() == newScale);
        assertTrue("incorrect value", bNumber.unscaledValue().toString().equals(b));
    }

    /**
     * Set a new scale
     */
    @Test
    public void testSetScaleRoundDown() {
        String a = "1231212478987482988429808779810457634781384756794987";
        String b = "123121247898748298842980877981045763478138";
        int aScale = 28;
        int newScale = 18;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = aNumber.setScale(newScale, BigDecimal.ROUND_DOWN);
        assertTrue("incorrect scale", bNumber.scale() == newScale);
        assertTrue("incorrect value", bNumber.unscaledValue().toString().equals(b));
    }

    /**
     * Set a new scale
     */
    @Test
    public void testSetScaleRoundCeiling() {
        String a = "1231212478987482988429808779810457634781384756794987";
        String b = "123121247898748298842980877981045763478139";
        int aScale = 28;
        int newScale = 18;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = aNumber.setScale(newScale, BigDecimal.ROUND_CEILING);
        assertTrue("incorrect scale", bNumber.scale() == newScale);
        assertTrue("incorrect value", bNumber.unscaledValue().toString().equals(b));
    }

    /**
     * Set a new scale
     */
    @Test
    public void testSetScaleRoundFloor() {
        String a = "1231212478987482988429808779810457634781384756794987";
        String b = "123121247898748298842980877981045763478138";
        int aScale = 28;
        int newScale = 18;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = aNumber.setScale(newScale, BigDecimal.ROUND_FLOOR);
        assertTrue("incorrect scale", bNumber.scale() == newScale);
        assertTrue("incorrect value", bNumber.unscaledValue().toString().equals(b));
    }

    /**
     * Set a new scale
     */
    @Test
    public void testSetScaleRoundHalfUp() {
        String a = "1231212478987482988429808779810457634781384756794987";
        String b = "123121247898748298842980877981045763478138";
        int aScale = 28;
        int newScale = 18;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = aNumber.setScale(newScale, BigDecimal.ROUND_HALF_UP);
        assertTrue("incorrect scale", bNumber.scale() == newScale);
        assertTrue("incorrect value", bNumber.unscaledValue().toString().equals(b));
    }

    /**
     * Set a new scale
     */
    @Test
    public void testSetScaleRoundHalfDown() {
        String a = "1231212478987482988429808779810457634781384756794987";
        String b = "123121247898748298842980877981045763478138";
        int aScale = 28;
        int newScale = 18;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = aNumber.setScale(newScale, BigDecimal.ROUND_HALF_DOWN);
        assertTrue("incorrect scale", bNumber.scale() == newScale);
        assertTrue("incorrect value", bNumber.unscaledValue().toString().equals(b));
    }

    /**
     * Set a new scale
     */
    @Test
    public void testSetScaleRoundHalfEven() {
        String a = "1231212478987482988429808779810457634781384756794987";
        String b = "123121247898748298842980877981045763478138";
        int aScale = 28;
        int newScale = 18;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = aNumber.setScale(newScale, BigDecimal.ROUND_HALF_EVEN);
        assertTrue("incorrect scale", bNumber.scale() == newScale);
        assertTrue("incorrect value", bNumber.unscaledValue().toString().equals(b));
    }

    /**
     * SetScale(int, RoundingMode)
     */
    @Test
    public void testSetScaleIntRoundingMode() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = 28;
        int newScale = 18;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal result = aNumber.setScale(newScale, RoundingMode.HALF_EVEN);
        String res = "123121247898748298842980.877981045763478138";
        int resScale = 18;
        assertEquals("incorrect value", res, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * Move the decimal point to the left; the shift value is positive
     */
    @Test
    public void testMovePointLeftPos() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = 28;
        int shift = 18;
        int resScale = 46;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = aNumber.movePointLeft(shift);
        assertTrue("incorrect scale", bNumber.scale() == resScale);
        assertTrue("incorrect value", bNumber.unscaledValue().toString().equals(a));
    }

    /**
     * Move the decimal point to the left; the shift value is positive
     */
    @Test
    public void testMovePointLeftNeg() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = 28;
        int shift = -18;
        int resScale = 10;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = aNumber.movePointLeft(shift);
        assertTrue("incorrect scale", bNumber.scale() == resScale);
        assertTrue("incorrect value", bNumber.unscaledValue().toString().equals(a));
    }

    /**
     * Move the decimal point to the right; the shift value is positive
     */
    @Test
    public void testMovePointRightPosGreater() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = 28;
        int shift = 18;
        int resScale = 10;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = aNumber.movePointRight(shift);
        assertTrue("incorrect scale", bNumber.scale() == resScale);
        assertTrue("incorrect value", bNumber.unscaledValue().toString().equals(a));
    }

    /**
     * Move the decimal point to the right; the shift value is positive
     */
    @Test
    public void testMovePointRightPosLess() {
        String a = "1231212478987482988429808779810457634781384756794987";
        String b = "123121247898748298842980877981045763478138475679498700";
        int aScale = 28;
        int shift = 30;
        int resScale = 0;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = aNumber.movePointRight(shift);
        assertTrue("incorrect scale", bNumber.scale() == resScale);
        assertTrue("incorrect value", bNumber.unscaledValue().toString().equals(b));
    }

    /**
     * Move the decimal point to the right; the shift value is positive
     */
    @Test
    public void testMovePointRightNeg() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = 28;
        int shift = -18;
        int resScale = 46;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = aNumber.movePointRight(shift);
        assertTrue("incorrect scale", bNumber.scale() == resScale);
        assertTrue("incorrect value", bNumber.unscaledValue().toString().equals(a));
    }

    /**
     * Move the decimal point to the right when the scale overflows
     */
    @Test
    public void testMovePointRightException() {
        String a = "12312124789874829887348723648726347429808779810457634781384756794987";
        int aScale = Integer.MAX_VALUE; //2147483647
        int shift = -18;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        try {
            aNumber.movePointRight(shift);
            fail("ArithmeticException has not been caught");
        } catch (ArithmeticException e) {
            assertEquals("Improper exception message", "Underflow", e.getMessage());
        }
    }

    /**
     * precision()
     */
    @Test
    public void testPrecision() {
        String a = "12312124789874829887348723648726347429808779810457634781384756794987";
        int aScale = 14;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        int prec = aNumber.precision();
        assertEquals(68, prec);
    }
}
