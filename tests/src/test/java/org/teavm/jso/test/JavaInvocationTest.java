/*
 *  Copyright 2016 Alexey Andreev.
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

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class JavaInvocationTest {
    @Test
    public void callStaticMethod() {
        assertEquals(7, staticInvocation(5));
    }

    @Test
    public void callMemberMethod() {
        assertEquals(7, Num.create(5).add(Num.create(2)).value());
    }

    @JSBody(params = "a", script = "return javaMethods.get('org.teavm.jso.test.JavaInvocationTest.sum(II)I')"
            + ".invoke(a, 2);")
    private static native int staticInvocation(int a);

    private static int sum(int a, int b) {
        return a + b;
    }

    static abstract class Num implements JSObject {
        @JSBody(params = "n", script = "return n;")
        public static native Num create(int n);

        @JSBody(params = {}, script = "return this;")
        public native int value();

        @JSBody(params = "other", script = "return javaMethods.get('org.teavm.jso.test.JavaInvocationTest$Num"
                + ".addImpl(I)I').invoke(this, other);")
        public native Num add(Num other);

        private int addImpl(int other) {
            return value() + other;
        }
    }
}
