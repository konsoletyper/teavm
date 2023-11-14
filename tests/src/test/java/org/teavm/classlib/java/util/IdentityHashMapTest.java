/*
 *  Copyright 2014 Alexey Andreev.
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

/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.teavm.classlib.java.util;

import static org.junit.Assert.assertEquals;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class IdentityHashMapTest {

    @Test
    public void sameObjectOnce() {
        Map<Object, Object> m = new IdentityHashMap<>();
        Object key = new Object();
        m.put(key, "val1");
        m.put(key, "val2");
        assertEquals(1, m.keySet().size());
    }

    @Test
    public void twoObjectsTwice() {
        Map<Object, Object> m = new IdentityHashMap<>();
        Object key1 = new Object();
        Object key2 = new Object();
        m.put(key1, "val1");
        m.put(key2, "val2");
        assertEquals(2, m.size());
        assertEquals("val1", m.get(key1));
        assertEquals("val2", m.get(key2));
    }

    @Test
    public void twoObjectsWithSameHashCode() {
        Map<A, Object> m = new IdentityHashMap<>();
        A key1 = new A("ciao");
        A key2 = new A("ciao");
        m.put(key1, "val1");
        m.put(key2, "val2");
        assertEquals(2, m.size());
        assertEquals("val1", m.get(key1));
        assertEquals("val2", m.get(key2));
    }

    @Test
    public void nullKeyAllowed() {
        Map<String, String> m = new IdentityHashMap<>();
        m.put(null, "val1");
        m.put(null, "val2");
        assertEquals(1, m.size());
        assertEquals("val2", m.get(null));
    }

    static class A {
        public A(String b) {
            this.b = b;
        }

        private String b;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            A a = (A) o;
            return Objects.equals(b, a.b);
        }

        @Override
        public int hashCode() {
            return Objects.hash(b);
        }
    }
}
