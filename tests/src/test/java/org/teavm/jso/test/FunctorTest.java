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
import static org.junit.Assert.assertSame;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
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
    public void functorParamsMarshaled() {
        assertEquals("(q,w)", testMethod((a, b) -> a + "," + b, "q", "w"));
    }

    @Test
    public void functorIdentityPreserved() {
        JSBiFunction javaFunction = (a, b) -> a + b;
        JSObject firstRef = getFunction(javaFunction);
        JSObject secondRef = getFunction(javaFunction);
        assertSame(firstRef, secondRef);
    }

    @Test
    public void functorWithDefaultMethodPassed() {
        assertEquals(123, callFunctionWithDefaultMethod(s -> s + 100));
    }

    @JSBody(params = "f", script = "return f(23);")
    private static native int callFunctionWithDefaultMethod(JSFunctionWithDefaultMethod f);

    @Test
    public void functorWithStaticMethodPassed() {
        assertEquals(123, callFunctionWithStaticMethod(s -> s + 100));
    }

    @JSBody(params = "f", script = "return f(23);")
    private static native int callFunctionWithStaticMethod(JSFunctionWithStaticMethod f);

    @Test
    public void propertyWithNonAlphabeticFirstChar() {
        WithProperties wp = getWithPropertiesInstance();
        assertEquals("foo_ok", wp.get_foo());
        assertEquals("bar_ok", wp.get$bar());
        assertEquals("baz_ok", wp.propbaz());
    }

    @Test
    public void functorPassedBack() {
        JSBiFunction function = getBiFunction();
        assertEquals(23042, function.foo(23, 42));
    }

    @Test
    public void functorParamsMarshaledBack() {
        JSStringBiFunction function = getStringBiFunction();
        assertEquals("q,w", function.foo("q", "w"));
    }

    @Test
    public void castToFunctor() {
        JSBiFunction f = getBiFunctionAsObject().cast();
        assertEquals(23042, f.foo(23, 42));
    }

    @JSBody(params = { "f", "a", "b" }, script = "return '(' + f(a, b) + ')';")
    private static native String testMethod(JSBiFunction f, int a, int b);

    @JSBody(params = { "f", "a", "b" }, script = "return '(' + f(a, b) + ')';")
    private static native String testMethod(JSStringBiFunction f, String a, String b);

    @JSBody(script = ""
            + "return function(a, b) {"
                + "return a * 1000 + b;"
            + "};")
    private static native JSBiFunction getBiFunction();

    @JSBody(script = ""
            + "return function(a, b) {"
            + "return a * 1000 + b;"
            + "};")
    private static native JSObject getBiFunctionAsObject();

    @JSBody(script = ""
            + "return function(a, b) {"
            + "return a + ',' + b;"
            + "};")
    private static native JSStringBiFunction getStringBiFunction();

    @JSBody(params = "f", script = "return f;")
    private static native JSObject getFunction(JSBiFunction f);

    @JSBody(script = "return { _foo: 'foo_ok', $bar: 'bar_ok', baz: 'baz_ok' };")
    private static native WithProperties getWithPropertiesInstance();

    @JSFunctor
    interface JSBiFunction extends JSObject {
        int foo(int a, int b);
    }

    @JSFunctor
    interface JSStringBiFunction extends JSObject {
        String foo(String a, String b);
    }

    @JSFunctor
    interface JSFunctionWithDefaultMethod extends JSObject {
        int foo(int a);

        default String defaultMethod() {
            return "Content";
        }
    }

    @JSFunctor
    interface JSFunctionWithStaticMethod extends JSObject {
        int foo(int a);

        static String staticMethod() {
            return "Content";
        }
    }

    interface WithProperties extends JSObject {
        @JSProperty
        String get_foo();

        @JSProperty
        String get$bar();

        @JSProperty("baz")
        String propbaz();
    }
}
