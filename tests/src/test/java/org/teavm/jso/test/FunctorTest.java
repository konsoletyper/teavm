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
    public void functorIdentityPreserved() {
        JSBiFunction javaFunction = (a, b) -> a + b;
        JSObject firstRef = getFunction(javaFunction);
        JSObject secondRef = getFunction(javaFunction);
        assertSame(firstRef, secondRef);
    }
    
    @Test
    public void functorWithDefaultMethodPassed(){
        JSFunctionWithDefaultMethod javaFunction = (s) -> s+" returned";
        
        String returned = javaFunction.defaultMethod();
        
        assertEquals(returned, "Content returned");
    }
    
    @Test
    public void functorWithStaticMethodPassed(){
        JSFunctionWithStaticMethod javaFunction = (s) -> s+" returned";
        
        String returned = javaFunction.apply(JSFunctionWithStaticMethod.staticMethod());
        
        assertEquals(returned, "Content returned");
    }

    @Test
    public void propertyWithNonAlphabeticFirstChar() {
        WithProperties wp = getWithPropertiesInstance();
        assertEquals("foo_ok", wp.get_foo());
        assertEquals("bar_ok", wp.get$bar());
        assertEquals("baz_ok", wp.propbaz());
    }

    @JSBody(params = { "f", "a", "b" }, script = "return '(' + f(a, b) + ')';")
    private static native String testMethod(JSBiFunction f, int a, int b);

    @JSBody(params = "f", script = "return f;")
    private static native JSObject getFunction(JSBiFunction f);

    @JSBody(script = "return { _foo: 'foo_ok', $bar: 'bar_ok', baz: 'baz_ok' };")
    private static native WithProperties getWithPropertiesInstance();

    @JSFunctor
    interface JSBiFunction extends JSObject {
        int apply(int a, int b);
    }

    interface WithProperties extends JSObject {
        @JSProperty
        String get_foo();

        @JSProperty
        String get$bar();

        @JSProperty("baz")
        String propbaz();
    }
    
    @JSFunctor
    interface JSFunctionWithDefaultMethod extends JSObject {
        String apply(String a);
        
        default String defaultMethod(){
            return apply("Content");
        }
    }
        
    @JSFunctor
    interface JSFunctionWithStaticMethod extends JSObject {
        String apply(String a);
        
        public static String staticMethod(){
            return "Content";
        }
    }
      
}
