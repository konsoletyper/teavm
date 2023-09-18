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
import org.teavm.classlib.java.lang.TDouble;

public class DoubleParseTest {
    @Test
    public void parse() {
        assertEquals(1, TDouble.parseDouble("1"), 1E-12);
        assertEquals(23, TDouble.parseDouble("23"), 1E-12);
        assertEquals(23, TDouble.parseDouble("23.0"), 1E-12);
        assertEquals(23, TDouble.parseDouble("23E0"), 1E-12);
        assertEquals(23, TDouble.parseDouble("2.30000E1"), 1E-12);
        assertEquals(23, TDouble.parseDouble("0.23E2"), 1E-12);
        assertEquals(23, TDouble.parseDouble("0.000023E6"), 1E-12);
        assertEquals(23, TDouble.parseDouble("00230000e-4"), 1E-12);
        assertEquals(23, TDouble.parseDouble("2300000000000000000000e-20"), 1E-12);
        assertEquals(23, TDouble.parseDouble("2300000000000000000000e-20"), 1E-12);
        assertEquals(23, TDouble.parseDouble("23."), 1E-12);
        assertEquals(0.1, TDouble.parseDouble("0.1"), 0.001);
        assertEquals(0.1, TDouble.parseDouble(".1"), 0.001);
        assertEquals(0.1, TDouble.parseDouble(" .1"), 0.001);
        assertEquals(0.1, TDouble.parseDouble(".1 "), 0.001);
        assertEquals(-23, TDouble.parseDouble("-23"), 1E-12);
        assertEquals(0, TDouble.parseDouble("0.0"), 1E-12);
        assertEquals(0, TDouble.parseDouble("0"), 1E-12);
        assertEquals(0, TDouble.parseDouble("00"), 1E-12);
        assertEquals(0, TDouble.parseDouble("0."), 1E-12);
        assertEquals(0, TDouble.parseDouble(".0"), 1E-12);
        assertEquals(0, TDouble.parseDouble("23E-8000"), 1E-12);
        assertEquals(0, TDouble.parseDouble("00000"), 1E-12);
        assertEquals(0, TDouble.parseDouble("00000.0000"), 1E-12);

        assertEquals(9218868437227405312L, Double.doubleToLongBits(TDouble.parseDouble("7.17917199420887E309")));
        assertEquals(3823970227817620L, Double.doubleToLongBits(TDouble.parseDouble("1.889292320284411E-308")));
    }
}
