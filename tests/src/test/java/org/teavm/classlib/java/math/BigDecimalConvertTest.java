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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class BigDecimalConvertTest {
    /**
     * Double value of a negative BigDecimal
     */
    @Test
    public void testDoubleValueNeg() {
        String a = "-123809648392384754573567356745735.63567890295784902768787678287E+21";
        BigDecimal aNumber = new BigDecimal(a);
        double result = -1.2380964839238476E53;
        assertEquals("incorrect value", result, aNumber.doubleValue(), 0);
    }

    /**
     * Double value of a positive BigDecimal
     */
    @Test
    public void testDoubleValuePos() {
        String a = "123809648392384754573567356745735.63567890295784902768787678287E+21";
        BigDecimal aNumber = new BigDecimal(a);
        double result = 1.2380964839238476E53;
        assertEquals("incorrect value", result, aNumber.doubleValue(), 0);
    }

    /**
     * Double value of a large positive BigDecimal
     */
    @Test
    public void testDoubleValuePosInfinity() {
        String a = "123809648392384754573567356745735.63567890295784902768787678287E+400";
        BigDecimal aNumber = new BigDecimal(a);
        double result = Double.POSITIVE_INFINITY;
        assertEquals("incorrect value", result, aNumber.doubleValue(), 0);
    }

    /**
     * Double value of a large negative BigDecimal
     */
    @Test
    public void testDoubleValueNegInfinity() {
        String a = "-123809648392384754573567356745735.63567890295784902768787678287E+400";
        BigDecimal aNumber = new BigDecimal(a);
        double result = Double.NEGATIVE_INFINITY;
        assertEquals("incorrect value", result, aNumber.doubleValue(), 0);
    }

    /**
     * Double value of a small negative BigDecimal
     */
    @Test
    public void testDoubleValueMinusZero() {
        String a = "-123809648392384754573567356745735.63567890295784902768787678287E-400";
        BigDecimal aNumber = new BigDecimal(a);
        long minusZero = -9223372036854775808L;
        double result = aNumber.doubleValue();
        assertEquals(minusZero, Double.doubleToLongBits(result));
    }

    /**
     * Double value of a small positive BigDecimal
     */
    @Test
    public void testDoubleValuePlusZero() {
        String a = "123809648392384754573567356745735.63567890295784902768787678287E-400";
        BigDecimal aNumber = new BigDecimal(a);
        long zero = 0;
        double result = aNumber.doubleValue();
        assertTrue("incorrect value", Double.doubleToLongBits(result) == zero);
    }

    /**
     * Float value of a negative BigDecimal
     */
    @Test
    public void testFloatValueNeg() {
        String a = "-1238096483923847.6356789029578E+21";
        BigDecimal aNumber = new BigDecimal(a);
        float result = -1.2380965E36F;
        assertEquals("incorrect value", result, aNumber.floatValue(), 1E30);
    }

    /**
     * Float value of a positive BigDecimal
     */
    @Test
    public void testFloatValuePos() {
        String a = "1238096483923847.6356789029578E+21";
        BigDecimal aNumber = new BigDecimal(a);
        float result = 1.2380965E36F;
        assertEquals("incorrect value", result, aNumber.floatValue(), 1E30);
    }

    /**
     * Float value of a large positive BigDecimal
     */
    @Test
    public void testFloatValuePosInfinity() {
        String a = "123809648373567356745735.6356789787678287E+200";
        BigDecimal aNumber = new BigDecimal(a);
        float result = Float.POSITIVE_INFINITY;
        assertTrue("incorrect value", aNumber.floatValue() == result);
    }

    /**
     * Float value of a large negative BigDecimal
     */
    @Test
    public void testFloatValueNegInfinity() {
        String a = "-123809648392384755735.63567887678287E+200";
        BigDecimal aNumber = new BigDecimal(a);
        float result = Float.NEGATIVE_INFINITY;
        assertTrue("incorrect value", aNumber.floatValue() == result);
    }

    /**
     * Float value of a small negative BigDecimal
     */
    @Test
    public void testFloatValueMinusZero() {
        String a = "-123809648392384754573567356745735.63567890295784902768787678287E-400";
        BigDecimal aNumber = new BigDecimal(a);
        int minusZero = -2147483648;
        float result = aNumber.floatValue();
        assertEquals("incorrect value", Float.floatToIntBits(result), minusZero);
    }

    /**
     * Float value of a small positive BigDecimal
     */
    @Test
    public void testFloatValuePlusZero() {
        String a = "123809648392384754573567356745735.63567890295784902768787678287E-400";
        BigDecimal aNumber = new BigDecimal(a);
        int zero = 0;
        float result = aNumber.floatValue();
        assertTrue("incorrect value", Float.floatToIntBits(result) == zero);
    }

    /**
     * Integer value of a negative BigDecimal
     */
    @Test
    public void testIntValueNeg() {
        String a = "-123809648392384754573567356745735.63567890295784902768787678287E+21";
        BigDecimal aNumber = new BigDecimal(a);
        int result = 218520473;
        assertTrue("incorrect value", aNumber.intValue() == result);
    }

    /**
     * Integer value of a positive BigDecimal
     */
    @Test
    public void testIntValuePos() {
        String a = "123809648392384754573567356745735.63567890295784902768787678287E+21";
        BigDecimal aNumber = new BigDecimal(a);
        int result = -218520473;
        assertTrue("incorrect value", aNumber.intValue() == result);
    }

    /**
     * Long value of a negative BigDecimal
     */
    @Test
    public void testLongValueNeg() {
        String a = "-123809648392384754573567356745735.63567890295784902768787678287E+21";
        BigDecimal aNumber = new BigDecimal(a);
        long result = -1246043477766677607L;
        assertTrue("incorrect value", aNumber.longValue() == result);
    }

    /**
     * Long value of a positive BigDecimal
     */
    @Test
    public void testLongValuePos() {
        String a = "123809648392384754573567356745735.63567890295784902768787678287E+21";
        BigDecimal aNumber = new BigDecimal(a);
        long result = 1246043477766677607L;
        assertTrue("incorrect value", aNumber.longValue() == result);
    }

    /**
     * scaleByPowerOfTen(int n)
     */
    @Test
    public void testScaleByPowerOfTen1() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = 13;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal result = aNumber.scaleByPowerOfTen(10);
        String res = "1231212478987482988429808779810457634781384756794.987";
        int resScale = 3;
        assertEquals("incorrect value", res, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * scaleByPowerOfTen(int n)
     */
    @Test
    public void testScaleByPowerOfTen2() {
        String a = "1231212478987482988429808779810457634781384756794987";
        int aScale = -13;
        BigDecimal aNumber = new BigDecimal(new BigInteger(a), aScale);
        BigDecimal result = aNumber.scaleByPowerOfTen(10);
        String res = "1.231212478987482988429808779810457634781384756794987E+74";
        int resScale = -23;
        assertEquals("incorrect value", res, result.toString());
        assertEquals("incorrect scale", resScale, result.scale());
    }

    /**
     * Convert a positive BigDecimal to BigInteger
     */
    @Test
    public void testToBigIntegerPos1() {
        String a = "123809648392384754573567356745735.63567890295784902768787678287E+21";
        BigInteger bNumber = new BigInteger("123809648392384754573567356745735635678902957849027687");
        BigDecimal aNumber = new BigDecimal(a);
        BigInteger result = aNumber.toBigInteger();
        assertTrue("incorrect value", result.equals(bNumber));
    }

    /**
     * Convert a positive BigDecimal to BigInteger
     */
    @Test
    public void testToBigIntegerPos2() {
        String a = "123809648392384754573567356745735.63567890295784902768787678287E+15";
        BigInteger bNumber = new BigInteger("123809648392384754573567356745735635678902957849");
        BigDecimal aNumber = new BigDecimal(a);
        BigInteger result = aNumber.toBigInteger();
        assertTrue("incorrect value", result.equals(bNumber));
    }

    /**
     * Convert a positive BigDecimal to BigInteger
     */
    @Test
    public void testToBigIntegerPos3() {
        String a = "123809648392384754573567356745735.63567890295784902768787678287E+45";
        BigInteger bNumber = new BigInteger("12380964839238475457356735674573563567890295784902768787678287"
                + "0000000000000000");
        BigDecimal aNumber = new BigDecimal(a);
        BigInteger result = aNumber.toBigInteger();
        assertTrue("incorrect value", result.equals(bNumber));
    }

    /**
     * Convert a negative BigDecimal to BigInteger
     */
    @Test
    public void testToBigIntegerNeg1() {
        String a = "-123809648392384754573567356745735.63567890295784902768787678287E+21";
        BigInteger bNumber = new BigInteger("-123809648392384754573567356745735635678902957849027687");
        BigDecimal aNumber = new BigDecimal(a);
        BigInteger result = aNumber.toBigInteger();
        assertTrue("incorrect value", result.equals(bNumber));
    }

    /**
     * Convert a negative BigDecimal to BigInteger
     */
    @Test
    public void testToBigIntegerNeg2() {
        String a = "-123809648392384754573567356745735.63567890295784902768787678287E+15";
        BigInteger bNumber = new BigInteger("-123809648392384754573567356745735635678902957849");
        BigDecimal aNumber = new BigDecimal(a);
        BigInteger result = aNumber.toBigInteger();
        assertTrue("incorrect value", result.equals(bNumber));
    }

    /**
     * Convert a negative BigDecimal to BigInteger
     */
    @Test
    public void testToBigIntegerNeg3() {
        String a = "-123809648392384754573567356745735.63567890295784902768787678287E+45";
        BigInteger bNumber = new BigInteger("-12380964839238475457356735674573563567890295784902768787678287"
                + "0000000000000000");
        BigDecimal aNumber = new BigDecimal(a);
        BigInteger result = aNumber.toBigInteger();
        assertTrue("incorrect value", result.equals(bNumber));
    }

    /**
     * Convert a small BigDecimal to BigInteger
     */
    @Test
    public void testToBigIntegerZero() {
        String a = "-123809648392384754573567356745735.63567890295784902768787678287E-500";
        BigInteger bNumber = new BigInteger("0");
        BigDecimal aNumber = new BigDecimal(a);
        BigInteger result = aNumber.toBigInteger();
        assertTrue("incorrect value", result.equals(bNumber));
    }

    /**
     * toBigIntegerExact()
     */
    @Test
    public void testToBigIntegerExact1() {
        String a = "-123809648392384754573567356745735.63567890295784902768787678287E+45";
        BigDecimal aNumber = new BigDecimal(a);
        String res = "-123809648392384754573567356745735635678902957849027687876782870000000000000000";
        BigInteger result = aNumber.toBigIntegerExact();
        assertEquals("incorrect value", res, result.toString());
    }

    /**
     * toBigIntegerExact()
     */
    @Test
    public void testToBigIntegerExactException() {
        String a = "-123809648392384754573567356745735.63567890295784902768787678287E-10";
        BigDecimal aNumber = new BigDecimal(a);
        try {
            aNumber.toBigIntegerExact();
            fail("java.lang.ArithmeticException has not been thrown");
        } catch (java.lang.ArithmeticException e) {
            return;
        }
    }

    /**
     * Convert a positive BigDecimal to an engineering string representation
     */
    @Test
    public void testToEngineeringStringPos() {
        String a = "123809648392384754573567356745735.63567890295784902768787678287E-501";
        BigDecimal aNumber = new BigDecimal(a);
        String result = "123.80964839238475457356735674573563567890295784902768787678287E-471";
        assertEquals("incorrect value", result, aNumber.toEngineeringString());
    }

    /**
     * Convert a negative BigDecimal to an engineering string representation
     */
    @Test
    public void testToEngineeringStringNeg() {
        String a = "-123809648392384754573567356745735.63567890295784902768787678287E-501";
        BigDecimal aNumber = new BigDecimal(a);
        String result = "-123.80964839238475457356735674573563567890295784902768787678287E-471";
        assertEquals("incorrect value", result, aNumber.toEngineeringString());
    }

    /**
     * Convert a negative BigDecimal to an engineering string representation
     */
    @Test
    public void testToEngineeringStringZeroPosExponent() {
        String a = "0.0E+16";
        BigDecimal aNumber = new BigDecimal(a);
        String result = "0E+15";
        assertEquals("incorrect value", result, aNumber.toEngineeringString());
    }

    /**
     * Convert a negative BigDecimal to an engineering string representation
     */
    @Test
    public void testToEngineeringStringZeroNegExponent() {
        String a = "0.0E-16";
        BigDecimal aNumber = new BigDecimal(a);
        String result = "0.00E-15";
        assertEquals("incorrect value", result, aNumber.toEngineeringString());
    }

    /**
     * Convert a negative BigDecimal with a negative exponent to a plain string
     * representation; scale == 0.
     */
    @Test
    public void testToPlainStringNegNegExp() {
        String a = "-123809648392384754573567356745735.63567890295784902768787678287E-100";
        BigDecimal aNumber = new BigDecimal(a);
        String result = "-0.0000000000000000000000000000000000000000000000000000000000000000000123809648392"
                + "38475457356735674573563567890295784902768787678287";
        assertTrue("incorrect value", aNumber.toPlainString().equals(result));
    }

    /**
     * Convert a negative BigDecimal with a positive exponent
     * to a plain string representation;
     * scale == 0.
     */
    @Test
    public void testToPlainStringNegPosExp() {
        String a = "-123809648392384754573567356745735.63567890295784902768787678287E100";
        BigDecimal aNumber = new BigDecimal(a);
        String result = "-1238096483923847545735673567457356356789029578490276878767828"
                + "700000000000000000000000000000000000000000000000000000000000000000000000";
        assertTrue("incorrect value", aNumber.toPlainString().equals(result));
    }

    /**
     * Convert a positive BigDecimal with a negative exponent
     * to a plain string representation;
     * scale == 0.
     */
    @Test
    public void testToPlainStringPosNegExp() {
        String a = "123809648392384754573567356745735.63567890295784902768787678287E-100";
        BigDecimal aNumber = new BigDecimal(a);
        String result = "0.00000000000000000000000000000000000000000000000000000000"
                + "0000000000012380964839238475457356735674573563567890295784902768787678287";
        assertTrue("incorrect value", aNumber.toPlainString().equals(result));
    }

    /**
     * Convert a negative BigDecimal with a negative exponent
     * to a plain string representation;
     * scale == 0.
     */
    @Test
    public void testToPlainStringPosPosExp() {
        String a = "123809648392384754573567356745735.63567890295784902768787678287E+100";
        BigDecimal aNumber = new BigDecimal(a);
        String result = "123809648392384754573567356745735635678902957849027687876782870000000000"
                + "0000000000000000000000000000000000000000000000000000000000000";
        assertTrue("incorrect value", aNumber.toPlainString().equals(result));
    }

    /**
     * Convert a BigDecimal to a string representation;
     * scale == 0.
     */
    @Test
    public void testToStringZeroScale() {
        String a = "-123809648392384754573567356745735635678902957849027687876782870";
        BigDecimal aNumber = new BigDecimal(new BigInteger(a));
        String result = "-123809648392384754573567356745735635678902957849027687876782870";
        assertTrue("incorrect value", aNumber.toString().equals(result));
    }

    /**
     * Convert a positive BigDecimal to a string representation
     */
    @Test
    public void testToStringPos() {
        String a = "123809648392384754573567356745735.63567890295784902768787678287E-500";
        BigDecimal aNumber = new BigDecimal(a);
        String result = "1.2380964839238475457356735674573563567890295784902768787678287E-468";
        assertTrue("incorrect value", aNumber.toString().equals(result));
    }

    /**
     * Convert a negative BigDecimal to a string representation
     */
    @Test
    public void testToStringNeg() {
        String a = "-123.4564563673567380964839238475457356735674573563567890295784902768787678287E-5";
        BigDecimal aNumber = new BigDecimal(a);
        String result = "-0.001234564563673567380964839238475457356735674573563567890295784902768787678287";
        assertEquals("incorrect value", result, aNumber.toString());
    }

    /**
     * Create a BigDecimal from a positive long value; scale == 0
     */
    @Test
    public void testValueOfPosZeroScale() {
        long a = 98374823947823578L;
        BigDecimal aNumber = BigDecimal.valueOf(a);
        String result = "98374823947823578";
        assertTrue("incorrect value", aNumber.toString().equals(result));
    }

    /**
     * Create a BigDecimal from a negative long value; scale is 0
     */
    @Test
    public void testValueOfNegZeroScale() {
        long a = -98374823947823578L;
        BigDecimal aNumber = BigDecimal.valueOf(a);
        String result = "-98374823947823578";
        assertTrue("incorrect value", aNumber.toString().equals(result));
    }

    /**
     * Create a BigDecimal from a negative long value; scale is positive
     */
    @Test
    public void testValueOfNegScalePos() {
        long a = -98374823947823578L;
        int scale = 12;
        BigDecimal aNumber = BigDecimal.valueOf(a, scale);
        String result = "-98374.823947823578";
        assertTrue("incorrect value", aNumber.toString().equals(result));
    }

    /**
     * Create a BigDecimal from a negative long value; scale is negative
     */
    @Test
    public void testValueOfNegScaleNeg() {
        long a = -98374823947823578L;
        int scale = -12;
        BigDecimal aNumber = BigDecimal.valueOf(a, scale);
        String result = "-9.8374823947823578E+28";
        assertTrue("incorrect value", aNumber.toString().equals(result));
    }

    /**
     * Create a BigDecimal from a negative long value; scale is positive
     */
    @Test
    public void testValueOfPosScalePos() {
        long a = 98374823947823578L;
        int scale = 12;
        BigDecimal aNumber = BigDecimal.valueOf(a, scale);
        String result = "98374.823947823578";
        assertTrue("incorrect value", aNumber.toString().equals(result));
    }

    /**
     * Create a BigDecimal from a negative long value; scale is negative
     */
    @Test
    public void testValueOfPosScaleNeg() {
        long a = 98374823947823578L;
        int scale = -12;
        BigDecimal aNumber = BigDecimal.valueOf(a, scale);
        String result = "9.8374823947823578E+28";
        assertTrue("incorrect value", aNumber.toString().equals(result));
    }

    /**
     * valueOf(Double.NaN)
     */
    @Test
    public void testValueOfDoubleNaN() {
        double a = Double.NaN;
        try {
            BigDecimal.valueOf(a);
            fail("NumberFormatException has not been thrown for Double.NaN");
        } catch (NumberFormatException e) {
            return;
        }
    }
}
