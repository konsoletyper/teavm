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
import org.teavm.jso.JSClass;
import org.teavm.jso.JSExport;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
@OnlyPlatform({TestPlatform.JAVASCRIPT, TestPlatform.WEBASSEMBLY_GC})
@EachTestCompiledSeparately
public class FunctorTest {
    @Test
    public void functorPassed() {
        assertEquals("(5)", testMethod((a, b) -> a + b, 2, 3));
        assertEquals("null", testMethod(null, 2, 3));
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
        var f = (JSBiFunction) getBiFunctionAsObject();
        assertEquals(23042, f.foo(23, 42));
    }

    @Test
    public void propsWithFunction() {
        var o = PropsObjectWithFunctor.create((a, b) -> a + 10 * b);
        assertEquals(123, acceptPropsObjectWithFunctor(o));
    }

    @Test
    public void functorTakingJavaClassWithExportedMembers() {
        var result = acceptJavaClass(obj -> "(" + obj.getFoo() + ")", new JavaClassWithExportedMembers());
        assertEquals("(fromJava: foo): js", result);
    }
    
    @Test
    public void varargs() {
        var result = acceptVarargs((first, remaining) -> {
            var sb = new StringBuilder();
            sb.append(first).append("|");
            for (var rem : remaining) {
                sb.append(rem).append(";");
            }
            return sb.toString();
        });
        assertEquals("called:23|foo;bar;", result);
    }

    @JSBody(params = { "f", "a", "b" }, script = "return f != null ? '(' + f(a, b) + ')' : 'null';")
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

    @JSClass(transparent = true)
    abstract static class PropsObjectWithFunctor implements JSObject {
        static PropsObjectWithFunctor create(JSBiFunction foo) {
            PropsObjectWithFunctor result = JSObjects.create();
            result.setFoo(foo);
            return result;
        }

        @JSProperty
        public native void setFoo(JSBiFunction foo);
    }

    @JSBody(params = "obj", script = "return 100 + obj.foo(3, 2);")
    private static native int acceptPropsObjectWithFunctor(JSObject obj);

    @JSBody(params = { "functor", "obj" }, script = "return functor(obj) + ': js';")
    private static native String acceptJavaClass(FunctorTakingJavaClass functor, Object obj);

    @JSBody(params = "functor", script = "return 'called:' + functor(23, 'foo', 'bar');")
    private static native String acceptVarargs(FunctorWithVarargs functor);

    @JSFunctor
    interface FunctorTakingJavaClass extends JSObject {
        String accept(JavaClassWithExportedMembers obj);
    }

    static class JavaClassWithExportedMembers {
        @JSProperty
        @JSExport
        String getFoo() {
            return "fromJava: foo";
        }
    }
    
    @JSFunctor
    interface FunctorWithVarargs extends JSObject {
        String accept(int first, String... remaining);
    }
}
