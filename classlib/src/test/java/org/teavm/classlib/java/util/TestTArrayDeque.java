/*
 *  Copyright 2017 Adam Ryan.
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
import org.junit.Test;

public class TestTArrayDeque {
    @Test
    public void eachRemovedObjectShouldReduceTheSizeByOne() {
        TArrayDeque<Object> arrayDeque = new TArrayDeque<>();
        Object object1 = new Object();
        Object object2 = new Object();
        Object object3 = new Object();
        arrayDeque.add(object1);
        Assert.assertTrue(arrayDeque.size() == 1);
        arrayDeque.remove(object1);
        Assert.assertTrue(arrayDeque.size() == 0);
        arrayDeque.add(object1);
        arrayDeque.add(object2);
        arrayDeque.add(object3);
        Assert.assertTrue(arrayDeque.size() == 3);
        arrayDeque.remove(object1);
        arrayDeque.remove(object2);
        arrayDeque.remove(object3);
        Assert.assertTrue(arrayDeque.size() == 0);
        arrayDeque.remove(object1);
        Assert.assertTrue(arrayDeque.size() == 0);
    }

    @Test
    public void removeFirstShouldNotContainTheFirstAddedObject() {
        TArrayDeque<Object> arrayDeque1 = new TArrayDeque<>();
        Object object1 = new Object();
        Object object2 = new Object();
        Object object3 = new Object();
        arrayDeque1.add(object1);
        arrayDeque1.add(object2);
        arrayDeque1.add(object3);
        arrayDeque1.removeFirst();
        Assert.assertTrue(arrayDeque1.size() == 2);
        Assert.assertTrue(arrayDeque1.contains(object2));
        Assert.assertTrue(arrayDeque1.contains(object3));

        TArrayDeque<Object> arrayDeque2 = new TArrayDeque<>();
        arrayDeque2.add(object1);
        arrayDeque2.add(object2);
        arrayDeque2.add(object3);
        arrayDeque2.remove(object1);
        arrayDeque2.removeFirst();
        Assert.assertTrue(arrayDeque2.size() == 1);
        Assert.assertTrue(arrayDeque2.contains(object3));

        TArrayDeque<Object> arrayDeque3 = new TArrayDeque<>();
        arrayDeque3.add(object1);
        arrayDeque3.add(object2);
        arrayDeque3.add(object3);
        arrayDeque3.remove(object2);
        arrayDeque3.removeFirst();
        Assert.assertTrue(arrayDeque3.size() == 1);
        Assert.assertTrue(arrayDeque3.contains(object3));

        TArrayDeque<Object> arrayDeque4 = new TArrayDeque<>();
        arrayDeque4.add(object1);
        arrayDeque4.add(object2);
        arrayDeque4.add(object3);
        arrayDeque4.remove(object3);
        arrayDeque4.removeFirst();
        Assert.assertTrue(arrayDeque4.size() == 1);
        Assert.assertTrue(arrayDeque4.contains(object2));
    }

    @Test
    public void removeLastShouldNotContainTheLastAddedObject() {
        TArrayDeque<Object> arrayDeque1 = new TArrayDeque<>();
        Object object1 = new Object();
        Object object2 = new Object();
        Object object3 = new Object();
        arrayDeque1.add(object1);
        arrayDeque1.add(object2);
        arrayDeque1.add(object3);
        arrayDeque1.removeLast();
        Assert.assertTrue(arrayDeque1.size() == 2);
        Assert.assertTrue(arrayDeque1.contains(object1));
        Assert.assertTrue(arrayDeque1.contains(object2));

        TArrayDeque<Object> arrayDeque2 = new TArrayDeque<>();
        arrayDeque2.add(object1);
        arrayDeque2.add(object2);
        arrayDeque2.add(object3);
        arrayDeque2.remove(object3);
        arrayDeque2.removeLast();
        Assert.assertTrue(arrayDeque2.size() == 1);
        Assert.assertTrue(arrayDeque2.contains(object1));

        TArrayDeque<Object> arrayDeque3 = new TArrayDeque<>();
        arrayDeque3.add(object1);
        arrayDeque3.add(object2);
        arrayDeque3.add(object3);
        arrayDeque3.remove(object2);
        arrayDeque3.removeLast();
        Assert.assertTrue(arrayDeque3.size() == 1);
        Assert.assertTrue(arrayDeque3.contains(object1));

        TArrayDeque<Object> arrayDeque4 = new TArrayDeque<>();
        arrayDeque4.add(object1);
        arrayDeque4.add(object2);
        arrayDeque4.add(object3);
        arrayDeque4.remove(object3);
        arrayDeque4.removeLast();
        Assert.assertTrue(arrayDeque4.size() == 1);
        Assert.assertTrue(arrayDeque4.contains(object1));
    }
}