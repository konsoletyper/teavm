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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class BigDecimalCompareTest {
    /**
     * Abs() of a negative BigDecimal
     */
    @Test
    public void testAbsNeg() {
        String a = "-123809648392384754573567356745735.63567890295784902768787678287E+21";
        BigDecimal aNumber = new BigDecimal(a);
        String result = "123809648392384754573567356745735635678902957849027687.87678287";
        assertEquals(result, aNumber.abs().toString(), "incorrect value");
    }

    /**
     * Abs() of a positive BigDecimal
     */
    @Test
    public void testAbsPos() {
        String a = "123809648392384754573567356745735.63567890295784902768787678287E+21";
        BigDecimal aNumber = new BigDecimal(a);
        String result = "123809648392384754573567356745735635678902957849027687.87678287";
        assertEquals(result, aNumber.abs().toString(), "incorrect value");
    }

    /**
     * Abs(MathContext) of a negative BigDecimal
     */
    @Test
    public void testAbsMathContextNeg() {
        String a = "-123809648392384754573567356745735.63567890295784902768787678287E+21";
        BigDecimal aNumber = new BigDecimal(a);
        int precision = 15;
        RoundingMode rm = RoundingMode.HALF_DOWN;
        MathContext mc = new MathContext(precision, rm);
        String result = "1.23809648392385E+53";
        int resScale = -39;
        BigDecimal res = aNumber.abs(mc);
        assertEquals(result, res.toString(), "incorrect value");
        assertEquals(resScale, res.scale(), "incorrect scale");
    }

    /**
     * Abs(MathContext) of a positive BigDecimal
     */
    @Test
    public void testAbsMathContextPos() {
        String a = "123809648392384754573567356745735.63567890295784902768787678287E+21";
        BigDecimal aNumber = new BigDecimal(a);
        int precision = 41;
        RoundingMode rm = RoundingMode.HALF_EVEN;
        MathContext mc = new MathContext(precision, rm);
        String result = "1.2380964839238475457356735674573563567890E+53";
        int resScale = -13;
        BigDecimal res = aNumber.abs(mc);
        assertEquals(result, res.toString(), "incorrect value");
        assertEquals(resScale, res.scale(), "incorrect scale");
    }

    /**
     * Compare to a number of an equal scale
     */
    @Test
    public void testCompareEqualScale1() {
        String a = "12380964839238475457356735674573563567890295784902768787678287";
        int aScale = 18;
        String b = "4573563567890295784902768787678287";
        int bScale = 18;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        int result = 1;
        assertEquals(result, aNumber.compareTo(bNumber), "incorrect result");
    }

    /**
     * Compare to a number of an equal scale
     */
    @Test
    public void testCompareEqualScale2() {
        String a = "12380964839238475457356735674573563567890295784902768787678287";
        int aScale = 18;
        String b = "4573563923487289357829759278282992758247567890295784902768787678287";
        int bScale = 18;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        int result = -1;
        assertEquals(result, aNumber.compareTo(bNumber), "incorrect result");
    }

    /**
     * Compare to a number of an greater scale
     */
    @Test
    public void testCompareGreaterScale1() {
        String a = "12380964839238475457356735674573563567890295784902768787678287";
        int aScale = 28;
        String b = "4573563567890295784902768787678287";
        int bScale = 18;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        int result = 1;
        assertEquals(result, aNumber.compareTo(bNumber), "incorrect result");
    }

    /**
     * Compare to a number of an greater scale
     */
    @Test
    public void testCompareGreaterScale2() {
        String a = "12380964839238475457356735674573563567890295784902768787678287";
        int aScale = 48;
        String b = "4573563567890295784902768787678287";
        int bScale = 2;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        int result = -1;
        assertEquals(result, aNumber.compareTo(bNumber), "incorrect result");
    }

    /**
     * Compare to a number of an less scale
     */
    @Test
    public void testCompareLessScale1() {
        String a = "12380964839238475457356735674573563567890295784902768787678287";
        int aScale = 18;
        String b = "4573563567890295784902768787678287";
        int bScale = 28;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        int result = 1;
        assertEquals(result, aNumber.compareTo(bNumber), "incorrect result");
    }

    /**
     * Compare to a number of an less scale
     */
    @Test
    public void testCompareLessScale2() {
        String a = "12380964839238475457356735674573";
        int aScale = 36;
        String b = "45735635948573894578349572001798379183767890295784902768787678287";
        int bScale = 48;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        int result = -1;
        assertEquals(result, aNumber.compareTo(bNumber), "incorrect result");
    }

    /**
     * Equals() for unequal BigDecimals
     */
    @Test
    public void testEqualsUnequal1() {
        String a = "92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = -24;
        String b = "7472334223847623782375469293018787918347987234564568";
        int bScale = 13;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        assertFalse(aNumber.equals(bNumber));
    }

    /**
     * Equals() for unequal BigDecimals
     */
    @Test
    public void testEqualsUnequal2() {
        String a = "92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = -24;
        String b = "92948782094488478231212478987482988429808779810457634781384756794987";
        int bScale = 13;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        assertFalse(aNumber.equals(bNumber));
    }

    /**
     * Equals() for unequal BigDecimals
     */
    @Test
    public void testEqualsUnequal3() {
        String a = "92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = -24;
        String b = "92948782094488478231212478987482988429808779810457634781384756794987";
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        assertFalse(aNumber.equals(b));
    }

    /**
     * equals() for equal BigDecimals
     */
    @Test
    public void testEqualsEqual() {
        String a = "92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = -24;
        String b = "92948782094488478231212478987482988429808779810457634781384756794987";
        int bScale = -24;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        assertEquals(aNumber, bNumber);
    }

    /**
     * equals() for equal BigDecimals
     */
    @Test
    public void testEqualsNull() {
        String a = "92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = -24;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        assertFalse(aNumber.equals(null));
    }

    /**
     * hashCode() for equal BigDecimals
     */
    @Test
    public void testHashCodeEqual() {
        String a = "92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = -24;
        String b = "92948782094488478231212478987482988429808779810457634781384756794987";
        int bScale = -24;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        assertEquals(aNumber.hashCode(), bNumber.hashCode(), "incorrect value");
    }

    /**
     * hashCode() for unequal BigDecimals
     */
    @Test
    public void testHashCodeUnequal() {
        String a = "8478231212478987482988429808779810457634781384756794987";
        int aScale = 41;
        String b = "92948782094488478231212478987482988429808779810457634781384756794987";
        int bScale = -24;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        assertTrue(aNumber.hashCode() != bNumber.hashCode(), "incorrect value");
    }

    /**
     * max() for equal BigDecimals
     */
    @Test
    public void testMaxEqual() {
        String a = "8478231212478987482988429808779810457634781384756794987";
        int aScale = 41;
        String b = "8478231212478987482988429808779810457634781384756794987";
        int bScale = 41;
        String c = "8478231212478987482988429808779810457634781384756794987";
        int cScale = 41;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal cNumber = new BigDecimal(new BigInteger(c), cScale);
        assertEquals(cNumber, aNumber.max(bNumber), "incorrect value");
    }

    /**
     * max() for unequal BigDecimals
     */
    @Test
    public void testMaxUnequal1() {
        String a = "92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = 24;
        String b = "92948782094488478231212478987482988429808779810457634781384756794987";
        int bScale = 41;
        String c = "92948782094488478231212478987482988429808779810457634781384756794987";
        int cScale = 24;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal cNumber = new BigDecimal(new BigInteger(c), cScale);
        assertEquals(cNumber, aNumber.max(bNumber), "incorrect value");
    }

    /**
     * max() for unequal BigDecimals
     */
    @Test
    public void testMaxUnequal2() {
        String a = "92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = 41;
        String b = "94488478231212478987482988429808779810457634781384756794987";
        int bScale = 41;
        String c = "92948782094488478231212478987482988429808779810457634781384756794987";
        int cScale = 41;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal cNumber = new BigDecimal(new BigInteger(c), cScale);
        assertEquals(cNumber, aNumber.max(bNumber), "incorrect value");
    }

    /**
     * min() for equal BigDecimals
     */
    @Test
    public void testMinEqual() {
        String a = "8478231212478987482988429808779810457634781384756794987";
        int aScale = 41;
        String b = "8478231212478987482988429808779810457634781384756794987";
        int bScale = 41;
        String c = "8478231212478987482988429808779810457634781384756794987";
        int cScale = 41;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal cNumber = new BigDecimal(new BigInteger(c), cScale);
        assertEquals(cNumber, aNumber.min(bNumber), "incorrect value");
    }

    /**
     * min() for unequal BigDecimals
     */
    @Test
    public void testMinUnequal1() {
        String a = "92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = 24;
        String b = "92948782094488478231212478987482988429808779810457634781384756794987";
        int bScale = 41;
        String c = "92948782094488478231212478987482988429808779810457634781384756794987";
        int cScale = 41;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal cNumber = new BigDecimal(new BigInteger(c), cScale);
        assertEquals(cNumber, aNumber.min(bNumber), "incorrect value");
    }

    /**
     * min() for unequal BigDecimals
     */
    @Test
    public void testMinUnequal2() {
        String a = "92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = 41;
        String b = "94488478231212478987482988429808779810457634781384756794987";
        int bScale = 41;
        String c = "94488478231212478987482988429808779810457634781384756794987";
        int cScale = 41;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal bNumber = new BigDecimal(new BigInteger(b), bScale);
        BigDecimal cNumber = new BigDecimal(new BigInteger(c), cScale);
        assertEquals(cNumber, aNumber.min(bNumber), "incorrect value");
    }

    /**
     * plus() for a positive BigDecimal
     */
    @Test
    public void testPlusPositive() {
        String a = "92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = 41;
        String c = "92948782094488478231212478987482988429808779810457634781384756794987";
        int cScale = 41;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal cNumber = new BigDecimal(new BigInteger(c), cScale);
        assertEquals(cNumber, aNumber.plus(), "incorrect value");
    }

    /**
     * plus(MathContext) for a positive BigDecimal
     */
    @Test
    public void testPlusMathContextPositive() {
        String a = "92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = 41;
        int precision = 37;
        RoundingMode rm = RoundingMode.FLOOR;
        MathContext mc = new MathContext(precision, rm);
        String c = "929487820944884782312124789.8748298842";
        int cScale = 10;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal res = aNumber.plus(mc);
        assertEquals(c, res.toString(), "incorrect value");
        assertEquals(cScale, res.scale(), "incorrect scale");
    }

    /**
     * plus() for a negative BigDecimal
     */
    @Test
    public void testPlusNegative() {
        String a = "-92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = 41;
        String c = "-92948782094488478231212478987482988429808779810457634781384756794987";
        int cScale = 41;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal cNumber = new BigDecimal(new BigInteger(c), cScale);
        assertEquals(cNumber, aNumber.plus(), "incorrect value");
    }

    /**
     * plus(MathContext) for a negative BigDecimal
     */
    @Test
    public void testPlusMathContextNegative() {
        String a = "-92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = 49;
        int precision = 46;
        RoundingMode rm = RoundingMode.CEILING;
        MathContext mc = new MathContext(precision, rm);
        String c = "-9294878209448847823.121247898748298842980877981";
        int cScale = 27;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal res = aNumber.plus(mc);
        assertEquals(c, res.toString(), "incorrect value");
        assertEquals(cScale, res.scale(), "incorrect scale");
    }

    /**
     * negate() for a positive BigDecimal
     */
    @Test
    public void testNegatePositive() {
        String a = "92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = 41;
        String c = "-92948782094488478231212478987482988429808779810457634781384756794987";
        int cScale = 41;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal cNumber = new BigDecimal(new BigInteger(c), cScale);
        assertEquals(cNumber, aNumber.negate(), "incorrect value");
    }

    /**
     * negate(MathContext) for a positive BigDecimal
     */
    @Test
    public void testNegateMathContextPositive() {
        String a = "92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = 41;
        int precision = 37;
        RoundingMode rm = RoundingMode.FLOOR;
        MathContext mc = new MathContext(precision, rm);
        String c = "-929487820944884782312124789.874829884";
        int cScale = 10;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal res = aNumber.negate(mc);
        String resString = res.toString();
        assertEquals(c, resString.substring(0, resString.length() - 1), "incorrect value");
        assertEquals(cScale, res.scale(), "incorrect scale");
    }

    /**
     * negate() for a negative BigDecimal
     */
    @Test
    public void testNegateNegative() {
        String a = "-92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = 41;
        String c = "92948782094488478231212478987482988429808779810457634781384756794987";
        int cScale = 41;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal cNumber = new BigDecimal(new BigInteger(c), cScale);
        assertEquals(cNumber, aNumber.negate(), "incorrect value");
    }

    /**
     * negate(MathContext) for a negative BigDecimal
     */
    @Test
    public void testNegateMathContextNegative() {
        String a = "-92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = 49;
        int precision = 46;
        RoundingMode rm = RoundingMode.CEILING;
        MathContext mc = new MathContext(precision, rm);
        String c = "9294878209448847823.12124789874829884298087798";
        int cScale = 27;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal res = aNumber.negate(mc);
        String resString = res.toString();
        assertEquals(c, resString.substring(0, resString.length() - 1), "incorrect value");
        assertEquals(cScale, res.scale(), "incorrect scale");
    }

    /**
     * signum() for a positive BigDecimal
     */
    @Test
    public void testSignumPositive() {
        String a = "92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = 41;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        assertEquals(1, aNumber.signum(), "incorrect value");
    }

    /**
     * signum() for a negative BigDecimal
     */
    @Test
    public void testSignumNegative() {
        String a = "-92948782094488478231212478987482988429808779810457634781384756794987";
        int aScale = 41;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        assertEquals(-1, aNumber.signum(), "incorrect value");
    }

    /**
     * signum() for zero
     */
    @Test
    public void testSignumZero() {
        String a = "0";
        int aScale = 41;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        assertEquals(0, aNumber.signum(), "incorrect value");
    }

    /*
     * Regression test for HARMONY-6406
     */
    @Test
    public void testApproxPrecision() {
        BigDecimal testInstance = BigDecimal.TEN.multiply(new BigDecimal("0.1"));
        int result = testInstance.compareTo(new BigDecimal("1.00"));
        assertEquals(0, result);
    }
}
