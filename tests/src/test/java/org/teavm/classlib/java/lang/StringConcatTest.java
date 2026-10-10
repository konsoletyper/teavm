/*
 *  Copyright 2026 Alexey Andreev.
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
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class StringConcatTest {
    // javac passes a constant that contains \u0001 or \u0002 as a bootstrap argument of
    // makeConcatWithConstants, not inside the recipe.
    @Test
    public void constantArgumentAfterValue() {
        int value = 23;
        assertEquals("a23\u0001", "a" + value + "\u0001");
    }

    @Test
    public void constantArgumentBeforeValue() {
        long value = 42;
        assertEquals("\u0002b42", "\u0002b" + value);
    }

    @Test
    public void constantArgumentsBetweenValues() {
        String first = "x";
        double second = 1.5;
        assertEquals("x\u0001y1.5\u0002", first + "\u0001y" + second + "\u0002");
    }
}
