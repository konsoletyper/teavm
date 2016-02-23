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

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class FunctorTest {
    @Test
    public void functorPassed() {
        assertEquals("(5)", testMethod((a, b) -> a + b, 2, 3));
    }

    @Test
    public void functorIdentityPreserved() {
        JSBiFunction javaFunction = (a, b) -> a + b;
        JSObject firstRef = getFunction(javaFunction);
        JSObject secondRef = getFunction(javaFunction);
        assertSame(firstRef, secondRef);
    }

    @JSBody(params = { "f", "a", "b" }, script = "return '(' + f(a, b) + ')';")
    private static native String testMethod(JSBiFunction f, int a, int b);

    @JSBody(params = "f", script = "return f;")
    private static native JSObject getFunction(JSBiFunction f);

    @JSFunctor
    interface JSBiFunction extends JSObject {
        int apply(int a, int b);
    }
}
