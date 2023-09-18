/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.classlib.impl;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.teavm.classlib.java.lang.TStringBuilder;

public class DoubleWriteTest {
    @Test
    public void lowerUpperEquidistant() {
        var d = Double.longBitsToDouble(1551003667821398223L);
        assertEquals("5.5497208273234845E-205", toString(d));
    }

    @Test
    public void absLowNegativeExponent() {
        var d = Double.longBitsToDouble(-4656062020850909140L);
        assertEquals("-0.0022393517064785064", toString(d));
    }

    @Test
    public void values() {
        assertEquals("1.23456789E150", toString(1.23456789E150));
        assertEquals("10.0", toString(10.0));
        assertEquals("20.0", toString(20.0));
        assertEquals("100.0", toString(100.0));
        assertEquals("1000.0", toString(1000.0));
        assertEquals("0.1", toString(0.1));
        assertEquals("0.01", toString(0.01));
        assertEquals("0.001", toString(0.001));
        assertEquals("1.0E20", toString(1e20));
        assertEquals("2.0E20", toString(2e20));
        assertEquals("1.0E-12", toString(1e-12));
        assertEquals("-1.23456789E150", toString(-1.23456789E150));
        assertEquals("1.23456789E-150", toString(1.23456789E-150));
        assertEquals("1.79769313486231E308", toString(1.79769313486231E308));
        assertEquals("3.0E-308", toString(3E-308));
        assertEquals("1200.0", toString(1200.0));
        assertEquals("0.023", toString(0.023));
        assertEquals("0.0", toString(0.0));
        assertEquals("1.0", toString(1.0));
    }

    private String toString(double d) {
        return new TStringBuilder().append(d).toString();
    }
}
