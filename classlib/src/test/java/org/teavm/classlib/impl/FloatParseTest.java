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
import org.teavm.classlib.java.lang.TFloat;

public class FloatParseTest {
    @Test
    public void parse() {
        assertEquals(1, TFloat.parseFloat("1"), 1E-12f);
        assertEquals(23, TFloat.parseFloat("23"), 1E-12F);
        assertEquals(23, TFloat.parseFloat("23.0"), 1E-12F);
        assertEquals(23, TFloat.parseFloat("23E0"), 1E-12F);
        assertEquals(23, TFloat.parseFloat("2.30000E1"), 1E-12F);
        assertEquals(23, TFloat.parseFloat("0.23E2"), 1E-12F);
        assertEquals(23, TFloat.parseFloat("0.000023E6"), 1E-12F);
        assertEquals(23, TFloat.parseFloat("00230000e-4"), 1E-12F);
        assertEquals(23, TFloat.parseFloat("2300000000000000000000e-20"), 1E-12F);
        assertEquals(23, TFloat.parseFloat("2300000000000000000000e-20"), 1E-12F);
        assertEquals(23, TFloat.parseFloat("2300000000000000000000e-20"), 1E-12F);
        assertEquals(23, TFloat.parseFloat("23."), 1E-12F);
        assertEquals(0.1F, TFloat.parseFloat("0.1"), 0.001F);
        assertEquals(0.1F, TFloat.parseFloat(".1"), 0.001F);
        assertEquals(0.1F, TFloat.parseFloat(" .1"), 0.001F);
        assertEquals(0.1F, TFloat.parseFloat(".1 "), 0.001F);
        assertEquals(-23, TFloat.parseFloat("-23"), 1E-12F);
        assertEquals(0, TFloat.parseFloat("0.0"), 1E-12F);
        assertEquals(0, TFloat.parseFloat("0"), 1E-12F);
        assertEquals(0, TFloat.parseFloat("00"), 1E-12F);
        assertEquals(0, TFloat.parseFloat(".0"), 1E-12F);
        assertEquals(0, TFloat.parseFloat("0."), 1E-12F);
        assertEquals(0, TFloat.parseFloat("23E-8000"), 1E-12F);
        assertEquals(0, TFloat.parseFloat("00000"), 1E-12F);
        assertEquals(0, TFloat.parseFloat("00000.0000"), 1E-12F);
        assertEquals(4499999285F, TFloat.parseFloat("4499999285"), 100F);
        assertEquals(0.4499999285F, TFloat.parseFloat("0.4499999285"), 1E-9F);
    }
}
