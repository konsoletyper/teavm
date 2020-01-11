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
import static org.junit.Assert.assertTrue;
import java.lang.reflect.Field;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.classlib.support.Reflectable;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class FieldTest {
    @Test
    public void fieldsEnumerated() {
        new ReflectableType();
        StringBuilder sb = new StringBuilder();
        for (Field field : ReflectableType.class.getDeclaredFields()) {
            sb.append(field).append(";");
        }
        assertEquals(""
                + "public int org.teavm.classlib.java.lang.reflect.FieldTest$ReflectableType.a;"
                + "private boolean org.teavm.classlib.java.lang.reflect.FieldTest$ReflectableType.b;"
                + "java.lang.Object org.teavm.classlib.java.lang.reflect.FieldTest$ReflectableType.c;"
                + "java.lang.String org.teavm.classlib.java.lang.reflect.FieldTest$ReflectableType.d;"
                + "long org.teavm.classlib.java.lang.reflect.FieldTest$ReflectableType.e;"
                + "private static short org.teavm.classlib.java.lang.reflect.FieldTest$ReflectableType.f;"
                + "long org.teavm.classlib.java.lang.reflect.FieldTest$ReflectableType.g;"
                + "static boolean org.teavm.classlib.java.lang.reflect.FieldTest$ReflectableType.initialized;",
                sb.toString());
    }

    @Test
    public void fieldRead() throws NoSuchFieldException, IllegalAccessException {
        ReflectableType instance = new ReflectableType();
        Field field = instance.getClass().getDeclaredField("a");
        Object result = field.get(instance);
        assertEquals(23, result);
    }

    @Test
    public void fieldReadLong() throws NoSuchFieldException, IllegalAccessException {
        ReflectableType instance = new ReflectableType();
        Field field = instance.getClass().getDeclaredField("g");
        Object result = field.get(instance);
        assertEquals(Long.MAX_VALUE, result);
    }

    @Test
    public void fieldWritten() throws NoSuchFieldException, IllegalAccessException {
        ReflectableType instance = new ReflectableType();
        Field field = instance.getClass().getDeclaredField("a");
        field.set(instance, 234);
        assertEquals(234, instance.a);
    }

    @Test(expected = IllegalAccessException.class)
    @SkipJVM
    public void fieldCannotBeRead() throws NoSuchFieldException, IllegalAccessException {
        ReflectableType instance = new ReflectableType();
        Field field = instance.getClass().getDeclaredField("e");
        field.get(instance);
    }

    @Test(expected = IllegalAccessException.class)
    @SkipJVM
    public void fieldCannotBeWritten() throws NoSuchFieldException, IllegalAccessException {
        ReflectableType instance = new ReflectableType();
        Field field = instance.getClass().getDeclaredField("e");
        field.set(instance, 1L);
    }

    @Test
    public void staticFieldRead() throws NoSuchFieldException, IllegalAccessException {
        Field field = ReflectableType.class.getDeclaredField("f");
        field.setAccessible(true);
        Object result = field.get(null);
        assertTrue(ReflectableType.initialized);
        assertEquals(ReflectableType.f, result);
    }

    @Test
    public void staticFieldWritten() throws NoSuchFieldException, IllegalAccessException {
        Field field = ReflectableType.class.getDeclaredField("f");
        field.setAccessible(true);
        field.set(null, (short) 999);
        assertTrue(ReflectableType.initialized);
        assertEquals((short) 999, ReflectableType.f);
    }

    @Test
    public void dependencyMaintainedForGet() throws NoSuchFieldException, IllegalAccessException {
        ReflectableType instance = new ReflectableType();
        instance.c = new Foo(123);
        Field field = ReflectableType.class.getDeclaredField("c");
        Foo result = (Foo) field.get(instance);
        assertEquals(123, result.getValue());
    }

    @Test
    public void dependencyMaintainedForSet() throws NoSuchFieldException, IllegalAccessException {
        ReflectableType instance = new ReflectableType();
        Field field = ReflectableType.class.getDeclaredField("c");
        field.set(instance, new Foo(123));
        assertEquals(123, ((Foo) instance.c).getValue());
    }

    static class ReflectableType {
        @Reflectable public int a;
        @Reflectable private boolean b;
        @Reflectable Object c;
        @Reflectable String d;
        long e;
        @Reflectable private static short f = 99;
        @Reflectable long g;

        static boolean initialized = true;

        public ReflectableType() {
            a = 23;
            b = true;
            c = "foo";
            d = "bar";
            e = 42;
            g = Long.MAX_VALUE;
        }
    }

    static class Foo {
        int value;

        public Foo(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
