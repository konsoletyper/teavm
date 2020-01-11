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
package org.teavm.classlib.java.lang.reflect;

import static org.junit.Assert.assertEquals;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.classlib.support.Reflectable;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class MethodTest {
    @Test
    public void methodsEnumerated() {
        callMethods();

        String text = collectMethods(Foo.class.getDeclaredMethods());

        assertEquals(""
                + "java.lang.Object Foo.baz();"
                + "public void Foo.accept(long);"
                + "public void Foo.bar(java.lang.Object);",
                text);
    }

    @Test
    public void publicMethodsEnumerated() {
        callMethods();

        String text = collectMethods(Foo.class.getMethods());

        assertEquals("public void Foo.accept(long);public void Foo.bar(java.lang.Object);", text);
    }

    @Test
    public void inheritedPublicMethodEnumerated() {
        callMethods();

        String text = collectMethods(SubClass.class.getMethods());

        assertEquals("public void SubClass.g();public void SuperClass.f();", text);
    }

    @Test
    public void bridgeMethodNotFound() throws NoSuchMethodException {
        callMethods();

        Method method = SubClassWithBridge.class.getMethod("f");

        assertEquals(String.class, method.getReturnType());
    }

    @Test
    public void methodInvoked() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Foo foo = new Foo();
        Method method = foo.getClass().getMethod("bar", Object.class);
        method.invoke(foo, "23");
        assertEquals("23", foo.baz());
    }

    @Test
    public void methodInvoked2() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Foo foo = new Foo();
        foo.bar("42");
        Method method = foo.getClass().getDeclaredMethod("baz");
        assertEquals("42", method.invoke(foo));
    }

    @Test
    public void staticInitializerCalled() throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        Method method = WithInitializer.class.getMethod("f");
        method.invoke(null);
        assertEquals("init;f();", WithInitializer.log);
    }

    private void callMethods() {
        new Foo().bar(null);
        new Foo().baz();
        new Foo().accept(0);
        new SuperClass().f();
        new SubClass().g();
        new SuperClassWithBridge().f();
        new SubClassWithBridge().f();
    }

    private String collectMethods(Method[] methods) {
        List<String> lines = new ArrayList<>();
        for (Method method : methods) {
            if (!method.getDeclaringClass().equals(Object.class)) {
                lines.add(method + ";");
            }
        }
        StringBuilder sb = new StringBuilder();
        Collections.sort(lines);
        for (String line : lines) {
            sb.append(line);
        }
        return sb.toString().replace("org.teavm.classlib.java.lang.reflect.MethodTest$", "");
    }

    static class Foo {
        Object value;

        @Reflectable
        public void accept(long l) {
        }

        @Reflectable
        public void bar(Object value) {
            this.value = value;
        }

        @Reflectable
        Object baz() {
            return value;
        }
    }

    static class SuperClass {
        @Reflectable
        public void f() {
        }
    }

    static class SubClass extends SuperClass {
        @Reflectable
        public void g() {
        }
    }

    static class SuperClassWithBridge {
        @Reflectable
        public Object f() {
            return null;
        }
    }

    static class SubClassWithBridge {
        @Reflectable
        public String f() {
            return null;
        }
    }

    static class WithInitializer {
        static String log = "";

        static {
            log += "init;";
        }

        @Reflectable public static void f() {
            log += "f();";
        }
    }
}
