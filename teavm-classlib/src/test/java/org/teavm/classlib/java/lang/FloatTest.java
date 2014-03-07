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

/**
 *
 * @author Alexey Andreev
 */
public class FloatTest {
    @Test
    public void parsed() {
        assertEquals(23, Double.parseDouble("23"), 1E-12);
        assertEquals(23, Double.parseDouble("23.0"), 1E-12);
        assertEquals(23, Double.parseDouble("23E0"), 1E-12);
        assertEquals(23, Double.parseDouble("2.30000E1"), 1E-12);
        assertEquals(23, Double.parseDouble("0.23E2"), 1E-12);
        assertEquals(23, Double.parseDouble("0.000023E6"), 1E-12);
        assertEquals(23, Double.parseDouble("00230000e-4"), 1E-12);
        assertEquals(23, Double.parseDouble("2300000000000000000000e-20"), 1E-12);
        assertEquals(23, Double.parseDouble("2300000000000000000000e-20"), 1E-12);
    }

    @Test
    public void negativeParsed() {
        assertEquals(-23, Double.parseDouble("-23"), 1E-12);
    }

    @Test
    public void zeroParsed() {
        assertEquals(0, Double.parseDouble("0.0"), 1E-12);
        assertEquals(0, Double.parseDouble("23E-8000"), 1E-12);
        assertEquals(0, Double.parseDouble("00000"), 1E-12);
        assertEquals(0, Double.parseDouble("00000.0000"), 1E-12);
    }
}
