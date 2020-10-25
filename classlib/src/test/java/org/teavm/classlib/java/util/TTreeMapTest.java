/*
 *  Copyright 2020 Ihromant.
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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TTreeMapTest {
    private TNavigableMap<Integer, Integer> toTest;

    @Before
    public void init() {
        toTest = new TTreeMap<>((a, b) -> a - b);
        toTest.put(1, 1);
        toTest.put(3, 15);
        toTest.put(4, 20);
        toTest.put(6, 13);
        toTest.put(10, 119);
    }

    @Test
    public void testSubMap() {
        Assert.assertEquals("", printMapForEach(toTest.subMap(0, 1)));
        Assert.assertEquals("", printMapForEach(toTest.subMap(7, 9)));
        Assert.assertEquals("3 = 15, 4 = 20, 6 = 13, ", printMapForEach(toTest.subMap(3, 9)));
        Assert.assertEquals("10 = 119, ", printMapForEach(toTest.subMap(10, 29)));
        Assert.assertEquals("", printMapForEach(toTest.subMap(29, 100)));

        Assert.assertEquals("", printMapIterator(toTest.subMap(0, 1)));
        Assert.assertEquals("", printMapIterator(toTest.subMap(7, 9)));
        Assert.assertEquals("3 = 15, 4 = 20, 6 = 13, ", printMapIterator(toTest.subMap(3, 9)));
        Assert.assertEquals("10 = 119, ", printMapIterator(toTest.subMap(10, 29)));
        Assert.assertEquals("", printMapIterator(toTest.subMap(29, 100)));
    }

    private static <K, V> String printMapForEach(TMap<K, V> myMap) {
        StringBuilder sb = new StringBuilder();
        myMap.entrySet().forEach(entry -> {
            sb.append(entry.getKey());
            sb.append(" = ");
            sb.append(entry.getValue());
            sb.append(", ");
        });
        return sb.toString();
    }

    private static <K, V> String printMapIterator(TMap<K, V> myMap) {
        StringBuilder sb = new StringBuilder();
        for (TIterator<TMap.Entry<K, V>> it = myMap.entrySet().iterator(); it.hasNext();) {
            TMap.Entry<K, V> entry = it.next();
            sb.append(entry.getKey());
            sb.append(" = ");
            sb.append(entry.getValue());
            sb.append(", ");
        }
        return sb.toString();
    }
}
