/*
 *  Copyright 2020 Joerg Hohwiller.
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
package org.teavm.classlib.java.util.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.classlib.support.MapTest2Support;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class ConcurrentHashMapTest {

    private static final String UNDEFINED_KEY = "undefined-key";

    private static final String KEY1 = "key1";

    private static final String VALUE1 = "Value1";

    private static final String KEY2 = "Key2";

    private static final Integer VALUE2 = new Integer(42);

    private static void fillValues(Map<String, Object> map) {

        map.put(KEY1, VALUE1);
        map.put(KEY2, VALUE2);
    }

    private static void checkValues(Map<String, Object> map) {

        assertNull(map.get(UNDEFINED_KEY));
        assertEquals(VALUE1, map.get(KEY1));
        assertEquals(VALUE2, map.get(KEY2));
        assertTrue(map.containsKey(KEY1));
        assertTrue(map.keySet().contains(KEY1));
        assertTrue(map.containsValue(VALUE1));
        assertTrue(map.values().contains(VALUE1));
        assertTrue(map.containsKey(KEY2));
        assertTrue(map.keySet().contains(KEY2));
        assertTrue(map.containsValue(VALUE2));
        assertTrue(map.values().contains(VALUE2));
        assertFalse(map.containsKey(UNDEFINED_KEY));
        assertFalse(map.keySet().contains(UNDEFINED_KEY));
        int size = map.size();
        assertEquals(size, map.keySet().size());
        assertEquals(size, map.entrySet().size());
        assertEquals(size, map.values().size());
    }

    private static void checkEmpty(Map<?, ?> map) {

        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
        assertTrue(map.keySet().isEmpty());
        assertTrue(map.values().isEmpty());
        assertTrue(map.entrySet().isEmpty());
    }

    private static void fillAndCheck(Map<String, Object> map) {

        fillValues(map);
        checkValues(map);
    }

    private static void checkNew(Map<String, Object> map) {

        checkEmpty(map);
        fillAndCheck(map);
    }

    @Test
    public void test_Constructor() {

        new MapTest2Support(new ConcurrentHashMap<>()).runTest();

        ConcurrentMap<String, Object> map = new ConcurrentHashMap<>();
        checkNew(map);
    }

    @Test
    public void test_ConstructorI() {

        checkNew(new ConcurrentHashMap<>(9));
        checkNew(new ConcurrentHashMap<>(0));
    }

    @Test
    public void test_ConstructorIF() {

        checkNew(new ConcurrentHashMap<>(10, 0.5f));
        checkNew(new ConcurrentHashMap<>(10, 0.75f));
    }

    @Test
    public void test_ConstructorIFI() {

        checkNew(new ConcurrentHashMap<>(10, 0.5f, 1));
        checkNew(new ConcurrentHashMap<>(0, 0.75f, 2));
    }

    @Test
    public void test_ConstructorLjava_util_Map() {

        Map<String, Object> parentMap = new TreeMap<>();
        fillValues(parentMap);
        ConcurrentMap<String, Object> map = new ConcurrentHashMap<>(parentMap);
        checkValues(map);
    }

    @Test
    public void test_clear() {

        ConcurrentMap<String, Object> map = new ConcurrentHashMap<>();
        fillValues(map);
        checkValues(map);
        map.clear();
        checkEmpty(map);
    }

    @Test
    public void test_equalsLjava_lang_Object() {

        ConcurrentMap<String, Object> map = new ConcurrentHashMap<>();
        fillAndCheck(map);
        ConcurrentMap<String, Object> map2 = new ConcurrentHashMap<>();
        fillAndCheck(map2);
        assertTrue(map.equals(map2));
    }

    @Test
    public void test_hashCode() {

        ConcurrentMap<String, Object> map = new ConcurrentHashMap<>();
        fillAndCheck(map);
        ConcurrentMap<String, Object> map2 = new ConcurrentHashMap<>();
        assertNotEquals(map.hashCode(), map2.hashCode());
        fillAndCheck(map2);
        assertEquals(map.hashCode(), map2.hashCode());
    }

    @Test
    public void test_putAllLjava_util_Map() {

        Map<String, Object> parentMap = new TreeMap<>();
        fillValues(parentMap);
        ConcurrentMap<String, Object> map = new ConcurrentHashMap<>();
        map.putAll(parentMap);
        checkValues(map);
    }

    @Test
    public void test_removeLjava_lang_Object() {

        ConcurrentMap<String, Object> map = new ConcurrentHashMap<>();
        fillAndCheck(map);
        assertNull(map.remove(UNDEFINED_KEY));
        assertFalse(map.remove(KEY1, new Object()));
        assertEquals(2, map.size());
        assertTrue(map.remove(KEY1, VALUE1));
        assertEquals(1, map.size());
        assertEquals(VALUE2, map.remove(KEY2));
        assertTrue(map.isEmpty());
    }

    @Test
    public void test_toString() {

        ConcurrentMap<String, Object> map = new ConcurrentHashMap<>();
        fillAndCheck(map);
        assertEquals("{key1=Value1, Key2=42}", map.toString());
    }

}
