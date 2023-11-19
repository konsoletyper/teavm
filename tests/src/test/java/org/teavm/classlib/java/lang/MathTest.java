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
package org.teavm.classlib.java.lang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@SkipPlatform(TestPlatform.WASI)
public class MathTest {
    private static void sameDouble(double a, double b) {
        assertEquals(Double.valueOf(a), Double.valueOf(b));
    }

    private static void sameFloat(float a, float b) {
        assertEquals(Float.valueOf(a), Float.valueOf(b));
    }

    @Test
    public void sinComputed() {
        assertEquals(0.90929742682568, Math.sin(2), 1E-14);
    }

    @Test
    public void expComputed() {
        assertEquals(3.4212295362896734, Math.exp(1.23), 1E-14);
    }

    @Test
    public void cbrtComputed() {
        assertEquals(3.0, Math.cbrt(27.0), 1E-14);
        assertEquals(-3.0, Math.cbrt(-27.0), 1E-14);
        assertEquals(0, Math.cbrt(0), 1E-14);
    }

    @Test
    public void ulpComputed() {
        assertEquals(1.1920928955078125E-7, Math.ulp(1), 1E-25);
        assertEquals(1.4210854715202004e-14, Math.ulp(123.456), 1E-25);
        assertEquals(6.32E-322, Math.ulp(Math.pow(2, -1015)), 1E-323);

        assertEquals(7.62939453125E-6F, Math.ulp(123.456F), 1E-8F);
        assertEquals(8.968310171678829E-44F, Math.ulp((float) Math.pow(2, -120)), 1E-45F);
    }

    @Test
    public void sinhComputed() {
        assertEquals(1.3097586593745313E53, Math.sinh(123), 1E40);
    }

    @Test
    public void getExponentComputed() {
        assertEquals(6, Math.getExponent(123.456));
    }

    @Test
    public void testAbs() {
        sameFloat(Float.POSITIVE_INFINITY, Math.abs(Float.NEGATIVE_INFINITY));
        sameDouble(Double.POSITIVE_INFINITY, Math.abs(Double.NEGATIVE_INFINITY));
        sameDouble(5.0, Math.abs(-5.0));
        sameDouble(3.0, Math.abs(3.0));
        sameDouble(3.0, Math.abs(-3.0));
        sameDouble(5.0, Math.abs(5.0));
        sameFloat(0.0f, Math.abs(-0.0f));
        sameFloat(0.0f, Math.abs(0.0f));
        sameDouble(0.0, Math.abs(-0.0));
        sameDouble(0.0, Math.abs(0.0));
    }

    @Test
    public void signumWorks() {
        sameDouble(1.0, Math.signum(3.0));
        sameDouble(-1.0, Math.signum(-4.0));
        sameFloat(1f, Math.signum(3f));
        sameFloat(-1f, Math.signum(-4f));

        sameDouble(0.0, Math.signum(0.0));
        sameDouble(-0.0, Math.signum(-0.0));
        sameDouble(Double.NaN, Math.signum(Double.NaN));
        sameFloat(0.0f, Math.signum(0.0f));
        sameFloat(-0.0f, Math.signum(-0.0f));
        sameFloat(Float.NaN, Math.signum(Float.NaN));
        sameDouble(-1.0, Math.signum(-Double.MIN_VALUE));
        sameDouble(1.0, Math.signum(Double.MIN_VALUE));

        sameFloat((float) -1, Math.signum(Float.NEGATIVE_INFINITY));
        sameFloat(1F, Math.signum(Float.POSITIVE_INFINITY));
    }

    @Test
    public void copySignWorks() {
        sameDouble(1.0, Math.copySign(1.0, 0.0));
        sameDouble(-1.0, Math.copySign(1.0, -0.0));
        sameDouble(1.0, Math.copySign(1.0, Double.NaN));
        sameDouble(Double.NaN, Math.copySign(Double.NaN, -1.0));
        sameDouble(Double.POSITIVE_INFINITY, Math.copySign(Double.NEGATIVE_INFINITY, 1.0));
        sameFloat(1.0f, Math.copySign(1.0f, 0.0f));
        sameFloat(-1.0f, Math.copySign(1.0f, -0.0f));
        sameFloat(1.0f, Math.copySign(1.0f, Float.NaN));
        sameFloat(Float.NaN, Math.copySign(Float.NaN, -1.0f));
        sameFloat(Float.POSITIVE_INFINITY, Math.copySign(Float.NEGATIVE_INFINITY, 1.0f));
    }

