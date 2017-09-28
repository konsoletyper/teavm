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
    public void testRemoveObject() {
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
}