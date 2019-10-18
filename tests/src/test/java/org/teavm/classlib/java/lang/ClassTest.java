/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.classlib.java.lang;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class ClassTest {
    @Test
    public void classNameEvaluated() {
        assertEquals("java.lang.Object", Object.class.getName());
        assertEquals("[Ljava.lang.Object;", Object[].class.getName());
        assertEquals("int", int.class.getName());
        assertEquals("[I", int[].class.getName());
    }

    @Test
    public void classSimpleNameEvaluated() {

    }

    @Test
    public void objectClassNameEvaluated() {
        assertEquals("java.lang.Object", new Object().getClass().getName());
    }

    @Test
    public void superClassFound() {
        assertEquals(Number.class, Integer.class.getSuperclass());
    }

    @Test
    public void superClassOfObjectIsNull() {
        assertNull(Object.class.getSuperclass());
    }

    @Test
    public void superClassOfArrayIsObject() {
        assertEquals(Object.class, Runnable[].class.getSuperclass());
    }

    @Test
    public void superClassOfPrimitiveIsNull() {
        assertNull(int.class.getSuperclass());
    }

    @Test
    public void objectClassConsideredNotArray() {
        assertFalse(Object.class.isArray());
    }

    @Test
    public void arrayClassConsideredArray() {
        assertTrue(Object[].class.isArray());
    }

    @Test
    public void arrayComponentTypeDetected() {
        assertEquals(Object.class, Object[].class.getComponentType());
    }

    @Test
    public void arrayOfArraysComponentTypeDetected() {
        assertEquals(Object[].class, Object[][].class.getComponentType());
    }

    @Test
    public void nonArrayComponentTypeIsNull() {
        assertNull(Object.class.getComponentType());
    }

    @Test
    public void castingAppropriateObject() {
        Object obj = 23;
        assertEquals(Integer.valueOf(23), Integer.class.cast(obj));
    }

    @Test(expected = ClassCastException.class)
    public void inappropriateObjectCastingFails() {
        Object obj = 23;
        Float.class.cast(obj);
    }

    @Test
    public void instanceCreatedThroughReflection() throws Exception {
        Runnable instance = (Runnable) Class.forName(TestObject.class.getName()).newInstance();
        instance.run();
        assertEquals(TestObject.class, instance.getClass());
        assertEquals(1, ((TestObject) instance).getCounter());
    }

    @Test
    public void instanceCreatedThroughReflectionAsync() throws Exception {
        Runnable instance = TestObjectAsync.class.newInstance();
        instance.run();
        assertEquals(TestObjectAsync.class, instance.getClass());
        assertEquals(2, ((TestObjectAsync) instance).getCounter());
    }

    @Test
    public void classProperties() {
        class B {
        }

        @SuppressWarnings("Convert2Lambda")
        Runnable r = new Runnable() {
            @Override
            public void run() {
            }
        };

        String testName = "org.teavm.classlib.java.lang.ClassTest";

        testClassProperties(getClass(), "ClassTest", testName, null, null);
        testClassProperties(new ClassTest[0].getClass(), "ClassTest[]", testName + "[]", null, null);
        testClassProperties(int.class, "int", "int", null, null);
        testClassProperties(new int[0].getClass(), "int[]", "int[]", null, null);
        testClassProperties(new A().getClass(), "A", testName + ".A", ClassTest.class, ClassTest.class);
        testClassProperties(new A[0].getClass(), "A[]", testName + ".A[]", null, null);
        testClassProperties(new B().getClass(), "B", null, null, ClassTest.class);
        testClassProperties(new B[0].getClass(), "B[]", null, null, null);
        testClassProperties(r.getClass(), "", null, null, ClassTest.class);
    }

    private void testClassProperties(Class<?> cls, String expectedSimpleName, String expectedCanonicalName,
            Class<?> expectedDeclaringClass, Class<?> expectedEnclosingClass) {
        assertEquals(expectedSimpleName, cls.getSimpleName());
        assertEquals(expectedCanonicalName, cls.getCanonicalName());
        assertEquals(expectedDeclaringClass, cls.getDeclaringClass());
        assertEquals(expectedEnclosingClass, cls.getEnclosingClass());
    }

    @Test
    public void annotationsExposed() {
        Annotation[] annotations = A.class.getAnnotations();
        assertEquals(1, annotations.length);
        assertTrue(TestAnnot.class.isAssignableFrom(annotations[0].getClass()));
    }

    @Test
    public void annotationFieldsExposed() {
        AnnotWithDefaultField annot = B.class.getAnnotation(AnnotWithDefaultField.class);
        assertEquals(2, annot.x());
        annot = C.class.getAnnotation(AnnotWithDefaultField.class);
        assertEquals(3, annot.x());
    }

    @Test
    public void annotationFieldTypesSupported() {
        AnnotWithVariousFields annot = D.class.getAnnotation(AnnotWithVariousFields.class);
        assertEquals(true, annot.a());
        assertEquals((byte) 2, annot.b());
        assertEquals((short) 3, annot.c());
        assertEquals(4, annot.d());
        assertEquals(5L, annot.e());
        assertEquals(6.5, annot.f(), 0.01);
        assertEquals(7.2, annot.g(), 0.01);
        assertArrayEquals(new int[] { 2, 3 }, annot.h());
        assertEquals(RetentionPolicy.CLASS, annot.i());
        assertEquals(Retention.class, annot.j().annotationType());
        assertEquals(1, annot.k().length);
        assertEquals(RetentionPolicy.RUNTIME, annot.k()[0].value());
        assertEquals("foo", annot.l());
        assertArrayEquals(new String[] { "bar" }, annot.m());
        assertEquals(Integer.class, annot.n());
    }

    @TestAnnot
    private static class A {
    }

    @AnnotWithDefaultField
    private static class B {
    }

    @AnnotWithDefaultField(x = 3)
    private static class C {
    }

    @AnnotWithVariousFields(a = true, b = 2, c = 3, d = 4, e = 5, f = 6.5f, g = 7.2, h = { 2, 3 },
            i = RetentionPolicy.CLASS, j = @Retention(RetentionPolicy.SOURCE),
            k = { @Retention(RetentionPolicy.RUNTIME) }, l = "foo", m = "bar", n = Integer.class)
    private static class D {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface TestAnnot {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface AnnotWithDefaultField {
        int x() default 2;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface AnnotWithVariousFields {
        boolean a();

        byte b();

        short c();

        int d();

        long e();

        float f();

        double g();

        int[] h();

        RetentionPolicy i();

        Retention j();

        Retention[] k();

        String l();

        String[] m();

        Class<?> n();
    }

    static class InnerClass {
    }
}