    @Test
    public void roundWorks() {
        assertEquals(1, Math.round(1.3));
        assertEquals(2, Math.round(1.8));
        assertEquals(-1, Math.round(-1.3));
        assertEquals(-2, Math.round(-1.8));
    }

    @Test
    public void nextWorks() {
        sameDouble(-Double.MIN_VALUE, Math.nextDown(0.0));
        sameDouble(Double.MIN_VALUE, Math.nextUp(0.0));
        sameDouble(-Double.MIN_VALUE, Math.nextDown(-0.0));
        sameDouble(Double.MIN_VALUE, Math.nextUp(-0.0));
        sameFloat(-Float.MIN_VALUE, Math.nextDown(0.0f));
        sameFloat(Float.MIN_VALUE, Math.nextUp(0.0f));
        sameFloat(-Float.MIN_VALUE, Math.nextDown(-0.0f));
        sameFloat(Float.MIN_VALUE, Math.nextUp(-0.0f));
        sameDouble(0.10000000000000002, Math.nextUp(0.1));
        sameDouble(0.9999999999999999, Math.nextDown(1.0));
        sameDouble(-0.09999999999999999, Math.nextUp(-0.1));
        sameDouble(-1.0000000000000002, Math.nextDown(-1.0));
        sameFloat(0.10000001f, Math.nextUp(0.1f));
        sameFloat(0.99999994f, Math.nextDown(1.0f));
        sameFloat(-0.099999994f, Math.nextUp(-0.1f));
        sameFloat(-1.0000001f, Math.nextDown(-1.0f));
        sameFloat(Float.NEGATIVE_INFINITY, Math.nextDown(Float.NEGATIVE_INFINITY));
        sameFloat(Float.intBitsToFloat(Float.floatToIntBits(Float.POSITIVE_INFINITY) - 1),
                Math.nextDown(Float.POSITIVE_INFINITY));
        sameFloat(Float.POSITIVE_INFINITY, Math.nextUp(Float.POSITIVE_INFINITY));
        sameFloat(Float.intBitsToFloat(Float.floatToIntBits(Float.NEGATIVE_INFINITY) - 1),
                Math.nextUp(Float.NEGATIVE_INFINITY));
        sameDouble(Double.NEGATIVE_INFINITY, Math.nextDown(Double.NEGATIVE_INFINITY));
        sameDouble(Double.longBitsToDouble(Double.doubleToLongBits(Double.POSITIVE_INFINITY) - 1),
                Math.nextDown(Double.POSITIVE_INFINITY));
        sameDouble(Double.POSITIVE_INFINITY, Math.nextUp(Double.POSITIVE_INFINITY));
        sameDouble(Double.longBitsToDouble(Double.doubleToLongBits(Double.NEGATIVE_INFINITY) - 1),
                Math.nextUp(Double.NEGATIVE_INFINITY));
    }

    @Test
    public void exponentWorks() {
        assertEquals(0, Math.getExponent(1.0f));
        assertEquals(-127, Math.getExponent(Float.MIN_VALUE));
        assertEquals(127, Math.getExponent(Float.MAX_VALUE));
        assertEquals(128, Math.getExponent(Float.POSITIVE_INFINITY));
        assertEquals(128, Math.getExponent(Float.NEGATIVE_INFINITY));
        assertEquals(128, Math.getExponent(Float.NaN));
        assertEquals(0, Math.getExponent(1.0));
        assertEquals(-1023, Math.getExponent(Double.MIN_VALUE));
        assertEquals(1023, Math.getExponent(Double.MAX_VALUE));
        assertEquals(1024, Math.getExponent(Double.POSITIVE_INFINITY));
        assertEquals(1024, Math.getExponent(Double.NEGATIVE_INFINITY));
        assertEquals(1024, Math.getExponent(Double.NaN));
    }

