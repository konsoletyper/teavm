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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.classlib.support.Reflectable;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@SkipPlatform({ TestPlatform.C, TestPlatform.WEBASSEMBLY, TestPlatform.WASI })
public class AtomicLongFieldUpdaterReflectionTest {
    private Class<ClassWithField> getInstanceType() {
        return ClassWithField.class;
    }

    @Test
    public void getSet() {
        var updater = AtomicLongFieldUpdater.newUpdater(getInstanceType(), "value");
        var obj = new ClassWithField();
        obj.value = 1;

        assertEquals(1, updater.get(obj));
        updater.set(obj, 2);
        assertEquals(2, obj.value);

        assertEquals(2, updater.getAndSet(obj, 3));
        assertEquals(3, obj.value);

        assertFalse(updater.compareAndSet(obj, 2, 4));
        assertEquals(3, obj.value);
        assertTrue(updater.compareAndSet(obj, 3, 4));
        assertEquals(4, obj.value);

        assertEquals(4, updater.getAndIncrement(obj));
        assertEquals(5, obj.value);

        assertEquals(5, updater.getAndDecrement(obj));
        assertEquals(4, obj.value);

        assertEquals(4, updater.getAndAdd(obj, 2));
        assertEquals(6, obj.value);

        assertEquals(7, updater.incrementAndGet(obj));
        assertEquals(7, obj.value);

        assertEquals(6, updater.decrementAndGet(obj));
        assertEquals(6, obj.value);

        assertEquals(8, updater.addAndGet(obj, 2));
        assertEquals(8, obj.value);

        assertEquals(8, updater.getAndUpdate(obj, v -> v * 2));
        assertEquals(16, obj.value);

        assertEquals(8, updater.updateAndGet(obj, v -> v / 2));
        assertEquals(8, obj.value);

        assertEquals(8, updater.getAndAccumulate(obj, 3, (x, y) -> x * y));
        assertEquals(24, obj.value);

        assertEquals(48, updater.accumulateAndGet(obj, 2, (x, y) -> x * y));
        assertEquals(48, obj.value);
    }

    @Test
    public void nonVolatileField() {
        try {
            AtomicLongFieldUpdater.newUpdater(getInstanceType(), "nonVolatileValue");
            fail("Expected exception not thrown");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    @Test
    public void refField() {
        try {
            AtomicLongFieldUpdater.newUpdater(getInstanceType(), "refValue");
            fail("Expected exception not thrown");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    @Test
    public void staticField() {
        try {
            AtomicLongFieldUpdater.newUpdater(getInstanceType(), "staticValue");
            fail("Expected exception not thrown");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    @Test
    public void wrongFieldName() {
        try {
            AtomicLongFieldUpdater.newUpdater(getInstanceType(), "foo");
            fail("Expected exception not thrown");
        } catch (RuntimeException e) {
            assertEquals(NoSuchFieldException.class, e.getCause().getClass());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void invalidClass() {
        var updater = (AtomicLongFieldUpdater<?>) AtomicLongFieldUpdater.newUpdater(getInstanceType(), "value");
        var objUpdater = (AtomicLongFieldUpdater<Object>) updater;
        try {
            objUpdater.set(new Object(), 1);
            fail("Expected exception not thrown");
        } catch (ClassCastException e) {
            // ok
        }

        try {
            objUpdater.set(null, 2);
            fail("Expected exception not thrown");
        } catch (ClassCastException e) {
            // ok
        }
    }

    static class ClassWithField {
        @Reflectable
        volatile long value;

        @Reflectable
        long nonVolatileValue;

        @Reflectable
        volatile Object refValue;

        @Reflectable
        static volatile long staticValue;
    }
}
