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
    public void roundWorks() {
        assertEquals(1, Math.round(1.3));
        assertEquals(2, Math.round(1.8));
        assertEquals(-1, Math.round(-1.3));
        assertEquals(-2, Math.round(-1.8));
    }
}
