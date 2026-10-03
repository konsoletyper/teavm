/*
 *  Copyright 2024 Alexey Andreev.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Set;
import java.util.WeakHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.teavm.classlib.support.GCSupport;
import org.teavm.classlib.support.MapTest2Support;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTest;
import org.teavm.junit.TestPlatform;

@TeaVMTest
@SkipPlatform(TestPlatform.WEBASSEMBLY_GC)
public class WeakHashMapTest {
    static class MockMap<K, V> extends AbstractMap<K, V> {
        @Override
        public Set<Entry<K, V>> entrySet() {
            return null;
        }
        @Override
        public int size() {
            return 0;
        }
    }

    Object[] keyArray = new Object[100];
    Object[] valueArray = new Object[100];
    WeakHashMap<Object, Object> whm;

    @Test
    public void constructor() {
        new MapTest2Support(new WeakHashMap<>()).runTest();

        whm = new WeakHashMap<>();
        for (int i = 0; i < 100; i++) {
            whm.put(keyArray[i], valueArray[i]);
        }
        for (int i = 0; i < 100; i++) {
            assertSame(valueArray[i], whm.get(keyArray[i]), "Incorrect value retrieved");
        }
    }

    @Test
    public void constructorI() {
        whm = new WeakHashMap<>(50);
        for (int i = 0; i < 100; i++) {
            whm.put(keyArray[i], valueArray[i]);
        }
        for (int i = 0; i < 100; i++) {
            assertSame(valueArray[i], whm.get(keyArray[i]), "Incorrect value retrieved");
        }

        var empty = new WeakHashMap<>(0);
        assertNull(empty.get("nothing"), "Empty weakhashmap access");
        empty.put("something", "here");
        assertSame("here", empty.get("something"), "cannot get element");
    }

    @Test
    public void constructorIF() {
        whm = new WeakHashMap<>(50, 0.5f);
        for (int i = 0; i < 100; i++) {
            whm.put(keyArray[i], valueArray[i]);
        }
        for (int i = 0; i < 100; i++) {
            assertSame(valueArray[i], whm.get(keyArray[i]), "Incorrect value retrieved");
        }

        var empty = new WeakHashMap<>(0, 0.75f);
        assertNull(empty.get("nothing"), "Empty hashtable access");
        empty.put("something", "here");
        assertSame("here", empty.get("something"), "cannot get element");
    }

    @Test
    public void constructorLjava_util_Map() {
        var map = new WeakHashMap<>(new MockMap<>());
        assertEquals(0, map.size(), "Size should be 0");
    }

    @Test
    public void clearMethod() {
        whm = new WeakHashMap<>();
        for (int i = 0; i < 100; i++) {
            whm.put(keyArray[i], valueArray[i]);
        }
        whm.clear();
        assertTrue(whm.isEmpty(), "Cleared map should be empty");
        for (int i = 0; i < 100; i++) {
            assertNull(whm.get(keyArray[i]), "Cleared map should only return null");
        }

    }

    @Test
    public void containsKey() {
        whm = new WeakHashMap<>();
        for (int i = 0; i < 100; i++) {
            whm.put(keyArray[i], valueArray[i]);
        }
        for (int i = 0; i < 100; i++) {
            assertTrue(whm.containsKey(keyArray[i]), "Should contain referenced key");
        }
        keyArray[25] = null;
        keyArray[50] = null;
    }

    @Test
    public void containsValue() {
        whm = new WeakHashMap<>();
        for (int i = 0; i < 100; i++) {
            whm.put(keyArray[i], valueArray[i]);
        }
        for (int i = 0; i < 100; i++) {
            assertTrue(whm.containsValue(valueArray[i]), "Should contain referenced value");
        }
        keyArray[25] = null;
        keyArray[50] = null;
    }

    @Test
    public void entrySet() {
        var weakMap = new WeakHashMap<>();
        for (int i = 0; i < 100; i++) {
            weakMap.put(keyArray[i], valueArray[i]);
        }

        var keys = Arrays.asList(keyArray);
        var values = Arrays.asList(valueArray);

        // Check the entry set has correct size & content
        var entrySet = weakMap.entrySet();
        assertEquals(100, entrySet.size(), "Assert 0: Incorrect number of entries returned");
        var it = entrySet.iterator();
        while (it.hasNext()) {
            var entry = it.next();
            assertTrue(keys.contains(entry.getKey()), "Assert 1: Invalid map entry key returned");
            assertTrue(values.contains(entry.getValue()), "Assert 2: Invalid map entry value returned");
            assertTrue(entrySet.contains(entry), "Assert 3: Entry not in entry set");
        }

        // Dereference a single key, then try to
        // force a collection of the weak ref'd obj
        keyArray[50] = null;
        GCSupport.tryToTriggerGC();

        assertEquals(99, entrySet.size(), "Assert 4: Incorrect number of entries after gc");
        assertSame(entrySet.iterator().next(), entrySet.iterator().next(), "Assert 5: Entries not identical");

        // remove alternate entries using the iterator, and ensure the
        // iteration count is consistent
        int size = entrySet.size();
        it = entrySet.iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
            size--;
            if (it.hasNext()) {
                it.next();
            }

        }
        assertEquals(size, entrySet.size(), "Assert 6: entry set count mismatch");

        int entries = 0;
        it = entrySet.iterator();
        while (it.hasNext()) {
            it.next();
            entries++;
        }
        assertEquals(size, entries, "Assert 6: count mismatch");

        it = entrySet.iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
        assertEquals(0, entrySet.size(), "Assert 7: entry set not empty");
        assertFalse(entrySet.iterator().hasNext(), "Assert 8:  iterator not empty");
    }

    @Test
    public void entrySet2() {
        whm = new WeakHashMap<>();
        for (int i = 0; i < 100; i++) {
            whm.put(keyArray[i], valueArray[i]);
        }
        var keys = Arrays.asList(keyArray);
        var values = Arrays.asList(valueArray);
        var entrySet = whm.entrySet();
        assertEquals(100, entrySet.size(), "Incorrect number of entries returned--wanted 100, got: " + entrySet.size());
        for (var entry : entrySet) {
            assertTrue(keys.contains(entry.getKey()), "Invalid map entry returned--bad key");
            assertTrue(values.contains(entry.getValue()), "Invalid map entry returned--bad key");
        }
        keys = null;
        values = null;
        keyArray[50] = null;

        GCSupport.tryToTriggerGC();

        assertEquals(99, entrySet.size(),
                "Incorrect number of entries returned after gc--wanted 99, got: " + entrySet.size());
    }

    @Test
    public void get() {
        assertTrue(true, "Used to test");
    }

    @Test
    public void isEmpty() {
        whm = new WeakHashMap<>();
        assertTrue(whm.isEmpty(), "New map should be empty");
        Object myObject = new Object();
        whm.put(myObject, myObject);
        assertFalse(whm.isEmpty(), "Map should not be empty");
        whm.remove(myObject);
        assertTrue(whm.isEmpty(), "Map with elements removed should be empty");
    }

    @Test
    public void put() {
        var map = new WeakHashMap<>();
        map.put(null, "value"); // add null key
        GCSupport.tryToTriggerGC();
        map.remove("nothing"); // Cause objects in queue to be removed
        assertEquals(1, map.size(), "null key was removed");
    }

    @Test
    public void putAll() {
        var mockMap = new MockMap<>();
        var map = new WeakHashMap<>();
        map.putAll(mockMap);
        assertEquals(0, map.size(), "Size should be 0");
    }

    @Test
    public void remove() {
        whm = new WeakHashMap<>();
        for (int i = 0; i < 100; i++) {
            whm.put(keyArray[i], valueArray[i]);
        }

        assertSame(valueArray[25], whm.remove(keyArray[25]), "Remove returned incorrect value");
        assertNull(whm.remove(keyArray[25]), "Remove returned incorrect value");
        assertEquals(99, whm.size(), "Size should be 99 after remove");
    }

    @Test
    public void size() {
        assertTrue(true, "Used to test");
    }

    @Test
    public void keySet() {
        whm = new WeakHashMap<>();
        for (int i = 0; i < 100; i++) {
            whm.put(keyArray[i], valueArray[i]);
        }

        var keys = Arrays.asList(keyArray);
        var values = Arrays.asList(valueArray);

        var keySet = whm.keySet();
        assertEquals(100, keySet.size(), "Incorrect number of keys returned,");
        for (var key : keySet) {
            assertTrue(keys.contains(key), "Invalid map entry returned--bad key");
        }
        keys = null;
        values = null;
        keyArray[50] = null;

        GCSupport.tryToTriggerGC();

        assertEquals(99, keySet.size(), "Incorrect number of keys returned after gc,");
    }

    @Test
    public void keySetHasNext() {
        var map = new WeakHashMap<>();
        var cl = new ConstantHashClass(2);
        map.put(new ConstantHashClass(1), null);
        map.put(cl, null);
        map.put(new ConstantHashClass(3), null);
        var iter = map.keySet().iterator();
        iter.next();
        iter.next();
        GCSupport.tryToTriggerGC();
        assertFalse(iter.hasNext(), "Wrong hasNext() value");
    }

    static class ConstantHashClass {
        private int id;

        public ConstantHashClass(int id) {
            this.id = id;
        }

        public int hashCode() {
            return 0;
        }

        public String toString() {
            return "ConstantHashClass[id=" + id + "]";
        }
    }


    @Test
    public void values() {
        whm = new WeakHashMap<>();
        for (int i = 0; i < 100; i++) {
            whm.put(keyArray[i], valueArray[i]);
        }

        var keys = Arrays.asList(keyArray);
        var values = Arrays.asList(valueArray);

        var valuesCollection = whm.values();
        assertEquals(100, valuesCollection.size(), "Incorrect number of keys returned,");
        for (Object value : valuesCollection) {
            assertTrue(values.contains(value), "Invalid map entry returned--bad value");
        }
        keys = null;
        values = null;
        keyArray[50] = null;

        GCSupport.tryToTriggerGC();

        assertEquals(99, valuesCollection.size(), "Incorrect number of keys returned after gc");
    }

    @BeforeEach
    public void setUp() {
        for (int i = 0; i < 100; i++) {
            keyArray[i] = new Object();
            valueArray[i] = new Object();
        }
    }

}
