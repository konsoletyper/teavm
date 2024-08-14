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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Set;
import java.util.WeakHashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.classlib.support.GCSupport;
import org.teavm.classlib.support.MapTest2Support;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@SkipPlatform({TestPlatform.WEBASSEMBLY, TestPlatform.WASI})
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
            assertSame("Incorrect value retrieved", valueArray[i], whm.get(keyArray[i]));
        }
    }

    @Test
    public void constructorI() {
        whm = new WeakHashMap<>(50);
        for (int i = 0; i < 100; i++) {
            whm.put(keyArray[i], valueArray[i]);
        }
        for (int i = 0; i < 100; i++) {
            assertSame("Incorrect value retrieved", valueArray[i], whm.get(keyArray[i]));
        }

        var empty = new WeakHashMap<>(0);
        assertNull("Empty weakhashmap access", empty.get("nothing"));
        empty.put("something", "here");
        assertSame("cannot get element", "here", empty.get("something"));
    }

    @Test
    public void constructorIF() {
        whm = new WeakHashMap<>(50, 0.5f);
        for (int i = 0; i < 100; i++) {
            whm.put(keyArray[i], valueArray[i]);
        }
        for (int i = 0; i < 100; i++) {
            assertSame("Incorrect value retrieved", valueArray[i], whm.get(keyArray[i]));
        }

        var empty = new WeakHashMap<>(0, 0.75f);
        assertNull("Empty hashtable access", empty.get("nothing"));
        empty.put("something", "here");
        assertSame("cannot get element", "here", empty.get("something"));
    }

    @Test
    public void constructorLjava_util_Map() {
        var map = new WeakHashMap<>(new MockMap<>());
        assertEquals("Size should be 0", 0, map.size());
    }

    @Test
    public void clearMethod() {
        whm = new WeakHashMap<>();
        for (int i = 0; i < 100; i++) {
            whm.put(keyArray[i], valueArray[i]);
        }
        whm.clear();
        assertTrue("Cleared map should be empty", whm.isEmpty());
        for (int i = 0; i < 100; i++) {
            assertNull("Cleared map should only return null", whm.get(keyArray[i]));
        }

    }

    @Test
    public void containsKey() {
        whm = new WeakHashMap<>();
        for (int i = 0; i < 100; i++) {
            whm.put(keyArray[i], valueArray[i]);
        }
        for (int i = 0; i < 100; i++) {
            assertTrue("Should contain referenced key", whm.containsKey(keyArray[i]));
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
            assertTrue("Should contain referenced value", whm.containsValue(valueArray[i]));
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
        assertEquals("Assert 0: Incorrect number of entries returned", 100, entrySet.size());
        var it = entrySet.iterator();
        while (it.hasNext()) {
            var entry = it.next();
            assertTrue("Assert 1: Invalid map entry key returned", keys.contains(entry.getKey()));
            assertTrue("Assert 2: Invalid map entry value returned", values.contains(entry.getValue()));
            assertTrue("Assert 3: Entry not in entry set", entrySet.contains(entry));
        }

        // Dereference a single key, then try to
        // force a collection of the weak ref'd obj
        keyArray[50] = null;
        GCSupport.tryToTriggerGC();

        assertEquals("Assert 4: Incorrect number of entries after gc", 99, entrySet.size());
        assertSame("Assert 5: Entries not identical", entrySet.iterator().next(), entrySet.iterator().next());

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
        assertEquals("Assert 6: entry set count mismatch", size, entrySet.size());

        int entries = 0;
        it = entrySet.iterator();
        while (it.hasNext()) {
            it.next();
            entries++;
        }
        assertEquals("Assert 6: count mismatch", size, entries);

        it = entrySet.iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
        assertEquals("Assert 7: entry set not empty", 0, entrySet.size());
        assertFalse("Assert 8:  iterator not empty", entrySet.iterator().hasNext());
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
        assertEquals("Incorrect number of entries returned--wanted 100, got: " + entrySet.size(),
                100, entrySet.size());
        for (var entry : entrySet) {
            assertTrue("Invalid map entry returned--bad key", keys.contains(entry.getKey()));
            assertTrue("Invalid map entry returned--bad key", values.contains(entry.getValue()));
        }
        keys = null;
        values = null;
        keyArray[50] = null;

        GCSupport.tryToTriggerGC();

        assertEquals("Incorrect number of entries returned after gc--wanted 99, got: " + entrySet.size(),
                99, entrySet.size());
    }

    @Test
    public void get() {
        assertTrue("Used to test", true);
    }

    @Test
    public void isEmpty() {
        whm = new WeakHashMap<>();
        assertTrue("New map should be empty", whm.isEmpty());
        Object myObject = new Object();
        whm.put(myObject, myObject);
        assertFalse("Map should not be empty", whm.isEmpty());
        whm.remove(myObject);
        assertTrue("Map with elements removed should be empty", whm.isEmpty());
    }

    @Test
    public void put() {
        var map = new WeakHashMap<>();
        map.put(null, "value"); // add null key
        GCSupport.tryToTriggerGC();
        map.remove("nothing"); // Cause objects in queue to be removed
        assertEquals("null key was removed", 1, map.size());
    }

    @Test
    public void putAll() {
        var mockMap = new MockMap<>();
        var map = new WeakHashMap<>();
        map.putAll(mockMap);
        assertEquals("Size should be 0", 0, map.size());
    }

    @Test
    public void remove() {
        whm = new WeakHashMap<>();
        for (int i = 0; i < 100; i++) {
            whm.put(keyArray[i], valueArray[i]);
        }

        assertSame("Remove returned incorrect value", valueArray[25], whm.remove(keyArray[25]));
        assertNull("Remove returned incorrect value", whm.remove(keyArray[25]));
        assertEquals("Size should be 99 after remove", 99, whm.size());
    }

    @Test
    public void size() {
        assertTrue("Used to test", true);
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
        assertEquals("Incorrect number of keys returned,", 100, keySet.size());
        for (var key : keySet) {
            assertTrue("Invalid map entry returned--bad key", keys.contains(key));
        }
        keys = null;
        values = null;
        keyArray[50] = null;

        GCSupport.tryToTriggerGC();

        assertEquals("Incorrect number of keys returned after gc,", 99, keySet.size());
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
        assertFalse("Wrong hasNext() value", iter.hasNext());
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
        assertEquals("Incorrect number of keys returned,", 100, valuesCollection.size());
        for (Object value : valuesCollection) {
            assertTrue("Invalid map entry returned--bad value", values.contains(value));
        }
        keys = null;
        values = null;
        keyArray[50] = null;

        GCSupport.tryToTriggerGC();

        assertEquals("Incorrect number of keys returned after gc", 99, valuesCollection.size());
    }

    @Before
    public void setUp() {
        for (int i = 0; i < 100; i++) {
            keyArray[i] = new Object();
            valueArray[i] = new Object();
        }
    }

}
