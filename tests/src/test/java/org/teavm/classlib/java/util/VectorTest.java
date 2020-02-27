/*
 *  Copyright 2017 Alexey Andreev.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.classlib.support.ListTestSupport;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class VectorTest {
    Object[] objArray;
    private Vector<Object> tVector = new Vector<>();
    private String vString = "[Test 0, Test 1, Test 2, Test 3, Test 4, Test 5, Test 6, Test 7, Test 8, Test 9, "
            + "Test 10, Test 11, Test 12, Test 13, Test 14, Test 15, Test 16, Test 17, Test 18, Test 19, Test 20, "
            + "Test 21, Test 22, Test 23, Test 24, Test 25, Test 26, Test 27, Test 28, Test 29, Test 30, Test 31, "
            + "Test 32, Test 33, Test 34, Test 35, Test 36, Test 37, Test 38, Test 39, Test 40, Test 41, Test 42, "
            + "Test 43, Test 44, Test 45, Test 46, Test 47, Test 48, Test 49, Test 50, Test 51, Test 52, Test 53, "
            + "Test 54, Test 55, Test 56, Test 57, Test 58, Test 59, Test 60, Test 61, Test 62, Test 63, Test 64, "
            + "Test 65, Test 66, Test 67, Test 68, Test 69, Test 70, Test 71, Test 72, Test 73, Test 74, Test 75, "
            + "Test 76, Test 77, Test 78, Test 79, Test 80, Test 81, Test 82, Test 83, Test 84, Test 85, Test 86, "
            + "Test 87, Test 88, Test 89, Test 90, Test 91, Test 92, Test 93, Test 94, Test 95, Test 96, Test 97, "
            + "Test 98, Test 99]";

    public VectorTest() {
        for (int i = 0; i < 100; i++) {
            tVector.addElement("Test " + i);
        }
        objArray = new Object[100];
        for (int i = 0; i < 100; i++) {
            objArray[i] = "Test " + i;
        }
    }

    @Test
    public void test_Constructor() {
        Vector<Integer> tv = new Vector<>(100);
        for (int i = 0; i < 100; i++) {
            tv.addElement(i);
        }
        new ListTestSupport(tv).runTest();

        tv = new Vector<>(200);
        for (int i = -50; i < 150; i++) {
            tv.addElement(i);
        }
        new ListTestSupport(tv.subList(50, 150)).runTest();

        Vector<String> v = new Vector<>();
        assertEquals("Vector creation failed", 0, v.size());
        assertEquals("Wrong capacity", 10, v.capacity());
    }

    @Test
    public void test_ConstructorI() {
        // Test for method java.util.Vector(int)

        Vector<String> v = new Vector<>(100);
        assertEquals("Vector creation failed", 0, v.size());
        assertEquals("Wrong capacity", 100, v.capacity());
    }

    @Test
    public void test_ConstructorII() {
        // Test for method java.util.Vector(int, int)

        Vector<Object> v = new Vector<>(2, 10);
        v.addElement(new Object());
        v.addElement(new Object());
        v.addElement(new Object());

        assertEquals("Failed to inc capacity by proper amount", 12, v.capacity());

        Vector<String> grow = new Vector<>(3, -1);
        grow.addElement("one");
        grow.addElement("two");
        grow.addElement("three");
        grow.addElement("four");
        assertEquals("Wrong size", 4, grow.size());
        assertEquals("Wrong capacity", 6, grow.capacity());

        Vector<String> emptyVector = new Vector<>(0, 0);
        emptyVector.addElement("one");
        assertEquals("Wrong size", 1, emptyVector.size());
        emptyVector.addElement("two");
        emptyVector.addElement("three");
        assertEquals("Wrong size", 3, emptyVector.size());

        try {
            new Vector<>(-1, 0);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Excepted
        }
    }

    @Test
    public void test_ConstructorLjava_util_Collection() {
        // Test for method java.util.Vector(java.util.Collection)
        Collection<String> l = new LinkedList<>();
        for (int i = 0; i < 100; i++) {
            l.add("Test " + i);
        }
        Vector<String> myVector = new Vector<>(l);
        assertTrue("Vector is not correct size", myVector.size() == objArray.length);
        for (int counter = 0; counter < objArray.length; counter++) {
            assertTrue("Vector does not contain correct elements", myVector.contains(((List<?>) l).get(counter)));
        }
    }

    @Test
    public void test_addILjava_lang_Object() {
        // Test for method void java.util.Vector.add(int, java.lang.Object)
        Object o = new Object();
        Object prev = tVector.get(45);
        tVector.add(45, o);
        assertTrue("Failed to add Object", tVector.get(45) == o);
        assertTrue("Failed to fix-up existing indices", tVector.get(46) == prev);
        assertEquals("Wrong size after add", 101, tVector.size());

        prev = tVector.get(50);
        tVector.add(50, null);
        assertNull("Failed to add null", tVector.get(50));
        assertTrue("Failed to fix-up existing indices after adding null", tVector.get(51) == prev);
        assertEquals("Wrong size after add", 102, tVector.size());
    }

    @Test
    public void test_addLjava_lang_Object() {
        // Test for method boolean java.util.Vector.add(java.lang.Object)
        Object o = new Object();
        tVector.add(o);
        assertTrue("Failed to add Object", tVector.lastElement() == o);
        assertEquals("Wrong size after add", 101, tVector.size());

        tVector.add(null);
        assertNull("Failed to add null", tVector.lastElement());
        assertEquals("Wrong size after add", 102, tVector.size());
    }

    @Test
    public void test_addAllILjava_util_Collection() {
        // Test for method boolean java.util.Vector.addAll(int,
        // java.util.Collection)
        Collection<String> l = new LinkedList<>();
        for (int i = 0; i < 100; i++) {
            l.add("Test " + i);
        }
        Vector<String> v = new Vector<>();
        tVector.addAll(50, l);
        for (int i = 50; i < 100; i++) {
            assertTrue("Failed to add all elements", tVector.get(i) == ((List<String>) l).get(i - 50));
        }
        v = new Vector<>();
        v.add("one");
        int r = 0;
        try {
            v.addAll(3, Arrays.asList(new String[] { "two", "three" }));
        } catch (ArrayIndexOutOfBoundsException e) {
            r = 1;
        }
        assertTrue("Invalid add: " + r, r == 1);
        l = new LinkedList<>();
        l.add(null);
        l.add("gah");
        l.add(null);
        tVector.addAll(50, l);
        assertNull("Wrong element at position 50--wanted null", tVector.get(50));
        assertEquals("Wrong element at position 51--wanted 'gah'", "gah", tVector.get(51));
        assertNull("Wrong element at position 52--wanted null", tVector.get(52));

        try {
            v.addAll(-1, null);
            fail("Should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // Excepted
        }
    }

    @Test
    public void test_addAllLjava_util_Collection() {
        // Test for method boolean java.util.Vector.addAll(java.util.Collection)
        Vector<String> v = new Vector<>();
        Collection<String> l = new LinkedList<>();
        for (int i = 0; i < 100; i++) {
            l.add("Test " + i);
        }
        v.addAll(l);
        assertTrue("Failed to add all elements", tVector.equals(v));

        v.addAll(l);
        int vSize = tVector.size();
        for (int counter = vSize - 1; counter >= 0; counter--) {
            assertTrue("Failed to add elements correctly", v.get(counter) == v.get(counter + vSize));
        }

        l = new LinkedList<>();
        l.add(null);
        l.add("gah");
        l.add(null);
        tVector.addAll(l);
        assertNull("Wrong element at 3rd last position--wanted null", tVector.get(vSize));
        assertEquals("Wrong element at 2nd last position--wanted 'gah'", "gah", tVector.get(vSize + 1));
        assertNull("Wrong element at last position--wanted null", tVector.get(vSize + 2));
    }

    @Test
    public void test_addElementLjava_lang_Object() {
        // Test for method void java.util.Vector.addElement(java.lang.Object)
        Vector<Object> v = vectorClone(tVector);
        v.addElement("Added Element");
        assertTrue("Failed to add element", v.contains("Added Element"));
        assertEquals("Added Element to wrong slot", "Added Element", v.elementAt(100));
        v.addElement(null);
        assertTrue("Failed to add null", v.contains(null));
        assertNull("Added null to wrong slot", v.elementAt(101));
    }

    @Test
    public void test_addElementLjava_lang_Object_subtest0() {
        // Test for method void java.util.Vector.addElement(java.lang.Object)
        Vector<Object> v = vectorClone(tVector);
        v.addElement("Added Element");
        assertTrue("Failed to add element", v.contains("Added Element"));
        assertEquals("Added Element to wrong slot", "Added Element", v.elementAt(100));
        v.addElement(null);
        assertTrue("Failed to add null", v.contains(null));
        assertNull("Added null to wrong slot", v.elementAt(101));
    }

    @Test
    public void test_capacity() {
        // Test for method int java.util.Vector.capacity()

        Vector<String> v = new Vector<>(9);
        assertEquals("Incorrect capacity returned", 9, v.capacity());
    }

    @Test
    public void test_clear() {
        // Test for method void java.util.Vector.clear()
        Vector<Object> orgVector = vectorClone(tVector);
        tVector.clear();
        assertEquals("a) Cleared Vector has non-zero size", 0, tVector.size());
        Enumeration<Object> e = orgVector.elements();
        while (e.hasMoreElements()) {
            assertTrue("a) Cleared vector contained elements", !tVector.contains(e.nextElement()));
        }

        tVector.add(null);
        tVector.clear();
        assertEquals("b) Cleared Vector has non-zero size", 0, tVector.size());
        e = orgVector.elements();
        while (e.hasMoreElements()) {
            assertTrue("b) Cleared vector contained elements", !tVector.contains(e.nextElement()));
        }
    }

    @Test
    public void test_clone() {
        // Test for method java.lang.Object java.util.Vector.clone()
        tVector.add(25, null);
        tVector.add(75, null);
        @SuppressWarnings("unchecked")
        Vector<Object> v = (Vector<Object>) tVector.clone();
        Enumeration<Object> orgNum = tVector.elements();
        Enumeration<Object> cnum = v.elements();

        int index = 0;
        while (orgNum.hasMoreElements()) {
            assertTrue("Not enough elements copied", cnum.hasMoreElements());
            assertSame("Vector cloned improperly, element " + index++ + " does not match",
                    orgNum.nextElement(), cnum.nextElement());
        }
        assertTrue("Not enough elements copied", !cnum.hasMoreElements());
    }

    @Test
    public void test_containsLjava_lang_Object() {
        // Test for method boolean java.util.Vector.contains(java.lang.Object)
        assertTrue("Did not find element", tVector.contains("Test 42"));
        assertTrue("Found bogus element", !tVector.contains("Hello"));
        assertTrue("Returned true looking for null in vector without null element", !tVector.contains(null));
        tVector.insertElementAt(null, 20);
        assertTrue("Returned false looking for null in vector with null element", tVector.contains(null));
    }

    @Test
    public void test_containsAllLjava_util_Collection() {
        // Test for method boolean
        // java.util.Vector.containsAll(java.util.Collection)
        Collection<Object> s = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            s.add("Test " + i);
        }

        assertTrue("Returned false for valid collection", tVector.containsAll(s));
        s.add(null);
        assertTrue("Returned true for invlaid collection containing null", !tVector.containsAll(s));
        tVector.add(25, null);
        assertTrue("Returned false for valid collection containing null", tVector.containsAll(s));
        s = new HashSet<>();
        s.add(new Object());
        assertTrue("Returned true for invalid collection", !tVector.containsAll(s));
    }

    @Test
    public void test_copyInto$Ljava_lang_Object() {
        // Test for method void java.util.Vector.copyInto(java.lang.Object [])

        Object[] a = new Object[100];
        tVector.setElementAt(null, 20);
        tVector.copyInto(a);

        for (int i = 0; i < 100; i++) {
            assertTrue("copyInto failed", a[i] == tVector.elementAt(i));
        }
    }

    @Test
    public void test_elementAtI() {
        // Test for method java.lang.Object java.util.Vector.elementAt(int)
        assertEquals("Incorrect element returned", "Test 18", tVector.elementAt(18));
        tVector.setElementAt(null, 20);
        assertNull("Incorrect element returned--wanted null", tVector.elementAt(20));
    }

    @Test
    public void test_elements() {
        // Test for method java.util.Enumeration java.util.Vector.elements()
        tVector.insertElementAt(null, 20);
        Enumeration<Object> e = tVector.elements();
        int i = 0;
        while (e.hasMoreElements()) {
            assertTrue("Enumeration returned incorrect element at pos: " + i, e.nextElement() == tVector.elementAt(i));
            i++;
        }
        assertTrue("Invalid enumeration", i == tVector.size());
    }

    @Test
    public void test_ensureCapacityI() {
        // Test for method void java.util.Vector.ensureCapacity(int)

        Vector<Object> v = new Vector<>(9);
        v.ensureCapacity(20);
        assertEquals("ensureCapacity failed to set correct capacity", 20, v.capacity());
        v = new Vector<>(100);
        assertEquals("ensureCapacity reduced capacity", 100, v.capacity());

        v.ensureCapacity(150);
        assertEquals("ensuieCapacity failed to set to be twice the old capacity", 200, v.capacity());

        v = new Vector<>(9, -1);
        v.ensureCapacity(20);
        assertEquals("ensureCapacity failed to set to be minCapacity", 20, v.capacity());
        v.ensureCapacity(15);
        assertEquals("ensureCapacity reduced capacity", 20, v.capacity());
        v.ensureCapacity(35);
        assertEquals("ensuieCapacity failed to set to be twice the old capacity", 40, v.capacity());

        v = new Vector<>(9, 4);
        v.ensureCapacity(11);
        assertEquals("ensureCapacity failed to set correct capacity", 13, v.capacity());
        v.ensureCapacity(5);
        assertEquals("ensureCapacity reduced capacity", 13, v.capacity());
        v.ensureCapacity(20);
        assertEquals("ensuieCapacity failed to set to be twice the old capacity", 20, v.capacity());
    }

    @Test
    public void test_equalsLjava_lang_Object() {
        // Test for method boolean java.util.Vector.equals(java.lang.Object)
        Vector<Object> v = new Vector<>();
        for (int i = 0; i < 100; i++) {
            v.addElement("Test " + i);
        }
        assertTrue("a) Equal vectors returned false", tVector.equals(v));
        v.addElement(null);
        assertTrue("b) UnEqual vectors returned true", !tVector.equals(v));
        tVector.addElement(null);
        assertTrue("c) Equal vectors returned false", tVector.equals(v));
        tVector.removeElementAt(22);
        assertTrue("d) UnEqual vectors returned true", !tVector.equals(v));
        assertTrue("e) Equal vectors returned false", tVector.equals(tVector));
        assertFalse("f) UnEqual vectors returned true", tVector.equals(new Object()));
        assertFalse("g) Unequal vectors returned true", tVector.equals(null));
    }

    @Test
    public void test_firstElement() {
        // Test for method java.lang.Object java.util.Vector.firstElement()
        assertEquals("Returned incorrect firstElement", "Test 0", tVector.firstElement());
        tVector.insertElementAt(null, 0);
        assertNull("Returned incorrect firstElement--wanted null", tVector.firstElement());

        Vector<Object> v = new Vector<>();
        try {
            v.firstElement();
            fail("Should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
            // Excepted
        }
    }

    @Test
    public void test_getI() {
        // Test for method java.lang.Object java.util.Vector.get(int)
        assertEquals("Get returned incorrect object", "Test 80", tVector.get(80));
        tVector.add(25, null);
        assertNull("Returned incorrect element--wanted null", tVector.get(25));
    }

    @Test
    public void test_hashCode() {
        // Test for method int java.util.Vector.hashCode()
        int hashCode = 1; // one
        tVector.insertElementAt(null, 20);
        for (int i = 0; i < tVector.size(); i++) {
            Object obj = tVector.elementAt(i);
            hashCode = 31 * hashCode + (obj == null ? 0 : obj.hashCode());
        }
        assertTrue("Incorrect hashCode returned.  Wanted: " + hashCode + " got: " + tVector.hashCode(),
                tVector.hashCode() == hashCode);
    }

    @Test
    public void test_indexOfLjava_lang_Object() {
        // Test for method int java.util.Vector.indexOf(java.lang.Object)
        assertEquals("Incorrect index returned", 10, tVector.indexOf("Test 10"));
        assertEquals("Index returned for invalid Object", -1, tVector.indexOf("XXXXXXXXXXX"));
        tVector.setElementAt(null, 20);
        tVector.setElementAt(null, 40);
        assertTrue("Incorrect indexOf returned for null: " + tVector.indexOf(null), tVector.indexOf(null) == 20);
    }

    @Test
    public void test_indexOfLjava_lang_ObjectI() {
        // Test for method int java.util.Vector.indexOf(java.lang.Object, int)
        assertEquals("Failed to find correct index", tVector.indexOf("Test 98", 50), 98);
        assertTrue("Found index of bogus element", tVector.indexOf("Test 1001", 50) == -1);
        tVector.setElementAt(null, 20);
        tVector.setElementAt(null, 40);
        tVector.setElementAt(null, 60);
        assertTrue("a) Incorrect indexOf returned for null: " + tVector.indexOf(null, 25),
                tVector.indexOf(null, 25) == 40);
        assertTrue("b) Incorrect indexOf returned for null: " + tVector.indexOf(null, 20),
                tVector.indexOf(null, 20) == 20);
        try {
            tVector.indexOf("Test 98", -1);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // ok
        }
        assertEquals(-1, tVector.indexOf("Test 98", 1000));
        assertEquals(-1, tVector.indexOf("Test 98", Integer.MAX_VALUE));
        assertEquals(-1, tVector.indexOf("Test 98", tVector.size()));
        assertEquals(98, tVector.indexOf("Test 98", 0));
        try {
            tVector.indexOf("Test 98", Integer.MIN_VALUE);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // ok
        }
    }

    @Test
    public void test_insertElementAtLjava_lang_ObjectI() {
        // Test for method void
        // java.util.Vector.insertElementAt(java.lang.Object, int)
        Vector<Object> v = vectorClone(tVector);
        String prevElement = (String) v.elementAt(99);
        v.insertElementAt("Inserted Element", 99);
        assertEquals("Element not inserted", "Inserted Element", v.elementAt(99));
        assertTrue("Elements shifted incorrectly", ((String) v.elementAt(100)).equals(prevElement));
        v.insertElementAt(null, 20);
        assertNull("null not inserted", v.elementAt(20));

        try {
            tVector.insertElementAt("Inserted Element", -1);
            fail("Should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // Excepted
        }

        try {
            tVector.insertElementAt(null, -1);
            fail("Should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // Excepted
        }

        try {
            tVector.insertElementAt("Inserted Element", tVector.size() + 1);
            fail("Should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // Excepted
        }

        try {
            tVector.insertElementAt(null, tVector.size() + 1);
            fail("Should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // Excepted
        }
    }

    @Test
    public void test_isEmpty() {
        // Test for method boolean java.util.Vector.isEmpty()Vector
        Vector<Object> v = new Vector<>();
        assertTrue("Empty vector returned false", v.isEmpty());
        v.addElement(new Object());
        assertTrue("non-Empty vector returned true", !v.isEmpty());
    }

    @Test
    public void test_lastElement() {
        // Test for method java.lang.Object java.util.Vector.lastElement()
        assertEquals("Incorrect last element returned", "Test 99", tVector.lastElement());
        tVector.addElement(null);
        assertNull("Incorrect last element returned--wanted null", tVector.lastElement());

        Vector<String> vector = new Vector<>();
        try {
            vector.lastElement();
            fail("Should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
            // Excepted
        }
    }

    @Test
    public void test_lastIndexOfLjava_lang_Object() {
        // Test for method int java.util.Vector.lastIndexOf(java.lang.Object)
        Vector<String> v = new Vector<>(9);
        for (int i = 0; i < 9; i++) {
            v.addElement("Test");
        }
        v.addElement("z");
        assertEquals("Failed to return correct index", 8, v.lastIndexOf("Test"));
        tVector.setElementAt(null, 20);
        tVector.setElementAt(null, 40);
        assertTrue("Incorrect lastIndexOf returned for null: " + tVector.lastIndexOf(null),
                tVector.lastIndexOf(null) == 40);
    }

    @Test
    public void test_lastIndexOfLjava_lang_ObjectI() {
        // Test for method int java.util.Vector.lastIndexOf(java.lang.Object,
        // int)
        assertEquals("Failed to find object", 0, tVector.lastIndexOf("Test 0", 0));
        assertTrue("Found Object outside of index", tVector.lastIndexOf("Test 0", 10) > -1);
        tVector.setElementAt(null, 20);
        tVector.setElementAt(null, 40);
        tVector.setElementAt(null, 60);
        assertTrue("Incorrect lastIndexOf returned for null: " + tVector.lastIndexOf(null, 15),
                tVector.lastIndexOf(null, 15) == -1);
        assertTrue("Incorrect lastIndexOf returned for null: " + tVector.lastIndexOf(null, 45),
                tVector.lastIndexOf(null, 45) == 40);

        assertEquals(-1, tVector.lastIndexOf("Test 98", -1));
        assertEquals(-1, tVector.lastIndexOf("Test 98", 0));
        try {
            assertEquals(-1, tVector.lastIndexOf("Test 98", 1000));
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            assertEquals(-1, tVector.lastIndexOf("Test 98", Integer.MAX_VALUE));
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            tVector.lastIndexOf("Test 98", tVector.size());
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            tVector.indexOf("Test 98", Integer.MIN_VALUE);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // ok
        }
    }

    @Test
    public void test_removeI() {
        // Test for method java.lang.Object java.util.Vector.remove(int)
        Object removeElement = tVector.get(36);
        Object result = tVector.remove(36);
        assertFalse("Contained element after remove", tVector.contains("Test 36"));
        assertEquals("Should return the element that was removed", removeElement, result);
        assertEquals("Failed to decrement size after remove", 99, tVector.size());
        tVector.add(20, null);
        removeElement = tVector.get(19);
        result = tVector.remove(19);
        assertNull("Didn't move null element over", tVector.get(19));
        assertEquals("Should return the element that was removed", removeElement, result);
        removeElement = tVector.get(19);
        result = tVector.remove(19);
        assertNotNull("Didn't remove null element", tVector.get(19));
        assertEquals("Should return the element that was removed", removeElement, result);
        assertEquals("Failed to decrement size after removing null", 98, tVector.size());

        try {
            tVector.remove(-1);
            fail("Should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // Excepted
        }

        try {
            tVector.remove(tVector.size());
            fail("Should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // Excepted
        }
    }

    @Test
    public void test_removeLjava_lang_Object() {
        // Test for method boolean java.util.Vector.remove(java.lang.Object)
        tVector.remove("Test 0");
        assertTrue("Contained element after remove", !tVector.contains("Test 0"));
        assertEquals("Failed to decrement size after remove", 99, tVector.size());
        tVector.add(null);
        tVector.remove(null);
        assertTrue("Contained null after remove", !tVector.contains(null));
        assertEquals("Failed to decrement size after removing null", 99, tVector.size());
    }

    @Test
    public void test_removeAllLjava_util_Collection() {
        // Test for method boolean
        // java.util.Vector.removeAll(java.util.Collection)
        Vector<Object> v = new Vector<>();
        Collection<String> l = new LinkedList<>();
        for (int i = 0; i < 5; i++) {
            l.add("Test " + i);
        }
        v.addElement(l);

        Collection<Object> s = new HashSet<>();
        Object o = v.firstElement();
        s.add(o);
        v.removeAll(s);
        assertTrue("Failed to remove items in collection", !v.contains(o));
        v.removeAll(l);
        assertTrue("Failed to remove all elements", v.isEmpty());

        v.add(null);
        v.add(null);
        v.add("Boom");
        v.removeAll(s);
        assertEquals("Should not have removed any elements", 3, v.size());
        l = new LinkedList<>();
        l.add(null);
        v.removeAll(l);
        assertEquals("Should only have one element", 1, v.size());
        assertEquals("Element should be 'Boom'", "Boom", v.firstElement());
    }

    @Test
    public void test_removeAllElements() {
        // Test for method void java.util.Vector.removeAllElements()
        Vector<Object> v = vectorClone(tVector);
        v.removeAllElements();
        assertEquals("Failed to remove all elements", 0, v.size());
    }

    @Test
    public void test_removeElementLjava_lang_Object() {
        // Test for method boolean
        // java.util.Vector.removeElement(java.lang.Object)
        Vector<Object> v = vectorClone(tVector);
        v.removeElement("Test 98");
        assertEquals("Element not removed", "Test 99", v.elementAt(98));
        assertTrue("Vector is wrong size after removal: " + v.size(), v.size() == 99);
        tVector.addElement(null);
        v.removeElement(null);
        assertTrue("Vector is wrong size after removing null: " + v.size(), v.size() == 99);
    }

    @Test
    public void test_removeElementAtI() {
        // Test for method void java.util.Vector.removeElementAt(int)
        Vector<Object> v = vectorClone(tVector);
        int size = v.size();
        v.removeElementAt(50);
        assertEquals("Failed to remove element", -1, v.indexOf("Test 50", 0));
        assertEquals("Test 51", v.get(50));
        assertEquals(size - 1, v.size());

        tVector.insertElementAt(null, 60);
        assertNull(tVector.get(60));
        size = tVector.size();
        tVector.removeElementAt(60);
        assertNotNull("Element at 60 should not be null after removal", tVector.elementAt(60));
        assertEquals(size - 1, tVector.size());

        try {
            tVector.removeElementAt(-1);
            fail("Should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // Excepted
        }

        try {
            tVector.removeElementAt(tVector.size());
            fail("Should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // Excepted
        }
    }

    @Test
    public void test_removeRange() {
        MockVector<Object> myVector = new MockVector<>();
        myVector.removeRange(0, 0);

        try {
            myVector.removeRange(0, 1);
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // Excepted
        }

        int[] data = { 1, 2, 3, 4 };
        for (int i = 0; i < data.length; i++) {
            myVector.add(i, data[i]);
        }

        myVector.removeRange(0, 2);
        assertEquals(data[2], myVector.get(0));
        assertEquals(data[3], myVector.get(1));

        try {
            myVector.removeRange(-1, 1);
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // Excepted
        }

        try {
            myVector.removeRange(0, -1);
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // Excepted
        }
    }

    @Test
    public void test_retainAllLjava_util_Collection() {
        // Test for method boolean
        // java.util.Vector.retainAll(java.util.Collection)
        Object o = tVector.firstElement();
        tVector.add(null);
        Collection<Object> s = new HashSet<>();
        s.add(o);
        s.add(null);
        tVector.retainAll(s);
        assertTrue("Retained items other than specified",
                tVector.size() == 2 && tVector.contains(o) && tVector.contains(null));
    }

    @Test
    public void test_setILjava_lang_Object() {
        // Test for method java.lang.Object java.util.Vector.set(int,
        // java.lang.Object)
        Object o = new Object();
        Object previous = tVector.get(23);
        Object result = tVector.set(23, o);
        assertEquals("Should return the element previously at the specified position", previous, result);
        assertTrue("Failed to set Object", tVector.get(23) == o);

        previous = tVector.get(0);
        result = tVector.set(0, null);
        assertEquals("Should return the element previously at the specified position", previous, result);
        assertNull("Failed to set Object", tVector.get(0));

        try {
            tVector.set(-1, o);
            fail("Should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // Excepted
        }

        try {
            tVector.set(-1, null);
            fail("Should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // Excepted
        }

        try {
            tVector.set(tVector.size(), o);
            fail("Should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // Excepted
        }

        try {
            tVector.set(tVector.size(), null);
            fail("Should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // Excepted
        }
    }

    @Test
    public void test_setElementAtLjava_lang_ObjectI() {
        // Test for method void java.util.Vector.setElementAt(java.lang.Object,
        // int)
        Vector<Object> v = vectorClone(tVector);
        v.setElementAt("Inserted Element", 99);
        assertEquals("Element not set", "Inserted Element", v.elementAt(99));

        v.setElementAt(null, 0);
        assertNull("Null element not set", v.elementAt(0));

        try {
            v.setElementAt("Inserted Element", -1);
            fail("Should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // Excepted
        }

        try {
            v.setElementAt(null, -1);
            fail("Should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // Excepted
        }

        try {
            v.setElementAt("Inserted Element", v.size());
            fail("Should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // Excepted
        }

        try {
            v.setElementAt(null, v.size());
            fail("Should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // Excepted
        }
    }

    @Test
    public void test_setSizeI() {
        // Test for method void java.util.Vector.setSize(int)
        Vector<Object> v = vectorClone(tVector);
        int oldSize = v.size();
        Object preElement = v.get(10);
        v.setSize(10);
        assertEquals("Failed to set size", 10, v.size());
        assertEquals("All components at index newSize and greater should be discarded", -1, v.indexOf(preElement));
        try {
            v.get(oldSize - 1);
        } catch (ArrayIndexOutOfBoundsException e) {
            // Excepted;
        }

        oldSize = v.size();
        v.setSize(20);
        assertEquals("Failed to set size", 20, v.size());
        for (int i = oldSize; i < v.size(); i++) {
            assertNull(v.get(i));
        }

        try {
            v.setSize(-1);
            fail("Should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // Excepted
        }
    }

    @Test
    public void test_size() {
        // Test for method int java.util.Vector.size()
        assertEquals("Returned incorrect size", 100, tVector.size());
    }

    @Test
    public void test_subListII() {
        // Test for method java.util.List java.util.Vector.subList(int, int)
        List<Object> sl = tVector.subList(10, 25);
        assertEquals("Returned sublist of incorrect size", 15, sl.size());
        for (int i = 10; i < 25; i++) {
            assertTrue("Returned incorrect sublist", sl.contains(tVector.get(i)));
        }
    }

    @Test
    public void test_toArray() {
        // Test for method java.lang.Object [] java.util.Vector.toArray()
        assertTrue("Returned incorrect array", Arrays.equals(objArray, tVector.toArray()));
    }

    @Test
    public void test_toArray$Ljava_lang_Object() {
        // Test for method java.lang.Object []
        // java.util.Vector.toArray(java.lang.Object [])
        Object[] o = new Object[1000];
        Object f = new Object();
        for (int i = 0; i < o.length; i++) {
            o[i] = f;
        }
        tVector.toArray(o);
        assertNull("Failed to set slot to null", o[100]);
        for (int i = 0; i < tVector.size(); i++) {
            assertTrue("Returned incorrect array", tVector.elementAt(i) == o[i]);
        }
    }

    @Test
    public void test_toString() {
        // Ensure toString works with self-referencing elements.
        Vector<Object> vec = new Vector<>(3);
        vec.add(null);
        vec.add(new Object());
        vec.add(vec);
        assertNotNull(vec.toString());

        // Test for method java.lang.String java.util.Vector.toString()
        assertTrue("Incorrect String returned", tVector.toString().equals(vString));

        Vector<Object> v = new Vector<>();
        v.addElement("one");
        v.addElement(v);
        v.addElement("3");
        // test last element
        v.addElement(v);
        String result = v.toString();
        assertTrue("should contain self ref", result.indexOf("(this") > -1);
    }

    @Test
    public void test_override_size() throws Exception {
        Vector<Object> v = new Vector<>();
        Vector<Object> testv = new MockVector<>();
        // though size is overriden, it should passed without exception
        testv.add(1);
        testv.add(2);
        testv.clear();

        testv.add(1);
        testv.add(2);
        v.add(1);
        v.add(2);
        // RI's bug here
        assertTrue(testv.equals(v));
    }

    @Test
    public void test_trimToSize() {
        // Test for method void java.util.Vector.trimToSize()
        Vector<Object> v = new Vector<>(10);
        v.addElement(new Object());
        v.trimToSize();
        assertEquals("Failed to trim capacity", 1, v.capacity());
    }

    @SuppressWarnings("unchecked")
    protected <T> Vector<T> vectorClone(Vector<T> s) {
        return (Vector<T>) s.clone();
    }

    class SubVector<E> extends Vector<E> {

        private static final long serialVersionUID = 1L;

        public SubVector() {
            super();
        }

        @Override
        public synchronized boolean add(E obj) {
            super.addElement(obj);
            return true;
        }

        @Override
        public synchronized void addElement(E obj) {
            super.add(obj);
        }

        /**
         * @tests java.util.Vector#add(Object)
         */
        @SuppressWarnings("nls")
        public void test_add() {
            SubVector<String> subvector = new SubVector<>();
            subvector.add("foo");
            subvector.addElement("bar");
            assertEquals("Expected two elements in vector", 2, subvector.size());
        }
    }

    public class MockVector<T> extends Vector<T> {
        private static final long serialVersionUID = -8036311869188435980L;

        @Override
        public synchronized int size() {
            return 0;
        }

        @Override
        public void removeRange(int start, int end) {
            super.removeRange(start, end);
        }
    }
}
