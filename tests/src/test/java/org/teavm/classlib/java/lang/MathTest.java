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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
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
        assertEquals(0x1p-52, Math.ulp(1.0), 0.0);
        assertEquals(0x1p-52, Math.ulp(-1.0), 0.0);
        assertEquals(0x1p-56, Math.ulp(0.1), 0.0);
        assertEquals(0x1p-46, Math.ulp(-123.456), 0.0);
        assertEquals(0x1p971, Math.ulp(Double.MAX_VALUE), 0.0);
        assertEquals(Double.MIN_VALUE, Math.ulp(-Double.MIN_VALUE), 0.0);

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
        assertEquals(1, Math.round(0.5));
        assertEquals(3, Math.round(2.5));
        assertEquals(0, Math.round(-0.5));
        assertEquals(-1, Math.round(-1.5));
        assertEquals(-2, Math.round(-2.5));
        assertEquals(0, Math.round(-0.5f));
        assertEquals(-1, Math.round(-1.5f));
        assertEquals(3, Math.round(2.5f));
        assertEquals(5, Math.round(4.999));
        assertEquals(4, Math.round(4.499999999999999));
        assertEquals(0, Math.round(-0.3));
        assertEquals(-1, Math.round(-0.5000000000000001));

        assertEquals(0, Math.round(0.49999999999999994));
        assertEquals(0, Math.round(0.49999997f));
        assertEquals(1L << 52, Math.round(0x1.0p52 - 0.5));
        assertEquals(1L << 60, Math.round(0x1.0p60));
        assertEquals(1 << 24, Math.round(0x1.0p24f));

        assertEquals(0, Math.round(Double.NaN));
        assertEquals(0, Math.round(Float.NaN));
        assertEquals(Long.MAX_VALUE, Math.round(Double.POSITIVE_INFINITY));
        assertEquals(Long.MIN_VALUE, Math.round(Double.NEGATIVE_INFINITY));
        assertEquals(Long.MAX_VALUE, Math.round(1e19));
        assertEquals(Long.MIN_VALUE, Math.round(-1e19));
        assertEquals(Integer.MAX_VALUE, Math.round(1e10f));
        assertEquals(Integer.MIN_VALUE, Math.round(-1e10f));
        assertEquals(Integer.MAX_VALUE, Math.round(Float.POSITIVE_INFINITY));
        assertEquals(3, StrictMath.round(2.5));
        assertEquals(0, StrictMath.round(-0.5f));
    }

    @Test
    public void rint() {
        sameDouble(1.0, Math.rint(1.3));
        sameDouble(2.0, Math.rint(1.8));
        sameDouble(0.0, Math.rint(0.5));
        sameDouble(2.0, Math.rint(1.5));
        sameDouble(2.0, Math.rint(2.5));
        sameDouble(4.0, Math.rint(3.5));
        sameDouble(-0.0, Math.rint(-0.5));
        sameDouble(-2.0, Math.rint(-1.5));
        sameDouble(-2.0, Math.rint(-2.5));
        sameDouble(-0.0, Math.rint(-0.3));
        sameDouble(-0.0, Math.rint(-0.0));
        sameDouble(0.0, Math.rint(0.0));
        sameDouble(0.0, Math.rint(0.49999999999999994));
        sameDouble(1e19, Math.rint(1e19));
        sameDouble(0x1.0p52, Math.rint(0x1.0p52 - 0.5));
        sameDouble(Double.NaN, Math.rint(Double.NaN));
        sameDouble(Double.POSITIVE_INFINITY, Math.rint(Double.POSITIVE_INFINITY));
        sameDouble(Double.NEGATIVE_INFINITY, Math.rint(Double.NEGATIVE_INFINITY));
        sameDouble(2.0, StrictMath.rint(2.5));
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
    public void nextAfterWorks() {
        sameDouble(1.0000000000000002, Math.nextAfter(1.0, 2.0));
        sameDouble(0.9999999999999999, Math.nextAfter(1.0, 0.0));
        sameDouble(-0.0, Math.nextAfter(0.0, -0.0));
        sameDouble(Double.NaN, Math.nextAfter(1.0, Double.NaN));
        sameDouble(Double.NaN, Math.nextAfter(Double.NaN, 1.0));
        sameFloat(1.0000001f, Math.nextAfter(1.0f, 2.0));
        sameFloat(0.99999994f, Math.nextAfter(1.0f, 0.0));
        sameFloat(-0.0f, Math.nextAfter(0.0f, -0.0));
        sameFloat(Float.NaN, Math.nextAfter(1.0f, Double.NaN));
        sameFloat(Float.NaN, Math.nextAfter(Float.NaN, 1.0));
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

    @Test
    public void ceilFloorModDiv() {
        assertEquals(-2, Math.ceilMod(+4, +3));
        assertEquals(+2, Math.ceilMod(-4, -3));
        assertEquals(+1, Math.ceilMod(+4, -3));
        assertEquals(-1, Math.ceilMod(-4, +3));
        assertEquals(+1, Math.floorMod(+4, +3));
        assertEquals(-1, Math.floorMod(-4, -3));
        assertEquals(-2, Math.floorMod(+4, -3));
        assertEquals(+2, Math.floorMod(-4, +3));
        assertEquals(+2, Math.ceilDiv(5, 3));
        assertEquals(+2, Math.ceilDiv(6, 3));
        assertEquals(-1, Math.ceilDiv(-5, 3));
        assertEquals(-2, Math.ceilDiv(-6, 3));
        assertEquals(+1, Math.floorDiv(5, 3));
        assertEquals(+2, Math.floorDiv(6, 3));
        assertEquals(-2, Math.floorDiv(-5, 3));
        assertEquals(-2, Math.floorDiv(-6, 3));
    }
    
    @Test
    public void scalbDouble() {
        assertEquals(12.0, Math.scalb(3.0, 2), 0.01);
        assertEquals(0.75, Math.scalb(3.0, -2), 0.01);
        assertEquals(-0.75, Math.scalb(-3.0, -2), 0.01);
        assertTrue(Double.isNaN(Math.scalb(Double.NaN, -2)));
        assertTrue(Double.isNaN(Math.scalb(Double.NaN, 2)));
        assertEquals(Double.POSITIVE_INFINITY, Math.scalb(3.0, 20000), 1.0);
        assertEquals(Double.POSITIVE_INFINITY, Math.scalb(Double.POSITIVE_INFINITY, -100), 1.0);
        assertEquals(Double.POSITIVE_INFINITY, Math.scalb(Double.POSITIVE_INFINITY, -20000), 1.0);
        assertTrue(Math.scalb(3.0, -20000) == 0);
        assertEquals(2.57e-322, Math.scalb(1.03e-321, -2), Double.MIN_VALUE);
        assertEquals(4.11e-321, Math.scalb(1.03e-321, 2), Double.MIN_VALUE);
        assertEquals(3.0e-323, Math.scalb(1.03e-321, -5), Double.MIN_VALUE);
        assertEquals(0, Math.scalb(1.03e-321, -10), Double.MIN_VALUE);
        assertEquals(0, Math.scalb(1.03e-321, -100), Double.MIN_VALUE);
        assertEquals(1.807872510037101e-308, Math.scalb(1.03e-321, 44), Double.MIN_VALUE);
        assertEquals(3.6157450200742022E-308, Math.scalb(1.03e-321, 45), Double.MIN_VALUE);
        assertEquals(1.03e-321, Math.scalb(3.6157450200742022E-308, -45), Double.MIN_VALUE);
        assertEquals(0x1p1000, Math.scalb(1.0, 1000), 0.0);
        assertEquals(0x1p1023, Math.scalb(1.0, 1023), 0.0);
        assertEquals(Double.MAX_VALUE, Math.scalb(Double.MAX_VALUE / 2, 1), 0.0);
        assertEquals(Double.POSITIVE_INFINITY, Math.scalb(1.0, 1024), 1.0);
        assertEquals(Double.NEGATIVE_INFINITY, Math.scalb(-1.0, 1024), 1.0);
        assertEquals(Double.POSITIVE_INFINITY, Math.scalb(Double.MAX_VALUE, 1), 1.0);
    }
    
    @Test
    public void scalbFloat() {
        assertEquals(12f, Math.scalb(3f, 2), 0.01f);
        assertEquals(0.75f, Math.scalb(3f, -2), 0.01f);
        assertEquals(-0.75f, Math.scalb(-3f, -2), 0.01f);
        assertTrue(Float.isNaN(Math.scalb(Float.NaN, -2)));
        assertTrue(Float.isNaN(Math.scalb(Float.NaN, 2)));
        assertEquals(Float.POSITIVE_INFINITY, Math.scalb(3f, 20000), 1f);
        assertEquals(Float.POSITIVE_INFINITY, Math.scalb(Float.POSITIVE_INFINITY, -100), 1f);
        assertEquals(Float.POSITIVE_INFINITY, Math.scalb(Float.POSITIVE_INFINITY, -20000), 1f);
        assertTrue(Math.scalb(3f, -20000) == 0);
        assertEquals(7.3e-44f, Math.scalb(2.91e-43f, -2), Float.MIN_VALUE);
        assertEquals(1.166e-42f, Math.scalb(2.91e-43f, 2), Float.MIN_VALUE);
        assertEquals(8.4e-45f, Math.scalb(2.91e-43f, -5), Float.MIN_VALUE);
        assertEquals(0, Math.scalb(2.91e-43f, -10), Float.MIN_VALUE);
        assertEquals(0, Math.scalb(2.91e-43f, -100), Float.MIN_VALUE);
        assertEquals(9.550892e-39f, Math.scalb(2.91e-43f, 15), Float.MIN_VALUE);
        assertEquals(1.9101783e-38f, Math.scalb(2.91e-43f, 16), Float.MIN_VALUE);
        assertEquals(2.91e-43f, Math.scalb(1.9101783e-38f, -16), Float.MIN_VALUE);
    }

    @Test
    public void fmaDoubleRoundsOnce() {
        double t = 0x1.0p-30;
        sameDouble(0x1.0p-60, Math.fma(1 + t, 1 + t, -(1 + 2 * t)));
        assertEquals(0, (1 + t) * (1 + t) - (1 + 2 * t), 0);
        sameDouble(1.0222044616602375E-14, Math.fma(1.0000001, 1.0000001, -1.0000002));
        sameDouble(1.0222044616602375E-14, StrictMath.fma(1.0000001, 1.0000001, -1.0000002));
    }

    @Test
    public void fmaDoubleRange() {
        sameDouble(2 * Double.MIN_VALUE, Math.fma(Double.MIN_VALUE, 0.5, Double.MIN_VALUE));
        sameDouble(Double.MAX_VALUE, Math.fma(Double.MAX_VALUE, 2.0, -Double.MAX_VALUE));
        sameDouble(Double.POSITIVE_INFINITY, Math.fma(1e300, 1e300, Double.MAX_VALUE));
        sameDouble(Double.NEGATIVE_INFINITY, Math.fma(-1e300, 1e300, -Double.MAX_VALUE));
        sameDouble(Double.NEGATIVE_INFINITY, Math.fma(1e300, 1e300, Double.NEGATIVE_INFINITY));
    }

    @Test
    public void fmaDoubleSpecialValues() {
        sameDouble(Double.NaN, Math.fma(Double.POSITIVE_INFINITY, 1.0, Double.NEGATIVE_INFINITY));
        sameDouble(Double.NaN, Math.fma(Double.NEGATIVE_INFINITY, 2.0, Double.POSITIVE_INFINITY));
        sameDouble(Double.NaN, Math.fma(Double.POSITIVE_INFINITY, 0.0, 1.0));
        sameDouble(Double.NaN, Math.fma(Double.NaN, 1.0, 1.0));
        sameDouble(Double.NaN, Math.fma(1.0, 1.0, Double.NaN));
        sameDouble(Double.POSITIVE_INFINITY, Math.fma(Double.NEGATIVE_INFINITY, -1.0, 1.0));
        sameDouble(Double.POSITIVE_INFINITY, Math.fma(2.0, 3.0, Double.POSITIVE_INFINITY));
        sameDouble(0.0, Math.fma(3.0, 2.0, -6.0));
        sameDouble(0.0, Math.fma(1.0, 0.0, 0.0));
        sameDouble(-0.0, Math.fma(-1.0, 0.0, -0.0));
        sameDouble(-0.0, Math.fma(-1e-200, 1e-200, 0.0));
    }

    @Test
    public void fmaFloatRoundsOnce() {
        float x = Float.intBitsToFloat(0xb32a8000);
        float y = Float.intBitsToFloat(0xeef3b300);
        float z = Float.intBitsToFloat(0xb851b01e);
        assertEquals(0x62a24eb7, Float.floatToRawIntBits(Math.fma(x, y, z)));
        assertEquals(0x62a24eb8, Float.floatToRawIntBits((float) ((double) x * (double) y + (double) z)));

        float a = 1 + 0x1.0p-12f;
        sameFloat(0x1.0p-24f, Math.fma(a, a, -(1 + 0x1.0p-11f)));
        sameFloat(0x1.0p-24f, StrictMath.fma(a, a, -(1 + 0x1.0p-11f)));
    }

    @Test
    public void fmaFloatRange() {
        sameFloat(2 * Float.MIN_VALUE, Math.fma(Float.MIN_VALUE, 0.5f, Float.MIN_VALUE));
        sameFloat(Float.MAX_VALUE, Math.fma(Float.MAX_VALUE, 2f, -Float.MAX_VALUE));
        sameFloat(Float.POSITIVE_INFINITY, Math.fma(Float.MAX_VALUE, Float.MAX_VALUE, 0f));
        sameFloat(Float.NEGATIVE_INFINITY, Math.fma(Float.MAX_VALUE, Float.MAX_VALUE, Float.NEGATIVE_INFINITY));
    }

    @Test
    public void fmaFloatSpecialValues() {
        sameFloat(Float.NaN, Math.fma(Float.POSITIVE_INFINITY, 1f, Float.NEGATIVE_INFINITY));
        sameFloat(Float.NaN, Math.fma(Float.NaN, 1f, 1f));
        sameFloat(Float.NaN, Math.fma(0f, Float.POSITIVE_INFINITY, 1f));
        sameFloat(0f, Math.fma(3f, 2f, -6f));
        sameFloat(-0f, Math.fma(-1f, 0f, -0f));
        sameFloat(-0f, Math.fma(-1e-30f, 1e-30f, 0f));
    }

    @Test
    public void fmaDoubleRoundsCorrectly() {
        sameDouble(0x1.670922f6dcd9p-140,
                Math.fma(0x1.c51145efaa97cp383, 0x1.f3ddb2760b3c7p-469, -0x1.ba5483893af2dp-85));
        sameDouble(-0x0.0p0,
                Math.fma(-0x1.0028p-664, 0x1.6a2bab1ada3c5p-956, 0x0.0p0));
        sameDouble(0x1.40b7c80bbc3dp-69,
                Math.fma(0x1.897019d3f8d38p-18, 0x1.88f92ef14eef6p1, -0x1.2df94edd5a56ap-16));
        sameDouble(-0x1.8c0a7e497168p-57,
                Math.fma(0x1.c208c7d524754p-23, -0x1.37f305bf7f7c8p20, 0x1.1231f19a3796cp-2));
        sameDouble(0x1.232398509406p-95,
                Math.fma(0x1.27ef48cc81fedp-994, -0x1.3d62b22a118a2p955, 0x1.6ee564b41d69p-39));
        sameDouble(0x1.d581105cff0b8p-83,
                Math.fma(-0x1.d3b750f00a4c4p251, 0x1.d5c8844e96c72p-282, 0x1.ad26981c8cbacp-30));
        sameDouble(0x1.ea1cd601a9dc8p-6,
                Math.fma(-0x1.f716dd92de6e9p58, 0x1.eb2fe47bd6ebcp-11, 0x1.e2a37c978d225p48));
        sameDouble(0x1.68fcf743ebd8p-45,
                Math.fma(0x1.859fc0f16981p-16, -0x1.b00e64bbf305p22, 0x1.48c9bed232161p7));
        sameDouble(-0x0.0000016901af6p-1022,
                Math.fma(0x1.116ecee97bfp-21, -0x1.1643782024fb9p-971, 0x1.293664fffa09ap-992));
        sameDouble(-0x1.ab75c39fbp-1013,
                Math.fma(-0x1.7ce584d4322e3p-989, 0x1.0000000009p32, 0x1.7ce584d43f924p-957));
        sameDouble(0x1.45cd68758p-32,
                Math.fma(0x1.490a574652f15p278, -0x1.000000008p-258, 0x1.490a5746f7769p20));
        sameDouble(-0x0.0p0,
                Math.fma(-0x1.f1769f0219382p-725, 0x1.4fd2537d59dbep-375, 0x0.0p0));
        sameDouble(0x1.76a4a3cfd81b8p-1008,
                Math.fma(-0x1.ae7db0becc432p-954, -0x1.f7fd43b887678p-3, -0x1.a7c16d2fe1997p-956));
        sameDouble(0x1.74140e7cc4d7cp-41,
                Math.fma(-0x1.c6b77e4561d4ap-1005, -0x1.9c0788c369eb3p1017, -0x1.6dee5894592a7p13));
        sameDouble(-0x0.0000000035181p-1022,
                Math.fma(0x1.1d98e22c1ff18p16, -0x1.ab14a2ed77eabp-1017, 0x1.dc7506f4f02b8p-1001));
        sameDouble(0x1.3262ce3f863fcp879,
                Math.fma(0x1.e84976950146dp1016, 0x1.2fa075d85c79bp-83, -0x1.219081055d9a2p934));
        sameDouble(0x0.000000271c565p-1022,
                Math.fma(-0x1.8fd5848eddc5p-16, 0x1.db83d8cd4f244p-980, 0x1.73578cea5e03ap-995));
        sameDouble(-0x1.5378e3c8881p803,
                Math.fma(0x1.00000000000cp-141, -0x1.5c71284bed82bp998, 0x1.5c71284bed93p857));
        sameDouble(0x1.f9385f43734fp-46,
                Math.fma(-0x1.3ecf9e8ce50afp-999, -0x1.d9f81b2ab7b08p1006, -0x1.27214b74821d1p8));
        sameDouble(0x0.0000000219d8ep-1022,
                Math.fma(0x0.a12a0531a982dp-1022, -0x1.d11985166ced8p22, 0x1.24cd6452a9a2bp-1000));
        sameDouble(0x0.000000000182bp-1022,
                Math.fma(-0x1.b7fe5be4ba1dp-26, -0x1.9d95db1c7c5a4p-982, -0x1.636b74f185451p-1007));
        sameDouble(-0x1.aab18p934,
                Math.fma(0x1.2d8bcdcb3c1eep-23, 0x1.0000bp1011, -0x1.2d8c9d1b599aap988));
        sameDouble(-0x1.659a8e2959dp946,
                Math.fma(0x1.b6ebf61a4c84p-4, 0x1.267b872b49103p1004, -0x1.f8e6c7c21831ap1000));
        sameDouble(0x0.0000525541238p-1022,
                Math.fma(-0x1.0486c46ef68e6p-1013, -0x1.f24245fc6c988p26, -0x1.fb119c17b65bdp-987));
    }

    @Test
    public void sqrt() {
        sameDouble(3.0, Math.sqrt(9.0));
        sameDouble(1.4142135623730951, Math.sqrt(2.0));
        sameDouble(0.0, Math.sqrt(0.0));
        sameDouble(-0.0, Math.sqrt(-0.0));
        sameDouble(Double.NaN, Math.sqrt(-1.0));
        sameDouble(Double.NaN, Math.sqrt(Double.NaN));
        sameDouble(Double.POSITIVE_INFINITY, Math.sqrt(Double.POSITIVE_INFINITY));
        sameDouble(0x1.0p-537, Math.sqrt(Double.MIN_VALUE));
        sameDouble(3.0, StrictMath.sqrt(9.0));
    }

    @Test
    public void floor() {
        sameDouble(2.0, Math.floor(2.5));
        sameDouble(-3.0, Math.floor(-2.5));
        sameDouble(-0.0, Math.floor(-0.0));
        sameDouble(0.0, Math.floor(0.3));
        sameDouble(-1.0, Math.floor(-0.3));
        sameDouble(Double.NaN, Math.floor(Double.NaN));
        sameDouble(Double.NEGATIVE_INFINITY, Math.floor(Double.NEGATIVE_INFINITY));
        sameDouble(1e300, Math.floor(1e300));
        sameDouble(2.0, StrictMath.floor(2.5));
    }

    @Test
    public void ceil() {
        sameDouble(3.0, Math.ceil(2.5));
        sameDouble(-2.0, Math.ceil(-2.5));
        sameDouble(-0.0, Math.ceil(-0.3));
        sameDouble(0.0, Math.ceil(0.0));
        sameDouble(1.0, Math.ceil(0.3));
        sameDouble(Double.NaN, Math.ceil(Double.NaN));
        sameDouble(Double.POSITIVE_INFINITY, Math.ceil(Double.POSITIVE_INFINITY));
        sameDouble(-1e300, Math.ceil(-1e300));
        sameDouble(3.0, StrictMath.ceil(2.5));
    }

    @Test
    public void absOfSpecialValues() {
        sameDouble(Double.NaN, Math.abs(Double.NaN));
        sameFloat(Float.NaN, Math.abs(Float.NaN));
        sameDouble(Double.MIN_VALUE, Math.abs(-Double.MIN_VALUE));
        sameFloat(Float.MAX_VALUE, Math.abs(-Float.MAX_VALUE));
        sameDouble(2.5, StrictMath.abs(-2.5));
    }
}
