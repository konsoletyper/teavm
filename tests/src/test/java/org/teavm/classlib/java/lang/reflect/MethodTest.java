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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.classlib.support.Reflectable;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@EachTestCompiledSeparately
@SkipPlatform({TestPlatform.C, TestPlatform.WEBASSEMBLY, TestPlatform.WASI})
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
    public void methodsInheritedFromInterfaceEnumerated() {
        callMethods();

        String text = collectMethods(InterfaceImplementor.class.getMethods());
        assertEquals(""
                + "public default void SuperInterface.g();"
                + "public void InterfaceImplementor.f();"
                + "public void InterfaceImplementor.h();",
                text
        );
    }

    @Test
    public void methodsEnumeratedWhenClassImplementEmptyInterface() {
        callMethods();

        String text = collectMethods(EmptyInterfaceImplementor.class.getMethods());
        assertEquals("public void EmptyInterfaceImplementor.f();", text);
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
    public void virtualInvoke() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        var obj = new SubclassVirtualMethod();
        var method = SuperclassVirtualMethod.class.getDeclaredMethod("f");
        var result = method.invoke(obj);
        assertEquals("sub", result);

        var privateMethod = SuperclassVirtualMethod.class.getDeclaredMethod("g");
        privateMethod.setAccessible(true);
        result = privateMethod.invoke(obj);
        assertEquals("super", result);
    }

    @Test
    public void staticInitializerCalled() throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        Method method = WithInitializer.class.getMethod("f");
        method.invoke(null);
        assertEquals("init;f();", WithInitializer.log);
    }

    @Test
    public void enumMethodWithUnusedParameterType() {
        var methods = ClassWithMethodReferringToUnusedInterface.class.getDeclaredMethods();
        assertEquals(1, methods.length);
        assertEquals("f", methods[0].getName());
        assertEquals("java.util.function.Consumer", methods[0].getParameterTypes()[0].getName());
        assertTrue(methods[0].getReturnType().getComponentType().isInterface());
    }

    @Test
    public void avoidCallingVirtualMethod() throws InvocationTargetException, IllegalAccessException {
        var methods = new ArrayList<Method>();
        for (var cls : List.of(FirstClassWithVirtualMethod.class, SecondClassWithVirtualMethod.class)) {
            methods.addAll(List.of(cls.getDeclaredMethods()));
        }
        var o = new SecondClassWithVirtualMethod();
        var sb = new StringBuilder();
        for (var method : methods) {
            if (method.getDeclaringClass().isInstance(o)) {
                sb.append(method.invoke(o, (short) 23));
            }
        }
        assertEquals("g:23", sb.toString());
    }
    
    @Test
    public void invokeStaticMethodWithoutInitializer() throws Exception {
        var method = ClassWithoutInitializerWithStaticMethod.class.getDeclaredMethod("foo");
        assertEquals("result:foo", method.invoke(null));
    }
    
    @Test
    public void methodAnnotationsRead() throws Exception {
        var acceptMethod = Foo.class.getMethod("accept", long.class);
        var barMethod = Foo.class.getMethod("bar", Object.class);
        assertEquals(List.of("TestAnnot"), extractAnnotations(acceptMethod));
        assertEquals(List.of(), extractAnnotations(barMethod));

        assertEquals(TestAnnot.class, acceptMethod.getAnnotation(TestAnnot.class).annotationType());
        assertNull(barMethod.getAnnotation(TestAnnot.class));
    }

    @Test
    @SkipPlatform(TestPlatform.JAVASCRIPT)
    public void overriddenMethodAnnotations() throws Exception {        
        var method = SubclassVirtualMethod.class.getDeclaredMethod("g");
        assertNull(method.getAnnotation(TestAnnot.class));
        
        method = SuperclassVirtualMethod.class.getDeclaredMethod("g");
        assertEquals(TestAnnot.class, method.getAnnotation(TestAnnot.class).annotationType());
    }

    private void callMethods() {
        new Foo().bar(null);
        new Foo().baz();
        new Foo().accept(0);
        new SuperClass().f();
        new SubClass().g();
        new SuperClassWithBridge().f();
        new SubClassWithBridge().f();
        new InterfaceImplementor().f();
        new InterfaceImplementor().g();
        new InterfaceImplementor().h();
        new EmptyInterfaceImplementor().f();
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
    
    private List<? extends String> extractAnnotations(AnnotatedElement elem) {
        return Arrays.stream(elem.getDeclaredAnnotations())
                .map(a -> a.annotationType().getSimpleName())
                .filter(a -> !Objects.equals(a, "Reflectable"))
                .collect(Collectors.toList());
    }

    static class Foo {
        @TestAnnot
        Object value;

        @Reflectable
        @TestAnnot
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

    static class SuperclassVirtualMethod {
        @Reflectable
        public String f() {
            return "super";
        }

        @Reflectable
        @TestAnnot
        private String g() {
            return "super";
        }
    }

    static class SubclassVirtualMethod extends SuperclassVirtualMethod {
        @Override
        public String f() {
            return "sub";
        }

        @Reflectable
        private String g() {
            return "sub";
        }
    }

    interface SuperInterface {
        @Reflectable
        void f();

        @Reflectable
        default void g() {
        }
    }

    static class InterfaceImplementor implements SuperInterface {
        @Override
        public void f() {
        }

        @Reflectable
        public void h() {
        }
    }

    interface EmptySuperInterface {
    }

    static class EmptyInterfaceImplementor implements EmptySuperInterface {
        @Reflectable
        public void f() {
        }
    }

    static class ClassWithMethodReferringToUnusedInterface {
        @Reflectable
        public Function<Object, Object>[] f(Consumer<String> s) {
            return null;
        }
    }

    static class FirstClassWithVirtualMethod {
        @Reflectable
        public String f(int x) {
            return "f:" + x;
        }
    }

    static class SecondClassWithVirtualMethod {
        @Reflectable
        public String g(short x) {
            return "g:" + x;
        }
    }
    
    static class ClassWithoutInitializerWithStaticMethod {
        @Reflectable
        public static String foo() {
            return "result:foo";
        }
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @interface TestAnnot {
    }
}
