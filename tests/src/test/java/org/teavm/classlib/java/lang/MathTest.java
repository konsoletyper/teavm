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
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class MathTest {
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
        assertEquals(Float.valueOf(Float.POSITIVE_INFINITY), Float.valueOf(Math.abs(Float.NEGATIVE_INFINITY)));
        assertEquals(Double.valueOf(Double.POSITIVE_INFINITY), Double.valueOf(Math.abs(Double.NEGATIVE_INFINITY)));
        assertEquals(Double.valueOf(5.0), Double.valueOf(Math.abs(-5.0)));
        assertEquals(Double.valueOf(3.0), Double.valueOf(Math.abs(3.0)));
        assertEquals(Double.valueOf(3.0), Double.valueOf(Math.abs(-3.0)));
        assertEquals(Double.valueOf(5.0), Double.valueOf(Math.abs(5.0)));
        assertEquals(Float.valueOf(0.0f), Float.valueOf(Math.abs(-0.0f)));
        assertEquals(Float.valueOf(0.0f), Float.valueOf(Math.abs(0.0f)));
        assertEquals(Double.valueOf(0.0), Double.valueOf(Math.abs(-0.0)));
        assertEquals(Double.valueOf(0.0), Double.valueOf(Math.abs(0.0)));
    }

    @Test
    public void signumWorks() {
        assertEquals(Double.valueOf(1.0), Double.valueOf(Math.signum(3.0)));
        assertEquals(Double.valueOf(-1.0), Double.valueOf(Math.signum(-4.0)));
        assertEquals(Float.valueOf(1f), Float.valueOf(Math.signum(3f)));
        assertEquals(Float.valueOf(-1f), Float.valueOf(Math.signum(-4f)));

        assertEquals(Double.valueOf(0.0), Double.valueOf(Math.signum(0.0)));
        assertEquals(Double.valueOf(-0.0), Double.valueOf(Math.signum(-0.0)));
        assertTrue(Double.isNaN(Math.signum(Double.NaN)));
        assertEquals(Float.valueOf(0.0f), Float.valueOf(Math.signum(0.0f)));
        assertEquals(Float.valueOf(-0.0f), Float.valueOf(Math.signum(-0.0f)));
        assertTrue(Float.isNaN(Math.signum(Float.NaN)));
        assertEquals(Double.valueOf(-1.0), Double.valueOf(Math.signum(-Double.MIN_VALUE)));
        assertEquals(Double.valueOf(1.0), Double.valueOf(Math.signum(Double.MIN_VALUE)));

        assertEquals(Float.valueOf(-1), Float.valueOf(Math.signum(Float.NEGATIVE_INFINITY)));
        assertEquals(Float.valueOf(1), Float.valueOf(Math.signum(Float.POSITIVE_INFINITY)));
    }

    @Test
    public void copySignWorks() {
        assertEquals(Double.valueOf(1.0), Double.valueOf(Math.copySign(1.0, 0.0)));
        assertEquals(Double.valueOf(-1.0), Double.valueOf(Math.copySign(1.0, -0.0)));
        assertEquals(Double.valueOf(1.0), Double.valueOf(Math.copySign(1.0, Double.NaN)));
        assertEquals(Double.valueOf(Double.NaN), Double.valueOf(Math.copySign(Double.NaN, -1.0)));
        assertEquals(Double.valueOf(Double.POSITIVE_INFINITY),
                Double.valueOf(Math.copySign(Double.NEGATIVE_INFINITY, 1.0)));
        assertEquals(Float.valueOf(1.0f), Float.valueOf(Math.copySign(1.0f, 0.0f)));
        assertEquals(Float.valueOf(-1.0f), Float.valueOf(Math.copySign(1.0f, -0.0f)));
        assertEquals(Float.valueOf(1.0f), Float.valueOf(Math.copySign(1.0f, Float.NaN)));
        assertEquals(Float.valueOf(Float.NaN), Float.valueOf(Math.copySign(Float.NaN, -1.0f)));
        assertEquals(Float.valueOf(Float.POSITIVE_INFINITY),
                Float.valueOf(Math.copySign(Float.NEGATIVE_INFINITY, 1.0f)));
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
        assertEquals(Double.valueOf(-Double.MIN_VALUE), Double.valueOf(Math.nextDown(0.0)));
        assertEquals(Double.valueOf(Double.MIN_VALUE), Double.valueOf(Math.nextUp(0.0)));
        assertEquals(Double.valueOf(-Double.MIN_VALUE), Double.valueOf(Math.nextDown(-0.0)));
        assertEquals(Double.valueOf(Double.MIN_VALUE), Double.valueOf(Math.nextUp(-0.0)));
        assertEquals(Float.valueOf(-Float.MIN_VALUE), Float.valueOf(Math.nextDown(0.0f)));
        assertEquals(Float.valueOf(Float.MIN_VALUE), Float.valueOf(Math.nextUp(0.0f)));
        assertEquals(Float.valueOf(-Float.MIN_VALUE), Float.valueOf(Math.nextDown(-0.0f)));
        assertEquals(Float.valueOf(Float.MIN_VALUE), Float.valueOf(Math.nextUp(-0.0f)));
        assertEquals(Double.valueOf(0.10000000000000002), Double.valueOf(Math.nextUp(0.1)));
        assertEquals(Double.valueOf(0.9999999999999999), Double.valueOf(Math.nextDown(1.0)));
        assertEquals(Double.valueOf(-0.09999999999999999), Double.valueOf(Math.nextUp(-0.1)));
        assertEquals(Double.valueOf(-1.0000000000000002), Double.valueOf(Math.nextDown(-1.0)));
        assertEquals(Float.valueOf(0.10000001f), Float.valueOf(Math.nextUp(0.1f)));
        assertEquals(Float.valueOf(0.99999994f), Float.valueOf(Math.nextDown(1.0f)));
        assertEquals(Float.valueOf(-0.099999994f), Float.valueOf(Math.nextUp(-0.1f)));
        assertEquals(Float.valueOf(-1.0000001f), Float.valueOf(Math.nextDown(-1.0f)));
        assertEquals(Float.valueOf(Float.NEGATIVE_INFINITY), Float.valueOf(Math.nextDown(Float.NEGATIVE_INFINITY)));
        assertEquals(Float.valueOf(Float.intBitsToFloat(Float.floatToIntBits(Float.POSITIVE_INFINITY) - 1)),
                Float.valueOf(Math.nextDown(Float.POSITIVE_INFINITY)));
        assertEquals(Float.valueOf(Float.POSITIVE_INFINITY), Float.valueOf(Math.nextUp(Float.POSITIVE_INFINITY)));
        assertEquals(Float.valueOf(Float.intBitsToFloat(Float.floatToIntBits(Float.NEGATIVE_INFINITY) - 1)),
                Float.valueOf(Math.nextUp(Float.NEGATIVE_INFINITY)));
        assertEquals(Double.valueOf(Double.NEGATIVE_INFINITY), Double.valueOf(Math.nextDown(Double.NEGATIVE_INFINITY)));
        assertEquals(Double.valueOf(Double.longBitsToDouble(Double.doubleToLongBits(Double.POSITIVE_INFINITY) - 1)),
                Double.valueOf(Math.nextDown(Double.POSITIVE_INFINITY)));
        assertEquals(Double.valueOf(Double.POSITIVE_INFINITY), Double.valueOf(Math.nextUp(Double.POSITIVE_INFINITY)));
        assertEquals(Double.valueOf(Double.longBitsToDouble(Double.doubleToLongBits(Double.NEGATIVE_INFINITY) - 1)),
                Double.valueOf(Math.nextUp(Double.NEGATIVE_INFINITY)));
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
}
