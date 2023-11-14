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
import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
@OnlyPlatform(TestPlatform.JAVASCRIPT)
@EachTestCompiledSeparately
public class AnnotationsTest {
    @Test
    public void staticBodyWorks() {
        assertEquals(12, add(5, 7));
    }

    @Test
    public void memberBodyWorks() {
        assertEquals(12, convert(convert(5).add(convert(7))));
    }

    @Test
    public void abstractWrapperWorks() {
        AbstractWrapper obj = AbstractWrapper.create(5);
        assertEquals(5, obj.getValue());
        assertEquals(12, obj.testMethod(6));
        assertEquals(13, obj.renamedMethod(6));
        assertEquals(25, obj.javaMethod(6));
    }

    @Test
    public void interfaceWrapperWorks() {
        InterfaceWrapper obj = createWrapper(5);
        assertEquals(5, obj.getValue());
        assertEquals(12, obj.testMethod(6));
        assertEquals(13, obj.renamedMethod(6));
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

    static abstract class AbstractWrapper implements JSObject {
        private AbstractWrapper() {
        }

        @JSProperty
        public abstract int getValue();

        public abstract int testMethod(int num);

        @JSMethod("renamedJSMethod")
        public abstract int renamedMethod(int num);

        public final int javaMethod(int num) {
            return testMethod(num) + renamedMethod(num);
        }

        @JSBody(params = "value", script = ""
                + "return {"
                    + "'value' : value, "
                    + "testMethod : function(num) { return this.value + num + 1; }, "
                    + "renamedJSMethod : function(num) { return this.value + num + 2; }"
                + "};")
        public static native AbstractWrapper create(int value);
    }

    interface InterfaceWrapper extends JSObject {
        @JSProperty
        int getValue();

        int testMethod(int num);

        @JSMethod("renamedJSMethod")
        int renamedMethod(int num);
    }

    @JSBody(params = "value", script = ""
            + "return {"
                + "value : value, "
                + "testMethod : function(num) { return this.value + num + 1; }, "
                + "renamedJSMethod : function(num) { return this.value + num + 2; }"
            + "};")
    public static native InterfaceWrapper createWrapper(int value);
}
