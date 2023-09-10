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

public class FloatWriteTest {
    @Test
    public void values() {
        assertEquals("1.234E25", toString(1.234E25F));
        assertEquals("9.8765E30", toString(9.8765E30F));
        assertEquals("-1.234E25", toString(-1.234E25F));
        assertEquals("-9.8765E30", toString(-9.8765E30F));
        assertEquals("3.402823E38", toString(3.402823E38f));
        assertEquals("9.8764E-30", toString(9.8764E-30F));
        assertEquals("-1.234E-25", toString(-1.234E-25F));
        assertEquals("-9.8764E-30", toString(-9.8764E-30F));
        assertEquals("1.17549E-38", toString(1.17549E-38f));
        assertEquals("1200.0", toString(1200f));
        assertEquals("0.023", toString(0.023f));
        assertEquals("0.0", toString(0f));
        assertEquals("1.0", toString(1f));
    }

    private String toString(float f) {
        return new TStringBuilder().append(f).toString();
    }
}
