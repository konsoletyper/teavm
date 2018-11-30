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
package org.teavm.metaprogramming.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.teavm.metaprogramming.Metaprogramming.arrayClass;
import static org.teavm.metaprogramming.Metaprogramming.emit;
import static org.teavm.metaprogramming.Metaprogramming.exit;
import static org.teavm.metaprogramming.Metaprogramming.findClass;
import static org.teavm.metaprogramming.Metaprogramming.lazy;
import static org.teavm.metaprogramming.Metaprogramming.unsupportedCase;
import java.util.function.Consumer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.metaprogramming.CompileTime;
import org.teavm.metaprogramming.Meta;
import org.teavm.metaprogramming.ReflectClass;
import org.teavm.metaprogramming.Value;
import org.teavm.metaprogramming.reflect.ReflectField;
import org.teavm.metaprogramming.reflect.ReflectMethod;
import org.teavm.metaprogramming.test.subpackage.MetaprogrammingGenerator;

@CompileTime
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class MetaprogrammingTest {
    @Test
    public void works() {
        assertEquals("java.lang.Object".length() + 2, classNameLength(Object.class, 2));
        assertEquals("java.lang.Integer".length() + 3, classNameLength(Integer.valueOf(5).getClass(), 3));
    }

    @Meta
    static native int classNameLength(Class<?> cls, int add);
    static void classNameLength(ReflectClass<?> cls, Value<Integer> add) {
        if (cls != findClass(Object.class) && cls != findClass(Integer.class)) {
            unsupportedCase();
            return;
        }
        int length = cls.getName().length();
        exit(() -> length + add.get());
    }

    @Test
    public void getsField() {
        Context ctx = new Context();
        ctx.a = 2;
        ctx.b = 3;

        assertEquals(2, getField(ctx.getClass(), ctx));
    }

    @Meta
    private static native Object getField(Class<?> cls, Object obj);
    private static void getField(ReflectClass<Object> cls, Value<Object> obj) {
        if (cls.getAnnotation(MetaprogrammingClass.class) == null) {
            unsupportedCase();
            return;
        }
        ReflectField field = cls.getField("a");
        exit(() -> field.get(obj));
    }
    @Test
    public void setsField() {
        Context ctx = new Context();
        setField(ctx.getClass(), ctx, 3);

        assertEquals(3, ctx.a);
    }

    @Meta
    private static native void setField(Class<?> cls, Object obj, Object value);
    private static void setField(ReflectClass<Object> cls, Value<Object> obj, Value<Object> value) {
        if (cls.getAnnotation(MetaprogrammingClass.class) == null) {
            unsupportedCase();
            return;
        }
        ReflectField field = cls.getField("a");
        emit(() -> field.set(obj, value));
    }

    @Test
    public void methodInvoked() {
        assertEquals("debug!", callDebug(A.class, new A()));
        assertEquals("missing", callDebug(B.class, new B()));
        assertEquals("missing", callDebug(A.class, new A(), "foo", 23));
        assertEquals("debug!foo:23", callDebug(B.class, new B(), "foo", 23));
    }

    @Meta
    private static native String callDebug(Class<?> cls, Object obj);
    private static void callDebug(ReflectClass<?> cls, Value<Object> obj) {
        if (cls.getAnnotation(MetaprogrammingClass.class) == null) {
            unsupportedCase();
            return;
        }
        ReflectMethod method = cls.getMethod("debug");
        if (method == null) {
            exit(() -> "missing");
        } else {
            exit(() -> method.invoke(obj.get()));
        }
    }

    @Meta
    private static native String callDebug(Class<?> cls, Object obj, String a, int b);
    private static void callDebug(ReflectClass<?> cls, Value<Object> obj, Value<String> a, Value<Integer> b) {
        if (cls.getAnnotation(MetaprogrammingClass.class) == null) {
            unsupportedCase();
            return;
        }
        ReflectClass<String> stringClass = findClass(String.class);
        ReflectClass<Integer> intClass =  findClass(int.class);
        ReflectMethod method = cls.getMethod("debug", stringClass, intClass);
        if (method == null) {
            exit(() -> "missing");
        } else {
            exit(() -> method.invoke(obj.get(), a.get(), b.get()));
        }
    }

    @Test
    public void constructorInvoked() {
        assertEquals(C.class.getName(), callConstructor(C.class).getClass().getName());
        assertNull(callConstructor(D.class));

        assertNull(callConstructor(C.class, "foo", 23));

        D instance = (D) callConstructor(D.class, "foo", 23);
        assertEquals(D.class.getName(), instance.getClass().getName());
        assertEquals("foo", instance.a);
        assertEquals(23, instance.b);
    }

    @Meta
    private static native Object callConstructor(Class<?> type);
    private static void callConstructor(ReflectClass<?> type) {
        if (type.getAnnotation(MetaprogrammingClass.class) == null) {
            unsupportedCase();
            return;
        }
        ReflectMethod ctor = type.getMethod("<init>");
        if (ctor != null) {
            exit(() -> ctor.construct());
        } else {
            exit(() -> null);
        }
    }

    @Meta
    private static native Object callConstructor(Class<?> type, String a, int b);
    private static void callConstructor(ReflectClass<?> type, Value<String> a, Value<Integer> b) {
        if (type.getAnnotation(MetaprogrammingClass.class) == null) {
            unsupportedCase();
            return;
        }
        ReflectClass<String> stringClass = findClass(String.class);
        ReflectClass<Integer> intClass = findClass(int.class);
        ReflectMethod ctor = type.getMethod("<init>", stringClass, intClass);
        if (ctor != null) {
            exit(() -> ctor.construct(a, b));
        } else {
            exit(() -> null);
        }
    }

    @Test
    public void capturesArray() {
        assertEquals("23:foo", captureArray(23, "foo"));
    }

    @Meta
    private static native String captureArray(int a, String b);
    private static void captureArray(Value<Integer> a, Value<String> b) {
        Value<?>[] array = { a, emit(() -> ":"), b };
        exit(() -> String.valueOf(array[0].get()) + array[1].get() + array[2].get());
    }

    @Test
    public void isInstanceWorks() {
        assertTrue(isInstance(new Context(), Context.class));
        assertFalse(isInstance(23, Context.class));
    }

    @Meta
    private static native boolean isInstance(Object obj, Class<?> type);
    private static void isInstance(Value<Object> obj, ReflectClass<?> type) {
        if (type.getAnnotation(MetaprogrammingClass.class) == null) {
            unsupportedCase();
            return;
        }
        exit(() -> type.isInstance(obj.get()));
    }

    @Test
    public void capturesNull() {
        assertEquals("foo:", captureArgument("foo"));
    }

    @Meta
    private static native String captureArgument(String a);
    private static void captureArgument(Value<String> a) {
        exit(() -> a.get() + ":");
    }

    @Test
    public void annotationsWork() {
        assertEquals(""
                + "foo:23:Object\n"
                + "foo=!:42:String:int\n"
                + "f=!:23\n",
                readAnnotations(WithAnnotations.class, new WithAnnotations()));
    }

    @Meta
    private static native String readAnnotations(Class<?> cls, Object obj);
    private static void readAnnotations(ReflectClass<Object> cls, Value<Object> obj) {
        if (cls.getAnnotation(MetaprogrammingClass.class) == null) {
            unsupportedCase();
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(describeAnnotation(cls.getAnnotation(TestAnnotation.class))).append('\n');
        for (ReflectMethod method : cls.getDeclaredMethods()) {
            TestAnnotation annot = method.getAnnotation(TestAnnotation.class);
            if (annot == null) {
                continue;
            }
            sb.append(method.getName()).append('=').append(describeAnnotation(annot)).append('\n');
        }
        for (ReflectField field : cls.getDeclaredFields()) {
            TestAnnotation annot = field.getAnnotation(TestAnnotation.class);
            if (annot == null) {
                continue;
            }
            sb.append(field.getName()).append('=').append(describeAnnotation(annot)).append('\n');
        }
        String result = sb.toString();
        exit(() -> result);
    }

    private static String describeAnnotation(TestAnnotation annot) {
        StringBuilder sb = new StringBuilder();
        sb.append(annot.a()).append(':').append(annot.b());
        for (Class<?> cls : annot.c()) {
            sb.append(':').append(cls.getSimpleName());
        }
        return sb.toString();
    }

    @Test
    public void compileTimeAnnotationRespectsPackage() {
        assertEquals("(foo)", compileTimePackage(true));
    }

    @Meta
    private static native String compileTimePackage(boolean ignoreMe);
    private static void compileTimePackage(Value<Boolean> ignoreMe) {
        Value<String> result = new MetaprogrammingGenerator().addParentheses("foo");
        exit(() -> result.get());
    }

    @Test
    public void compileTimeAnnotationRespectsClass() {
        assertEquals("[foo]", compileTimeClass(true));
    }

    @Meta
    private static native String compileTimeClass(boolean ignoreMe);
    private static void compileTimeClass(Value<Boolean> ignoreMe) {
        Value<String> result = new MetaprogrammingGenerator2().addParentheses("foo");
        exit(() -> result.get());
    }

    @Test
    public void compileTimeAnnotationRespectsNestedClass() {
        assertEquals("{foo}", compileTimeNestedClass(true));
    }

    @Meta
    private static native String compileTimeNestedClass(boolean ignoreMe);
    private static void compileTimeNestedClass(Value<Boolean> ignoreMe) {
        Value<String> result = new MetaprogrammingGenerator3().addParentheses("foo");
        exit(() -> result.get());
    }

    @Test
    public void emitsClassLiteralFromReflectClass() {
        assertEquals(String[].class.getName(), emitClassLiteral(String.class));
    }

    @Meta
    private static native String emitClassLiteral(Class<?> cls);
    private static void emitClassLiteral(ReflectClass<?> cls) {
        if (!cls.isAssignableFrom(String.class)) {
            unsupportedCase();
            return;
        }
        ReflectClass<?> arrayClass = arrayClass(cls);
        exit(() -> arrayClass.asJavaClass().getName());
    }

    @Test
    public void createsArrayViaReflection() {
        Object array = createArrayOfType(String.class, 10);
        assertEquals(String[].class, array.getClass());
        assertEquals(10, ((String[]) array).length);
    }

    @Meta
    private static native Object createArrayOfType(Class<?> cls, int size);
    private static void createArrayOfType(ReflectClass<?> cls, Value<Integer> size) {
        if (!cls.isAssignableFrom(String.class)) {
            unsupportedCase();
            return;
        }
        exit(() -> cls.createArray(size.get()));
    }

    @Test
    public void getsArrayElementViaReflection() {
        assertEquals("foo", getArrayElement(String[].class, new String[] { "foo" }, 0));
    }

    @Meta
    private static native Object getArrayElement(Class<?> type, Object array, int index);
    private static void getArrayElement(ReflectClass<?> type, Value<Object> array, Value<Integer> index) {
        if (!type.isAssignableFrom(String[].class)) {
            unsupportedCase();
            return;
        }
        exit(() -> type.getArrayElement(array.get(), index.get()));
    }

    @Test
    public void lazyWorks() {
        WithSideEffect a = new WithSideEffect(10);
        WithSideEffect b = new WithSideEffect(20);
        assertEquals(1, withLazy(a, b));
        assertEquals(1, a.reads);
        assertEquals(0, b.reads);

        a = new WithSideEffect(-10);
        b = new WithSideEffect(20);
        assertEquals(1, withLazy(a, b));
        assertEquals(1, a.reads);
        assertEquals(1, b.reads);

        a = new WithSideEffect(-10);
        b = new WithSideEffect(-20);
        assertEquals(2, withLazy(a, b));
        assertEquals(1, a.reads);
        assertEquals(1, b.reads);
    }

    @Meta
    private static native int withLazy(WithSideEffect a, WithSideEffect b);
    private static void withLazy(Value<WithSideEffect> a, Value<WithSideEffect> b) {
        Value<Boolean> first = lazy(() -> a.get().getValue() > 0);
        Value<Boolean> second = lazy(() -> b.get().getValue() > 0);
        exit(() -> first.get() || second.get() ? 1 : 2);
    }

    @Test
    public void conditionalWorks() {
        assertEquals("int", fieldType(Context.class, "a"));
        assertEquals("int", fieldType(Context.class, "b"));
        assertNull(fieldType(Context.class, "c"));
    }

    @Meta
    private static native String fieldType(Class<?> cls, String name);
    private static void fieldType(ReflectClass<Object> cls, Value<String> name) {
        if (cls.getAnnotation(MetaprogrammingClass.class) == null) {
            unsupportedCase();
            return;
        }

        Value<String> result = lazy(() -> null);
        for (ReflectField field : cls.getDeclaredFields()) {
            String type = field.getType().getName();
            String fieldName = field.getName();
            Value<String> existing = result;
            result = lazy(() -> fieldName.equals(name.get()) ? type : existing.get());
        }
        Value<String> type = result;
        exit(() -> type.get());
    }

    @Test
    public void conditionalActionWorks() {
        class TypeConsumer implements Consumer<String> {
            String type;
            @Override public void accept(String t) {
                type = t;
            }
        }
        TypeConsumer consumer = new TypeConsumer();

        fieldType(Context.class, "a", consumer);
        assertEquals("int", consumer.type);

        fieldType(Context.class, "b", consumer);
        assertEquals("int", consumer.type);

        fieldType(Context.class, "c", consumer);
        assertNull(consumer.type);
    }

    @Meta
    private static native void fieldType(Class<?> cls, String name, Consumer<String> typeConsumer);
    private static void fieldType(ReflectClass<Object> cls, Value<String> name, Value<Consumer<String>> typeConsumer) {
        if (cls.getAnnotation(MetaprogrammingClass.class) == null) {
            unsupportedCase();
            return;
        }

        Value<Void> result = lazy(() -> {
            typeConsumer.get().accept(null);
            return null;
        });
        for (ReflectField field : cls.getDeclaredFields()) {
            String type = field.getType().getName();
            String fieldName = field.getName();
            Value<Void> existing = result;
            result = lazy(() -> {
                if (fieldName.equals(name.get())) {
                    typeConsumer.get().accept(type);
                    return null;
                } else {
                    return existing.get();
                }
            });
        }
        Value<Void> type = result;
        emit(() -> type);
    }

    @Test
    public void unassignedLazyEvaluated() {
        withUnassignedLazy(Context.class);
        assertEquals(23, counter);
    }

    @Meta
    private static native void withUnassignedLazy(Class<?> cls);
    private static void withUnassignedLazy(ReflectClass<Object> cls) {
        if (cls.getAnnotation(MetaprogrammingClass.class) == null) {
            unsupportedCase();
            return;
        }

        emit(() -> counter = 42);
        Value<Object> value = lazy(() -> counter = 23);
        emit(() -> {
            value.get();
        });
    }

    private static int counter;

    @Test
    public void arrayTypeSelected() {
        assertEquals(String[].class, createInstance(String.class, 1).getClass());
        assertEquals(String[][].class, createInstance(String[].class, 1).getClass());
    }

    @Meta
    private static native Object createInstance(Class<?> cls, int size);
    private static void createInstance(ReflectClass<?> cls, Value<Integer> size) {
        if (!cls.isAssignableFrom(String.class) && !cls.isAssignableFrom(String[].class)) {
            unsupportedCase();
            return;
        }
        exit(() -> cls.createArray(size.get()));
    }

    @MetaprogrammingClass
    static class Context {
        public int a;
        public int b;
    }

    @MetaprogrammingClass
    class A {
        public String debug() {
            return "debug!";
        }
    }

    @MetaprogrammingClass
    class B {
        public String debug(String a, int b) {
            return "debug!" + a + ":" + b;
        }
    }

    @MetaprogrammingClass
    static class C {
        public C() {
        }
    }

    @MetaprogrammingClass
    static class D {
        String a;
        int b;

        public D(String a, int b) {
            this.a = a;
            this.b = b;
        }
    }

    @TestAnnotation(a = "foo", c = Object.class)
    @MetaprogrammingClass
    static class WithAnnotations {
        @TestAnnotation(c = {})
        int f;

        @TestAnnotation(b = 42, c = { String.class, int.class })
        int foo() {
            return 0;
        }
    }

    static class MetaprogrammingGenerator3 {
        public Value<String> addParentheses(String value) {
            return emit(() -> "{" + value + "}");
        }
    }

    static class WithSideEffect {
        private int value;
        public int reads;

        public WithSideEffect(int value) {
            this.value = value;
        }

        public int getValue() {
            ++reads;
            return value;
        }
    }
}
