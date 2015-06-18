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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.junit.Test;

/**
 *
 * @author Alexey Andreev
 */
public class ClassTest {
    @Test
    public void classNameEvaluated() {
        assertEquals("java.lang.Object", Object.class.getName());
        assertEquals("[Ljava.lang.Object;", Object[].class.getName());
        assertEquals("int", int.class.getName());
        assertEquals("[I", int[].class.getName());
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
        Runnable instance = (Runnable)Class.forName(TestObject.class.getName()).newInstance();
        instance.run();
        assertEquals(TestObject.class, instance.getClass());
        assertEquals(1, ((TestObject)instance).getCounter());
    }

    @Test
    public void declaringClassFound() {
        assertEquals(ClassTest.class, new A().getClass().getDeclaringClass());
    }

    @Test
    public void annotationsExposed() {
        Annotation[] annotations = A.class.getAnnotations();
        assertEquals(1, annotations.length);
        assertTrue(TestAnnot.class.isAssignableFrom(annotations[0].getClass()));
    }

    @TestAnnot
    private static class A {
    }

    @Retention(RetentionPolicy.RUNTIME)
    static @interface TestAnnot {
    }
}
