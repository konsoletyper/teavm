/*
 *  Copyright 2020 Alexey Andreev.
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
package org.teavm.classlib.java.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import org.junit.Test;

public class CollectionsFactoryTest {

    @Test
    public void createList() {
        assertEquals(
                TArrays.asList(1, 2, 3),
                CollectionsFactory.createList(1, 2, 3)
        );
    }

    @Test
    public void createList_zero_elements() {
        assertSame(CollectionsFactory.createList(), TCollections.emptyList());
    }

    @Test(expected = NullPointerException.class)
    public void createList_null_element() {
        CollectionsFactory.createList(new Object[] { null });
    }

    @Test(expected = NullPointerException.class)
    public void createList_null_array() {
        CollectionsFactory.createList((Object[]) null);
    }

    @Test
    public void createSet() {
        assertEquals(
                new THashSet<>(TArrays.asList(1, 2, 3)),
                CollectionsFactory.createSet(1, 2, 3)
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void createSet_duplicate_elements() {
        CollectionsFactory.createSet(1, 1, 1);
    }

    @Test
    public void createSet_zero_elements() {
        assertSame(CollectionsFactory.createSet(), TCollections.emptySet());
    }

    @Test(expected = NullPointerException.class)
    public void createSet_null_element() {
        CollectionsFactory.createSet(new Object[] { null });
    }

    @Test(expected = NullPointerException.class)
    public void createSet_null_array() {
        CollectionsFactory.createSet((Object[]) null);
    }

    @Test
    public void createMap() {
        final THashMap<Integer, String> hashMap = new THashMap<>();
        hashMap.put(1, "one");
        hashMap.put(2, "two");
        hashMap.put(3, "three");

        assertEquals(
                hashMap,
                CollectionsFactory.createMap(
                        TMap.entry(1, "one"),
                        TMap.entry(2, "two"),
                        TMap.entry(3, "three")
                )
        );
    }

    @Test(expected = NullPointerException.class)
    public void createMap_null_key() {
        CollectionsFactory.createMap(TMap.entry(null, "value"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void createMap_duplicate_key() {
        CollectionsFactory.createMap(
                TMap.entry(1, "value"),
                TMap.entry(1, "another value")
        );
    }

    @Test(expected = NullPointerException.class)
    public void createMap_null_value() {
        CollectionsFactory.createMap(TMap.entry("key", null));
    }

    @Test
    public void createMap_duplicate_value() {
        final THashMap<Integer, String> hashMap = new THashMap<>();
        hashMap.put(1, "value");
        hashMap.put(2, "value");

        assertEquals(
                hashMap,
                CollectionsFactory.createMap(
                        TMap.entry(1, "value"),
                        TMap.entry(2, "value")
                )
        );
    }

    @Test
    public void createMap_zero_elements() {
        assertSame(CollectionsFactory.createMap(), TCollections.emptyMap());
    }

    @Test(expected = NullPointerException.class)
    public void createMap_null_array() {
        CollectionsFactory.createMap((TMap.Entry<Object, Object>[]) null);
    }
}