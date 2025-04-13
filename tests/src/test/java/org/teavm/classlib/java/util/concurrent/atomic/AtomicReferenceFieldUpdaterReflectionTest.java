/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.classlib.java.util.concurrent.atomic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.classlib.support.Reflectable;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@SkipPlatform({ TestPlatform.C, TestPlatform.WEBASSEMBLY, TestPlatform.WASI })
public class AtomicReferenceFieldUpdaterReflectionTest {
    private Class<ClassWithField> getInstanceType() {
        return ClassWithField.class;
    }

    @Test
    public void getSet() {
        var updater = AtomicReferenceFieldUpdater.newUpdater(getInstanceType(), ValueClass.class, "value");
        var obj = new ClassWithField();
        var a = new ValueClass("a");
        var b = new ValueClass("b");
        var c = new ValueClass("c");
        obj.value = a;

        assertSame(a, updater.get(obj));
        updater.set(obj, b);
        assertSame(b, obj.value);

        assertSame(b, updater.getAndSet(obj, a));
        assertSame(a, obj.value);

        assertFalse(updater.compareAndSet(obj, b, c));
        assertSame(a, obj.value);
        assertTrue(updater.compareAndSet(obj, a, c));
        assertSame(c, obj.value);

        assertSame(c, updater.getAndUpdate(obj, v -> new ValueClass(v.v + "1")));
        assertEquals("c1", obj.value.v);

        assertEquals("c11", updater.updateAndGet(obj, v -> new ValueClass(v.v + "1")).v);
        assertEquals("c11", obj.value.v);

        assertEquals("c11", updater.getAndAccumulate(obj, b, (x, y) -> new ValueClass(x.v + "," + y.v)).v);
        assertEquals("c11,b", obj.value.v);

        assertEquals("c11,b,a", updater.accumulateAndGet(obj, a, (x, y) -> new ValueClass(x.v + "," + y.v)).v);
        assertEquals("c11,b,a", obj.value.v);
    }

    @Test
    public void wrongType() {
        try {
            AtomicReferenceFieldUpdater.newUpdater(getInstanceType(), String.class, "value");
            fail("Expected exception not thrown");
        } catch (ClassCastException e) {
            // ok
        }
    }

    @Test
    public void nonVolatileField() {
        try {
            AtomicReferenceFieldUpdater.newUpdater(getInstanceType(), ValueClass.class, "nonVolatileValue");
            fail("Expected exception not thrown");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    @Test
    public void primitiveField() {
        try {
            AtomicReferenceFieldUpdater.newUpdater(getInstanceType(), int.class, "intValue");
            fail("Expected exception not thrown");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    @Test
    public void staticField() {
        try {
            AtomicReferenceFieldUpdater.newUpdater(getInstanceType(), ValueClass.class, "staticValue");
            fail("Expected exception not thrown");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    @Test
    public void wrongFieldName() {
        try {
            AtomicReferenceFieldUpdater.newUpdater(getInstanceType(), ValueClass.class, "foo");
            fail("Expected exception not thrown");
        } catch (RuntimeException e) {
            assertEquals(NoSuchFieldException.class, e.getCause().getClass());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void invalidClass() {
        var updater = (AtomicReferenceFieldUpdater<?, ?>) AtomicReferenceFieldUpdater.newUpdater(
                getInstanceType(), ValueClass.class, "value");
        var objUpdater = (AtomicReferenceFieldUpdater<Object, ValueClass>) updater;
        try {
            objUpdater.set(new Object(), new ValueClass(""));
            fail("Expected exception not thrown");
        } catch (ClassCastException e) {
            // ok
        }

        try {
            objUpdater.set(null, new ValueClass(""));
            fail("Expected exception not thrown");
        } catch (ClassCastException e) {
            // ok
        }
    }

    static class ClassWithField {
        @Reflectable
        volatile ValueClass value;

        @Reflectable
        ValueClass nonVolatileValue;

        @Reflectable
        volatile int intValue;

        @Reflectable
        static volatile ValueClass staticValue;
    }

    private static class ValueClass {
        String v;

        ValueClass(String v) {
            this.v = v;
        }
    }
}
