/*
 *  Copyright 2026 Alexey Andreev.
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class ClassValueTest {
    private static int computations;

    private static final ClassValue<String> NAMES = new ClassValue<>() {
        @Override
        protected String computeValue(Class<?> type) {
            computations++;
            return "value for " + type.getName();
        }
    };

    @Test
    public void removeEvictsCachedValue() {
        computations = 0;
        assertEquals("value for java.lang.String", NAMES.get(String.class));
        assertEquals("value for java.lang.String", NAMES.get(String.class));
        assertEquals(1, computations);

        NAMES.remove(String.class);
        assertEquals("value for java.lang.String", NAMES.get(String.class));
        assertEquals(2, computations);
    }

    @Test
    public void removeOfAbsentClassIsNoOp() {
        NAMES.remove(Integer.class);
    }

    @Test
    public void removeCalledThroughInterface() {
        // the shape of scala.runtime.ClassValueCompat (Scala 2.13): remove()
        // is inherited from ClassValue to satisfy an interface and called
        // through that interface
        CacheInterface<String> cache = new Cache();
        assertEquals("computed java.lang.String", cache.get(String.class));
        cache.remove(String.class);
        assertEquals("computed java.lang.String", cache.get(String.class));
    }

    interface CacheInterface<T> {
        T get(Class<?> cls);

        void remove(Class<?> cls);
    }

    static class Cache extends ClassValue<String> implements CacheInterface<String> {
        @Override
        protected String computeValue(Class<?> type) {
            return "computed " + type.getName();
        }
    }
}
