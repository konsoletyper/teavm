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

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class ImplementationTest {
    @Test
    public void respectsPrecedence() {
        assertEquals(12, mul(add(2, 2), 3));
        assertEquals(8, add(2, mul(2, 3)));
    }

    @JSBody(params = { "a", "b" }, script = "return a + b;")
    static native int add(int a, int b);

    @JSBody(params = { "a", "b" }, script = "return a * b;")
    static native int mul(int a, int b);

    @Test
    public void inliningUsageCounterWorksProperly() {
        ForInliningTest instance = ForInliningTest.create();
        wrongInlineCandidate(instance.foo());
        assertEquals(1, instance.counter);
    }

    @JSBody(params = "a", script = "console.log(a, a);")
    private static native void wrongInlineCandidate(JSObject a);

    static class ForInliningTest implements JSObject {
        public int counter;

        public ForInliningTest foo() {
            ++counter;
            return this;
        }

        public static ForInliningTest create() {
            return new ForInliningTest();
        }
    }
}
