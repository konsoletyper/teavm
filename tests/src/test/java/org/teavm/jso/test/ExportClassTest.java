/*
 *  Copyright 2017 Alexey Andreev.
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
public class ExportClassTest {
    @Test
    public void simpleClassExported() {
        assertEquals("(OK)", callIFromJs(new SimpleClass()));
        assertEquals("[OK]", callIFromJs(new DerivedSimpleClass()));
    }

    @Test
    public void classWithPropertiesExported() {
        var o = new ClassWithProperty("q");
        assertEquals("q", extractFoo(o));

        setFoo(o);
        assertEquals("w", o.fooValue);
    }

    @JSBody(params = "a", script = "return a.foo('OK');")
    private static native String callIFromJs(I a);

    @JSBody(params = "a", script = "return a.foo;")
    private static native String extractFoo(J a);

    @JSBody(params = "a", script = "a.foo = 'w';")
    private static native void setFoo(J a);

    interface I extends JSObject {
        String foo(String a);
    }

    static class SimpleClass implements I {
        @Override
        public String foo(String a) {
            return "(" + a + ")";
        }
    }

    static class DerivedSimpleClass implements I {
        @Override
        public String foo(String a) {
            return "[" + a + "]";
        }
    }

    interface J extends JSObject {
        @JSProperty
        String getFoo();

        @JSProperty
        void setFoo(String value);
    }

    static class ClassWithProperty implements J {
        String fooValue;

        ClassWithProperty(String fooValue) {
            this.fooValue = fooValue;
        }

        @Override
        public String getFoo() {
            return fooValue;
        }

        @Override
        public void setFoo(String value) {
            fooValue = value;
        }
    }
}
