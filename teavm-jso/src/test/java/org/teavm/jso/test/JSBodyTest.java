/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.jso.test;

import static org.junit.Assert.*;
import org.junit.Test;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

/**
 *
 * @author Alexey Andreev
 */
public class JSBodyTest {
    @Test
    public void staticWorks() {
        assertEquals(12, add(5, 7));
    }

    @Test
    public void memberWorks() {
        assertEquals(12, convert(convert(5).add(convert(7))));
    }

    @JSBody(params = { "a", "b" }, script = "return a + b;")
    private static native int add(int a, int b);

    @JSBody(params = "n", script = "return n;")
    private static native Num convert(int n);

    @JSBody(params = "n", script = "return n;")
    private static native int convert(Num n);

    static abstract class Num implements JSObject {
        @JSBody(params = "other", script = "return this + other;")
        public final native Num add(Num other);
    }
}