    @Test
    public void minMax() {
        sameDouble(-1.0, Math.max(-2.0, -1.0));
        sameDouble(-2.0, Math.min(-2.0, -1.0));
        sameDouble(Double.NaN, Math.min(Double.NaN, Double.POSITIVE_INFINITY));
        sameDouble(Double.NaN, Math.min(Double.POSITIVE_INFINITY, Double.NaN));
        sameDouble(Double.NaN, Math.max(Double.NaN, Double.POSITIVE_INFINITY));
        sameDouble(Double.NaN, Math.max(Double.POSITIVE_INFINITY, Double.NaN));
        sameDouble(-0.0, Math.min(-0.0, 0.0));
        sameDouble(-0.0, Math.min(0.0, -0.0));
        sameDouble(0.0, Math.max(-0.0, 0.0));
        sameDouble(0.0, Math.max(0.0, -0.0));
        sameFloat(-1.0f, Math.max(-2.0f, -1.0f));
        sameFloat(-2.0f, Math.min(-2.0f, -1.0f));
        sameFloat(Float.NaN, Math.min(Float.NaN, Float.POSITIVE_INFINITY));
        sameFloat(Float.NaN, Math.min(Float.POSITIVE_INFINITY, Float.NaN));
        sameFloat(Float.NaN, Math.max(Float.NaN, Float.POSITIVE_INFINITY));
        sameFloat(Float.NaN, Math.max(Float.POSITIVE_INFINITY, Float.NaN));
        sameFloat(-0.0f, Math.min(-0.0f, 0.0f));
        sameFloat(-0.0f, Math.min(0.0f, -0.0f));
        sameFloat(0.0f, Math.max(-0.0f, 0.0f));
        sameFloat(0.0f, Math.max(0.0f, -0.0f));
    }

    @Test
    public void exacts() {
        try {
            Math.incrementExact(Integer.MAX_VALUE);
            fail();
        } catch (ArithmeticException e) {
            // ok
        }
        try {
            Math.negateExact(Integer.MIN_VALUE);
            fail();
        } catch (ArithmeticException e) {
            // ok
        }
        try {
            Math.toIntExact((long) Integer.MAX_VALUE + 1);
            fail();
        } catch (ArithmeticException e) {
            // ok
        }
        try {
            Math.addExact(Integer.MAX_VALUE, Integer.MAX_VALUE);
            fail();
        } catch (ArithmeticException e) {
            // ok
        }
        try {
            Math.subtractExact(Integer.MIN_VALUE, 2);
            fail();
        } catch (ArithmeticException e) {
            // ok
        }
        try {
            Math.multiplyExact(Integer.MIN_VALUE, -1);
            fail();
        } catch (ArithmeticException e) {
            // ok
        }
        try {
            Math.multiplyExact(Integer.MIN_VALUE, 2);
            fail();
        } catch (ArithmeticException e) {
            // ok
        }
        try {
            Math.multiplyExact(1 << 30, 2);
            fail();
        } catch (ArithmeticException e) {
            // ok
        }
        try {
            Math.divideExact(Integer.MIN_VALUE, -1);
            fail();
        } catch (ArithmeticException e) {
            // ok
        }
        try {
            Math.divideExact(Integer.MIN_VALUE, -1);
            fail();
        } catch (ArithmeticException e) {
            // ok
        }
        IntStream.rangeClosed(-10, 10).forEach(x -> {
            assertEquals(x + 1, Math.incrementExact(x));
            assertEquals(x - 1, Math.decrementExact(x));
            assertEquals(-x, Math.negateExact(x));
            IntStream.rangeClosed(-10, 10).forEach(y -> {
                assertEquals(x + y, Math.addExact(x, y));
                assertEquals(x - y, Math.subtractExact(x, y));
                assertEquals(x * y, Math.multiplyExact(x, y));
                if (y != 0) {
                    assertEquals(x / y, Math.divideExact(x, y));
                }
            });
        });
        LongStream.rangeClosed(-10, 10).forEach(x -> {
            assertEquals(x + 1, Math.incrementExact(x));
            assertEquals(x - 1, Math.decrementExact(x));
            assertEquals(-x, Math.negateExact(x));
            assertEquals((int) x, Math.toIntExact(x));
            LongStream.rangeClosed(-10, 10).forEach(y -> {
                assertEquals(x + y, Math.addExact(x, y));
                assertEquals(x - y, Math.subtractExact(x, y));
                assertEquals(x * y, Math.multiplyExact(x, y));
                if (y != 0) {
                    assertEquals(x / y, Math.divideExact(x, y));
                }
            });
        });
    }
}
